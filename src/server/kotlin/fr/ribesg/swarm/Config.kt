package fr.ribesg.swarm

import org.slf4j.event.Level

data class Config(
    val host: String,
    val port: Int,
    val key: String,
    val requestLoggingLevel: String,
    val slackWebHook: String,
    val sessionSecret: String,
    val accounts: List<Account>
) {

    fun validated(arguments: Arguments): Config {
        require(host.isNotBlank()) { "host cannot be blank" }
        require(port in 1..65535) { "port needs to be in range 1..65535" }
        if (!arguments.development) {
            require(key.isNotBlank()) { "key cannot be blank" }
        }
        require(requestLoggingLevel in Level.values().map(Level::toString)) {
            "requestLoggingLevel should be one of ${Level.values().contentToString()}"
        }
        require(sessionSecret.isNotBlank()) { "sessionSecret cannot be blank" }
        require(accounts.isNotEmpty()) { "accounts cannot be empty" }
        return this
    }

    data class Account(
        val user: String,
        val password: String,
        val isAdmin: Boolean
    )

}
