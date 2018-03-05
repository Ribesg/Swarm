package fr.ribesg.swarm.database.tables

import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.extensions.runRawSql
import fr.ribesg.swarm.model.database.DbDiskDatum
import org.jetbrains.exposed.sql.*

/**
 * Holds both live and archived disk data points for each host.
 */
internal object DiskDataTable : Table() {

    /**
     * The id
     */
    val ID = integer("ID").autoIncrement().primaryKey()

    /**
     * Reference to a row of [DataTable]
     */
    val DATA_REF = integer("DATA_REF").references(DataTable.ID, ReferenceOption.CASCADE)

    /**
     * Name of the device (limited to 127 bytes on linux)
     */
    val DEVICE = varchar("INTERFACE", 128)

    /**
     * Read time per thousand
     */
    val READ_USAGE = integer("READ_USAGE").nullable()

    /**
     * Write time per thousand
     */
    val WRITE_USAGE = integer("WRITE_USAGE").nullable()

    /**
     * Effective read speed
     */
    val READ_SPEED = long("READ_SPEED").nullable()

    /**
     * Effective write speed
     */
    val WRITE_SPEED = long("WRITE_SPEED").nullable()

    /**
     * Disk space used
     */
    val USED_SPACE = long("USED_SPACE")

    /**
     * Total disk space
     */
    val TOTAL_SPACE = long("AVAILABLE_SPACE")

    /**
     * Inserts the provided data into the table.
     *
     * @param data the data to insert
     *
     * @return the ids of the inserted data
     */
    fun insert(data: Iterable<DbDiskDatum>): List<Int> {
        Database.checkInTransaction()
        return batchInsert(data) { datum ->
            this[DATA_REF] = datum.dataRef
            this[DEVICE] = datum.device
            this[READ_USAGE] = datum.readUsage
            this[WRITE_USAGE] = datum.writeUsage
            this[READ_SPEED] = datum.readSpeed
            this[WRITE_SPEED] = datum.writeSpeed
            this[USED_SPACE] = datum.usedSpace
            this[TOTAL_SPACE] = datum.totalSpace
        }.map {
            it.values.first() as Int
        }
    }

    /**
     * Gets all rows matching the provided [DataTable] id.
     *
     * @param dataRef the id of the matching row in [DataTable]
     *
     * @return all rows matching the provided [DataTable] id
     */
    fun getData(dataRef: Int): List<DbDiskDatum> {
        Database.checkInTransaction()
        return select { DATA_REF eq dataRef }.map(this::rowToDiskDatum)
    }

    /**
     * Gets all rows matching the provided [DataTable] ids.
     *
     * @param dataRefs the ids of the matching rows in [DataTable]
     *
     * @return all rows matching the provided [DataTable] ids
     */
    fun getData(dataRefs: List<Int>): Map<Int, List<DbDiskDatum>> {
        Database.checkInTransaction()
        return select { DATA_REF inList dataRefs }.map(this::rowToDiskDatum).groupBy(DbDiskDatum::dataRef)
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
        SchemaUtils.createIndex(arrayOf(DATA_REF, DEVICE), true)
    }

    /**
     * Setups constraints on per thousand values and other positive integers.
     */
    // language=SQL
    private fun setupConstraints() = Database.exec {
        val diskDataTable = nameInDatabaseCase()
        runRawSql("""

            ALTER TABLE $diskDataTable
            ADD CONSTRAINT ${diskDataTable}_${READ_USAGE.name}_POSITIVE
                CHECK ${READ_USAGE.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $diskDataTable
            ADD CONSTRAINT ${diskDataTable}_${WRITE_USAGE.name}_POSITIVE
                CHECK ${WRITE_USAGE.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $diskDataTable
            ADD CONSTRAINT ${diskDataTable}_${READ_SPEED.name}_POSITIVE
                CHECK ${READ_SPEED.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $diskDataTable
            ADD CONSTRAINT ${diskDataTable}_${WRITE_SPEED.name}_POSITIVE
                CHECK ${WRITE_SPEED.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $diskDataTable
            ADD CONSTRAINT ${diskDataTable}_${USED_SPACE.name}_POSITIVE
                CHECK ${USED_SPACE.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $diskDataTable
            ADD CONSTRAINT ${diskDataTable}_${TOTAL_SPACE.name}_POSITIVE
                CHECK ${TOTAL_SPACE.name} >= 0;

        """)
    }

    /**
     * Creates a DiskDatum from a ResultRow.
     *
     * @param row the result row to convert
     *
     * @return a DiskDatum from the provided ResultRow
     */
    internal fun rowToDiskDatum(row: ResultRow): DbDiskDatum =
        DbDiskDatum(
            row[ID],
            row[DATA_REF],
            row[DEVICE],
            row[READ_USAGE],
            row[WRITE_USAGE],
            row[READ_SPEED],
            row[WRITE_SPEED],
            row[USED_SPACE],
            row[TOTAL_SPACE]
        )

}
