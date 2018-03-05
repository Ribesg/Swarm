package fr.ribesg.swarm.data

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.database.tables.HostsTable
import fr.ribesg.swarm.model.database.DbHost
import fr.ribesg.swarm.model.input.*
import fr.ribesg.swarm.model.input.InputDisk.InputDiskDevice
import fr.ribesg.swarm.model.input.InputNet.InputNetInterface
import fr.ribesg.swarm.model.input.dragonfly.DragonflyPayload
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

/**
 * A [DataHandler] used for testing, producing test data all by itself
 */
object TestDataHandler : CommonDataHandler() {

    /**
     * The amount of test hosts
     */
    private const val HOSTS_COUNT = 50

    /**
     * The logger
     */
    private val log = Log.get(TestDataHandler::class)

    /**
     * The random instance used everywhere
     */
    private val random = Random()

    /**
     * The executor containing threads feeding live data
     */
    private val executor = Executors.newCachedThreadPool()

    /**
     * The previous [InputCpu] produced for each test host
     */
    private val lastCpu = HashMap<String, InputCpu>()

    /**
     * The previous [InputRam] produced for each test host
     */
    private val lastRam = HashMap<String, InputRam>()

    /**
     * The previous [InputNet] produced for each test host
     */
    private val lastNet = HashMap<String, InputNet>()

    /**
     * The previous [InputDisk] produced for each test host
     */
    private val lastDisk = HashMap<String, InputDisk>()

    init {
        Database.exec {
            log.debug("Generating test data...")
            val preGenTime = clock.instant().minusSeconds(7 * 60).toEpochMilli()
            val now = clock.instant().toEpochMilli()
            repeat(HOSTS_COUNT) { i ->
                val from = preGenTime + random.nextInt(5000)
                val host = "test-1337-${4242 + i}"
                val isFullCpu = random.nextFloat() > .9
                val hasIo = random.nextBoolean()
                val hasSteal = random.nextBoolean()
                val maxRam = randomMaxRam()
                val maxSwap = randomMaxSwap(maxRam)
                val netIfs = randomNetInterfaces()
                val maxNetIn = randomMaxNet()
                val maxNetOut = randomMaxNet()
                val devices = randomDisks()
                val devicesSizes = randomDisksSizes(devices)
                val isFullIo = random.nextFloat() > .9
                val maxIoSpeed = randomMaxIoSpeed()
                var lastDataDate: Long = 0
                HostsTable.upsert(DbHost(host, maxRam, maxSwap))
                Database.push((from..now step 5000).map { date ->
                    lastDataDate = date
                    randomData(
                        host, date, isFullCpu, hasIo, hasSteal, maxRam, maxSwap, netIfs, maxNetIn,
                        maxNetOut, devices, devicesSizes, isFullIo, maxIoSpeed
                    )
                })
                val nextDataDate = lastDataDate + 5000
                executor.submit {
                    try {
                        Thread.sleep(Math.max(0, (nextDataDate - now) * 1000))
                        while (isHostname(host)) {
                            Database.push(randomData(
                                host, clock.instant().toEpochMilli(), isFullCpu, hasIo, hasSteal, maxRam, maxSwap, netIfs,
                                maxNetIn, maxNetOut, devices, devicesSizes, isFullIo, maxIoSpeed
                            ))
                            Thread.sleep(5000)
                        }
                    } catch (t: Throwable) {
                        log.error("Fatal error in TestDataHandler task", t)
                    }
                }
            }
        }
    }

    private fun randomData(
        host: String, date: Long, isFullCpu: Boolean, hasIo: Boolean, hasSteal: Boolean, maxRam: Long, maxSwap: Long,
        netIfs: Set<String>, maxNetIn: Long, maxNetOut: Long, devices: List<String>, devicesSizes: List<Long>,
        isFullIo: Boolean, maxIoSpeed: Long
    ): InputDatum {
        val cpu = randomCpuInput(isFullCpu, hasIo, hasSteal, lastCpu[host])
        val ram = randomRamData(lastRam[host], maxRam, maxSwap)
        val net = randomNetData(lastNet[host], netIfs, maxNetIn, maxNetOut)
        val disk = randomDiskData(lastDisk[host], devices, devicesSizes, isFullIo, maxIoSpeed)
        lastCpu[host] = cpu
        lastRam[host] = ram
        lastNet[host] = net
        return InputDatum(
            host,
            date,
            cpu,
            ram,
            net,
            disk
        )
    }

    private fun randomMaxRam(): Long =
        Math.pow(2.0, random.nextInt(10).toDouble()).toLong() * 1024 * 1024

    private fun randomMaxSwap(ram: Long): Long =
        if (random.nextBoolean()) {
            val f = random.nextFloat()
            when {
                f > .9 -> 100 * 1024 * 1024
                f > .8 -> 10 * 1024 * 1024
                f > .7 -> 5 * 1024 * 1024
                f > .5 -> ram * 2
                f > .3 -> ram / 2
                else   -> ram
            }
        } else {
            0
        }

    private fun randomNetInterfaces(): Set<String> {
        val result = HashSet<String>()
        result.add("lo")
        result.add("eth0")
        if (random.nextFloat() > .337) {
            result.add("eth1")
            if (random.nextFloat() > .667) {
                result.add("eth2")
            }
        }
        return result
    }

    private fun randomMaxNet(): Long =
        Math.pow(2.0, random.nextInt(10).toDouble()).toLong() * 1024 * 1024

