@file:Suppress("NOTHING_TO_INLINE")

package fr.ribesg.swarm.extensions

fun <T> List<T>.batched(batchSize: Int): List<List<T>> {
    check(batchSize > 0) { "Invalid value for parameter 'batchSize': $batchSize (should be greater than 0)" }
    return if (batchSize >= size) {
        listOf(this)
    } else {
        List((size + batchSize - 1) / batchSize) { i ->
            subList(i * batchSize, ((i + 1) * batchSize).coerceAtMost(size))
        }
    }
}

inline fun <T> List<T>.forEachBatch(batchSize: Int, action: (List<T>) -> Unit) =
    batched(batchSize).forEach(action)

inline fun <T, R> Collection<T>.letIfAnyOrNull(block: (it: Iterable<T>) -> R): R? =
    if (isEmpty()) null else block(this)
