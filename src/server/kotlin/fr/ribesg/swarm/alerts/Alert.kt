package fr.ribesg.swarm.alerts

data class Alert(
    val host: String,
    val date: Long,
    val type: AlertType,
    val level: AlertLevel,
    val message: String
)
