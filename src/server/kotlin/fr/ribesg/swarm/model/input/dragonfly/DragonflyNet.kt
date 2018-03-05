package fr.ribesg.swarm.model.input.dragonfly

/**
 * A parsed version of the [DragonflyPayload.net] object values.
 */
class DragonflyNet(raw: String) {

    /**
     * The bash array
     */
    private val bashArray: Array<String>

    /**
     * The counter of received bytes
     */
    val inBytes: Long
        get() = bashArray[0].toLong()

    /**
     * The counter of received packets
     */
    val inPackets: Long
        get() = bashArray[1].toLong()

    /**
     * The counter of reception errors
     */
    val inErrors: Long
        get() = bashArray[2].toLong()

    /**
     * Unused counter
     */
    val inDrops: Long
        get() = bashArray[3].toLong()

    /**
     * Unused counter
     */
    val inFifo: Long
        get() = bashArray[4].toLong()

    /**
     * Unused counter
     */
    val inFrame: Long
        get() = bashArray[5].toLong()

    /**
     * Unused counter
     */
    val inCompressed: Long
        get() = bashArray[6].toLong()

    /**
     * Unused counter
     */
    val inMulticast: Long
        get() = bashArray[7].toLong()

    /**
     * The counter of transmitted bytes
     */
    val outBytes: Long
        get() = bashArray[8].toLong()

    /**
     * The counter of transmitted packets
     */
    val outPackets: Long
        get() = bashArray[9].toLong()

    /**
     * The counter of transmission errors
     */
    val outErrors: Long
        get() = bashArray[10].toLong()

    /**
     * Unused counter
     */
    val outDrops: Long
        get() = bashArray[11].toLong()

    /**
     * Unused counter
     */
    val outFifo: Long
        get() = bashArray[12].toLong()

    /**
     * Unused counter
     */
    val outCollisions: Long
        get() = bashArray[13].toLong()

    /**
     * Unused counter
     */
    val outCarrier: Long
        get() = bashArray[14].toLong()

    /**
     * Unused counter
     */
    val outCompressed: Long
        get() = bashArray[15].toLong()

    init {
        val values = raw.split(' ')
        require(values.size == 16) { "Invalid NET data array length: ${values.size} ($raw)" }
        bashArray = values.toTypedArray()
    }

}
