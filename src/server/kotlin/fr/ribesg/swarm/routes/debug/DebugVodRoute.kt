package fr.ribesg.swarm.routes.debug

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.ribesg.swarm.Log
import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.response.*
import io.ktor.http.*
import io.ktor.response.respondText
import io.ktor.routing.*
import kotlinx.coroutines.experimental.runBlocking

object DebugVodRoute {

    private const val PATH = "/data/debug/vod"

    private val log = Log.get(DebugVodRoute::class)

    private val client = HttpClient(Apache)

    private val jackson = jacksonObjectMapper()

    fun setup(routeManager: Route) {
        routeManager.get(PATH) { handle(call) }
    }

    private suspend fun ApplicationCall.respondError(url: String?, t: Throwable) {
        val result = Result(url, false, null, t.javaClass.simpleName, null)
        respondText(
            jackson.writeValueAsString(result),
            ContentType.Application.Json,
            HttpStatusCode.InternalServerError
        )
    }

    private suspend fun handle(call: ApplicationCall) {
        val url = call.parameters["url"]
        if (url.isNullOrBlank()) {
            val result = Result(url, false, null, "Missing or empty parameter 'url'", null)
            call.respondText(
                jackson.writeValueAsString(result),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return
        }
        val response = try {
            runBlocking {
                client.get<HttpResponse>(url!!)
            }
        } catch (t: Throwable) {
            log.error("Failed to query url '$url'", t)
            call.respondError(url, t)
            return
        }
        if (response.status != HttpStatusCode.OK) {
            log.error("Failed to query url '$url', received status code ${response.status.value}")
            val result = Result(url, false, response.status.value, "API error (${response.status.value})", null)
            call.respondText(
                jackson.writeValueAsString(result),
                ContentType.Application.Json,
                response.status
            )
            return
        }
        val body = try {
            response.readText()
        } catch (t: Throwable) {
            log.error("Failed to read response for query to url '$url'", t)
            call.respondError(url, t)
            return
        }
        val json = try {
            jackson.readTree(body)
        } catch (t: Throwable) {
            log.error("Failed to parse response for query to url '$url':\n$body\n", t)
            call.respondError(url, t)
            return
        }
        val result = Result(url, true, 200, null, json)
        try {
            call.respondText(jackson.writeValueAsString(result), ContentType.Application.Json)
        } catch (t: Throwable) {
            log.error("Failed to return result", t)
            call.respondError(url, t)
            return
        }
    }

    private data class Result(
        val url: String?,
        val success: Boolean,
        val receivedCode: Int?,
        val error: String?,
        val data: JsonNode?
    )

}
