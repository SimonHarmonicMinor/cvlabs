package com.kirekov.cvlabs.features.points

data class FeaturePointOperator(val size: Int, val offset: Int, val threshold: Double) {
    init {
        if (offset <= 0)
            throw IllegalArgumentException(
                "Смещение должно быть не меньше 1"
            )
        if (threshold <= 0)
            throw IllegalArgumentException(
                "Пороговое значение должно быть не меньше 1"
            )
        if (size <= 0 || size % 2 == 0)
            throw java.lang.IllegalArgumentException()
    }
}