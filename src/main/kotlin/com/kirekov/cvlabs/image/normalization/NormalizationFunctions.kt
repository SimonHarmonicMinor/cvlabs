package com.kirekov.cvlabs.image.normalization

fun List<Double>.normalize(newMin: Double, newMax: Double): List<Double> {
    val oldMin = this.min() as Double
    val oldMax = this.max() as Double
    return this.map { x ->
        (x - oldMin) * ((newMax - newMin) / (oldMax - oldMin)) + newMin
    }
}

fun DoubleArray.normalize(newMin: Double, newMax: Double): DoubleArray {
    val oldMin = this.min() as Double
    val oldMax = this.max() as Double
    return this.map { x ->
        (x - oldMin) * ((newMax - newMin) / (oldMax - oldMin)) + newMin
    }.toDoubleArray()
}