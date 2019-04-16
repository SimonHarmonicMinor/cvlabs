package com.kirekov.cvlabs.features.points

import com.kirekov.cvlabs.extension.add
import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import kotlin.math.pow


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

        val pointsValues = (0 until image.width).flatMap { i ->
            (0 until image.height).map { j ->
                val value = applyMoravecToPoint(i, j, startPos, endPos, offsetList)
                Triple(i, j, value)
            }.filter { x -> x.third > threshold }
        }
            .map { v -> Point(v.first, v.second, v.third) }
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
        val derivativesX = image.applyFilter(SobelFilter(SobelType.X))
        val derivativesY = image.applyFilter(SobelFilter(SobelType.Y))

        val pointsValues = (0 until image.width).map { i ->
            (0 until image.height).map { j ->
                val pointResult = applyHarrisToPoint(
                    i,
                    j,
                    gaussianFilter,
                    derivativesX,
                    derivativesY
                )
                val a = pointResult.first
                val b = pointResult.second
                val c = pointResult.third
                Triple(i, j, harrisCalculationMethod.calculateDetectionValue(a, b, c))
            }
        }

        val pointValuesFiltered = mutableListOf<Point>()
        for (i in 0 until image.width) {
            for (j in 0 until image.height) {
                val current = pointsValues[i][j]
                val triples = mutableListOf<Triple<Int, Int, Double>>()

                for (x in i - 1..i + 1) {
                    for (y in j - 1..j + 1) {
                        if (x < 0 || x >= image.width || y < 0 || y >= image.height)
                            continue
                        triples.add(pointsValues[x][y])
                    }
                }

                if (triples.maxBy { it.third } == current && current.third >= threshold)
                    pointValuesFiltered.add(Point(current.first, current.second, current.third))
            }
        }


        return FeaturePoints(image.width, image.height, pointValuesFiltered)
    }

    fun calculatePoint(i: Int, j: Int): Double {
        val gaussianFilter = if (size % 2 == 1) GaussianFilter(size) else GaussianFilter(size + 1)
        val derivativesX = image.applyFilter(SobelFilter(SobelType.X))
        val derivativesY = image.applyFilter(SobelFilter(SobelType.Y))
        val pointResult = applyHarrisToPoint(
            i,
            j,
            gaussianFilter,
            derivativesX,
            derivativesY
        )
        val a = pointResult.first
        val b = pointResult.second
        val c = pointResult.third
        return harrisCalculationMethod.calculateDetectionValue(a, b, c)
    }

    private fun applyHarrisToPoint(
        i: Int,
        j: Int,
        gaussianFilter: GaussianFilter,
        derivativesX: GrayScaledImage,
        derivativesY: GrayScaledImage
    )
            : Triple<Double, Double, Double> {
        val endPos = (gaussianFilter.size - 1) / 2
        val startPos = endPos * -1

        val result = (startPos..endPos).flatMap { x ->
            (startPos..endPos).map { y ->
                val filterValue = gaussianFilter.getValue(x, y)
                val xDerivative = derivativesX.getPixelValue(i + x, j + y)
                val yDerivative = derivativesY.getPixelValue(i + x, j + y)
                Triple(
                    xDerivative.pow(2) * filterValue,
                    xDerivative * yDerivative * filterValue,
                    yDerivative.pow(2) * filterValue
                )
            }
        }

        return result.reduce { x, y -> x.add(y) }
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
