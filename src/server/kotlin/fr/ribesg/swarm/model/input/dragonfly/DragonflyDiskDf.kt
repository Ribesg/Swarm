package fr.ribesg.swarm.model.input.dragonfly

/**
 * A parsed version of the [DragonflyPayload.DiskData.df] array values.
 */
class DragonflyDiskDf(raw: String) {

    /**
     * The bash array
     */
    private val bashArray: Array<String>

    /**
     * The filesystem
     *
     * Only filesystems starting with "/dev/" should be sent by Dragonfly clients, with the "/dev/" prefix removed
     */
    val filesystem: String
        get() = bashArray[0]

    /**
     * The type of filesystem
     *
     * eg. xfs, ext4
     */
    val type: String
        get() = bashArray[1]

    /**
     * The amount of blocks of 1000 bytes on the filesystem
     *
     * This is more or less the total size of the disk device in kilobytes
     */
    val oneThousandBlocks: Long
        get() = bashArray[2].toLong()

    /**
     * The amount of 1K-blocks used
     *
     * This is more or less the amount of space used on the filesystem in kilobytes
     */
    val used: Long
        get() = bashArray[3].toLong()

    /**
     * The amount of 1K-blocks available
     *
     * This is more or less the amount of available space on the filesystem in kilobytes
     */
    val available: Long
        get() = bashArray[4].toLong()

    /**
     * The filesystem usage percentage
     */
    val usePercentage: String
        get() = bashArray[5]

    /**
     * Where the filesystem is mounted
     *
     * eg. /
     */
    val mountedOn: String
        get() = bashArray[6]

    init {
        val values = raw.split(' ')
        require(values.size == 7) { "Invalid Disk DF data array length: ${values.size} ($raw)" }
        bashArray = values.toTypedArray()
    }

}
