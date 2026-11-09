package io.github.yusufb7841.ratiolab.providers.sec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against a small local HTTP server (built into the JDK), never against SEC. */
class SecClientTest {

	private static final String USER_AGENT = "RatioLab Test test@example.com";

	private static final String BODY = "{\"cik\": 0}";

	private HttpServer server;

	/** What the local server answers. */
	private volatile int status = 200;

	private volatile boolean compressed = true;

	/** What the local server received. */
	private volatile String receivedPath;

	private volatile Headers receivedHeaders;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			receivedPath = exchange.getRequestURI().getPath();
			receivedHeaders = exchange.getRequestHeaders();
			byte[] body = compressed ? gzip(BODY) : BODY.getBytes(UTF_8);
			if (compressed) {
				exchange.getResponseHeaders().set("Content-Encoding", "gzip");
			}
			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void fetchCompanyFacts_sendsTheUserAgentAndAsksForGzip() {
		SecClient client = client(USER_AGENT, server.getAddress().getPort());
		client.fetchCompanyFacts("0000000000");

		assertThat(receivedPath).isEqualTo("/api/xbrl/companyfacts/CIK0000000000.json");
		assertThat(client.companyFactsUrl("0000000000")).endsWith(receivedPath);
		assertThat(receivedHeaders.getFirst("User-Agent")).isEqualTo(USER_AGENT);
		assertThat(receivedHeaders.getFirst("Accept-Encoding")).isEqualTo("gzip");
	}

	@Test
	void fetchCompanyFacts_compressedResponse_isDecompressed() {
		SecClient.FetchResult result = client(USER_AGENT, server.getAddress().getPort()).fetchCompanyFacts("0000000000");

		assertThat(result).isEqualTo(new SecClient.FetchResult(200, BODY));
	}

	@Test
	void fetchCompanyFacts_uncompressedResponse_isReturnedUnchanged() {
		compressed = false;

		assertThat(client(USER_AGENT, server.getAddress().getPort()).fetchCompanyFacts("0000000000").body())
			.isEqualTo(BODY);
	}

	@Test
	void fetchCompanyFacts_notFound_isReportedWithTheHttpStatus() {
		status = 404;

		assertProviderError(client(USER_AGENT, server.getAddress().getPort()), "HTTP_404");
	}

	@Test
	void fetchCompanyFacts_serverUnreachable_isReportedAsUnavailable() throws IOException {
		int unusedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			unusedPort = socket.getLocalPort();
		}

		assertProviderError(client(USER_AGENT, unusedPort), "UNAVAILABLE");
	}

	@Test
	void fetchCompanyFacts_withoutUserAgent_sendsNoRequest() {
		SecClient client = client("", server.getAddress().getPort());

		assertThatThrownBy(() -> client.fetchCompanyFacts("0000000000")).isInstanceOf(IllegalStateException.class);
		assertThat(receivedPath).isNull();
	}

	private static SecClient client(String userAgent, int port) {
		RatioLabProperties.Sec sec = new RatioLabProperties.Sec(userAgent, URI.create("http://127.0.0.1:" + port),
				Duration.ofSeconds(1), Duration.ofSeconds(2), Map.of());
		return new SecClient(new RatioLabProperties(Path.of("unused"), List.of(), null, sec));
	}

	private static void assertProviderError(SecClient client, String expectedCode) {
		assertThatThrownBy(() -> client.fetchCompanyFacts("0000000000")).isInstanceOf(ProviderResponseException.class)
			.extracting(e -> ((ProviderResponseException) e).code())
			.isEqualTo(expectedCode);
	}

	private static byte[] gzip(String text) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
			out.write(text.getBytes(UTF_8));
		}
		return bytes.toByteArray();
	}
}
