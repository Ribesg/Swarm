package fr.ribesg.swarm.model.input

data class InputRam(
    val ramUsed: Long,
    val swapUsed: Long,
    val ramTotal: Long,
    val swapTotal: Long
)
