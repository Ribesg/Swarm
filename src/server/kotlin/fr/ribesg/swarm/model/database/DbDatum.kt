package fr.ribesg.swarm.model.database

data class DbDatum(
    val id: Int = -1,
    val type: DbDataType,
    val host: String,
    val date: Long,
    val cpuBusy: Int?,
    val cpuIo: Int?,
    val cpuSteal: Int?,
    val ramUsed: Long,
    val swapUsed: Long
)
