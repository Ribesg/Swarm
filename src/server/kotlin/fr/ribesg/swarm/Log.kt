package fr.ribesg.swarm

import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import kotlin.reflect.KClass


/**
 * A Logger.
 */
class Log private constructor(name: String) {

    companion object {

        /**
         * Holds all loggers
         */
        private val loggers = HashMap<String, Log>()

        /**
         * Globally set debug flag for all loggers
         */
        var debug = false
            set(value) {
                field = value
                loggers.values.forEach { it.debug = field }
            }

        /**
         * Globally set verbose flag for all loggers
         */
        var verbose = false
            set(value) {
                field = value
                loggers.values.forEach { it.verbose = field }
            }

        /**
         * Gets a logger or create it if it does not exists yet.
         *
         * @param clazz the class the logger logs from
         * @param suffix an optional suffix which should be added to the logger's name
         *
         * @return a logger
         */
        fun get(clazz: KClass<*>, suffix: String = ""): Log {
            requireNotNull(clazz.qualifiedName) { "Invalid parameter clazz: no local or anonymous class allowed" }
            return get(clazz.qualifiedName!! + suffix)
        }

        /**
         * Gets a logger or create it if it does not exists yet.
         *
         * Please use [Log.get] when possible.
         *
         * @param name the name of the logger
         *
         * @return a logger
         */
        fun get(name: String): Log = name.takeLast(20).let { loggers.getOrPut(it) { Log(it) } }

        /**
         * Flushes all loggers.
         */
        fun flush() {
            loggers.values.forEach(Log::flush)
        }

    }

    /**
     * The Java logger
     */
    private val logger: Logger

    /**
     * Debug mode of the logger. Can be changed at any time.
     */
    private var debug = Companion.debug
        set(value) {
            field = value
            val level = if (verbose) Level.FINEST else if (field) Level.FINE else Level.INFO
            logger.level = level
        }

    /**
     * Verbose mode of the logger. Can be changed at any time.
     */
    private var verbose = Companion.verbose
        set(value) {
            field = value
            val level = if (field) Level.FINEST else if (debug) Level.FINE else Level.INFO
            logger.level = level
        }

    init {
        logger = Logger.getLogger(name)
        logger.useParentHandlers = false
        for (h in logger.handlers) {
            logger.removeHandler(h)
        }
        logger.level = if (verbose) Level.FINEST else if (debug) Level.FINE else Level.INFO
        logger.addHandler(object : ConsoleHandler() {
            init {
                formatter = object : SimpleFormatter() {

                    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                    private val date = Date()

                    override fun format(record: LogRecord): String {
                        date.time = record.millis
                        return StringBuilder().apply {
                            append(dateFormat.format(date))
                            if (record.loggerName.length >= 20) {
                                append(" [..${record.loggerName.takeLast(18)}]")
                            } else {
                                append(" [${record.loggerName.padStart(20)}]")
                            }
                            append(" ${record.level.toString().padEnd(7)}:")
                            append(" ${record.message}\n")
                            if (record.thrown != null) {
                                append("${record.thrown.asString()}\n")
                            }
                        }.toString()
                    }
                }
                setOutputStream(System.out)
                level = Level.ALL
            }
        })
    }

    /**
     * Prints a verbose message with an optional associated stacktrace.
     *
     * @param message the message
     * @param t the exception, if any
     */
    fun verbose(message: String, t: Throwable? = null) {
        if (verbose) {
            logger.log(Level.FINEST, message, t)
        }
    }

    /**
     * Prints a debug message with an optional associated stacktrace.
     *
     * @param message the message
     * @param t the exception, if any
     */
    fun debug(message: String, t: Throwable? = null) {
        if (verbose || debug) {
            logger.log(Level.FINE, message, t)
        }
    }

    /**
     * Prints an info message with an optional associated stacktrace.
     *
     * @param message the message
     * @param t the exception, if any
     */
    fun info(message: String, t: Throwable? = null) {
        logger.log(Level.INFO, message, t)
    }

    /**
     * Prints a warn message with an optional associated stacktrace.
     *
     * @param message the message
     * @param t the exception, if any
     */
    fun warn(message: String, t: Throwable? = null) {
        logger.log(Level.WARNING, message, t)
    }

    /**
     * Prints an error message with an optional associated stacktrace.
     *
     * @param message the message
     * @param t the exception, if any
     */
    fun error(message: String, t: Throwable? = null) {
        logger.log(Level.SEVERE, message, t)
        flush()
    }

    /**
     * Flushes this logger.
     */
    fun flush() {
        logger.handlers.forEach(Handler::flush)
    }

    /**
     * Converts a Throwable into a String.
     */
    private fun Throwable.asString(): String =
        StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                printStackTrace(pw)
                sw.toString()
            }
        }

}
