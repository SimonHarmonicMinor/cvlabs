package com.kirekov.cvlabs.features.points.descriptors

import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.Point
import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.Filter
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import com.kirekov.cvlabs.image.normalization.normalizeVector
import kotlin.math.pow
import kotlin.streams.toList


class ImageDescriptors(
    val image: GrayScaledImage,
    val descriptors: List<Descriptor>
) {
    val featurePoints: FeaturePoints
        get() = FeaturePoints(
            image.width,
            image.height,
            descriptors.map { it.point }.toTypedArray()
        )

    fun findClosestTo(
        descriptor: Descriptor,
        descriptorsDistance: DescriptorsDistance,
        count: Int = 1
    ): List<DescriptorAndDistance> {
        return descriptors
            .parallelStream()
            .map {
                DescriptorAndDistance(
                    it,
                    descriptorsDistance.calculateDistance(it, descriptor)
                )
            }
            .toList()
            .sortedBy { it.distance }
            .subList(0, count)
    }

    data class DescriptorAndDistance(val descriptor: Descriptor, val distance: Double)

    companion object {
        fun of(
            image: GrayScaledImage,
            featurePoints: FeaturePoints,
            windowSize: Int,
            smallWindowSize: Int,
            histogramSize: Int
        ): ImageDescriptors {
            if (windowSize % 2 == 0 || smallWindowSize % 2 == 0)
                throw IllegalArgumentException("Размер окон не может быть четным")

            val xDerivative = image.applyFilter(SobelFilter(SobelType.X))
            val yDerivative = image.applyFilter(SobelFilter(SobelType.Y))

            val gradientValues = xDerivative.mapImages(yDerivative) { x, y ->
                Math.sqrt(x.pow(2) + y.pow(2))
            }
            val gradientAngles = xDerivative.mapImages(yDerivative) { x, y ->
                Math.atan2(y, x)
            }

            val wholeSize = windowSize * smallWindowSize
            val gaussianFilter = GaussianFilter(wholeSize)

            val endPos = (windowSize - 1) / 2
            val startPos = endPos * -1

            val descriptors = featurePoints.toList().stream().map { point ->
                val histograms = (startPos..endPos).flatMap { i ->
                    (startPos..endPos).map { j ->
                        calculateHistogramOfSmallWindow(
                            point.x + i * smallWindowSize,
                            point.y + j * smallWindowSize,
                            histogramSize,
                            gaussianFilter,
                            i * smallWindowSize,
                            j * smallWindowSize,
                            smallWindowSize,
                            gradientValues,
                            gradientAngles
                        ).normalize()
                    }
                }
                Descriptor(histograms, point)
            }.toList()
            return ImageDescriptors(image, descriptors)
        }

        private fun calculateHistogramOfSmallWindow(
            i: Int,
            j: Int,
            histogramSize: Int,
            filter: Filter,
            subFilterX: Int,
            subFilterY: Int,
            subFilterSize: Int,
            gradientValues: GrayScaledImage,
            gradientAngles: GrayScaledImage
        ): Histogram {
            val endPos = (subFilterSize - 1) / 2
            val startPos = endPos * -1

            val step = Math.PI * 2 / (histogramSize - 1)
            val angles = (0 until histogramSize).map {
                it * step
            }.toDoubleArray()

            val histogramBuilder = HistogramBuilder(angles)

            val pairs = (startPos..endPos).toList().stream().flatMap { x ->
                (startPos..endPos).toList().stream().map { y ->
                    val gradientValue =
                        gradientValues.getPixelValue(i + x, j + y) *
                                filter.getValue(subFilterX + x, subFilterY + y)
                    val gradientAngle = gradientAngles.getPixelValue(i + x, j + y)
                    Pair(gradientValue, gradientAngle)
                }
            }

            pairs.forEach { histogramBuilder.addGradient(it.first, it.second) }

            return histogramBuilder.build()
        }
    }
}

class Descriptor {
    val values: List<Double>
    val point: Point

    constructor(histograms: List<Histogram>, point: Point) {
        this.values = histograms.flatten()
        this.point = point
    }

    private constructor(values: DoubleArray, point: Point) {
        this.values = values.toList()
        this.point = point
    }

    fun normalize(): Descriptor {
        return Descriptor(values.normalizeVector().toDoubleArray(), point)
    }
}

