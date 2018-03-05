package fr.ribesg.swarm.model.output

data class OutputRamData(
    val data: List<OutputRamDataPoint>,
    val maxRam: Long,
    val maxSwap: Long
) {

    data class OutputRamDataPoint(
        val date: Long,
        val ram: Long,
        val swap: Long
    )

}
