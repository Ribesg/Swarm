package fr.ribesg.swarm.model.input.dragonfly

/**
 * A parsed version of the [DragonflyPayload.cpu] bash array.
 */
class DragonflyCpu(raw: String) {

    /**
     * The bash array
     */
    private val bashArray: Array<String>

    /**
     * The "user" counter
     */
    val user: Long
        get() = bashArray[0].toLong()

    /**
     * The "nice" counter
     */
    val nice: Long
        get() = bashArray[1].toLong()

    /**
     * The "system" counter
     */
    val system: Long
        get() = bashArray[2].toLong()

    /**
     * The "idle" counter
     */
    val idle: Long
        get() = bashArray[3].toLong()

    /**
     * The "iowait" counter
     */
    val iowait: Long
        get() = bashArray[4].toLong()

    /**
     * The "irq" counter
     */
    val irq: Long
        get() = bashArray[5].toLong()

    /**
     * The "softirq" counter
     */
    val softirq: Long
        get() = bashArray[6].toLong()

    /**
     * The "steal" counter
     */
    val steal: Long
        get() = bashArray.getOrNull(7)?.toLong() ?: 0

    /**
     * The "guest" counter
     */
    val guest: Long
        get() = bashArray.getOrNull(8)?.toLong() ?: 0

    /**
     * The "guest_nice" counter
     */
    val guestNice: Long
        get() = bashArray.getOrNull(9)?.toLong() ?: 0

    /**
     * Sum of all non particularly interesting counters into one 'busy' counter
     */
    val busySum: Long
        get() = user + nice + system + irq + softirq + guest + guestNice

    init {
        val values = raw.split(' ')
        require(values.size in 7..10) { "Invalid CPU data length: ${values.size} ($raw)" }
        bashArray = values.toTypedArray()
    }

}
