package fr.ribesg.swarm.database

import fr.ribesg.swarm.*
import fr.ribesg.swarm.database.tables.*
import fr.ribesg.swarm.model.database.*
import fr.ribesg.swarm.model.input.InputDatum
import fr.ribesg.swarm.model.output.*
import fr.ribesg.swarm.model.output.OutputDiskIoData.*
import fr.ribesg.swarm.model.output.OutputDiskSpaceData.OutputDiskSpaceDatum
import fr.ribesg.swarm.model.output.OutputNetData.*
import fr.ribesg.swarm.model.output.OutputRamData.OutputRamDataPoint
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Exposes the Database package to other parts of Swarm.
 */
class Database(arguments: Arguments) {

    companion object {

        /**
         * The path to the DB file: swarm.db in the working directory.
         */
        private const val DB_PATH_FILE = "jdbc:h2:file:./swarm;trace_level_file=0"

        /**
         * The path to the memory DB.
         */
        private const val DB_PATH_MEM = "jdbc:h2:mem:swarm;DB_CLOSE_DELAY=-1"

        /**
         * The class of the DB Driver.
         */
        private const val DB_DRIVER = "org.h2.Driver"

        /**
         * The logger
         */
        private val log = Log.get(Database::class)

        /**
         * Checks that the current code is running inside a [Transaction].
         */
        fun checkInTransaction() {
            checkNotNull(TransactionManager.currentOrNull()) {
                "Should be called inside a transaction"
            }
        }

        /**
         * Executes the provided task inside a transaction if needed.
         *
         * @param task The task to execute
         */
        fun exec(task: Transaction.() -> Unit): Unit = call(task)

        /**
         * Executes the provided callable inside a transaction if needed.
         *
         * @param callable The callable to execute
         *
         * @return The result of the callable
         */
        fun <T> call(callable: Transaction.() -> T): T = transaction {
            addLogger(Slf4jSqlDebugLogger)
            callable()
        }

    }

    init {
        val path = if (arguments.development) DB_PATH_MEM else DB_PATH_FILE
        Database.connect(path, DB_DRIVER)
        DatabaseVersionManager.run()
    }

    /**
     * Pushes the provided datum into the database.
     *
     * @param datum the datum to push
     */
    fun push(datum: InputDatum) = exec {
        HostsTable.upsert(inputToDbHost(datum))
        val dataRef = DataTable.insert(inputToDbDatum(datum))
        NetDataTable.insert(inputToDbNetData(dataRef, datum))
        DiskDataTable.insert(inputToDbDiskData(dataRef, datum))
    }

    /**
     * Pushes the provided data into the database.
     *
     * @param data the data points to push
     */
    fun push(data: Iterable<InputDatum>) = exec {
        val dataRefs = DataTable.insert(data.map(this@Database::inputToDbDatum))
        val zipped = dataRefs.zip(data)
        val netData = LinkedList<DbNetDatum>()
        val diskData = LinkedList<DbDiskDatum>()
        zipped.forEach { (dataRef, datum) ->
            netData.addAll(inputToDbNetData(dataRef, datum))
            diskData.addAll(inputToDbDiskData(dataRef, datum))
        }
        NetDataTable.insert(netData)
        DiskDataTable.insert(diskData)
    }

    /**
     * Gets a list of all [HostsTable.HOST] values in [HostsTable].
     *
     * @return a list of all [HostsTable.HOST] values in [HostsTable]
     */
    fun getHostnames(): List<String> = call {
        HostsTable.getHostNames()
    }

    /**
     * Gets a list of all hosts in [HostsTable].
     *
     * @return a list of all hosts in [HostsTable]
     */
    fun getHosts(): List<DbHost> = call {
        HostsTable.getHosts()
    }

    /**
     * Removes an host and all its associated data in all tables.
     */
    fun removeHost(host: String) = exec {
        HostsTable.deleteWhere { HostsTable.HOST eq host }
    }

    /**
     * Deletes all data with the provided type(s) and older than the provided date.
     *
     * @param before the maximum date of the data to remove in milliseconds, exclusive
     * @param types one or more types of data to delete
     *
     * @return the amount of rows deleted
     */
    fun deleteData(before: Long, vararg types: DbDataType) = call {
        DataTable.deleteData(before, *types)
    }

