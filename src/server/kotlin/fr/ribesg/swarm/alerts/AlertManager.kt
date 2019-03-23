package fr.ribesg.swarm.alerts

import fr.ribesg.swarm.Log
import fr.ribesg.swarm.alerts.AlertEventType.*
import fr.ribesg.swarm.alerts.AlertLevel.*
import fr.ribesg.swarm.alerts.AlertType.*
import fr.ribesg.swarm.data.DataHandler
import fr.ribesg.swarm.model.database.DbDatum
import fr.ribesg.swarm.model.output.OutputAlertData
import java.time.*
import java.util.*
import java.util.concurrent.Executors

object AlertManager {

    private val log = Log.get(AlertManager::class)

    private val executor = Executors.newSingleThreadExecutor()

    private val currentAlerts: MutableMap<String, MutableMap<AlertType, Alert>> = HashMap()

    private val alertEvents: MutableList<AlertEvent> = LinkedList()

    fun run(currentMinute: Instant) {
        executor.submit {
            Thread.sleep(1)
            runTask(currentMinute)
        }
    }

    private fun runTask(currentMinute: Instant) {
        try {
            log.info("Checking for alerts...")
            val fiveMinutesAgo = currentMinute - Duration.ofMinutes(5)
            val data = DataHandler().getAlertData(fiveMinutesAgo.toEpochMilli(), currentMinute.toEpochMilli())
            data.forEach { hostData -> runForHost(currentMinute, hostData) }
            if (alertEvents.isNotEmpty()) {
                log.info("Logging ${alertEvents.size} alert events to Slack")
                SlackAlertLogger.log(alertEvents)
            } else {
                log.info("No alert event")
            }
            alertEvents.clear()
        } catch (t: Throwable) {
            log.error("Error in Alert Manager", t)
        }
    }

    private fun runForHost(currentMinute: Instant, hostData: OutputAlertData) {
        checkNotReporting(currentMinute, hostData)
        checkCpuLoad(currentMinute, hostData)
        checkRamUsage(hostData)
        checkDiskUsage(currentMinute, hostData)
    }

    private fun getCurrentAlert(host: String, type: AlertType): Alert? =
        currentAlerts.getOrPut(host) { HashMap() }[type]

    private fun registerAlert(host: String, type: AlertType, alert: Alert, replace: Boolean) {
        val hostMap = currentAlerts.getOrPut(host) { HashMap() }
        if (replace) {
            hostMap[type] = alert
        } else {
            hostMap.putIfAbsent(type, alert)
        }
    }

    private fun removeAlert(host: String, type: AlertType) {
        currentAlerts
            .getOrDefault(host, mutableMapOf())
            .remove(type)
    }

    private fun checkNotReporting(currentMinute: Instant, hostData: OutputAlertData) {
        val host = hostData.host.host
        var date: Long = currentMinute.toEpochMilli()
        var alertLevel: AlertLevel = NONE
        val now = Instant.now().toEpochMilli()
        val thirtySecondsAgo = now - Duration.ofSeconds(30).toMillis()
        val oneMinuteAgo = now - Duration.ofMinutes(1).toMillis()
        val fiveMinutesAgo = now - Duration.ofMinutes(5).toMillis()
        val mostRecentLiveDatumDate = hostData.mostRecentLiveData[host]?.date
        if (mostRecentLiveDatumDate == null || mostRecentLiveDatumDate < oneMinuteAgo) {
            date = mostRecentLiveDatumDate ?: fiveMinutesAgo
            alertLevel = CRITICAL
        } else if (mostRecentLiveDatumDate < thirtySecondsAgo) {
            date = mostRecentLiveDatumDate
            alertLevel = WARNING
        }
        val message =
            when {
                alertLevel == NONE     -> "Report received ${(now - date) / 1000} seconds ago"
                date == fiveMinutesAgo -> "No reports received in the last 5 minutes"
                else                   -> "No reports received in the last ${(now - date) / 1000} seconds"
            }
        pushEventIfNeeded(host, date, NOT_REPORTING, alertLevel, message)
    }

    private fun checkCpuLoad(currentMinute: Instant, hostData: OutputAlertData) {
        if (hostData.data.isEmpty()) return
        if (hostData.data.all { it.cpuBusy == null }) return
        val host = hostData.host.host
        val lastTwoMinutesCpuLoad = hostData.data.takeLast(2).map {
            (it.cpuBusy ?: 0) + (it.cpuIo ?: 0) + (it.cpuSteal ?: 0)
        }.sum() / 2
        val lastFiveMinutesCpuLoad = hostData.data.filter { it.cpuBusy != null }.map {
            it.cpuBusy!! + it.cpuIo!! + it.cpuSteal!!
        }.average()
        val date: Long
        val alertLevel: AlertLevel?
        when {

            lastFiveMinutesCpuLoad > 9900 -> {
                date = hostData.data.first().date
                alertLevel = CRITICAL
            }

            lastTwoMinutesCpuLoad > 9990  -> {
                date = hostData.data.takeLast(2).first().date
                alertLevel = WARNING
            }

            lastFiveMinutesCpuLoad < 7000 -> {
                date = currentMinute.toEpochMilli()
                alertLevel = NONE
            }

            else                          -> {
                date = currentMinute.toEpochMilli()
                alertLevel = null
            }

        }
        val message =
            when (alertLevel) {
                CRITICAL -> "*%.1f%%* CPU load over 5 minutes".format(lastTwoMinutesCpuLoad / 100.0)
                WARNING  -> "*%.1f%%* CPU load over 2 minutes".format(lastFiveMinutesCpuLoad / 100.0)
                NONE     -> "CPU load back to *%.1f%%* over 5 minutes".format(lastFiveMinutesCpuLoad / 100.0)
                else     -> null
            }
        pushEventIfNeeded(host, date, HIGH_CPU_LOAD, alertLevel, message)
    }

