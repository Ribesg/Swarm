package fr.ribesg.swarm.routes.app

import fr.ribesg.swarm.*
import fr.ribesg.swarm.data.DataHandler
import io.ktor.application.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respondText
import io.ktor.routing.*

object DiskSpaceDataRoute {

    private const val PATH = "/data/disk/space"

    private val log = Log.get(DiskSpaceDataRoute::class)

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
            call.respondText("Unknown host '$host'", status = NotFound)
            return
        }

        val data = try {
            DataHandler().getDiskSpaceData(host)
        } catch (t: Throwable) {
            log.error("Failed to retrieve Disk space data for host '$host'", t)
            call.respondText("Failed to get Disk space data", status = InternalServerError)
            return
        }

        val json = try {
            jackson.writeValueAsString(data)
        } catch (t: Throwable) {
            log.error("Failed to serialize Disk space data", t)
            log.debug("Data was: $data")
            call.respondText("Failed to serialize Disk space data", status = InternalServerError)
            return
        }

        call.respondText(json, ContentType.Application.Json)
    }

}
