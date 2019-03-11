package com.kirekov.cvlabs.features.points.descriptors

import com.kirekov.cvlabs.image.normalization.normalizeVector
import kotlin.math.absoluteValue

class Histogram(private val values: List<Double>) : Iterable<Double> {
    override fun iterator(): Iterator<Double> {
        return values.iterator()
    }

    val size = values.size

    fun getValue(index: Int): Double {
        return values[index]
    }

    fun normalize(): Histogram {
        return Histogram(values.normalizeVector())
    }
}

class HistogramBuilder(private val angles: DoubleArray) {
    val values = DoubleArray(angles.size)

    fun addGradient(gradientValue: Double, gradientAngle: Double) {
        var isOk = false
        for (i in 0 until values.size) {
            if (gradientAngle <= angles[i]) {
                if (i == 0) {
                    values[i] += gradientValue
                    isOk = true
                    break
                }

                val midAngle = ((angles[i] - angles[i - 1]) / 2) + angles[i - 1]
                val halfValue = gradientValue / 2
                values[i - 1] += halfValue
                values[i] += halfValue

                val deltaValue = ((gradientAngle - midAngle).absoluteValue / midAngle) * halfValue

                if (gradientAngle > midAngle) {
                    values[i] += deltaValue
                    values[i - 1] -= deltaValue
                } else {
                    values[i] -= deltaValue
                    values[i - 1] += deltaValue
                }

                isOk = true
                break
            }
        }

        if (!isOk) {
            values[values.lastIndex] += gradientValue
        }
    }

    fun build(): Histogram {
        return Histogram(values.toList())
    }
}