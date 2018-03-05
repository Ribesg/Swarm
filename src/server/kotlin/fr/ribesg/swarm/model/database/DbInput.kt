package fr.ribesg.swarm.model.database

data class DbInput(
    val host: DbHost,
    val datum: DbDatum,
    val net: List<DbNetDatum>,
    val disk: List<DbDiskDatum>
)
