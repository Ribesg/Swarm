package fr.ribesg.swarm.model.output

data class OutputDiskSpaceData(val data: List<OutputDiskSpaceDatum>) {

    data class OutputDiskSpaceDatum(
        val name: String,
        val size: Long,
        val used: Float,
        val timeLeft: Long?
    )

}
