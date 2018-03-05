package fr.ribesg.swarm.model.output

import fr.ribesg.swarm.extensions.letIfAnyOrNull
import fr.ribesg.swarm.model.output.OutputNetData.OutputNetDataPointValues

class NetChartData(output: OutputNetData) : ShapeChartData(
    data = mutableListOf(
        ChartShape(
            color = "#FFBA08",
            legend = "Received",
            points = output.data.map {
                val inBytes = it.values.values.mapNotNull(OutputNetDataPointValues::inBytes)
                Point(it.date, inBytes.letIfAnyOrNull(Iterable<Long>::sum))
            },
            shape = Shape.LINE,
            side = Side.LEFT
        ),
        ChartShape(
            color = "#157F1F",
            legend = "Transmitted",
            points = output.data.map {
                val outBytes = it.values.values.mapNotNull(OutputNetDataPointValues::outBytes)
                Point(it.date, outBytes.letIfAnyOrNull(Iterable<Long>::sum))
            },
            shape = Shape.LINE,
            side = Side.LEFT
        )
    ).apply {
        val hasErrors = output.data.any {
            it.values.values.any {
                it.inErrors != null && it.inErrors > 0 || it.outErrors != null && it.outErrors > 0
            }
        }
        if (hasErrors) add(
            ChartShape(
                color = "#3E92CC",
                legend = "Errors",
                points = output.data.map {
                    val errors = it.values.values.flatMap { listOf(it.inErrors, it.outErrors) }.filterNotNull()
                    Point(it.date, errors.letIfAnyOrNull(Iterable<Long>::sum))
                },
                shape = Shape.LINE,
                side = Side.RIGHT
            )
        )
    }
) {

    object EMPTY {
        val data: List<*>? = null
    }

}
