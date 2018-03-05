package org.slf4j.impl

import org.slf4j.spi.LoggerFactoryBinder

@Suppress("UNUSED")
class StaticLoggerBinder : LoggerFactoryBinder {

    companion object {

        private val SINGLETON = StaticLoggerBinder()

        @JvmStatic
        fun getSingleton() = SINGLETON

        @JvmField
        var REQUESTED_API_VERSION = "1.7.13"

    }

    override fun getLoggerFactory() = LoggerFactory

    override fun getLoggerFactoryClassStr(): String = LoggerFactory::class.java.name

}
