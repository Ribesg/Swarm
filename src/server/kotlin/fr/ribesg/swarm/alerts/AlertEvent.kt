package fr.ribesg.swarm.alerts

data class AlertEvent(
    val alert: Alert,
    val type: AlertEventType
)
