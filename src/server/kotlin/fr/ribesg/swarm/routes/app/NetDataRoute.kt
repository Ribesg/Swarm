package fr.ribesg.swarm.routes.app

import fr.ribesg.swarm.*
import fr.ribesg.swarm.data.DataHandler
import fr.ribesg.swarm.model.DataMode
import fr.ribesg.swarm.model.output.NetChartData
import io.ktor.application.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respondText
import io.ktor.routing.*

object NetDataRoute {

    private const val PATH = "/data/net"

    private val log = Log.get(NetDataRoute::class)

    fun setup(routeManager: Route) {
        routeManager.get(PATH) { handle(call) }
    }

    private suspend fun handle(call: ApplicationCall) {
        val mode = call.parameters["mode"]
        val host = call.parameters["host"]

        if (mode == null || mode.isBlank()) {
            log.info("Missing or empty parameter 'mode'")
            call.respondText("Missing or empty parameter 'mode'", status = BadRequest)
            return
        }

        if (host == null || host.isBlank()) {
            log.info("Missing or empty parameter 'host'")
            call.respondText("Missing or empty parameter 'host'", status = BadRequest)
            return
        }

        try {
            DataMode.valueOf(mode)
        } catch (e: IllegalArgumentException) {
            log.info("Invalid mode: '$mode'")
            call.respondText("Invalid parameter 'mode'", status = BadRequest)
            return
        }

        if (!DataHandler().isHostname(host)) {
            log.info("Host not found: $host")
            call.respondText("Unknown host '$host'", status = NotFound)
            return
        }

        val data = try {
            DataHandler().getNetData(host, DataMode.valueOf(mode))
        } catch (t: Throwable) {
            log.error("Failed to retrieve Network data for host '$host' and mode '$mode'", t)
            call.respondText("Failed to get Network data", status = InternalServerError)
            return
        }

        val json = try {
            jackson.writeValueAsString(data ?: NetChartData.EMPTY)
        } catch (t: Throwable) {
            log.error("Failed to serialize Network data", t)
            log.debug("Data was: $data")
            call.respondText("Failed to serialize Network data", status = InternalServerError)
            return
        }

        call.respondText(json, ContentType.Application.Json)
    }

}
