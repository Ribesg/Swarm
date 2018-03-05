package fr.ribesg.swarm.routes.debug

import fr.ribesg.swarm.*
import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.database.tables.DataTable
import io.ktor.application.*
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.*
import org.jetbrains.exposed.sql.selectAll
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.*

object DebugDataRoute {

    private const val PATH = "/data/debug/data"

    private val log = Log.get(DebugDataRoute::class)

    fun setup(routeManager: Route) {
        routeManager.get(PATH) { handle(call) }
    }

    private suspend fun handle(call: ApplicationCall) {
        val data = Database.call { DataTable.selectAll().map(DataTable::rowToDatum) }
        val format = DateFormat.getInstance()
        format.timeZone = TimeZone.getTimeZone(ZoneOffset.UTC)
        val rows = data.map {
            ResultRow(
                it.id,
                it.type.name.toLowerCase(),
                it.host,
                format.format(Date(it.date)),
                it.cpuBusy?.let { "${it / 100.0}%" },
                it.cpuIo?.let { "${it / 100.0}%" },
                it.cpuSteal?.let { "${it / 100.0}%" },
                String.format("%d", it.ramUsed),
                String.format("%d", it.swapUsed)
            )
        }
        val result = Result(rows)
        call.respondText(jackson.writeValueAsString(result), ContentType.Application.Json)
    }

    private data class Result(
        val rows: List<ResultRow>
    )

    private data class ResultRow(
        val id: Int,
        val type: String,
        val host: String,
        val date: String,
        val cpuBusy: String?,
        val cpuIo: String?,
        val cpuSteal: String?,
        val ramUsed: String,
        val swapUsed: String
    )

}
