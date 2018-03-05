@file:Suppress("NOTHING_TO_INLINE")

package fr.ribesg.swarm.extensions

import java.io.*

inline fun Throwable.stackTraceString(): String =
    StringWriter().use { sw ->
        PrintWriter(sw).use { pw ->
            printStackTrace(pw)
            sw.toString()
        }
    }
