package org.slf4j.impl

import org.slf4j.ILoggerFactory

object LoggerFactory : ILoggerFactory {

    override fun getLogger(name: String?) = Logger(name)

}
