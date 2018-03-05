package fr.ribesg.swarm.model.database

data class DbHost(
    val host: String,
    val ramTotal: Long,
    val swapTotal: Long
)
