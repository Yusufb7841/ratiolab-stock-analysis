package io.github.yusufb7841.ratiolab.providers.sec;

import java.util.List;

/** Validated content of a companyfacts response, limited to the requested concepts. */
public record SecCompanyFacts(long cik, String entityName, List<SecFact> facts) {
}
