package com.kirekov.cvlabs.extension

fun Triple<Double, Double, Double>.add(triple: Triple<Double, Double, Double>):
        Triple<Double, Double, Double> {
    return Triple(
        this.first + triple.first,
        this.second + triple.second,
        this.third + triple.third
    )
}

fun Triple<Double, Double, Double>.multiply(filterValue: Double):
        Triple<Double, Double, Double> {
    return Triple(
        this.first * filterValue,
        this.second * filterValue,
        this.third * filterValue
    )
}