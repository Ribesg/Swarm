package fr.ribesg.swarm.model.database

import fr.ribesg.swarm.model.DataMode
import java.time.Duration

enum class DbDataType(

    /**
     * Interval between two points of this data type
     */
    val interval: Duration

) {

    LIVE(Duration.ofSeconds(5)),

    HOUR(Duration.ofMinutes(1)),

    DAY(Duration.ofMinutes(20)),

    WEEK(Duration.ofHours(2)),

    ;

    companion object {

        fun fromMode(mode: DataMode) = when (mode) {
            DataMode.LIVE -> LIVE
            DataMode.HOUR -> HOUR
            DataMode.DAY  -> DAY
            DataMode.WEEK -> WEEK
        }

    }

}
