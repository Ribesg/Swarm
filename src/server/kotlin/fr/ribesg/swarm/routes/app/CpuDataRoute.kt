package fr.ribesg.swarm.routes.app

import fr.ribesg.swarm.*
import fr.ribesg.swarm.data.DataHandler
import fr.ribesg.swarm.model.DataMode
import fr.ribesg.swarm.model.output.CpuChartData
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respondText
import io.ktor.routing.*


private const val PATH = "/data/cpu"

private val log = Log.get("CpuDataRoute")

fun Route.setupCpuDataRoute() = get(PATH) {

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
        DataHandler().getCpuData(host, DataMode.valueOf(mode))
    } catch (t: Throwable) {
        log.error("Failed to retrieve CPU data for host '$host' and mode '$mode'", t)
        call.respondText("Failed to get CPU data", status = InternalServerError)
        return@get
    }

    val json = try {
        jackson.writeValueAsString(data ?: CpuChartData.EMPTY)
    } catch (t: Throwable) {
        log.error("Failed to serialize CPU data", t)
        log.debug("Data was: $data")
        call.respondText("Failed to serialize CPU data", status = InternalServerError)
        return@get
    }

    call.respondText(json, ContentType.Application.Json)

}
