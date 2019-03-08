package com.kirekov.cvlabs.features.points

import com.kirekov.cvlabs.extension.add
import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import java.util.*
import kotlin.math.pow
import kotlin.streams.toList


interface FeaturePointsCalculator {
    fun calculate(): FeaturePoints
}

class MorravecCalculator(
    val size: Int,
    val offset: Int,
    val threshold: Double,
    val image: GrayScaledImage
) :
    FeaturePointsCalculator {
    override fun calculate(): FeaturePoints {
        val offsetList = listOf(-offset, 0, offset)
        val endPos = (size - 1) / 2
        val startPos = endPos * -1

        val pointsValues = (0 until image.width).toList().parallelStream().flatMap { i ->
            (0 until image.height).toList().stream().map { j ->
                val value = applyMoravecToPoint(i, j, startPos, endPos, offsetList)
                Triple(i, j, value)
            }.filter { x -> x.third > threshold }
        }.toList()
            .map { Optional.ofNullable(it).map { v -> Point(v.first, v.second, v.third) }.get() }
            .toTypedArray()
        return FeaturePoints(image.width, image.height, pointsValues)
    }

    private fun applyMoravecToPoint(
        i: Int,
        j: Int,
        startPos: Int,
        endPos: Int,
        offsetList: List<Int>
    ): Double {
        val res = offsetList.flatMap { offsetX ->
            offsetList.map { offsetY ->
                (startPos..endPos).flatMap { x ->
                    (startPos..endPos).map { y ->
                        val imgValue = image
                            .getPixelValue(i - x, j - y)
                        val offsetImgValue = image
                            .getPixelValue(i - x - offsetX, j - y - offsetY)
                        (imgValue - offsetImgValue).pow(2)
                    }
                }.sum()
            }
        }.sorted()[1]
        return res
    }
}

class HarrisCalculator(
    val size: Int,
    val threshold: Double,
    val image: GrayScaledImage,
    val harrisCalculationMethod: HarrisCalculationMethod
) : FeaturePointsCalculator {
    override fun calculate(): FeaturePoints {
        val gaussianFilter = GaussianFilter(size)
        val pointsValues = (0 until image.width).toList().parallelStream().flatMap { i ->
            (0 until image.height).toList().stream().map { j ->
                val pointResult = applyHarrisToPoint(i, j, gaussianFilter)
                val a = pointResult.first
                val b = pointResult.second
                val c = pointResult.third
                Triple(i, j, harrisCalculationMethod.calculateDetectionValue(a, b, c))
            }.filter { x -> x.third > threshold }
        }.toList()
            .map { Optional.ofNullable(it).map { v -> Point(v.first, v.second, v.third) }.get() }
            .toTypedArray()

        return FeaturePoints(image.width, image.height, pointsValues)
    }

    private fun applyHarrisToPoint(i: Int, j: Int, gaussianFilter: GaussianFilter)
            : Triple<Double, Double, Double> {
        val endPos = (gaussianFilter.size - 1) / 2
        val startPos = endPos * -1

        val result = (startPos..endPos).flatMap { x ->
            (startPos..endPos).map { y ->
                val filterValue = gaussianFilter.getValue(x, y)
                val xDerivative =
                    image.applyFilterToPoint(SobelFilter(SobelType.X), i - x, j - y)
                val yDerivative =
                    image.applyFilterToPoint(SobelFilter(SobelType.Y), i - x, j - y)
                Triple(
                    xDerivative.pow(2) * gaussianFilter.getValue(x, y),
                    xDerivative * yDerivative * gaussianFilter.getValue(x, y),
                    yDerivative.pow(2) * gaussianFilter.getValue(x, y)
                )
            }
        }.reduce { x, y -> x.add(y) }

        return result
    }
}

interface HarrisCalculationMethod {
    fun calculateDetectionValue(a: Double, b: Double, c: Double): Double
}

class EigenValuesMethod : HarrisCalculationMethod {
    override fun calculateDetectionValue(a: Double, b: Double, c: Double): Double {
        val beta = c + a
        val gamma = a * c - b.pow(2)
        val d = Math.sqrt(beta.pow(2) - 4 * gamma)
        val n1 = (beta + d) / 2
        val n2 = (beta - d) / 2
        return Math.min(n1, n2)
    }
}

class OriginalHarrisMethod : HarrisCalculationMethod {
    override fun calculateDetectionValue(a: Double, b: Double, c: Double): Double {
        return a * c - b.pow(2) - 0.06 * (a + c).pow(2)
    }
}

class ForstnerGulchMethod : HarrisCalculationMethod {
    override fun calculateDetectionValue(a: Double, b: Double, c: Double): Double {
        return (a * c - b.pow(2)) / (a + c)
    }

}
