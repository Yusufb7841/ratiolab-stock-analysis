import { useEffect, useRef } from 'react'
import { LineChart } from 'echarts/charts'
import { AriaComponent, GridComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { PricePoint } from '../types/prices.ts'

// Register only the ECharts parts that are used, to keep the bundle small.
echarts.use([LineChart, GridComponent, TooltipComponent, AriaComponent, CanvasRenderer])

type Props = {
  ticker: string
  points: PricePoint[]
}

/** Line chart of daily closing prices. Values are converted to numbers for display only. */
export function PriceChart({ ticker, points }: Props) {
  const container = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!container.current) {
      return
    }
    const chart = echarts.init(container.current)
    chart.setOption({
      aria: { enabled: true },
      tooltip: { trigger: 'axis' },
      grid: { left: 64, right: 16, top: 32, bottom: 32 },
      xAxis: { type: 'category', data: points.map((p) => p.date) },
      yAxis: { type: 'value', scale: true, name: 'USD' },
      series: [
        {
          name: `${ticker} Schlusskurs`,
          type: 'line',
          showSymbol: false,
          data: points.map((p) => Number(p.close)),
        },
      ],
    })
    const resize = () => chart.resize()
    window.addEventListener('resize', resize)
    return () => {
      window.removeEventListener('resize', resize)
      chart.dispose()
    }
  }, [ticker, points])

  return <div ref={container} className="price-chart" aria-label={`Schlusskurse ${ticker}`} />
}
