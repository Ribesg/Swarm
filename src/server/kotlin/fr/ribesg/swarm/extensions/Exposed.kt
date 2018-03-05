@file:Suppress("NOTHING_TO_INLINE")

package fr.ribesg.swarm.extensions

import org.jetbrains.exposed.sql.Transaction

/**
 * A regex matching one or more space, line break or carriage return.
 */
private val rawSqlRegex = "[ \n\r]+".toRegex()

/**
 * Runs the provided, potentially multi-line, raw SQL query.
 *
 * @param sql the raw sql query, can be multi-line
 */
fun Transaction.runRawSql(sql: String) {
    exec(sql.replace(rawSqlRegex, " ").trim())
}
