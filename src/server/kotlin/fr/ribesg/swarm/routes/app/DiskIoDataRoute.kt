package fr.ribesg.swarm.routes.app

import fr.ribesg.swarm.*
import fr.ribesg.swarm.data.DataHandler
import fr.ribesg.swarm.model.DataMode
import fr.ribesg.swarm.model.output.DiskIoChartData
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respondText
import io.ktor.routing.*

private const val PATH = "/data/disk/io"

private val log = Log.get("DiskIoDataRoute")

fun Route.setupDiskIoDataRoute() = get(PATH) {

    val mode = call.parameters["mode"]
    val host = call.parameters["host"]

    if (mode == null || mode.isBlank()) {
        log.info("Missing or empty parameter 'mode'")
        call.respondText("Missing or empty parameter 'mode'", status = BadRequest)
        return@get
    }

    if (host == null || host.isBlank()) {
        log.info("Missing or empty parameter 'host'")
        call.respondText("Missing or empty parameter 'host'", status = BadRequest)
        return@get
    }

    try {
        DataMode.valueOf(mode)
    } catch (e: IllegalArgumentException) {
        log.info("Invalid mode: '$mode'")
        call.respondText("Invalid parameter 'mode'", status = BadRequest)
        return@get
    }

    if (!DataHandler().isHostname(host)) {
        log.info("Host not found: $host")
        call.respondText("Unknown host '$host'", status = NotFound)
        return@get
    }

    val data = try {
        DataHandler().getDiskIoData(host, DataMode.valueOf(mode))
    } catch (t: Throwable) {
        log.error("Failed to retrieve Disk IO data for host '$host' and mode '$mode'", t)
        call.respondText("Failed to get Disk IO data", status = InternalServerError)
        return@get
    }

    val json = try {
        jackson.writeValueAsString(data ?: DiskIoChartData.EMPTY)
    } catch (t: Throwable) {
        log.error("Failed to serialize Disk IO data", t)
        log.debug("Data was: $data")
        call.respondText("Failed to serialize Disk IO data", status = InternalServerError)
        return@get
    }

    call.respondText(json, ContentType.Application.Json)

}
