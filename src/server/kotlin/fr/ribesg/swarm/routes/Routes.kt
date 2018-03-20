package fr.ribesg.swarm.routes

import fr.ribesg.swarm.Config
import fr.ribesg.swarm.routes.app.*
import io.ktor.content.*
import io.ktor.routing.*

fun Route.setupRoutes(config: Config) {

    resource("/", "static/index.html")

    static("/assets/") {
        staticBasePackage = "static"
        resources("/")
    }

    setupDragonflyRoute(config)

    setupLoginRoute()
    setupLogoutRoute()

    route("/data") {
        requireLoggedIn()
        setupHostsRoute()
        setupCpuDataRoute()
        setupRamDataRoute()
        setupNetDataRoute()
        setupDiskIoDataRoute()
        setupDiskSpaceDataRoute()
    }

}
