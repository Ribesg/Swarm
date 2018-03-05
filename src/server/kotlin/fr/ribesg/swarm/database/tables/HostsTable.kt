package fr.ribesg.swarm.database.tables

import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.database.Database.exec
import fr.ribesg.swarm.extensions.runRawSql
import fr.ribesg.swarm.model.database.DbHost
import org.jetbrains.exposed.sql.*

/**
 * Holds the list of all known hosts and some metadata.
 */
internal object HostsTable : Table() {

    /**
     * The hostname
     */
    val HOST = varchar("HOST", 255).primaryKey()

    /**
     * The total RAM available on this server
     */
    val RAM_TOTAL = long("RAM_TOTAL")

    /**
     * The total SWAP available on this server
     */
    val SWAP_TOTAL = long("SWAP_TOTAL")

    /**
     * Updates the provided host, inserts it if it is not known yet.
     *
     * @param datum the host
     */
    fun upsert(datum: DbHost) = exec {
        val table = nameInDatabaseCase()
        val host = datum.host
        val ramTotal = datum.ramTotal
        val swapTotal = datum.swapTotal
        // language=SQL
        runRawSql("MERGE INTO $table VALUES ('$host', $ramTotal, $swapTotal);")
    }

    /**
     * Gets a list of all hostnames.
     *
     * @return a list of all hostnames
     */
    fun getHostNames(): List<String> {
        Database.checkInTransaction()
        return selectAll().map { it[HOST] }
    }

    /**
     * Gets a list of all hosts.
     *
     * @return a list of all hosts
     */
    fun getHosts(): List<DbHost> {
        Database.checkInTransaction()
        return selectAll().map(this::rowToHost)
    }

    /**
     * Gets a host matching the provided hostname.
     *
     * @return a host matching the provided hostname, if any, null otherwise
     */
    fun getHost(hostname: String): DbHost? {
        Database.checkInTransaction()
        return select { HOST eq hostname }.firstOrNull()?.let(this::rowToHost)
    }

    /**
     * Setups the constraints stating that both [RAM_TOTAL] and [SWAP_TOTAL] needs to be positive.
     */
    // language=SQL
    fun setupConstraints() = exec {
        val hostsTable = nameInDatabaseCase()
        runRawSql("""

            ALTER TABLE $hostsTable
            ADD CONSTRAINT ${hostsTable}_${RAM_TOTAL.name}_POSITIVE
                CHECK ${RAM_TOTAL.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $hostsTable
            ADD CONSTRAINT ${hostsTable}_${SWAP_TOTAL.name}_POSITIVE
                CHECK ${SWAP_TOTAL.name} >= 0;

        """)
    }

    /**
     * Creates a Host from a ResultRow.
     *
     * @param row the result row to convert
     *
     * @return a Host from the provided ResultRow
     */
    internal fun rowToHost(row: ResultRow): DbHost =
        DbHost(
            row[HOST],
            row[RAM_TOTAL],
            row[SWAP_TOTAL]
        )

}
