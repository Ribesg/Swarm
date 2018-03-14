@file:Suppress("NOTHING_TO_INLINE")

package fr.ribesg.swarm.extensions

/**
 * Returns the sum of all elements in the collection, or null if the collection is empty.
 */
inline fun Collection<Long>.sumOrNull(): Long? =
    if (isNotEmpty()) sum() else null

/**
 * Returns an average value of elements in the collection, or null if the collection is empty.
 */
inline fun Collection<Float>.averageOrNull(): Float? =
    if (isNotEmpty()) average().toFloat() else null
