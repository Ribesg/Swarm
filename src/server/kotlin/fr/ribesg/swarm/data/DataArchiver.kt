package fr.ribesg.swarm.data

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.alerts.AlertManager
import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.database.tables.*
import fr.ribesg.swarm.model.database.*
import fr.ribesg.swarm.model.database.DbDataType.*
import java.time.*
import java.util.*
import java.util.concurrent.Executors

/**
 * Handles the periodic conversion of live data into archived data and the cleanup of old data.
 */
class DataArchiver(private val database: Database) {

    companion object {

        private val ARCHIVE_LIVE_TO_HOUR_PERIOD = DbDataType.HOUR.interval

        private val ARCHIVE_HOUR_TO_DAY_PERIOD = DbDataType.DAY.interval

        private val ARCHIVE_DAY_TO_WEEK_PERIOD = DbDataType.WEEK.interval

    }

    /**
     * The logger
     */
    private val log = Log.get(DataArchiver::class)

    /**
     * A clock ticking every time we should archive LIVE data into HOUR data, i.e. every minute
     */
    private val liveClock: Clock

    /**
     * A clock ticking every time we should archive HOUR data into DAY data, i.e. every 20 minutes
     */
    private val hourClock: Clock

    /**
     * A clock ticking every time we should archive DAY data into WEEK data, i.e. every 2 hours
     */
    private val dayClock: Clock

    /**
     * The executor
     */
    private val executor = Executors.newFixedThreadPool(2)

    /**
     * The last time we archived LIVE data into HOUR data
     */
    private var lastRunLive: Instant

    /**
     * The last time we archived HOUR data into DAY data
     */
    private var lastRunHour: Instant

    /**
     * The last time we archived DAY data into WEEK data
     */
    private var lastRunDay: Instant

    init {
        val clock = Clock.systemUTC()
        liveClock = Clock.tick(clock, ARCHIVE_LIVE_TO_HOUR_PERIOD)
        hourClock = Clock.tick(clock, ARCHIVE_HOUR_TO_DAY_PERIOD)
        dayClock = Clock.tick(clock, ARCHIVE_DAY_TO_WEEK_PERIOD)
        lastRunLive = liveClock.instant().minus(ARCHIVE_LIVE_TO_HOUR_PERIOD).plusSeconds(30)
        lastRunHour = hourClock.instant().minus(ARCHIVE_HOUR_TO_DAY_PERIOD).plusSeconds(30)
        lastRunDay = dayClock.instant().minus(ARCHIVE_DAY_TO_WEEK_PERIOD).plusSeconds(30)
        executor.submit(this::loop)
    }

    /**
     * Runs the data archiver loop.
     */
    private fun loop() {
        while (true) try {
            if (lastRunLive != liveClock.instant()) {
                executor.submit(this::run)
            }
            Thread.sleep(1000)
        } catch (t: Throwable) {
            log.error("Fatal error in data archiver loop", t)
            System.exit(1)
        }
    }

    /**
     * Runs the data archiver.
     */
    private fun run() = Database.exec {
        log.debug("Archiving live data")
        runLiveArchiver()
        if (lastRunHour != hourClock.instant()) {
            runHourArchiver()
            if (lastRunDay != dayClock.instant()) {
                runDayArchiver()
            }
        }
        AlertManager.run(lastRunLive)
    }

    private fun runLiveArchiver() {
        lastRunLive = liveClock.instant()
        val currentMinuteMs = lastRunLive.toEpochMilli()
        val previousMinuteMs = currentMinuteMs - Duration.ofMinutes(1).toMillis()
        val fiveMinutesAgoMs = currentMinuteMs - Duration.ofMinutes(5).toMillis()
        val (archivedRows, archivesCreated) = archiveData(LIVE, HOUR, previousMinuteMs, currentMinuteMs)
        database.deleteData(fiveMinutesAgoMs, LIVE)
        log.info("Archived $archivedRows LIVE rows into $archivesCreated HOUR rows")
    }

    private fun runHourArchiver() {
        lastRunHour = hourClock.instant()
        val currentTwentyMinutesMs = lastRunHour.toEpochMilli()
        val previousTwentyMinutesMs = currentTwentyMinutesMs - Duration.ofMinutes(20).toMillis()
        val (archivedRows, archivesCreated) = archiveData(HOUR, DAY, previousTwentyMinutesMs, currentTwentyMinutesMs)
        log.info("Archived $archivedRows HOUR rows into $archivesCreated DAY rows")
    }

    private fun runDayArchiver() {
        lastRunDay = dayClock.instant()
        val currentTwoHoursMs = lastRunDay.toEpochMilli()
        val previousTwoHoursMs = currentTwoHoursMs - Duration.ofHours(2).toMillis()
        val oneMonthAgo = currentTwoHoursMs - Duration.ofDays(31).toMillis()
        val (archivedRows, archivesCreated) = archiveData(DAY, WEEK, previousTwoHoursMs, currentTwoHoursMs)
        database.deleteData(oneMonthAgo, HOUR, DAY, WEEK)
        log.info("Archived $archivedRows DAY rows into $archivesCreated WEEK rows")
    }

