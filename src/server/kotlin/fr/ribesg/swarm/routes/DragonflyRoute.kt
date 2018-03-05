package fr.ribesg.swarm.routes

import com.fasterxml.jackson.module.kotlin.readValue
import fr.ribesg.swarm.*
import fr.ribesg.swarm.data.DataHandler
import fr.ribesg.swarm.model.input.dragonfly.DragonflyPayload
import io.ktor.application.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.receiveText
import io.ktor.response.*
import io.ktor.routing.*
import java.time.*

object DragonflyRoute {

    private const val PATH = "/dragonfly"

    private val log = Log.get(DragonflyRoute::class)

    fun setup(routeManager: Route) {
        routeManager.post(PATH) { handle(call) }
    }

    private suspend fun handle(call: ApplicationCall) {
        val start = Instant.now()

        val payloadString = try {
            call.receiveText()
        } catch (t: Throwable) {
            log.error("Failed to read Dragonfly payload", t)
            call.respondText("Failed to read payload", status = InternalServerError)
            return
        }

        val payload = try {
            jackson.readValue<DragonflyPayload>(payloadString)
        } catch (t: Throwable) {
            log.error("Failed to parse Dragonfly payload", t)
            log.debug("Payload was:\n$payloadString")
            call.respondText("Malformed payload", status = BadRequest)
            return
        }

        try {
            payload.validate()
        } catch (t: Throwable) {
            log.error("Invalid Dragonfly payload", t)
            call.respondText("Invalid payload", status = BadRequest)
            return
        }

        if (payload.key != Arguments.key) {
            log.error("Invalid key provided in Dragonfly payload: ${payload.key}")
            call.respondText("Invalid key provided", status = Forbidden)
            return
        }

        try {
            DataHandler().handleDragonflyPayload(payload)
        } catch (t: Throwable) {
            log.error("Failed to handle Dragonfly payload", t)
            call.respondText("Failed to handle payload", status = InternalServerError)
            return
        }

        call.respond(OK)
        val duration = Duration.between(start, Instant.now())
        log.debug("Received data from ${payload.host} (in ${duration.toMillis()}ms)")
    }

}
