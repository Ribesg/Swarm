package fr.ribesg.swarm.routes.app

import fr.ribesg.swarm.*
import fr.ribesg.swarm.data.DataHandler
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.response.respondText
import io.ktor.routing.*


private const val PATH = "/data/hosts"

private val log = Log.get("HostsRoute")

fun Route.setupHostsRoute() = get(PATH) {

    val hosts = try {
        DataHandler().getHostnames()
    } catch (t: Throwable) {
        log.error("Failed to retrieve hostnames", t)
        call.respondText("Failed to get hostnames", status = InternalServerError)
        return@get
    }

    val json = try {
        jackson.writeValueAsString(Hosts(hosts))
    } catch (t: Throwable) {
        log.error("Failed to serialize hostnames", t)
        log.debug("Hostnames were: $hosts")
        call.respondText("Failed to serialize hostnames", status = InternalServerError)
        return@get
    }

    call.respondText(json, ContentType.Application.Json)

}

private data class Hosts(
    val hosts: List<String>
)
