package fr.ribesg.swarm

import com.xenomachina.argparser.ArgParser

class Arguments(argParser: ArgParser) {

    val debug: Boolean by argParser
        .flagging("-d", "--debug", help = "Enable debug mode")

    val verbose: Boolean by argParser
        .flagging("-v", "--verbose", help = "Enable verbose mode")

    val development: Boolean by argParser
        .flagging("--dev", "--development", help = "Enable development mode")

}
