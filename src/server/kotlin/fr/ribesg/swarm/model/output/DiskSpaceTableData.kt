package fr.ribesg.swarm.model.output

import java.time.Duration

class DiskSpaceTableData(output: OutputDiskSpaceData) {

    val columns = listOf(
        Column("Device Name", "name"),
        Column("Total Space", "size"),
        Column("Used Space", "used"),
        Column("Time to Full", "timeLeft")
    )

    val data = output.data.map { d ->
        Line(
            d.name,
            formatKiloBytes(d.size),
            "%.2f%% (${formatKiloBytes((d.size * d.used).toLong())})".format(100 * d.used),
            formatTimeLeft(d.timeLeft)
        )
    }

    data class Column(
        val legend: String,
        val selector: String
    )

    data class Line(
        val name: String,
        val size: String,
        val used: String,
        val timeLeft: String
    )

    private fun formatKiloBytes(kb: Long): String {
        return if (kb == 0L) "0" else {
            val units = arrayOf("k", "M", "G", "T", "P", "E", "Z", "Y")
            var unit = 0
            var value = kb

            while (value >= 10000) {
                unit++
                value = Math.round(value / 1000.0)
            }

            "$value ${units[Math.min(units.size - 1, unit)]}o"
        }
    }

    private fun formatTimeLeft(timeLeft: Long?): String {
        if (timeLeft == null) return "Unknown"
        val duration = Duration.ofSeconds(timeLeft)
        return when {
            duration < Duration.ZERO ||
            duration > Duration.ofDays(365)  -> "More than a year"
            duration > Duration.ofDays(60)   -> "${Math.floor(duration.toDays() / 30.0).toInt()} months"
            duration > Duration.ofDays(14)   -> "${Math.floor(duration.toDays() / 7.0).toInt()} weeks"
            duration > Duration.ofDays(2)    -> "${Math.floor(duration.toDays().toDouble()).toInt()} days"
            duration > Duration.ofHours(2)   -> "${Math.floor(duration.toHours().toDouble()).toInt()} hours"
            duration > Duration.ofMinutes(2) -> "${Math.floor(duration.toMinutes().toDouble()).toInt()} minutes"
            else                             -> "$timeLeft seconds"
        }
    }

}
