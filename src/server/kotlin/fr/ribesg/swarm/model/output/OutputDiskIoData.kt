package fr.ribesg.swarm.model.output

data class OutputDiskIoData(
    val data: List<OutputDiskIoDataPoint>
) {

    data class OutputDiskIoDataPoint(
        val date: Long,
        val values: Map<String, OutputDiskIoDataPointValues>
    )

    data class OutputDiskIoDataPointValues(
        val readUsage: Float?,
        val writeUsage: Float?,
        val readSpeed: Long?,
        val writeSpeed: Long?
    )

}
