package fr.ribesg.swarm.routes

import com.fasterxml.jackson.module.kotlin.readValue
import fr.ribesg.swarm.*
import fr.ribesg.swarm.Config.Account
import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*

private val log = Log.get("Authentication")

private lateinit var accounts: List<Account>

fun Application.installAuthentication(config: Config) {
    accounts = config.accounts
}

private data class Credentials(
    val user: String,
    val password: String
)

fun Route.setupLoginRoute() =
    post("/login") {
        val data = call.receiveText()
        val (user, password) = jackson.readValue<Credentials>(data)
        val account = accounts.singleOrNull { it.user == user }
        if (account != null && account.password == password) {
            call.setSession(true, account.isAdmin)
            call.respond(HttpStatusCode.OK)
        } else {
            call.clearSession()
            call.respond(HttpStatusCode.Forbidden)
        }
    }

fun Route.setupLogoutRoute() =
    post("/logout") {
        call.clearSession()
        call.respond(HttpStatusCode.OK)
    }

fun Route.requireLoggedIn() =
    intercept(ApplicationCallPipeline.Infrastructure) {
        if (!call.getSession().isLoggedIn) {
            call.respond(HttpStatusCode.Unauthorized, "Login required")
            finish()
        }
    }

fun Route.requireAdmin() =
    intercept(ApplicationCallPipeline.Infrastructure) {
        val session = call.getSession()
        if (!session.isAdmin) {
            if (session.isLoggedIn) {
                call.respond(HttpStatusCode.Forbidden, "Admin privileges required")
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Login required")
            }
            finish()
        }
    }
