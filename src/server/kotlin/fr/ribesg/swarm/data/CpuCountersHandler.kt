package fr.ribesg.swarm.data

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.model.input.InputCpu
import fr.ribesg.swarm.model.input.dragonfly.DragonflyCpu
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles conversion of CPU counters into CPU usage values.
 */
object CpuCountersHandler {

    /**
     * Max allowed age of previous CPU counters to be used to compute CPU usage
     */
    private const val MAX_PREVIOUS_COUNTER_AGE = 60L

    /**
     * The logger
     */
    private val log = Log.get(CpuCountersHandler::class)

    /**
     * Contains all previous CPU counters for each host and the date at which they were emitted
     */
    private val previousCountersValues: MutableMap<String, Pair<Long, DragonflyCpu>> = ConcurrentHashMap()

    /**
     * Registers the provided counters and computes CPU usage if possible.
     *
     * @param host the hostname
     * @param date the date in milliseconds from epoch
     * @param counters the CPU counters
     *
     * @return CPU usage, if possible, null otherwise
     */
    fun handle(host: String, date: Long, counters: DragonflyCpu): InputCpu {
        val previousPair = previousCountersValues[host]
        previousCountersValues[host] = date to counters

        if (previousPair == null) {
            log.debug("First cpu data received, nothing to return")
            return InputCpu.EMPTY
        }

        val (previousDate, previousCounters) = previousPair
        if (Duration.ofMillis(date - previousDate) > Duration.ofSeconds(MAX_PREVIOUS_COUNTER_AGE)) {
            log.debug("Previous cpu data is too old, nothing to return")
            return InputCpu.EMPTY
        }

        return computeCpuInput(previousCounters, counters)
    }

    /**
     * Computes [InputCpu] from two [DragonflyCpu] if possible.
     *
     * @param from the older set of counters
     * @param to the newer set of counters
     *
     * @return CPU usage
     */
    private fun computeCpuInput(from: DragonflyCpu, to: DragonflyCpu): InputCpu =
        if (hasOneCounterReset(from, to)) {
            log.debug("Counter has reset, nothing to return")
            InputCpu.EMPTY
        } else {
            val busyDiff = to.busySum - from.busySum
            val idleDiff = to.idle - from.idle
            val ioDiff = to.iowait - from.iowait
            val stealDiff = to.steal - from.steal
            val total = busyDiff + idleDiff + ioDiff + stealDiff
            InputCpu(
                (busyDiff * 10000 / total).toInt(),
                (ioDiff * 10000 / total).toInt(),
                (stealDiff * 10000 / total).toInt()
            )
        }

    /**
     * Checks if there is at least one counter reset between two sets of counters.
     *
     * @param from the older set of counters
     * @param to the newer set of counters
     *
     * @return if there was a counter reset between the two sets of counters
     */
    private fun hasOneCounterReset(from: DragonflyCpu, to: DragonflyCpu): Boolean =
        from.user > to.user ||
        from.nice > to.nice ||
        from.system > to.system ||
        from.idle > to.idle ||
        from.iowait > to.iowait ||
        from.irq > to.irq ||
        from.softirq > to.softirq ||
        from.steal > to.steal ||
        from.guest > to.guest ||
        from.guestNice > to.guestNice

}
