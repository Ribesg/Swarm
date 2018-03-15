package fr.ribesg.swarm.data

import fr.ribesg.swarm.Arguments
import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.model.DataMode
import fr.ribesg.swarm.model.input.dragonfly.DragonflyPayload
import fr.ribesg.swarm.model.output.*

/**
 * Handles incoming and outgoing data, basically a wrapper of the Database
 *
 * Call `DataHandler()` to get the singleton
 */
interface DataHandler {

    companion object {

        private lateinit var handler: DataHandler

        fun init(arguments: Arguments, database: Database) {
            handler = if (arguments.development) {
                TestDataHandler(database)
            } else {
                RealDataHandler(database)
            }
        }

        /**
         * Calling `DataHandler()` will return either the real one or a test one depending on the context.
         */
        operator fun invoke(): DataHandler = handler

    }

    /**
     * Handles an incoming Dragonfly payload.
     *
     * @param payload the Dragonfly payload
     */
    fun handleDragonflyPayload(payload: DragonflyPayload)

    /**
     * Checks that the provided hostname is known.
     *
     * @param host a hostname
     *
     * @return if the provided hostname is known
     */
    fun isHostname(host: String): Boolean

    /**
     * Gets a list of all known hostnames.
     *
     * @return a list of all known hostnames
     */
    fun getHostnames(): List<String>

    /**
     * Removes an host and all its associated data.
     */
    fun removeHost(host: String)

    /**
     * Gets data for a CPU chart matching the provided host and mode.
     *
     * @param host the hostname
     * @param mode the mode
     *
     * @return data for a CPU chart matching the provided host and mode
     */
    fun getCpuData(host: String, mode: DataMode): CpuChartData?

    /**
     * Gets data for a RAM chart matching the provided host and mode.
     *
     * @param host the hostname
     * @param mode the mode
     *
     * @return data for a RAM chart matching the provided host and mode
     */
    fun getRamData(host: String, mode: DataMode): RamChartData?

    /**
     * Gets data for a Network chart matching the provided host and mode.
     *
     * @param host the hostname
     * @param mode the mode
     *
     * @return data for a Network chart matching the provided host and mode
     */
    fun getNetData(host: String, mode: DataMode): NetChartData?

    /**
     * Gets data for a Disk IO chart matching the provided host and mode.
     *
     * @param host the hostname
     * @param mode the mode
     *
     * @return data for a Disk IO chart matching the provided host and mode
     */
    fun getDiskIoData(host: String, mode: DataMode): DiskIoChartData?

    /**
     * Gets disk space data.
     */
    fun getDiskSpaceData(host: String): DiskSpaceTableData

    /**
     * Gets alert data in the provided period.
     *
     * @param from the minimum date, inclusive
     * @param to the maximum date, exclusive
     *
     * @return alert data in the provided period
     */
    fun getAlertData(from: Long, to: Long): List<OutputAlertData>

}
