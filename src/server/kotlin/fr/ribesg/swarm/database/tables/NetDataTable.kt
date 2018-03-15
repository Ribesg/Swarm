package fr.ribesg.swarm.database.tables

import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.database.Database.Companion.exec
import fr.ribesg.swarm.extensions.runRawSql
import fr.ribesg.swarm.model.database.DbNetDatum
import org.jetbrains.exposed.sql.*

/**
 * Holds both live and archived network data points for each host.
 */
internal object NetDataTable : Table() {

    /**
     * The id
     */
    val ID = integer("ID").autoIncrement().primaryKey()

    /**
     * Reference to a row of [DataTable]
     */
    val DATA_REF = integer("DATA_REF").references(DataTable.ID, ReferenceOption.CASCADE)

    /**
     * Name of the interface (limited to 15 bytes on linux)
     */
    val INTERFACE = varchar("INTERFACE", 16)

    /**
     * Incoming bytes per 100 seconds
     */
    val IN_BYTES = long("IN_BYTES").nullable()

    /**
     * Outgoing bytes per 100 seconds
     */
    val OUT_BYTES = long("OUT_BYTES").nullable()

    /**
     * Incoming packets per 100 seconds
     */
    val IN_PACKETS = long("IN_PACKETS").nullable()

    /**
     * Outgoing packets per 100 seconds
     */
    val OUT_PACKETS = long("OUT_PACKETS").nullable()

    /**
     * Incoming errors per 100 seconds
     */
    val IN_ERRORS = long("IN_ERRORS").nullable()

    /**
     * Outgoing errors per 100 seconds
     */
    val OUT_ERRORS = long("OUT_ERRORS").nullable()

    /**
     * Inserts the provided data into the table.
     *
     * @param data the data to insert
     *
     * @return the ids of the inserted data
     */
    fun insert(data: Iterable<DbNetDatum>): List<Int> {
        Database.checkInTransaction()
        return batchInsert(data) { datum ->
            this[DATA_REF] = datum.dataRef
            this[INTERFACE] = datum.interfaceName
            this[IN_BYTES] = datum.inBytes
            this[OUT_BYTES] = datum.outBytes
            this[IN_PACKETS] = datum.inPackets
            this[OUT_PACKETS] = datum.outPackets
            this[IN_ERRORS] = datum.inErrors
            this[OUT_ERRORS] = datum.outErrors
        }.map {
            it.values.first() as Int
        }
    }

    /**
     * Gets all data matching the provided [DataTable] id.
     *
     * @param dataRef the id of the matching row in [DataTable]
     *
     * @return all data matching the provided [DataTable] id
     */
    fun getData(dataRef: Int): List<DbNetDatum> {
        Database.checkInTransaction()
        return select { DATA_REF eq dataRef }.map(this::rowToNetDatum)
    }

    /**
     * Gets all data matching the provided [DataTable] ids.
     *
     * @param dataRefs the ids of the matching rows in [DataTable]
     *
     * @return all data matching the provided [DataTable] ids
     */
    fun getData(dataRefs: Iterable<Int>): Map<Int, List<DbNetDatum>> {
        Database.checkInTransaction()
        return select { DATA_REF inList dataRefs }.map(this::rowToNetDatum).groupBy(DbNetDatum::dataRef)
    }

    /**
     * Setups the table's index and constraints.
     */
    fun setupIndexAndConstraints() {
        setupIndex()
        setupConstraints()
    }

    /**
     * Setups the table's index.
     */
    private fun setupIndex() {
        Database.checkInTransaction()
        SchemaUtils.createIndex(arrayOf(DATA_REF, INTERFACE), true)
    }

    /**
     * Setups constraints on per thousand values and other positive integers.
     */
    // language=SQL
    private fun setupConstraints() = exec {
        val netDataTable = nameInDatabaseCase()
        runRawSql("""

            ALTER TABLE $netDataTable
            ADD CONSTRAINT ${netDataTable}_${IN_BYTES.name}_POSITIVE
                CHECK ${IN_BYTES.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $netDataTable
            ADD CONSTRAINT ${netDataTable}_${OUT_BYTES.name}_POSITIVE
                CHECK ${OUT_BYTES.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $netDataTable
            ADD CONSTRAINT ${netDataTable}_${IN_PACKETS.name}_POSITIVE
                CHECK ${IN_PACKETS.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $netDataTable
            ADD CONSTRAINT ${netDataTable}_${OUT_PACKETS.name}_POSITIVE
                CHECK ${OUT_PACKETS.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $netDataTable
            ADD CONSTRAINT ${netDataTable}_${IN_ERRORS.name}_POSITIVE
                CHECK ${IN_ERRORS.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $netDataTable
            ADD CONSTRAINT ${netDataTable}_${OUT_ERRORS.name}_POSITIVE
                CHECK ${OUT_ERRORS.name} >= 0;

        """)
    }

    /**
     * Creates a NetDatum from a ResultRow.
     *
     * @param row the result row to convert
     *
     * @return a NetDatum from the provided ResultRow
     */
    internal fun rowToNetDatum(row: ResultRow): DbNetDatum =
        DbNetDatum(
            row[ID],
            row[DATA_REF],
            row[INTERFACE],
            row[IN_BYTES],
            row[OUT_BYTES],
            row[IN_PACKETS],
            row[OUT_PACKETS],
            row[IN_ERRORS],
            row[OUT_ERRORS]
        )

}