    /**
     * Gets alert data in the provided period.
     *
     * @param from the minimum date, inclusive
     * @param to the maximum date, exclusive
     *
     * @return the alert data in the provided period
     */
    fun getAlertData(from: Long, to: Long) = call {
        val data = DataTable.getData(DbDataType.HOUR, from, to)
        val diskData = DiskDataTable.getData(data.map(DbDatum::id))
        val mostRecentLiveData = DataTable
            .getData(DbDataType.LIVE, from, Instant.now().toEpochMilli())
            .groupBy { it.host }
            .mapValues { it.value.last() }
        return@call Triple(data, diskData, mostRecentLiveData)
    }

    /**
     * Gets CPU data with the provided filters.
     *
     * @param host the host
     * @param type the type of data
     * @param from the minimum date, inclusive
     * @param to the maximum date, exclusive
     *
     * @return the CPU data matching the provided filters
     */
    fun getCpuData(host: String, type: DbDataType, from: Long, to: Long) = call {
        val dataRows = DataTable.getData(type, from, to, host)
        return@call OutputCpuData(
            dataRows.map { datum ->
                OutputCpuData.CpuChartDataPoint(
                    datum.date,
                    datum.cpuBusy?.let { it / 100f },
                    datum.cpuIo?.let { it / 100f },
                    datum.cpuSteal?.let { it / 100f }
                )
            }
        )
    }

    /**
     * Gets RAM data with the provided filters.
     *
     * @param host the host
     * @param type the type of data
     * @param from the minimum date, inclusive
     * @param to the maximum date, exclusive
     *
     * @return the RAM data matching the provided filters
     */
    fun getRamData(host: String, type: DbDataType, from: Long, to: Long) = call {
        val hostDatum = HostsTable.getHost(host)!!
        val data = DataTable.getData(type, from, to, host)
        return@call OutputRamData(
            data.map { datum ->
                OutputRamDataPoint(datum.date, datum.ramUsed, datum.swapUsed)
            },
            hostDatum.ramTotal,
            hostDatum.swapTotal
        )
    }

    /**
     * Gets Network data with the provided filters.
     *
     * @param host the host
     * @param type the type of data
     * @param from the minimum date, inclusive
     * @param to the maximum date, exclusive
     *
     * @return the Network data matching the provided filters
     */
    fun getNetData(host: String, type: DbDataType, from: Long, to: Long) = call {
        val data = DataTable.getData(type, from, to, host)
        val netData = NetDataTable.getData(data.map(DbDatum::id))
        return@call OutputNetData(
            data.map { datum ->
                val values = netData[datum.id]
                    ?.groupBy { it.interfaceName }
                    ?.map {
                        it.key to OutputNetDataPointValues(
                            it.value.single().inBytes,
                            it.value.single().outBytes,
                            it.value.single().inErrors,
                            it.value.single().outErrors
                        )
                    }
                    ?.toMap()
                OutputNetDataPoint(datum.date, values ?: emptyMap())
            }
        )
    }

    /**
     * Gets Disk IO data with the provided filters.
     *
     * @param host the host
     * @param type the type of data
     * @param from the minimum date, inclusive
     * @param to the maximum date, exclusive
     *
     * @return the Disk IO data matching the provided filters
     */
    fun getDiskIoData(host: String, type: DbDataType, from: Long, to: Long) = call {
        val data = DataTable.getData(type, from, to, host)
        val diskData = DiskDataTable.getData(data.map(DbDatum::id))
        return@call OutputDiskIoData(
            data.map { datum ->
                val values = diskData[datum.id]
                    ?.groupBy { it.device }
                    ?.map {
                        it.key to OutputDiskIoDataPointValues(
                            it.value.single().readUsage?.let { it / 100f },
                            it.value.single().writeUsage?.let { it / 100f },
                            it.value.single().readSpeed,
                            it.value.single().writeSpeed
                        )
                    }
                    ?.toMap()
                OutputDiskIoDataPoint(datum.date, values ?: emptyMap())
            }
        )
    }

