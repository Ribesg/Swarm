package fr.ribesg.swarm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.xenomachina.argparser.*
import fr.ribesg.swarm.alerts.SlackAlertLogger
import fr.ribesg.swarm.data.DataHandler
import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.extensions.stackTraceString
import fr.ribesg.swarm.routes.Routes
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

val jackson: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

private val log = Log.get("Main")

fun main(args: Array<String>) = mainBody {
    try {
        Arguments.init(ArgParser(args))
        handleArguments()
        Database
        initSlackLogger()
        DataHandler()
        startServer()
    } catch (t: Throwable) {
        log.error("Error: ${t.stackTraceString()}")
    }
    Log.flush()
}

private fun handleArguments() {
    Log.debug = Arguments.debug
    Log.verbose = Arguments.verbose
}

private fun initSlackLogger() {
    if (Arguments.development || Arguments.slackHook == null) {
        SlackAlertLogger.disable()
    } else {
        SlackAlertLogger.init(Arguments.slackHook!!)
    }
}

// This function cannot be private for reasons
internal fun Application.swarm() {
    install(DefaultHeaders)
    install(CallLogging)
    routing(Routes)
}

private fun startServer() {
    embeddedServer(Netty, Arguments.port, Arguments.host, module = Application::swarm).start()
    log.info("Server started on ${Arguments.host}:${Arguments.port}")
}
