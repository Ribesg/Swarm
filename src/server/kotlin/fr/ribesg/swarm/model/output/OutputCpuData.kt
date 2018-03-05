package fr.ribesg.swarm.model.output

data class OutputCpuData(
    val data: List<CpuChartDataPoint>
) {

    data class CpuChartDataPoint(
        val date: Long,
        val busy: Float?,
        val io: Float?,
        val steal: Float?
    )

}
