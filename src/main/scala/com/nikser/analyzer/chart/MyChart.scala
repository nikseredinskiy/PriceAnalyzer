package com.nikser.analyzer.chart

import org.jfree.chart.axis.{NumberAxis, _}
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import scalax.chart.XYChart
import scalax.chart.module.XYChartFactories

object MyChart extends MyChart

trait MyChart extends XYChartFactories {
  private def XYDomainAxis(): NumberAxis = {
    val axis = new NumberAxis()
    axis.setAutoRangeIncludesZero(false)
    axis
  }

  object MyXYLineChart {
    def apply[A: ToXYDataset](data: A)(implicit theme: ChartTheme = ChartTheme.Default): XYChart = {
      val dataset = ToXYDataset[A].convert(data)

      val domainAxis = new DateAxis()
      val rangeAxis = XYDomainAxis()

      val renderer = new XYLineAndShapeRenderer(true, false)
      val plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer)

      XYChart(plot, title = "", legend = true)
    }
  }
}