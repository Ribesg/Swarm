package fr.ribesg.swarm.model.input

data class InputDisk(
    val disks: List<InputDiskDevice>
) {

    data class InputDiskDevice(
        val name: String,
        val usedKiloBytes: Long,
        val totalKiloBytes: Long,
        val readUsage: Int?,
        val writeUsage: Int?,
        val readSpeed: Long?,
        val writeSpeed: Long?
    )

}
