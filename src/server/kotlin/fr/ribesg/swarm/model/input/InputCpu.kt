package fr.ribesg.swarm.model.input

data class InputCpu(
    val busy: Int?,
    val io: Int?,
    val steal: Int?
) {

    companion object {

        val EMPTY = InputCpu(null, null, null)

    }

}