    fun getDiskSpaceData(host: String) = call {
        val minDuration = Duration.ofHours(6).seconds
        val instant = Instant.now()
        val now = instant.toEpochMilli()
        val oneDayAgo = instant.minus(1, ChronoUnit.DAYS).toEpochMilli()
        val oneWeekAgo = instant.minus(7, ChronoUnit.DAYS).toEpochMilli()

        val nowDatum = DataTable.getDatumClosestToDate(now, host)
        val oneDayDatum = DataTable.getDatumClosestToDate(oneDayAgo, host)
        val oneWeekDatum = DataTable.getDatumClosestToDate(oneWeekAgo, host)

        val nowDisks = DiskDataTable.getData(nowDatum.id)
        val oneDayDisks = DiskDataTable.getData(oneDayDatum.id)
        val oneWeekDisks = DiskDataTable.getData(oneWeekDatum.id)

        val oneDayDuration = (nowDatum.date - oneDayDatum.date) / 1000
        val oneWeekDuration = (nowDatum.date - oneWeekDatum.date) / 1000

        val diskTimeLeft = HashMap<String, Long>()
        nowDisks.forEach { nowDisk ->
            val oneDayDisk = oneDayDisks.singleOrNull { it.device == nowDisk.device }
            val oneWeekDisk = oneWeekDisks.singleOrNull { it.device == nowDisk.device }

            val oneDaySpaceDiff = oneDayDisk?.let { nowDisk.usedSpace - it.usedSpace }
            val oneWeekSpaceDiff = oneWeekDisk?.let { nowDisk.usedSpace - it.usedSpace }

            val oneDayFillSpeed = oneDaySpaceDiff?.let { it / oneDayDuration }
            val oneWeekFillSpeed = oneWeekSpaceDiff?.let { it / oneWeekDuration }

            var max: Long = Long.MIN_VALUE
            if (oneDayFillSpeed != null && oneDayDuration > minDuration) {
                max = Math.max(max, oneDayFillSpeed)
            }
            if (oneWeekFillSpeed != null && oneWeekDuration > minDuration) {
                max = Math.max(max, oneWeekFillSpeed)
            }
            if (max > Long.MIN_VALUE) {
                val availableSpace = nowDisk.totalSpace - nowDisk.usedSpace
                diskTimeLeft[nowDisk.device] = availableSpace / max
            }
        }
        return@call OutputDiskSpaceData(nowDisks.map { disk ->
            OutputDiskSpaceDatum(
                disk.device,
                disk.totalSpace,
                disk.usedSpace.toFloat() / disk.totalSpace,
                diskTimeLeft[disk.device]
            )
        })
    }

    /**
     * Converts an [InputDatum] into a [DbHost].
     *
     * @param input the input
     *
     * @return a db host object for the provide input
     */
    private fun inputToDbHost(input: InputDatum) =
        DbHost(input.host, input.ram.ramTotal, input.ram.swapTotal)

    /**
     * Converts an [InputDatum] into a [DbDatum].
     *
     * @param input the input
     *
     * @return a db datum object for the provided input
     */
    private fun inputToDbDatum(input: InputDatum) =
        DbDatum(
            type = DbDataType.LIVE,
            host = input.host,
            date = input.date,
            cpuBusy = input.cpu.busy,
            cpuIo = input.cpu.io,
            cpuSteal = input.cpu.steal,
            ramUsed = input.ram.ramUsed,
            swapUsed = input.ram.swapUsed
        )

    /**
     * Converts an [InputDatum] into a set of [DbNetDatum].
     *
     * @param dataRef the data ref
     * @param input the input
     *
     * @return a set of db net data for the provided input
     */
    private fun inputToDbNetData(dataRef: Int, input: InputDatum) =
        input.net.interfaces.map {
            DbNetDatum(
                dataRef = dataRef,
                interfaceName = it.name,
                inBytes = it.inBytes,
                outBytes = it.outBytes,
                inPackets = it.inPackets,
                outPackets = it.outPackets,
                inErrors = it.inErrors,
                outErrors = it.outErrors
            )
        }

    /**
     * Converts an [InputDatum] into a set of [DbDiskDatum].
     *
     * @param dataRef the data ref
     * @param input the input
     *
     * @return a set of db disk data for the provided input
     */
    private fun inputToDbDiskData(dataRef: Int, input: InputDatum) =
        input.disk.disks.map {
            DbDiskDatum(
                dataRef = dataRef,
                device = it.name,
                readUsage = it.readUsage,
                writeUsage = it.writeUsage,
                readSpeed = it.readSpeed,
                writeSpeed = it.writeSpeed,
                usedSpace = it.usedKiloBytes,
                totalSpace = it.totalKiloBytes
            )
        }

}
