package org.snd.metadata.providers.bangumi.model

/**
 * `1`, `2`, `3` 表示 `个人`, `公司`, `组合`
 *
 * Values: Individual,Corporation,Association
 */

enum class PersonType(val value: Int) {

    INDIVIDUAL(1),
    CORPORATION(2),
    ASSOCIATION(3);

    companion object {
        fun fromValue(value: Int): PersonType {
            return PersonType.values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid PersonType value: $value")
        }
    }
}

