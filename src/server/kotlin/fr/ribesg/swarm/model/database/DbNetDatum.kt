package fr.ribesg.swarm.model.database

data class DbNetDatum(
    val id: Int = -1,
    val dataRef: Int,
    val interfaceName: String,
    val inBytes: Long?,
    val outBytes: Long?,
    val inPackets: Long?,
    val outPackets: Long?,
    val inErrors: Long?,
    val outErrors: Long?
)
