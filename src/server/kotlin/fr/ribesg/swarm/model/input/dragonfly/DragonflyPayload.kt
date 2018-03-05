package fr.ribesg.swarm.model.input.dragonfly

/**
 * Model of the payload sent by Dragonfly clients.
 */
data class DragonflyPayload(

    /**
     * The Swarm key
     */
    val key: String,

    /**
     * The host name
     */
    val host: String,

    /**
     * The date at which the data point was created, in milliseconds since epoch
     */
    val date: Long,

    /**
     * A bash array containing CPU data
     *
     * Its fields are:
     * - user
     * - nice
     * - system
     * - idle
     * - iowait
     * - irq
     * - softirq
     * - steal (optional)
     * - guest (optional)
     * - guestNice (optional)
     *
     * @see DragonflyCpu
     */
    val cpu: String,

    /**
     * A bash array containing RAM data
     *
     * Its fields are:
     * - ramTotal
     * - ramFree
     * - swapTotal
     * - swapFree
     *
     * @see DragonflyRam
     */
    val ram: String,

    /**
     * A map of network interface names to bash arrays
     *
     * The bash arrays fields are:
     * - Reception
     *      - bytes
     *      - packets
     *      - errors
     *      - drops
     *      - fifo
     *      - frame
     *      - compressed
     *      - multicast
     * - Transmission
     *      - bytes
     *      - packets
     *      - errors
     *      - drops
     *      - fifo
     *      - collisions
     *      - carrier
     *      - compressed
     *
     * @see DragonflyNet
     */
    val net: Map<String, String>,

    /**
     * The disk data
     *
     * @see DiskData
     */
    val disk: DiskData

) {

    /**
     * Validates all fields of this model.
     */
    fun validate() {
        check(key.isNotBlank()) { "Invalid value for key: should not be blank" }
        check(host.isNotBlank()) { "Invalid value for host: should not be blank" }
        check(cpu.matches("(\\d+ ){6}\\d+( \\d+( \\d+( \\d+)?)?)?".toRegex())) {
            "Malformed CPU data array: $cpu"
        }
        check(ram.matches("(\\d+ ){3}\\d+".toRegex())) {
            "Malformed RAM data array: $ram"
        }
        for ((_, netData) in net) {
            check(netData.matches("(\\d+ ){15}\\d+".toRegex())) {
                "Malformed NET dat array: $netData"
            }
        }
        disk.validate()
    }

    /**
     * The disk data
     */
    data class DiskData(

        /**
         * Result of the command df -T filtered by filesystems starting with /dev/.
         *
         * It's a list of bash arrays with the following format:
         * - Filesystem
         * - Type
         * - 1K-blocks
         * - Used
         * - Available
         * - Use%
         * - Mounted on
         */
        val df: List<String>,

        /**
         * A list of disk io data bash arrays.
         *
         * The bash array fields are:
         * - major device number
         * - minor device number
         * - device name
         * - reads completed successfully
         * - reads merged
         * - sectors read (1 sector = 512 bytes)
         * - milliseconds spent reading
         * - writes completed successfully
         * - writes merged
         * - sectors written (1 sector = 512 bytes)
         * - milliseconds spent writing
         * - number of I/O operations in progress
         * - milliseconds spent doing I/O
         * - weighted number of milliseconds spent doing I/O
         *
         * Source: https://www.kernel.org/doc/Documentation/ABI/testing/procfs-diskstats
         */
        val diskstats: List<String>

    ) {

        fun validate() {
            for (dfValue in df) {
                check(dfValue.matches("(\\w+ ){2}(\\d+ ){3}(\\d+%) [\\w/]+".toRegex())) {
                    "Malformed DISK DF data array: $dfValue"
                }
            }
            for (diskstatsValue in diskstats) {
                check(diskstatsValue.matches("(\\d+ ){2}\\w+( \\d+){11}".toRegex())) {
                    "Malformed DISK STATS data array: $diskstatsValue"
                }
            }
        }

    }

}
