package fr.ribesg.swarm.model.output

import fr.ribesg.swarm.extensions.*
import fr.ribesg.swarm.model.output.OutputDiskIoData.OutputDiskIoDataPointValues

class DiskIoChartData(output: OutputDiskIoData) : ShapeChartData(
    data = listOf(
        ChartShape(
            stackId = "diskUsage",
            color = "#FFBA08",
            legend = "Read Usage",
            shape = Shape.AREA,
            side = Side.LEFT,
            points = output.data.map {
                val readUsages = it.values.values.mapNotNull(OutputDiskIoDataPointValues::readUsage)
                Point(it.date, readUsages.averageOrNull())
            }
        ),
        ChartShape(
            stackId = "diskUsage",
            color = "#157F1F",
            legend = "Write Usage",
            shape = Shape.AREA,
            side = Side.LEFT,
            points = output.data.map {
                val writeUsages = it.values.values.mapNotNull(OutputDiskIoDataPointValues::writeUsage)
                Point(it.date, writeUsages.averageOrNull())
            }
        ),
        ChartShape(
            color = "#3E92CC",
            legend = "Read Speed",
            points = output.data.map {
                val readSpeeds = it.values.values.mapNotNull(OutputDiskIoDataPointValues::readSpeed)
                Point(it.date, readSpeeds.sumOrNull())
            },
            shape = Shape.LINE,
            side = Side.RIGHT
        ),
        ChartShape(
            color = "#FE4A49",
            legend = "Write Speed",
            points = output.data.map {
                val writeSpeeds = it.values.values.mapNotNull(OutputDiskIoDataPointValues::writeSpeed)
                Point(it.date, writeSpeeds.sumOrNull())
            },
            shape = Shape.LINE,
            side = Side.RIGHT
        )
    )
) {

    object EMPTY {
        val data: List<*>? = null
        const val maxUsage: Int = 0
    }

    val maxUsage = computeMaxUsage()

    private fun computeMaxUsage(): Int {
        val dataMax =
            (data!![0].points + data[1].points)
                .groupBy { it.x }
                .values
                .map { it.mapNotNull { it.y }.sumBy(Number::toInt) }
                .max() ?: 0
        return Math.max(100, dataMax)
    }

}
