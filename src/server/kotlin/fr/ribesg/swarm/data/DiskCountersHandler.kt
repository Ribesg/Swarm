package fr.ribesg.swarm.data

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.model.input.InputDisk
import fr.ribesg.swarm.model.input.InputDisk.InputDiskDevice
import fr.ribesg.swarm.model.input.dragonfly.*
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles conversion of Disk counters into [InputDisk].
 */
object DiskCountersHandler {

    /**
     * Size of a disk sector in kilobytes
     */
    private const val SECTOR_SIZE = 512

    /**
     * Max allowed age of previous Disk counters value to be used to compute local Disk usage
     */
    private const val MAX_PREVIOUS_COUNTER_AGE = 60L

    /**
     * Mounts that are ignored
     */
    private val IGNORED_MOUNTS = arrayOf("/boot")

    /**
     * The logger
     */
    private val log = Log.get(DiskCountersHandler::class)

    /**
     * Contains all previous Disk counters and data for each host and the date at which they were emitted
     */
    private val previousCountersValues: MutableMap<String, Triple<Long, List<DragonflyDiskDf>, List<DragonflyDiskStats>>> = ConcurrentHashMap()

    /**
     * Registers the provided counters and computes local Disk usage values if possible.
     *
     * @param host the hostname
     * @param date the date in milliseconds from epoch
     * @param df the df result
     * @param diskstats the diskstats result
     *
     * @return local Disk usage values
     */
    fun handle(host: String, date: Long, df: List<DragonflyDiskDf>, diskstats: List<DragonflyDiskStats>): InputDisk {
        val previousPair = previousCountersValues[host]
        previousCountersValues[host] = Triple(date, df, diskstats)

        if (previousPair == null) {
            log.debug("First Disk data received, nothing to return")
            return InputDisk(emptyList())
        }

        val (previousDate, previousDf, previousDiskstats) = previousPair
        if (Duration.ofMillis(date - previousDate) > Duration.ofSeconds(MAX_PREVIOUS_COUNTER_AGE)) {
            log.debug("Previous Disk data is too old, nothing to return")
            return InputDisk(emptyList())
        }

        return computeDiskInput(date - previousDate, previousDf, previousDiskstats, df, diskstats)
    }

    /**
     * Computes an [InputDisk] from two sets of disk data.
     *
     * @param interval the number of milliseconds between the older and the newer datasets
     * @param fromDf df result of the older dataset
     * @param fromDiskStats diskstats result of the older dataset
     * @param toDf df result of the newer dataset
     * @param toDiskStats diskstats result of the newer dataset
     *
     * @return Disk input data, may be empty
     */
    private fun computeDiskInput(
        interval: Long,
        fromDf: List<DragonflyDiskDf>,
        fromDiskStats: List<DragonflyDiskStats>,
        toDf: List<DragonflyDiskDf>,
        toDiskStats: List<DragonflyDiskStats>
    ): InputDisk {
        val intervalSeconds = interval / 1000.0
        val fromFilesystems =
            fromDf.map(DragonflyDiskDf::filesystem) intersect
                fromDiskStats.map(DragonflyDiskStats::deviceName)
        val toFilesystems =
            toDf.map(DragonflyDiskDf::filesystem) intersect
                toDiskStats.map(DragonflyDiskStats::deviceName)
        val filesystems = (fromFilesystems intersect toFilesystems)
        val dfMap = toDf.filter { it.filesystem in filesystems }.map { it.filesystem to it }.toMap()
        val fromDiskStatsMap = fromDiskStats.filter { it.deviceName in filesystems }.map { it.deviceName to it }.toMap()
        val toDiskStatsMap = toDiskStats.filter { it.deviceName in filesystems }.map { it.deviceName to it }.toMap()
        return InputDisk(
            filesystems
                .map { filesystem ->
                    filesystem to dfMap[filesystem]!!
                }
                .filterNot { (_, df) ->
                    df.mountedOn in IGNORED_MOUNTS
                }
                .map { (filesystem, df) ->
                    val fromDeviceDiskStats = fromDiskStatsMap[filesystem]!!
                    val toDeviceDiskStats = toDiskStatsMap[filesystem]!!
                    val totalUsage =
                        (toDeviceDiskStats.msSpentOnIos - fromDeviceDiskStats.msSpentOnIos).nullIfNegativeOr {
                            it / interval.toDouble()
                        }
                    val sectorsRead =
                        (toDeviceDiskStats.sectorReads - fromDeviceDiskStats.sectorReads).nullIfNegative()
                    val sectorsWritten =
                        (toDeviceDiskStats.sectorWrites - fromDeviceDiskStats.sectorWrites).nullIfNegative()
                    var readUsage: Int? = null
                    var writeUsage: Int? = null
                    if (totalUsage != null && sectorsRead != null && sectorsWritten != null) {
                        val sectorsTouched = sectorsRead + sectorsWritten
                        readUsage = (10000 * totalUsage * sectorsRead / sectorsTouched).toInt()
                        writeUsage = (10000 * totalUsage * sectorsWritten / sectorsTouched).toInt()
                    }
                    val readBytes = sectorsRead?.let { (it * SECTOR_SIZE / intervalSeconds).toLong() }
                    val writtenBytes = sectorsWritten?.let { (it * SECTOR_SIZE / intervalSeconds).toLong() }
                    InputDiskDevice(
                        filesystem,
                        df.used,
                        df.oneThousandBlocks,
                        readUsage,
                        writeUsage,
                        readBytes,
                        writtenBytes
                    )
                }
        )
    }

    private fun <T : Number> T.nullIfNegative(): T? =
        if (this.toDouble() < 0) null else this

    private fun <T : Number, R> T.nullIfNegativeOr(transformer: (T) -> R) =
        if (this.toDouble() < 0) null else transformer(this)

}
