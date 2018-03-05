package org.slf4j.impl

import fr.ribesg.swarm.Log
import org.slf4j.helpers.*

class Logger(name: String?) : MarkerIgnoringBase() {

    private val log = Log.get(name?.let { "SLF4J - $name" } ?: "SLF4J")

    override fun isTraceEnabled() = Log.verbose

    override fun isDebugEnabled() = Log.debug

    override fun isInfoEnabled() = true

    override fun isWarnEnabled() = true

    override fun isErrorEnabled() = true

    override fun trace(msg: String?) =
        log.verbose("[SLF4J] " + (msg ?: "null"))

    override fun trace(msg: String?, t: Throwable?) =
        log.verbose("[SLF4J] " + (msg ?: "null"), t)

    override fun trace(format: String?, arg: Any?) {
        val tuple = MessageFormatter.format(format, arg)
        log.verbose("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        val tuple = MessageFormatter.arrayFormat(format, arguments)
        log.verbose("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        val tuple = MessageFormatter.format(format, arg1, arg2)
        log.verbose("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun debug(msg: String?) =
        log.debug("[SLF4J] " + (msg ?: "null"))

    override fun debug(msg: String?, t: Throwable?) =
        log.debug("[SLF4J] " + (msg ?: "null"), t)

    override fun debug(format: String?, arg: Any?) {
        val tuple = MessageFormatter.format(format, arg)
        log.debug("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        val tuple = MessageFormatter.arrayFormat(format, arguments)
        log.debug("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        val tuple = MessageFormatter.format(format, arg1, arg2)
        log.debug("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun info(msg: String?) =
        log.info("[SLF4J] " + (msg ?: "null"))

    override fun info(msg: String?, t: Throwable?) =
        log.info("[SLF4J] " + (msg ?: "null"), t)

    override fun info(format: String?, arg: Any?) {
        val tuple = MessageFormatter.format(format, arg)
        log.info("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        val tuple = MessageFormatter.arrayFormat(format, arguments)
        log.info("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        val tuple = MessageFormatter.format(format, arg1, arg2)
        log.info("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun warn(msg: String?) =
        log.warn("[SLF4J] " + (msg ?: "null"))

    override fun warn(msg: String?, t: Throwable?) =
        log.warn("[SLF4J] " + (msg ?: "null"), t)

    override fun warn(format: String?, arg: Any?) {
        val tuple = MessageFormatter.format(format, arg)
        log.warn("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        val tuple = MessageFormatter.arrayFormat(format, arguments)
        log.warn("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        val tuple = MessageFormatter.format(format, arg1, arg2)
        log.warn("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun error(msg: String?) =
        log.error("[SLF4J] " + (msg ?: "null"))

    override fun error(msg: String?, t: Throwable?) =
        log.error("[SLF4J] " + (msg ?: "null"), t)

    override fun error(format: String?, arg: Any?) {
        val tuple = MessageFormatter.format(format, arg)
        log.error("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        val tuple = MessageFormatter.arrayFormat(format, arguments)
        log.error("[SLF4J] " + tuple.message, tuple.throwable)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        val tuple = MessageFormatter.format(format, arg1, arg2)
        log.error("[SLF4J] " + tuple.message, tuple.throwable)
    }

}
