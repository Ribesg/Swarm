package fr.ribesg.swarm.routes.app

import fr.ribesg.swarm.*
import fr.ribesg.swarm.data.DataHandler
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respondText
import io.ktor.routing.*

private const val PATH = "disk/space"

private val log = Log.get("DiskSpaceDataRoute")

fun Route.setupDiskSpaceDataRoute() =

    get(PATH) {

        val host = call.parameters["host"]

        if (host == null || host.isBlank()) {
            log.info("Missing or empty parameter 'host'")
            call.respondText("Missing or empty parameter 'host'", status = BadRequest)
            return@get
        }

        if (!DataHandler().isHostname(host)) {
            log.info("Host not found: $host")
            call.respondText("Unknown host '$host'", status = NotFound)
            return@get
        }

        val data = try {
            DataHandler().getDiskSpaceData(host)
        } catch (t: Throwable) {
            log.error("Failed to retrieve Disk space data for host '$host'", t)
            call.respondText("Failed to get Disk space data", status = InternalServerError)
            return@get
        }

        val json = try {
            jackson.writeValueAsString(data)
        } catch (t: Throwable) {
            log.error("Failed to serialize Disk space data", t)
            log.debug("Data was: $data")
            call.respondText("Failed to serialize Disk space data", status = InternalServerError)
            return@get
        }

        call.respondText(json, ContentType.Application.Json)

    }
