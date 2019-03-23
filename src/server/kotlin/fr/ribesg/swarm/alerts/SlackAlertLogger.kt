package fr.ribesg.swarm.alerts

import `in`.ashwanthkumar.slack.webhook.Slack
import `in`.ashwanthkumar.slack.webhook.SlackAttachment
import fr.ribesg.swarm.Log
import fr.ribesg.swarm.alerts.AlertEventType.*
import fr.ribesg.swarm.alerts.AlertLevel.*
import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS
import java.util.HashMap
import kotlin.Comparator

/**
 * A logger logging alert events to a Slack channel.
 */
object SlackAlertLogger {

    /**
     * Logger used for errors while logging
     */
    private val log = Log.get(SlackAlertLogger::class)

    /**
     * If the logger has been initialized
     */
    private var initialized = false

    /**
     * If the logger has been disabled
     */
    private var disabled = false

    /**
     * The Slack instance
     */
    private lateinit var slack: Slack

    /**
     * If an alert is currently logged on Slack, the date at which it was logged
     */
    private val loggedAlerts: MutableMap<String, MutableMap<AlertType, Long>> = HashMap()

    /**
     * Initializes the logger.
     *
     * @param webHookUrl the WebHook URL
     */
    fun init(webHookUrl: String) {
        slack = Slack(webHookUrl)
        initialized = true
    }

    /**
     * Disables the logger.
     */
    fun disable() {
        initialized = true
        disabled = true
    }

    fun logHostRemoval(host: String) {
        check(initialized) { "SlackLogger isn't initialized" }
        if (disabled) return
        try {
            slack.push(SlackAttachment("Removed host '$host' and all associated data").color("good"))
        } catch (t: Throwable) {
            log.error("Failed to log to Slack", t)
        }
    }

    fun log(alerts: List<AlertEvent>) {
        require(alerts.isNotEmpty()) { "Cannot log an empty list of alert events" }
        check(initialized) { "SlackLogger isn't initialized" }
        if (disabled) return
        try {
            val attachments = alerts
                .mapNotNull { alertEvent ->
                    when (alertEvent.type) {
                        ESCALATED    -> AlertEvent(alertEvent.alert, STARTED)
                        STARTED      ->
                            when (alertEvent.alert.level) {
                                CRITICAL -> AlertEvent(alertEvent.alert, STARTED)
                                WARNING  -> null
                                NONE     -> null
                            }
                        DE_ESCALATED -> null
                        ENDED        -> alertEvent
                    }
                }
                .filter { alertEvent ->
                    val host = alertEvent.alert.host
                    val type = alertEvent.alert.type
                    val current = loggedAlerts[host]?.get(type)
                    when (alertEvent.type) {
                        STARTED -> {
                            loggedAlerts.getOrPut(host) { HashMap() }[type] = Instant.now().toEpochMilli()
                            if (type == AlertType.NOT_REPORTING) {
                                current == null || current < Instant.now().minus(1, HOURS).toEpochMilli()
                            } else {
                                current == null
                            }
                        }

                        ENDED   -> {
                            loggedAlerts[host]!!.remove(type)
                            current != null
                        }

                        else    -> {
                            // Not possible, should have been transformed by the mapNotNull
                            throw Error()
                        }
                    }
                }
                .sortedWith(Comparator { a, b ->
                    val event = a.type.compareTo(b.type)
                    if (event != 0) return@Comparator event
                    val level = a.alert.level.compareTo(b.alert.level)
                    if (level != 0) return@Comparator level
                    val type = a.alert.type.compareTo(b.alert.type)
                    if (type != 0) return@Comparator type
                    return@Comparator a.alert.host.compareTo(b.alert.host)
                })
                .map { event ->
                    val alert = event.alert
                    val text = eventTypeToText(event.type, alert.host) + alert.message
                    val fallback = text.replace("*", "")
                    val color = alertEventToColor(event.type, alert.level)
                    SlackAttachment(text)
                        .addMarkdownIn("text")
                        .color(color)
                        .fallback(fallback)
                }
            if (attachments.isNotEmpty()) {
                slack.push(attachments)
            }
        } catch (t: Throwable) {
            log.error("Failed to log to Slack", t)
        }
    }

    private fun alertEventToColor(type: AlertEventType, level: AlertLevel): String =
        when (type) {
            ESCALATED    -> "danger"
            STARTED      -> when (level) {
                CRITICAL -> "danger"
                WARNING  -> "warning"
                else     -> throw Error()
            }
            DE_ESCALATED -> "warning"
            ENDED        -> "good"
        }

    private fun eventTypeToText(type: AlertEventType, host: String): String =
        when (type) {
            ESCALATED    -> "*$host* | Escalated Alert: "
            STARTED      -> "*$host* | New Alert: "
            DE_ESCALATED -> "*$host* | De-Escalated Alert: "
            ENDED        -> "*$host* | Closed Alert: "
        }

}
