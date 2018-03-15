package fr.ribesg.swarm.database

import fr.ribesg.swarm.database.Database.Companion.exec
import fr.ribesg.swarm.database.tables.*
import org.jetbrains.exposed.sql.*

/**
 * Handles versioning and upgrading of the database.
 */
internal object DatabaseVersionManager {

    /**
     * The current runtime version of the database.
     */
    private val CODE_VERSION: Int
        get() = UPGRADERS.size

    /**
     * A list of functions upgrading the database from the each version to the next one.
     *
     * Eg. UPGRADERS[0] upgrades the database from version 0 to version 1.
     */
    private val UPGRADERS = listOf(
        this::upgradeFromVersion0ToVersion1,
        this::upgradeFromVersion1ToVersion2,
        this::upgradeFromVersion2ToVersion3,
        this::upgradeFromVersion3ToVersion4,
        this::upgradeFromVersion4ToVersion5,
        this::upgradeFromVersion5ToVersion6,
        this::upgradeFromVersion6ToVersion7,
        this::upgradeFromVersion7ToVersion8,
        this::upgradeFromVersion8ToVersion9
    )

    /**
     * Runs the [DatabaseVersionManager] routine.
     */
    fun run() = exec {
        if (VersionTable.exists()) {
            val currentVersion = VersionTable.getVersion()
            if (currentVersion < CODE_VERSION) {
                upgradeDatabase(currentVersion)
            }
        } else {
            // If there is no Version table, there is no database
            createDatabase()
        }
    }

    /**
     * Creates and initializes all database tables and content.
     */
    private fun createDatabase() {
        SchemaUtils.create(HostsTable)
        HostsTable.setupConstraints()

        SchemaUtils.create(DataTable)
        DataTable.setupIndexAndConstraints()

        SchemaUtils.create(NetDataTable)
        NetDataTable.setupIndexAndConstraints()

        SchemaUtils.create(DiskDataTable)
        DiskDataTable.setupIndexAndConstraints()

        SchemaUtils.create(VersionTable)
        VersionTable.setupConstraint()
        VersionTable.setVersion(CODE_VERSION)
    }

    /**
     * Uses the [UPGRADERS] to upgrade the database to the latest runtime version.
     */
    private fun upgradeDatabase(fromVersion: Int) {
        UPGRADERS.slice(fromVersion until CODE_VERSION).forEach { it() }
    }

    /**
     * Upgrades the database from version 0 to version 1.
     *
     * This upgrader does not do much, it's only here to demonstrate how the database upgrading works.
     */
    private fun upgradeFromVersion0ToVersion1() {
        VersionTable.setVersion(1)
    }

    /**
     * Upgrades the database from version 1 to version 2.
     *
     * This upgrader removes all old data.
     */
    private fun upgradeFromVersion1ToVersion2() {
        DataTable.deleteAll()
        VersionTable.setVersion(2)
    }

    /**
     * Upgrades the database from version 2 to version 3.
     *
     * This upgrader removes all old data and adds the network data table.
     */
    private fun upgradeFromVersion2ToVersion3() {
        DataTable.deleteAll()
        HostsTable.deleteAll()
        SchemaUtils.create(NetDataTable)
        NetDataTable.setupIndexAndConstraints()
        VersionTable.setVersion(3)
    }

    /**
     * Upgrades the database from version 3 to version 4.
     *
     * This upgrader removes all old data.
     */
    private fun upgradeFromVersion3ToVersion4() {
        NetDataTable.deleteAll()
        DataTable.deleteAll()
        HostsTable.deleteAll()
        VersionTable.setVersion(4)
    }

    /**
     * Upgrades the database from version 4 to version 5.
     *
     * This upgrader resets the database.
     */
    private fun upgradeFromVersion4ToVersion5() {
        SchemaUtils.drop(DataTable, DiskDataTable, HostsTable, NetDataTable, VersionTable)
        createDatabase()
    }

    /**
     * Upgrades the database from version 5 to version 6.
     *
     * This upgrader resets the database.
     */
    private fun upgradeFromVersion5ToVersion6() {
        SchemaUtils.drop(DataTable, DiskDataTable, HostsTable, NetDataTable, VersionTable)
        createDatabase()
    }

    /**
     * Upgrades the database from version 6 to version 7.
     *
     * This upgrader resets the database.
     */
    private fun upgradeFromVersion6ToVersion7() {
        SchemaUtils.drop(DataTable, DiskDataTable, HostsTable, NetDataTable, VersionTable)
        createDatabase()
    }

    /**
     * Upgrades the database from version 7 to version 8.
     *
     * This upgrader resets the database.
     */
    private fun upgradeFromVersion7ToVersion8() {
        SchemaUtils.drop(DataTable, DiskDataTable, HostsTable, NetDataTable, VersionTable)
        createDatabase()
    }

    /**
     * Upgrades the database from version 8 to version 9.
     *
     * This upgrader resets the database.
     */
    private fun upgradeFromVersion8ToVersion9() {
        SchemaUtils.drop(DataTable, DiskDataTable, HostsTable, NetDataTable, VersionTable)
        createDatabase()
    }

}
