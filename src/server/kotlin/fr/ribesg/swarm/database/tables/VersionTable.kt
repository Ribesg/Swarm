package fr.ribesg.swarm.database.tables

import fr.ribesg.swarm.database.Database
import fr.ribesg.swarm.database.Database.Companion.exec
import fr.ribesg.swarm.extensions.runRawSql
import org.jetbrains.exposed.sql.*

/**
 * The Version table.
 *
 * Holds a single line with the current db version.
 */
internal object VersionTable : Table() {

    /**
     * The lock column, always set to true with a constraint, preventing this table from having more than one row.
     *
     * @see setupConstraint
     */
    @Suppress("UNUSED")
    val LOCK = bool("LOCK").primaryKey().default(true)

    /**
     * The current db version.
     */
    val VERSION = integer("VERSION")

    /**
     * Gets the current db version.
     *
     * This should not be called if there is no current db version (i.e. no table or no row).
     *
     * @return the current db version
     */
    fun getVersion(): Int {
        Database.checkInTransaction()
        val firstRow = VersionTable.selectAll().firstOrNull()
        if (firstRow == null) {
            throw IllegalStateException("Empty Table Version")
        }
        return firstRow[VERSION]
    }

    /**
     * Sets the current db version.
     *
     * This works if the version is not set and if it is already set.
     *
     * @param newValue the new db version
     */
    fun setVersion(newValue: Int) = exec {
        val versionTable = VersionTable.nameInDatabaseCase()
        // language=SQL
        runRawSql("MERGE INTO $versionTable VALUES (TRUE, $newValue);")
    }

    /**
     * Setups the constraint enforcing the "lock" column to only contain the value "true".
     */
    fun setupConstraint() = exec {
        val versionTable = nameInDatabaseCase()
        // language=SQL
        runRawSql("""

            ALTER TABLE $versionTable
            ADD CONSTRAINT ${versionTable}_SINGLE_ROW
                CHECK ${LOCK.name} = TRUE;

        """)
    }

}
