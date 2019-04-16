package com.kirekov.cvlabs.features.points.descriptors

import com.kirekov.cvlabs.image.normalization.normalizeVector
import kotlin.math.abs

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


class HistogramBuilder(private val step: Double, private val histogramSize: Int) {
    val values = DoubleArray(histogramSize)
    val angles = (0..histogramSize).map { step * it }
    fun addGradient(gradientValue: Double, gradientAngle: Double) {
        for (i in 1..histogramSize) {
            if (gradientAngle <= angles[i]) {
                val midBin = angles[i] - (step / 2)
                val b = (gradientAngle - midBin) / step
                val a = 1 - abs(b)
                val c0 = a * gradientValue
                val c1 = abs(b) * gradientValue
                val j = i - 1
                values[j] += c0
                if (b >= 0) {
                    if (j < histogramSize - 1)
                        values[j + 1] += c1
                    else
                        values[0] += c1
                } else {
                    if (j > 0)
                        values[j - 1] += c1
                    else
                        values[histogramSize - 1] += c1
                }
                break
            }
        }
    }

    fun build(): Histogram {
        return Histogram(
            values.toList(),
            (0 until histogramSize).map {
                it * step + step / 2
            }
        )
    }
}