package fr.ribesg.swarm.model.database

data class DbDiskDatum(
    val id: Int = -1,
    val dataRef: Int,
    val device: String,
    val readUsage: Int?,
    val writeUsage: Int?,
    val readSpeed: Long?,
    val writeSpeed: Long?,
    val usedSpace: Long,
    val totalSpace: Long
)
