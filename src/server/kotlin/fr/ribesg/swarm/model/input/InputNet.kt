package fr.ribesg.swarm.model.input

data class InputNet(
    val interfaces: List<InputNetInterface>
) {

    data class InputNetInterface(
        val name: String,
        val inBytes: Long?,
        val outBytes: Long?,
        val inPackets: Long?,
        val outPackets: Long?,
        val inErrors: Long?,
        val outErrors: Long?
    )

}
