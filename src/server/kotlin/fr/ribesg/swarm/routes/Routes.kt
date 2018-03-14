package fr.ribesg.swarm.routes

import fr.ribesg.swarm.routes.app.*
import io.ktor.content.*
import io.ktor.routing.Routing

val Routes: Routing.() -> Unit = {

    resource("/", "static/index.html")

    static("/assets/") {
        staticBasePackage = "static"
        resources("/")
    }

    DragonflyRoute.setup(this)

    HostsRoute.setup(this)
    CpuDataRoute.setup(this)
    RamDataRoute.setup(this)
    NetDataRoute.setup(this)
    DiskIoDataRoute.setup(this)
    DiskSpaceDataRoute.setup(this)

    HostRemoveRoute.setup(this)

}
