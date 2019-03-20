package com.kirekov.cvlabs.features.points.descriptors

import com.kirekov.cvlabs.image.normalization.normalizeVector
import kotlin.math.absoluteValue

data class IndexValue<E>(val index: Int, val value: E)

private fun <E> List<E>.toIndexValueList(): List<IndexValue<E>> {
    return this.mapIndexed { index, item -> IndexValue(index, item) }
}

class Histogram(
    private val values: List<Double>,
    private val angles: List<Double>
) : Iterable<Double> {
    override fun iterator(): Iterator<Double> {
        return values.iterator()
    }

    val size = values.size

    fun getValue(index: Int): Double {
        return values[index]
    }

    fun normalize(): Histogram {
        return Histogram(values.normalizeVector(), angles)
    }

    fun getAngleWithHighestValue(): Double {
        val index = values.indexOf(values.max())
        return angles[index]
    }

    fun getTwoHighestAngles(differenceThreshold: Double = 0.8): List<Double> {
        val twoValues = values.toIndexValueList()
            .sortedByDescending { it.value }
            .take(2)

        val resultAngles = mutableListOf<Double>()
        resultAngles.add(angles[twoValues[0].index])
        if ((twoValues[1].value / twoValues[0].value) >= differenceThreshold) {
            resultAngles.add(angles[twoValues[1].index])
        }
        return resultAngles
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

                val deltaValue = ((gradientAngle - midAngle).absoluteValue / (angles[i] - midAngle)) * halfValue

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
        return Histogram(values.toList(), angles.toList())
    }
}