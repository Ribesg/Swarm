package fr.ribesg.swarm.model.input.dragonfly

/**
 * A parsed version of the [DragonflyPayload.DiskData.diskstats] array values.
 */
class DragonflyDiskStats(raw: String) {

    /**
     * The bash array
     */
    private val bashArray: Array<String>

    /**
     * The device major number
     */
    val deviceMajorNumber: Int
        get() = bashArray[0].toInt()

    /**
     * The device minor number
     */
    val deviceMinorNumber: Int
        get() = bashArray[1].toInt()

    /**
     * The device name
     */
    val deviceName: String
        get() = bashArray[2]

    /**
     * Counter of successful reads
     */
    val successfulReads: Long
        get() = bashArray[3].toLong()

    /**
     * Counter of merged reads
     */
    val mergedReads: Long
        get() = bashArray[4].toLong()

    /**
     * Counter of 512KB sector reads
     */
    val sectorReads: Long
        get() = bashArray[5].toLong()

    /**
     * Counter of milliseconds spent reading
     */
    val msSpentReading: Long
        get() = bashArray[6].toLong()

    /**
     * Counter of successful writes
     */
    val successfulWrites: Long
        get() = bashArray[7].toLong()

    /**
     * Counter of merged writes
     */
    val mergedWrites: Long
        get() = bashArray[8].toLong()

    /**
     * Counter of 512KB sector writes
     */
    val sectorWrites: Long
        get() = bashArray[9].toLong()

    /**
     * Counter of milliseconds spent writing
     */
    val msSpentWriting: Long
        get() = bashArray[10].toLong()

    /**
     * Counter of ios in progress
     */
    val iosInProgress: Long
        get() = bashArray[11].toLong()

    /**
     * Counter of milliseconds spent on ios (sum of [msSpentReading] and [msSpentWriting])
     */
    val msSpentOnIos: Long
        get() = bashArray[12].toLong()

    /**
     * Weird thing
     */
    val weightedMsSpentOnIos: Long
        get() = bashArray[13].toLong()

    init {
        val values = raw.split(' ')
        require(values.size == 14) { "Invalid Disk Stats data array length: ${values.size} ($raw)" }
        bashArray = values.toTypedArray()
    }

}
