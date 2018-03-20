package fr.ribesg.swarm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.*
import com.xenomachina.argparser.*
import fr.ribesg.swarm.alerts.SlackAlertLogger
import fr.ribesg.swarm.data.DataHandler
import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.extensions.stackTraceString
import fr.ribesg.swarm.routes.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.event.Level
import java.nio.file.*

val jackson: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

fun main(args: Array<String>) =
    Swarm.init(args)

object Swarm {

    lateinit var arguments: Arguments
        private set

    lateinit var config: Config
        private set

    lateinit var database: Database
        private set

    private val log = Log.get("Swarm")

    private val ktorModule: Application.() -> Unit = {

        install(DefaultHeaders)
        install(CallLogging) {
            level = Level.valueOf(config.requestLoggingLevel)
        }
        installSessions(config)
        installAuthentication(config)

        routing {
            setupRoutes(config)
        }

    }

    fun init(args: Array<String>) = mainBody {
        try {
            parseArguments(args)
            if (parseConfig()) return@mainBody
            initDatabase()
            DataHandler.init(arguments, database)
            initSlackLogger()
            startServer()
        } catch (t: Throwable) {
            log.error("Error: ${t.stackTraceString()}")
        } finally {
            Log.flush()
        }
    }

    private fun parseArguments(args: Array<String>) {
        arguments = Arguments(ArgParser(args))
        Log.debug = arguments.debug
        Log.verbose = arguments.verbose
    }

    private fun parseConfig(): Boolean {
        val path = Paths.get("config.yml")
        return if (Files.notExists(path)) {
            val resConfig = Config::class.java.getResourceAsStream("/config.yml")
            Files.copy(resConfig, path)
            log.info("Configuration file created, please edit it")
            true
        } else {
            val raw = path.toFile().readText()
            val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
            config = mapper.readValue(raw, Config::class.java).validated(arguments)
            log.info("Configuration loaded")
            false
        }
    }

    private fun initDatabase() {
        database = Database(arguments)
    }

    private fun initSlackLogger() {
        if (arguments.development || config.slackWebHook.isBlank()) {
            SlackAlertLogger.disable()
        } else {
            SlackAlertLogger.init(config.slackWebHook)
        }
    }

    private fun startServer() {
        embeddedServer(Netty, config.port, config.host, module = ktorModule).start()
        log.info("Server started on ${config.host}:${config.port}")
    }

}
