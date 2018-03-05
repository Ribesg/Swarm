package fr.ribesg.swarm.routes.app

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.alerts.SlackAlertLogger
import fr.ribesg.swarm.data.DataHandler
import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.response.*
import io.ktor.routing.*

object HostRemoveRoute {

    private const val PATH = "/data/remove"

    private val log = Log.get(HostRemoveRoute::class)

    fun setup(routeManager: Route) {
        routeManager.get(PATH) { handle(call) }
    }

    private suspend fun handle(call: ApplicationCall) {
        val host = call.parameters["host"]

        if (host == null || host.isBlank()) {
            log.info("Missing or empty parameter 'host'")
            call.respondText("Missing or empty parameter 'host'", status = BadRequest)
            return
        }

        if (!DataHandler().isHostname(host)) {
            log.info("Host not found: $host")
            call.respondText("Unknown host '$host'", status = HttpStatusCode.NotFound)
            return
        }

        try {
            DataHandler().removeHost(host)
        } catch (t: Throwable) {
            log.error("Failed to remove all data for host '$host'", t)
            call.respondText("Failed to remove host", status = InternalServerError)
            return
        }

        SlackAlertLogger.logHostRemoval(host)

        call.respond(HttpStatusCode.OK)
    }

}
