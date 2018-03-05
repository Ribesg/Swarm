package fr.ribesg.swarm.model.input

data class InputDatum(
    val host: String,
    val date: Long,
    val cpu: InputCpu,
    val ram: InputRam,
    val net: InputNet,
    val disk: InputDisk
)
