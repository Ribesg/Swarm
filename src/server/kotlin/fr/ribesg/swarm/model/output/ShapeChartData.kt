package fr.ribesg.swarm.model.output

import com.fasterxml.jackson.annotation.JsonValue

open class ShapeChartData(
    val data: List<ChartShape<*, *>>? = null
) {

    init {
        require(data?.isNotEmpty() != false)
    }

    enum class Curve(
        @JsonValue
        val value: String
    ) {
        LINEAR("Linear"),
        MONOTONE_X("MonotoneX"),
        CATMULL_ROM("CatmullRom"),
        ;
    }

    data class Point<out X : Number, out Y : Number?>(
        val x: X,
        val y: Y
    )

    enum class Shape {
        AREA,
        LINE,
        SYMBOL,
        ;

        @JsonValue
        @Suppress("UNUSED")
        val jsonValue = name.toLowerCase()
    }

    enum class Side {
        LEFT,
        RIGHT,
        ;

        @JsonValue
        @Suppress("UNUSED")
        val jsonValue = name.toLowerCase()
    }

    data class ChartShape<out X : Number, out Y : Number?>(
        val color: String? = null,
        val curve: Curve? = null,
        val legend: String? = null,
        val side: Side = Side.LEFT,
        val shape: Shape = Shape.LINE,
        val stackId: String? = null,
        val points: List<Point<X, Y>>
    ) {
        init {
            require(points.isNotEmpty())
        }
    }

}
