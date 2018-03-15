package fr.ribesg.swarm.routes

import fr.ribesg.swarm.Config
import fr.ribesg.swarm.routes.app.*
import io.ktor.content.*
import io.ktor.routing.Route

fun Route.setupRoutes(config: Config) {

    resource("/", "static/index.html")

    static("/assets/") {
        staticBasePackage = "static"
        resources("/")
    }

    setupDragonflyRoute(config)

    setupHostsRoute()
    setupCpuDataRoute()
    setupRamDataRoute()
    setupNetDataRoute()
    setupDiskIoDataRoute()
    setupDiskSpaceDataRoute()

}
