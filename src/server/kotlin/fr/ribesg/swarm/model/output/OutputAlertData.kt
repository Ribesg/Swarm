package fr.ribesg.swarm.model.output

import fr.ribesg.swarm.model.database.*

data class OutputAlertData(
    val host: DbHost,
    val data: List<DbDatum>,
    val diskData: List<DbDiskDatum>,
    val mostRecentLiveData: Map<String, DbDatum>
)
