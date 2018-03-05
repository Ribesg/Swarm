package fr.ribesg.swarm.model.input.dragonfly

/**
 * A parsed version of the [DragonflyPayload.ram] bash array.
 */
class DragonflyRam(raw: String) {

    /**
     * The bash array
     */
    private val bashArray: Array<String>

    /**
     * The total amount of RAM in bytes
     */
    val ramTotal: Long
        get() = bashArray[0].toLong()

    /**
     * The amount of available RAM in bytes
     */
    val ramFree: Long
        get() = bashArray[1].toLong()

    /**
     * The total amount of SWAP in bytes
     */
    val swapTotal: Long
        get() = bashArray[2].toLong()

    /**
     * The amount of available SWAP in bytes
     */
    val swapFree: Long
        get() = bashArray[3].toLong()

    /**
     * The amount of used RAM in bytes, computed from [ramTotal] and [ramFree]
     */
    val ramUsed: Long
        get() = ramTotal - ramFree

    /**
     * The amount of used SWAP in bytes, computed from [swapTotal] and [swapFree]
     */
    val swapUsed: Long
        get() = swapTotal - swapFree

    init {
        val values = raw.split(' ')
        require(values.size == 4) { "Invalid RAM data array length: ${values.size} ($raw)" }
        bashArray = values.toTypedArray()
    }

}
