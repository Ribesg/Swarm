package fr.ribesg.swarm.model.output

class RamChartData(output: OutputRamData) : ShapeChartData(
    data = mutableListOf(
        ChartShape(
            color = "#FFBA08",
            legend = "RAM Used",
            points = output.data.map { Point(it.date, it.ram) },
            shape = Shape.LINE,
            side = Side.LEFT
        )
    ).apply {
        if (output.maxSwap > 0) add(
            ChartShape(
                color = "#157F1F",
                legend = "SWAP Used",
                points = output.data.map { Point(it.date, it.swap) },
                shape = Shape.LINE,
                side = Side.RIGHT
            )
        )
    }
) {

    object EMPTY {
        val data: List<*>? = null
        const val maxRam: Long = 0
        const val maxSwap: Long = 0
    }

    val maxRam = output.maxRam

    val maxSwap = output.maxSwap

}
