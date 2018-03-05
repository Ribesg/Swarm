package fr.ribesg.swarm.model.output

data class OutputNetData(
    val data: List<OutputNetDataPoint>
) {

    data class OutputNetDataPoint(
        val date: Long,
        val values: Map<String, OutputNetDataPointValues>
    )

    data class OutputNetDataPointValues(
        val inBytes: Long?,
        val outBytes: Long?,
        val inErrors: Long?,
        val outErrors: Long?
    )

}
