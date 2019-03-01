package com.kirekov.cvlabs.features.points

data class FeaturePointOperator(val offset: Int, val threshold: Double) {
    init {
        if (offset <= 0)
            throw IllegalArgumentException(
                "Смещение должно быть не меньше 1"
            )
        if (threshold <= 0)
            throw IllegalArgumentException(
                "Пороговое значение должно быть не меньше 1"
            )
    }
}