    private fun checkRamUsage(hostData: OutputAlertData) {
        if (hostData.data.isEmpty()) return
        val host = hostData.host.host
        val date = hostData.data.first().date
        val lastFiveMinutesRamUsed = hostData.data.mapNotNull(DbDatum::ramUsed).average() / hostData.host.ramTotal
        val alertLevel =
            when {
                lastFiveMinutesRamUsed > 95 -> CRITICAL
                lastFiveMinutesRamUsed > 90 -> WARNING
                lastFiveMinutesRamUsed < 70 -> NONE
                else                        -> null
            }
        val message =
            when (alertLevel) {
                CRITICAL -> "*%.1f%%* RAM usage over 5 minutes".format(lastFiveMinutesRamUsed)
                WARNING  -> "*%.1f%%* RAM usage over 5 minutes".format(lastFiveMinutesRamUsed)
                NONE     -> "RAM usage back to *%.1f%%* over 5 minutes".format(lastFiveMinutesRamUsed)
                else     -> null
            }
        pushEventIfNeeded(host, date, HIGH_RAM_USAGE, alertLevel, message)
    }

    private fun checkDiskUsage(currentMinute: Instant, hostData: OutputAlertData) {
        if (hostData.diskData.isEmpty()) return
        val host = hostData.host.host
        val data = hostData.diskData
            .groupBy { it.dataRef }
            .map { (dataRef, diskData) ->
                Pair(
                    hostData.data.first { it.id == dataRef }.date,
                    diskData.map { (it.readUsage ?: 0) + (it.writeUsage ?: 0) }.average()
                )
            }
            .sortedBy { it.first }
        val lastTwoMinutesUsage = data.takeLast(2).map { it.second }.average()
        val lastFiveMinutesUsage = data.map { it.second }.average()
        val date: Long?
        val alertLevel: AlertLevel?
        when {

            lastTwoMinutesUsage > 9750  -> {
                date = data.takeLast(2).first().first
                alertLevel = CRITICAL
            }

            lastFiveMinutesUsage > 9000 -> {
                date = data.first().first
                alertLevel = WARNING
            }

            lastFiveMinutesUsage < 7000 -> {
                date = currentMinute.toEpochMilli()
                alertLevel = NONE
            }

            else                        -> {
                date = currentMinute.toEpochMilli()
                alertLevel = null
            }

        }
        val message =
            when (alertLevel) {
                CRITICAL -> "*%.1f%%* disk usage over 2 minutes".format(lastTwoMinutesUsage / 100.0)
                WARNING  -> "*%.1f%%* disk usage over 5 minutes".format(lastFiveMinutesUsage / 100.0)
                NONE     -> "Disk usage back to *%.1f%%* over 5 minutes".format(lastFiveMinutesUsage / 100.0)
                else     -> null
            }
        pushEventIfNeeded(host, date, HIGH_DISK_USAGE, alertLevel, message)
    }

    private fun pushEventIfNeeded(host: String, date: Long, type: AlertType, newAlertLevel: AlertLevel?, message: String?) {
        val ignored = listOf(NONE, null)
        val currentAlert = getCurrentAlert(host, type)
        val currentAlertLevel = currentAlert?.level
        if (currentAlertLevel in ignored && newAlertLevel in ignored) return
        if (currentAlertLevel in ignored) {
            // newAlertLevel is CRITICAL or WARNING
            val alert = Alert(host, date, type, newAlertLevel!!, message!!)
            registerAlert(host, type, alert, false)
            alertEvents.add(AlertEvent(alert, STARTED))
        }
        if (currentAlertLevel == WARNING && newAlertLevel == CRITICAL) {
            val alert = Alert(host, currentAlert.date, type, newAlertLevel, message!!)
            registerAlert(host, type, alert, true)
            alertEvents.add(AlertEvent(alert, ESCALATED))
        }
        if (currentAlertLevel == CRITICAL && newAlertLevel == WARNING) {
            val alert = Alert(host, currentAlert.date, type, newAlertLevel, message!!)
            registerAlert(host, type, alert, true)
            alertEvents.add(AlertEvent(alert, DE_ESCALATED))
        }
        if (newAlertLevel == NONE) {
            // currentAlertLevel is CRITICAL or WARNING
            removeAlert(host, type)
            alertEvents.add(AlertEvent(Alert(host, currentAlert!!.date, type, currentAlertLevel!!, message!!), ENDED))
        }
    }

}
