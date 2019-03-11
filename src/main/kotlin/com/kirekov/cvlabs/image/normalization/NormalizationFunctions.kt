package com.kirekov.cvlabs.image.normalization

import kotlin.math.pow

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

fun List<Double>.normalizeVector(): List<Double> {
    val vectorLength = Math.sqrt(this.map { it.pow(2) }.sum())
    return this.map { it / vectorLength }
}

fun DoubleArray.normalizeVector(): DoubleArray {
    val vectorLength = Math.sqrt(this.map { it.pow(2) }.sum())
    return this.map { it / vectorLength }.toDoubleArray()
}