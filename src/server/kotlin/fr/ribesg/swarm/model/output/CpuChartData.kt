package fr.ribesg.swarm.model.output

class CpuChartData(output: OutputCpuData) : ShapeChartData(
    data = listOf(
        ChartShape(
            stackId = "cpu",
            color = "#FFBA08",
            legend = "Busy",
            shape = Shape.AREA,
            points = output.data.map { Point(it.date, it.busy) }
        ),
        ChartShape(
            stackId = "cpu",
            color = "#157F1F",
            legend = "IO Wait",
            shape = Shape.AREA,
            points = output.data.map { Point(it.date, it.io) }
        ),
        ChartShape(
            stackId = "cpu",
            color = "#3E92CC",
            legend = "Steal",
            shape = Shape.AREA,
            points = output.data.map { Point(it.date, it.steal) }
        )
    )
) {

    object EMPTY {
        val data: List<*>? = null
    }

}