    private fun randomCpuInput(isFullCpu: Boolean, hasIo: Boolean, hasSteal: Boolean, previous: InputCpu?): InputCpu {
        if (random.nextFloat() > .98) {
            return InputCpu(null, null, null)
        } else {
            val total = if (isFullCpu) 1f else random.nextFloat()
            val busy =
                if (!hasIo && !hasSteal) {
                    total
                } else {
                    total * random.nextFloat()
                }
            val io =
                if (hasIo) {
                    if (hasSteal) {
                        (total - busy) * random.nextFloat()
                    } else {
                        total - busy
                    }
                } else {
                    0f
                }
            val steal = total - busy - io
            val busyInt = (busy * 10000).toInt()
            val ioInt = (io * 10000).toInt()
            val stealInt = (steal * 10000).toInt()
            return InputCpu(
                (busyInt + (previous?.busy ?: busyInt)) / 2,
                (ioInt + (previous?.io ?: ioInt)) / 2,
                (stealInt + (previous?.steal ?: stealInt)) / 2
            )
        }
    }

    private fun randomRamData(previous: InputRam?, maxRam: Long, maxSwap: Long): InputRam =
        InputRam(
            ((random.nextDouble() * maxRam).toLong() + (previous?.ramUsed ?: (maxRam / 2))) / 2,
            ((random.nextDouble() * maxSwap).toLong() + (previous?.swapUsed ?: (maxSwap / 2))) / 2,
            maxRam,
            maxSwap
        )

    private fun randomNetData(previous: InputNet?, netIfs: Set<String>, maxNetIn: Long, maxNetOut: Long): InputNet {
        val previousIfMap = previous?.interfaces?.map { it.name to it }?.toMap()
        return InputNet(
            netIfs.map { ifName ->
                if (random.nextFloat() > .98) {
                    InputNetInterface(ifName, null, null, null, null, null, null)
                } else {
                    val inBytes = ((random.nextDouble() * maxNetIn).toLong() + (previousIfMap?.get(ifName)?.inBytes
                                                                                ?: (maxNetIn / 2))) / 2
                    val outBytes = ((random.nextDouble() * maxNetOut).toLong() + (previousIfMap?.get(ifName)?.outBytes
                                                                                  ?: (maxNetOut / 2))) / 2
                    val inPackets = ((inBytes / 1000) * random.nextDouble() * 3).toLong()
                    val outPackets = ((outBytes / 1000) * random.nextDouble() * 3).toLong()
                    val inErrors = (if (random.nextFloat() > .9) random.nextInt(25) else 0).toLong()
                    val outErrors = (if (random.nextFloat() > .9) random.nextInt(25) else 0).toLong()
                    InputNetInterface(ifName, inBytes, outBytes, inPackets, outPackets, inErrors, outErrors)
                }
            }
        )
    }

    private fun randomDisks(): List<String> {
        val result = LinkedList<String>()
        result.add("sda")
        if (random.nextFloat() > .337) {
            result.add("sdb")
        }
        return result
    }

    private fun randomDisksSizes(devices: List<String>): List<Long> =
        devices.map {
            val f = random.nextFloat()
            return@map when {
                f > .9 -> 1024L * 1024 * 1024 * 16
                f > .8 -> 1024L * 1024 * 1024 * 4
                f > .7 -> 1024L * 1024 * 1024
                f > .5 -> 1024L * 1024 * 512
                f > -3 -> 1024L * 1024 * 120
                else   -> 1024L * 1024 * 50
            }
        }

    private fun randomMaxIoSpeed(): Long =
        if (random.nextBoolean()) 100_000_000 / 8 else 1_000_000_000 / 8

    private fun randomDiskData(previous: InputDisk?, devices: List<String>, devicesSizes: List<Long>, isFullIo: Boolean, maxIoSpeed: Long): InputDisk {
        val previousDiskMap = previous?.disks?.map { it.name to it }?.toMap()
        return InputDisk(
            devices.mapIndexed { index, device ->
                val size = devicesSizes[index]
                if (random.nextFloat() > .98) {
                    InputDiskDevice(device, (random.nextFloat() * size).toLong(), size, null, null, null, null)
                } else {
                    val previousDiskUsage = previousDiskMap?.get(device)?.usedKiloBytes
                                            ?: (random.nextFloat() * size).toLong()
                    val diskUsage = (previousDiskUsage + previousDiskUsage * (random.nextFloat() - .5) / 2).toLong()
                    val previousReadUsage = previousDiskMap?.get(device)?.readUsage ?: 2500
                    val previousWriteUsage = previousDiskMap?.get(device)?.writeUsage ?: 2500
                    val readUsage: Int
                    val writeUsage: Int
                    if (isFullIo) {
                        readUsage = 4000 + previousReadUsage / 10 + random.nextInt(1000)
                        writeUsage = 10000 - readUsage
                    } else {
                        readUsage = ((random.nextFloat() * 5000).toInt() + previousReadUsage) / 2
                        writeUsage = ((random.nextFloat() * 5000).toInt() + previousWriteUsage) / 2
                    }
                    val previousReadBytes = previousDiskMap?.get(device)?.readSpeed ?: (maxIoSpeed / 2)
                    val readKiloBytes = ((random.nextFloat() * maxIoSpeed).toLong() + previousReadBytes) / 2
                    val previousWrittenBytes = previousDiskMap?.get(device)?.writeSpeed ?: (maxIoSpeed / 2)
                    val writtenBytes = ((random.nextFloat() * maxIoSpeed).toLong() + previousWrittenBytes) / 2
                    InputDiskDevice(device, diskUsage, size, readUsage, writeUsage, readKiloBytes, writtenBytes)
                }
            }
        )
    }

    override fun handleDragonflyPayload(payload: DragonflyPayload) {
        log.debug("Ignoring Dragonfly payload from ${payload.host} in development mode")
    }

}
