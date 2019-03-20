package com.kirekov.cvlabs.features.points.descriptors

import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.Point
import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import com.kirekov.cvlabs.image.normalization.normalizeVector
import kotlin.math.*
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

        private fun normalizeAngle(angle: Double): Double {
            var resultAngle = angle
            while (resultAngle < 0)
                resultAngle += 2 * PI
            while (resultAngle >= 2 * PI)
                resultAngle -= 2 * PI
            return resultAngle
        }

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
                normalizeAngle(Math.atan2(y, x))
            }

            val wholeSize = windowSize * smallWindowSize
            val gaussianFilter = GaussianFilter(wholeSize)


            val step = Math.PI * 2 / (histogramSize - 1)
            val angles = (0 until histogramSize).map {
                it * step
            }.toDoubleArray()

            val descriptors = featurePoints.toList().stream().map { point ->

                val turnAngles = calculateAreaOrientationAngle(
                    point.x,
                    point.y,
                    gaussianFilter,
                    gradientValues,
                    36
                )

                val histogramsBuilders =
                    (0 until windowSize).map {
                        (0 until windowSize).map {
                            HistogramBuilder(angles)
                        }
                    }

                val halfSize = (wholeSize - 1) / 2

                (-halfSize..halfSize).forEach { i ->
                    (-halfSize..halfSize).forEach { j ->
                        turnAngles.forEach { turnAngle ->

                            val newI = (i * cos(turnAngle) + j * sin(turnAngle))
                                .roundToInt()
                            val newJ = (j * cos(turnAngle) - i * sin(turnAngle))
                                .roundToInt()

                            if (newI in (-halfSize..halfSize) && newJ in (-halfSize..halfSize)) {
                                val gradientValue =
                                    gradientValues.getPixelValue(
                                        point.x + i,
                                        point.y + j
                                    )

                                val gradientValueBlur = gradientValue *
                                        gaussianFilter
                                            .getValue(
                                                newI,
                                                newJ
                                            )

                                val gradientAngle = normalizeAngle(
                                    gradientAngles
                                        .getPixelValue(
                                            point.x + i,
                                            point.y + j
                                        ) - turnAngle
                                )

                                val hIndexI = (newI + halfSize) / smallWindowSize
                                val hIndexJ = (newJ + halfSize) / smallWindowSize


                                histogramsBuilders[hIndexI][hIndexJ]
                                    .addGradient(gradientValueBlur, gradientAngle)
                            }
                        }

                    }
                }


                Descriptor(
                    histogramsBuilders
                        .flatMap { it.map { h -> h.build() } },
                    point
                ).normalize()
            }.toList()
            return ImageDescriptors(image, descriptors)
        }

        private fun calculateAreaOrientationAngle(
            i: Int,
            j: Int,
            gaussianFilter: GaussianFilter,
            gradientValues: GrayScaledImage,
            histogramSize: Int
        ): List<Double> {
            val areaSize = gaussianFilter.size

            val step = Math.PI * 2 / (histogramSize - 1)
            val angles = (0 until histogramSize).map {
                it * step
            }.toDoubleArray()

            val histogramBuilder = HistogramBuilder(angles)

            val halfSize = (areaSize - 1) / 2

            val pairs = (-halfSize..halfSize).flatMap { x ->
                (-halfSize..halfSize).map { y ->
                    val xDerivativeSubtraction =
                        gradientValues.getPixelValue(i + x + 1, j + y) -
                                gradientValues.getPixelValue(i + x - 1, j + y)

                    val yDerivativeSubtraction =
                        gradientValues.getPixelValue(i + x, j + y + 1) -
                                gradientValues.getPixelValue(i + x, j + y - 1)

                    val magnitude =
                        Math.sqrt(
                            xDerivativeSubtraction.pow(2) +
                                    yDerivativeSubtraction.pow(2)
                        ) * gaussianFilter.getValue(
                            x,
                            y
                        )

                    val teta = normalizeAngle(
                        Math.atan2(
                            yDerivativeSubtraction,
                            xDerivativeSubtraction
                        )
                    )

                    Pair(magnitude, teta)
                }
            }

            pairs.forEach { histogramBuilder.addGradient(it.first, it.second) }
            return histogramBuilder
                .build()
                .getTwoHighestAngles()
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

