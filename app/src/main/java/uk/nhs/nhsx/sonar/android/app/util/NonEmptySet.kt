/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import java.io.Serializable

class NonEmptySet<T> private constructor(private val set: Set<T>) : Set<T> by set, Serializable {
    companion object {
        fun <T> create(set: Set<T>): NonEmptySet<T>? =
            if (set.isEmpty()) null
            else NonEmptySet(set)

        fun <T> create(first: T, vararg others: T): NonEmptySet<T> =
            NonEmptySet(setOf(first, *others))
    }

    override fun toString(): String =
        "NonEmptySet(set=$set)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NonEmptySet<*>

        if (set != other.set) return false

        return true
    }

    override fun hashCode(): Int =
        set.hashCode()
}

fun <T> nonEmptySetOf(first: T, vararg others: T): NonEmptySet<T> =
    NonEmptySet.create(first, *others)
