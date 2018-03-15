package fr.ribesg.swarm.data

import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.model.DataMode
import fr.ribesg.swarm.model.database.DbDataType
import fr.ribesg.swarm.model.output.*
import java.time.*

/**
 * Common code between [RealDataHandler] and [TestDataHandler].
 *
 * @see DataHandler
 */
abstract class CommonDataHandler(protected val database: Database) : DataHandler {

    /**
     * Live Clock
     */
    protected val clock: Clock =
        Clock.systemUTC()

    /**
     * Minute Clock
     */
    protected val minuteClock: Clock =
        Clock.tickMinutes(ZoneOffset.UTC)

    init {
        DataArchiver(database)
    }

    override fun isHostname(host: String): Boolean =
        host in database.getHostnames()

    override fun getHostnames(): List<String> =
        database.getHostnames().sorted()

    override fun removeHost(host: String) =
        database.removeHost(host)

    override fun getCpuData(host: String, mode: DataMode): CpuChartData? {
        val now = when (mode) {
            DataMode.LIVE -> clock.instant()
            else          -> minuteClock.instant()
        }
        val type = DbDataType.fromMode(mode)
        val from = when (mode) {
            DataMode.LIVE -> now.minus(Duration.ofMinutes(5))
            DataMode.HOUR -> now.minus(Duration.ofHours(1))
            DataMode.DAY  -> now.minus(Duration.ofDays(1))
            DataMode.WEEK -> now.minus(Duration.ofDays(7))
        }.toEpochMilli()
        val result = database.getCpuData(host, type, from, now.toEpochMilli())
        return if (result.data.size < 3) null else CpuChartData(result)
    }

    override fun getRamData(host: String, mode: DataMode): RamChartData? {
        val now = when (mode) {
            DataMode.LIVE -> clock.instant()
            else          -> minuteClock.instant()
        }
        val type = DbDataType.fromMode(mode)
        val from = when (mode) {
            DataMode.LIVE -> now.minus(Duration.ofMinutes(5))
            DataMode.HOUR -> now.minus(Duration.ofHours(1))
            DataMode.DAY  -> now.minus(Duration.ofDays(1))
            DataMode.WEEK -> now.minus(Duration.ofDays(7))
        }.toEpochMilli()
        val result = database.getRamData(host, type, from, now.toEpochMilli())
        return if (result.data.size < 3) null else RamChartData(result)
    }

    override fun getNetData(host: String, mode: DataMode): NetChartData? {
        val now = when (mode) {
            DataMode.LIVE -> clock.instant()
            else          -> minuteClock.instant()
        }
        val type = DbDataType.fromMode(mode)
        val from = when (mode) {
            DataMode.LIVE -> now.minus(Duration.ofMinutes(5))
            DataMode.HOUR -> now.minus(Duration.ofHours(1))
            DataMode.DAY  -> now.minus(Duration.ofDays(1))
            DataMode.WEEK -> now.minus(Duration.ofDays(7))
        }.toEpochMilli()
        val result = database.getNetData(host, type, from, now.toEpochMilli())
        return if (result.data.size < 3) null else NetChartData(result)
    }

    override fun getDiskIoData(host: String, mode: DataMode): DiskIoChartData? {
        val now = when (mode) {
            DataMode.LIVE -> clock.instant()
            else          -> minuteClock.instant()
        }
        val type = DbDataType.fromMode(mode)
        val from = when (mode) {
            DataMode.LIVE -> now.minus(Duration.ofMinutes(5))
            DataMode.HOUR -> now.minus(Duration.ofHours(1))
            DataMode.DAY  -> now.minus(Duration.ofDays(1))
            DataMode.WEEK -> now.minus(Duration.ofDays(7))
        }.toEpochMilli()
        val result = database.getDiskIoData(host, type, from, now.toEpochMilli())
        return if (result.data.size < 3) null else DiskIoChartData(result)
    }

    override fun getDiskSpaceData(host: String): DiskSpaceTableData {
        return DiskSpaceTableData(database.getDiskSpaceData(host))
    }

    override fun getAlertData(from: Long, to: Long): List<OutputAlertData> = Database.call {
        val hosts = database.getHosts()
        val (data, diskData, mostRecentLiveData) = database.getAlertData(from, to)
        return@call hosts.map { host ->
            val hostData = data.filter { it.host == host.host }
            val hostDiskData = diskData.filterKeys { it in hostData.map { it.id } }.flatMap { it.value }
            OutputAlertData(host, hostData, hostDiskData, mostRecentLiveData)
        }
    }

}
