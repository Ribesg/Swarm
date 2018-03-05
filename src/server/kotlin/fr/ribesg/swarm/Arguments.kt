package fr.ribesg.swarm

import com.xenomachina.argparser.*
import kotlin.properties.Delegates

object Arguments {

    private var arguments: ActualArguments by Delegates.notNull()

    val debug: Boolean by lazy { arguments.debug }

    val verbose: Boolean by lazy { arguments.verbose }

    val development: Boolean by lazy { arguments.development }

    val host: String by lazy { arguments.host }

    val port: Int by lazy { arguments.port }

    val key: String by lazy { arguments.key }

    val slackHook: String? by lazy {
        if (arguments.slackHook.isBlank()) null else arguments.slackHook
    }

    fun init(args: ArgParser) {
        arguments = ActualArguments(args)
    }

    private class ActualArguments(argParser: ArgParser) {

        val debug: Boolean by argParser
            .flagging("-d", "--debug", help = "Enable debug mode")

        val verbose: Boolean by argParser
            .flagging("-v", "--verbose", help = "Enable verbose mode")

        val development: Boolean by argParser
            .flagging("--dev", "--development", help = "Enable development mode")

        val host: String by argParser
            .storing("--host", help = "Local host to bind to")
            .default("0.0.0.0")

        val port: Int by argParser
            .storing("--port", help = "Local port to bind to", transform = String::toInt)
            .default(80)

        val key: String by argParser
            .storing("-k", "--key", help = "Key accepted from Dragonfly clients")

        val slackHook: String by argParser
            .storing("-s", "--slack-hook", help = "Slack WebHook used to send alerts")
            .default("")

    }

}
