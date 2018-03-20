package fr.ribesg.swarm.routes

import fr.ribesg.swarm.Config
import io.ktor.application.*
import io.ktor.sessions.*
import java.time.Duration

data class Session(
    val isLoggedIn: Boolean,
    val isAdmin: Boolean
)

private val defaultSession = Session(false, false)

fun Application.installSessions(config: Config) =

    install(Sessions) {
        cookie<Session>("swarm") {
            cookie.path = "/"
            cookie.duration = Duration.ofDays(7)
            transform(SessionTransportTransformerMessageAuthentication(config.sessionSecret.toByteArray(), "HmacSHA256"))
        }
    }

fun ApplicationCall.getSession(): Session =
    sessions.get<Session>() ?: defaultSession

fun ApplicationCall.setSession(isLoggedIn: Boolean, isAdmin: Boolean) =
    sessions.set(Session(isLoggedIn, isAdmin))

fun ApplicationCall.clearSession() =
    sessions.clear<Session>()
