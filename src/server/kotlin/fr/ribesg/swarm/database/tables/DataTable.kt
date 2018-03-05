package fr.ribesg.swarm.database.tables

import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.database.Database.exec
import fr.ribesg.swarm.extensions.runRawSql
import fr.ribesg.swarm.model.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.joda.time.*

/**
 * Holds both live and archived data points for each host.
 */
internal object DataTable : Table() {

    /**
     * The id
     */
    val ID = integer("ID").autoIncrement().primaryKey()

    /**
     * The type of data point
     *
     * @see DbDataType
     */
    val TYPE = enumeration("TYPE", DbDataType::class.java)

    /**
     * The hostname. References [HostsTable.HOST].
     */
    val HOST = varchar("HOST", 255).references(HostsTable.HOST, ReferenceOption.CASCADE)

    /**
     * The datetime of the data point
     */
    val DATE = datetime("DATE")

    /**
     * CPU busy time per thousand
     */
    val CPU_BUSY = integer("CPU_BUSY").nullable()

    /**
     * CPU iowait time per thousand
     */
    val CPU_IO = integer("CPU_IO").nullable()

    /**
     * CPU steal time per thousand
     */
    val CPU_STEAL = integer("CPU_STEAL").nullable()

    /**
     * RAM used in kilobytes
     */
    val RAM_USED = long("RAM_USED")

    /**
     * SWAP used in kilobytes
     */
    val SWAP_USED = long("SWAP_USED")

    /**
     * Inserts the provided datum into the table.
     *
     * @param datum the datum to push
     *
     * @return the id of the inserted datum
     */
    fun insert(datum: DbDatum): Int {
        Database.checkInTransaction()
        return insert {
            it[TYPE] = datum.type
            it[HOST] = datum.host
            it[DATE] = DateTime(datum.date)
            it[CPU_BUSY] = datum.cpuBusy
            it[CPU_IO] = datum.cpuIo
            it[CPU_STEAL] = datum.cpuSteal
            it[RAM_USED] = datum.ramUsed
            it[SWAP_USED] = datum.swapUsed
        } get ID
    }

    /**
     * Inserts the provided data into the table.
     *
     * @param data the data to push
     *
     * @return the ids of the inserted data
     */
    fun insert(data: Iterable<DbDatum>): List<Int> {
        Database.checkInTransaction()
        return batchInsert(data) { datum ->
            this[TYPE] = datum.type
            this[HOST] = datum.host
            this[DATE] = DateTime(datum.date)
            this[CPU_BUSY] = datum.cpuBusy
            this[CPU_IO] = datum.cpuIo
            this[CPU_STEAL] = datum.cpuSteal
            this[RAM_USED] = datum.ramUsed
            this[SWAP_USED] = datum.swapUsed
        }.map {
            it.values.first() as Int
        }
    }

    /**
     * Gets all data with the provided type between the two provided dates.
     *
     * @param from the minimum date in milliseconds, inclusive
     * @param to the maximum date in milliseconds, exclusive
     *
     * @return all data with the provided type between the two provided dates
     */
    fun getData(type: DbDataType, from: Long, to: Long, host: String? = null): List<DbDatum> {
        Database.checkInTransaction()
        val fromDate = DateTime(from)
        val toDate = DateTime(to - 1) // -1 = exclusive
        val typeAndDateCond = (TYPE eq type) and (DATE.between(fromDate, toDate))
        return if (host == null) {
            select { typeAndDateCond }
        } else {
            select { (HOST eq host) and typeAndDateCond }
        }.map(this::rowToDatum).sortedBy(DbDatum::date)
    }

    fun getDatumClosestToDate(date: Long, host: String): DbDatum {
        Database.checkInTransaction()
        val typeAndHostCond = (TYPE inList listOf(DbDataType.HOUR, DbDataType.LIVE)) and (HOST eq host)
        val dateTime = DateTime(date)
        val before = select { typeAndHostCond and (DATE lessEq dateTime) }
            .orderBy(DATE, false)
            .limit(1)
            .map(this::rowToDatum)
            .singleOrNull()
        val beforeDate = before?.let { DateTime(it.date) }
        val after = select { typeAndHostCond and (DATE greaterEq dateTime) }
            .orderBy(DATE, true)
            .limit(1)
            .map(this::rowToDatum)
            .singleOrNull()
        val afterDate = after?.let { DateTime(it.date) }
        return when {
            before == null                                                     -> after!!
            after == null                                                      -> before
            Duration(beforeDate!!, dateTime) < Duration(dateTime, afterDate!!) -> before
            else                                                               -> after
        }
    }

    /**
     * Deletes all rows with the provided type(s) and older than the provided date.
     *
     * @param before the maximum date of the rows to remove in milliseconds, exclusive
     * @param types one or more types of data to delete
     *
     * @return the amount of rows deleted
     */
    fun deleteData(before: Long, vararg types: DbDataType): Int {
        require(types.isNotEmpty()) { "At least one type should be provided" }
        Database.checkInTransaction()
        return deleteWhere { (TYPE inList types.toList()) and (DATE less DateTime(before)) }
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
        SchemaUtils.createIndex(arrayOf(TYPE, HOST, DATE), true)
    }

    /**
     * Setups constraints on per thousand values and other positive integers.
     */
    // language=SQL
    private fun setupConstraints() = exec {
        val dataTable = nameInDatabaseCase()
        runRawSql("""

            ALTER TABLE $dataTable
            ADD CONSTRAINT ${dataTable}_${CPU_BUSY.name}_INTERVAL
                CHECK ${CPU_BUSY.name} BETWEEN 0 AND 10000;

        """)
        runRawSql("""

            ALTER TABLE $dataTable
            ADD CONSTRAINT ${dataTable}_${CPU_IO.name}_INTERVAL
                CHECK ${CPU_IO.name} BETWEEN 0 AND 10000;

        """)
        runRawSql("""

            ALTER TABLE $dataTable
            ADD CONSTRAINT ${dataTable}_${CPU_STEAL.name}_INTERVAL
                CHECK ${CPU_STEAL.name} BETWEEN 0 AND 10000;

        """)
        runRawSql("""

            ALTER TABLE $dataTable
            ADD CONSTRAINT ${dataTable}_${RAM_USED.name}_INTERVAL
                CHECK ${RAM_USED.name} >= 0;

        """)
        runRawSql("""

            ALTER TABLE $dataTable
            ADD CONSTRAINT ${dataTable}_${SWAP_USED.name}_INTERVAL
                CHECK ${SWAP_USED.name} >= 0;

        """)
    }

    /**
     * Creates a Datum from a ResultRow.
     *
     * @param row the result row to convert
     *
     * @return a Datum from the provided ResultRow
     */
    internal fun rowToDatum(row: ResultRow): DbDatum =
        DbDatum(
            row[ID],
            row[TYPE],
            row[HOST],
            row[DATE].millis,
            row[CPU_BUSY],
            row[CPU_IO],
            row[CPU_STEAL],
            row[RAM_USED],
            row[SWAP_USED]
        )

}
