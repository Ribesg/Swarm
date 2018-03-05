package fr.ribesg.swarm.data

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.model.input.*
import fr.ribesg.swarm.model.input.dragonfly.*

/**
 * The real [DataHandler] used in production
 */
object RealDataHandler : CommonDataHandler() {

    /**
     * The logger
     */
    private val log = Log.get(RealDataHandler::class)

    /**
     * Date of the last message received from each host
     */
    private val lastMessageDate = HashMap<String, Long>()

    override fun handleDragonflyPayload(payload: DragonflyPayload) {
        val previousDate = lastMessageDate[payload.host]
        if (previousDate != null && previousDate > payload.date) {
            log.warn("Received message older than the last one for host ${payload.host}, ignoring")
        } else {
            lastMessageDate[payload.host] = payload.date
            Database.push(dragonflyPayloadToInput(payload))
        }
    }

    /**
     * Converts a Dragonfly payload into an [InputDatum].
     *
     * @param payload a Dragonfly payload
     *
     * @return an input containing the payload's data
     */
    private fun dragonflyPayloadToInput(payload: DragonflyPayload): InputDatum {
        val dragonflyCpu = DragonflyCpu(payload.cpu)
        val dragonflyRam = DragonflyRam(payload.ram)
        val dragonflyNet = payload.net.map { (ifName, array) -> ifName to DragonflyNet(array) }.toMap()
        val dragonflyDiskDf = payload.disk.df.map(::DragonflyDiskDf)
        val dragonflyDiskStats = payload.disk.diskstats.map(::DragonflyDiskStats)
        return InputDatum(
            payload.host,
            payload.date,
            CpuCountersHandler.handle(payload.host, payload.date, dragonflyCpu),
            InputRam(dragonflyRam.ramUsed, dragonflyRam.swapUsed, dragonflyRam.ramTotal, dragonflyRam.swapTotal),
            NetCountersHandler.handle(payload.host, payload.date, dragonflyNet),
            DiskCountersHandler.handle(payload.host, payload.date, dragonflyDiskDf, dragonflyDiskStats)
        )
    }

}
