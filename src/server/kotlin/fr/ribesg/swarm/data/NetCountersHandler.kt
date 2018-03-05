package fr.ribesg.swarm.data

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.model.input.InputNet
import fr.ribesg.swarm.model.input.dragonfly.DragonflyNet
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles conversion of Network counters into Network usage.
 */
object NetCountersHandler {

    /**
     * Max allowed age of previous Network counters to be used to compute Network usage, in seconds
     */
    private const val MAX_PREVIOUS_COUNTER_AGE = 60L

    /**
     * The logger
     */
    private val log = Log.get(NetCountersHandler::class)

    /**
     * Contains all previous Network counters for each interface for each host and the date at which they were emitted
     */
    private val previousCountersValues: MutableMap<String, Pair<Long, Map<String, DragonflyNet>>> = ConcurrentHashMap()

    /**
     * Registers the provided counters and computes Network usage if possible.
     *
     * @param host the hostname
     * @param date the date in milliseconds from epoch
     * @param counters the Network counters for each interface
     *
     * @return Network usage
     */
    fun handle(host: String, date: Long, counters: Map<String, DragonflyNet>): InputNet {
        val previousPair = previousCountersValues[host]
        previousCountersValues[host] = date to counters

        if (previousPair == null) {
            log.debug("First Network data received, nothing to return")
            return InputNet(emptyList())
        }

        val (previousDate, previousCounters) = previousPair
        if (Duration.ofMillis(date - previousDate) > Duration.ofSeconds(MAX_PREVIOUS_COUNTER_AGE)) {
            log.debug("Previous Network data is too old, nothing to return")
            return InputNet(emptyList())
        }

        return computeNetInput(date - previousDate, previousCounters, counters)
    }

    /**
     * Computes [InputNet] from two sets of interfaces' [DragonflyNet].
     *
     * @param interval interval between the two sets of data in milliseconds
     * @param from the older set of interfaces' data
     * @param to the newer set of interfaces' data
     *
     * @return Network input data, may be empty
     */
    private fun computeNetInput(
        interval: Long,
        from: Map<String, DragonflyNet>,
        to: Map<String, DragonflyNet>
    ): InputNet {
        val intervalSeconds = interval / 1000.0
        return InputNet(
            (from.keys intersect to.keys).map { interfaceName ->
                val ifFrom = from[interfaceName]!!
                val ifTo = to[interfaceName]!!
                val inBytes = ifTo.inBytes - ifFrom.inBytes
                val outBytes = ifTo.outBytes - ifFrom.outBytes
                val inPackets = ifTo.inPackets - ifFrom.inPackets
                val outPackets = ifTo.outPackets - ifFrom.outPackets
                val inErrors = ifTo.inErrors - ifFrom.inErrors
                val outErrors = ifTo.outErrors - ifFrom.outErrors
                InputNet.InputNetInterface(
                    interfaceName,
                    if (inBytes < 0) null else (inBytes / intervalSeconds).toLong(),
                    if (outBytes < 0) null else (outBytes / intervalSeconds).toLong(),
                    if (inPackets < 0) null else (inPackets / intervalSeconds).toLong(),
                    if (outPackets < 0) null else (outPackets / intervalSeconds).toLong(),
                    if (inErrors < 0) null else (inErrors / intervalSeconds).toLong(),
                    if (outErrors < 0) null else (outErrors / intervalSeconds).toLong()
                )
            }
        )
    }

}