    /**
     * Retrieves and converts the data of type [ofType] in the provided interval into a datum of type [asType].
     *
     * @param ofType the type of the data to archive
     * @param asType the type of the resulting archive datum
     * @param from the minimum date in milliseconds, inclusive
     * @param to the maximum date in milliseconds, exclusive
     *
     * @return the number of live rows archived and the number of archive rows added
     */
    private fun archiveData(ofType: DbDataType, asType: DbDataType, from: Long, to: Long): Pair<Int, Int> {
        val allData = DataTable.getData(ofType, from, to)
        val allDataRefs = allData.map(DbDatum::id)
        val allNetData = NetDataTable.getData(allDataRefs)
        val allDiskData = DiskDataTable.getData(allDataRefs)

        val newData = LinkedList<DbDatum>()
        val groupedNetData = LinkedList<List<List<DbNetDatum>>>()
        val groupedDiskData = LinkedList<List<List<DbDiskDatum>>>()

        val hosts = allData.map(DbDatum::host).distinct()
        hosts.forEach { host ->
            val data = allData.filter { it.host == host }
            val dataRefs = data.map(DbDatum::id)
            newData.add(mergeData(asType, from, data))
            groupedNetData.add(
                allNetData
                    .filterKeys { it in dataRefs }
                    .flatMap { it.value }
                    .groupBy { it.interfaceName }
                    .map { it.value }
            )
            groupedDiskData.add(
                allDiskData
                    .filterKeys { it in dataRefs }
                    .flatMap { it.value }
                    .groupBy { it.device }
                    .map { it.value }
            )
        }

        val newDataRefs = DataTable.insert(newData)

        val newNetData = LinkedList<DbNetDatum>()
        val newDiskData = LinkedList<DbDiskDatum>()
        newDataRefs.forEachIndexed { i, dataRef ->
            groupedNetData[i].forEach { newNetData.add(mergeNetData(dataRef, it)) }
            groupedDiskData[i].forEach { newDiskData.add(mergeDiskData(dataRef, it)) }
        }

        NetDataTable.insert(newNetData)
        DiskDataTable.insert(newDiskData)

        val rowsCount = allData.size + allNetData.size + allDiskData.size
        val archiveRowsCount = newData.size + newNetData.size + newDiskData.size
        return rowsCount to archiveRowsCount
    }

    /**
     * Merges a list of [DbDatum] into a single [DbDatum].
     *
     * @param asType the type of the resulting datum
     * @param date the date of the resulting datum
     * @param data the data to merge
     *
     * @return the provided data merged into a single datum
     */
    private fun mergeData(asType: DbDataType, date: Long, data: List<DbDatum>) = DbDatum(
        type = asType,
        host = data.last().host,
        date = date,
        cpuBusy = data.mapNotNull(DbDatum::cpuBusy).averageOrNull(),
        cpuIo = data.mapNotNull(DbDatum::cpuIo).averageOrNull(),
        cpuSteal = data.mapNotNull(DbDatum::cpuSteal).averageOrNull(),
        ramUsed = data.map(DbDatum::ramUsed).average().toLong(),
        swapUsed = data.map(DbDatum::swapUsed).average().toLong()
    )

    /**
     * Merges a list of [DbNetDatum] into a single [DbNetDatum].
     *
     * @param dataRef the data ref of the final datum
     * @param data the data to merge
     *
     * @return the provided data merged into a single datum
     */
    private fun mergeNetData(dataRef: Int, data: List<DbNetDatum>) = DbNetDatum(
        dataRef = dataRef,
        interfaceName = data.last().interfaceName,
        inBytes = data.mapNotNull(DbNetDatum::inBytes).averageOrNull(),
        outBytes = data.mapNotNull(DbNetDatum::outBytes).averageOrNull(),
        inPackets = data.mapNotNull(DbNetDatum::inPackets).averageOrNull(),
        outPackets = data.mapNotNull(DbNetDatum::outPackets).averageOrNull(),
        inErrors = data.mapNotNull(DbNetDatum::inErrors).averageOrNull(),
        outErrors = data.mapNotNull(DbNetDatum::outErrors).averageOrNull()
    )

    /**
     * Merges a list of [DbDiskDatum] into a single [DbDiskDatum].
     *
     * @param dataRef the data ref of the final datum
     * @param data the data to merge
     *
     * @return the provided data merged into a single datum
     */
    private fun mergeDiskData(dataRef: Int, data: List<DbDiskDatum>) = DbDiskDatum(
        dataRef = dataRef,
        device = data.last().device,
        readUsage = data.mapNotNull(DbDiskDatum::readUsage).averageOrNull(),
        writeUsage = data.mapNotNull(DbDiskDatum::writeUsage).averageOrNull(),
        readSpeed = data.mapNotNull(DbDiskDatum::readSpeed).averageOrNull(),
        writeSpeed = data.mapNotNull(DbDiskDatum::writeSpeed).averageOrNull(),
        usedSpace = data.last().usedSpace,
        totalSpace = data.last().totalSpace
    )

    /**
     * Averages a collection of Ints, returns null if the collection is empty.
     */
    private fun Iterable<Int>.averageOrNull(): Int? =
        average().let { if (it == Double.NaN) null else it.toInt() }

    /**
     * Averages a collection of Longs, returns null if the collection is empty.
     */
    private fun Iterable<Long>.averageOrNull(): Long? =
        average().let { if (it == Double.NaN) null else it.toLong() }

}
