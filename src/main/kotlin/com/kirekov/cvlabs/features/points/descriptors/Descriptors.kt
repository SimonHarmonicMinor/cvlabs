package com.kirekov.cvlabs.features.points.descriptors

import com.kirekov.cvlabs.extension.ThreadPool
import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.Point
import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import com.kirekov.cvlabs.image.normalization.normalizeVector
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.math.*


class ImageDescriptors(
    val image: GrayScaledImage,
    val descriptors: List<Descriptor>
) {
    val featurePoints: FeaturePoints
        get() = FeaturePoints(
            image.width,
            image.height,
            descriptors.map { it.point }
        )

    fun findClosestTo(
        descriptor: Descriptor,
        descriptorsDistance: DescriptorsDistance,
        count: Int = 1
    ): List<DescriptorAndDistance> {
        return descriptors
            .map {
                DescriptorAndDistance(
                    it,
                    descriptorsDistance.calculateDistance(it, descriptor)
                )
            }
            .sortedBy { it.distance }
            .subList(0, count)
    }


    data class DescriptorAndDistance(val descriptor: Descriptor, val distance: Double)

    companion object {

        fun normalizeAngle(angle: Double): Double {
            return (PI * 2 + angle) % (2 * PI)
        }

        fun of(
            image: GrayScaledImage,
            featurePoints: FeaturePoints,
            windowSize: Int,
            histogramRowSize: Int,
            histogramSize: Int
        ): ImageDescriptors = runBlocking {

            val xDerivative = async(ThreadPool.pool) {
                image.applyFilter(SobelFilter(SobelType.X))
            }

            val yDerivative = async(ThreadPool.pool) {
                image.applyFilter(SobelFilter(SobelType.Y))
            }


            val gradientValuesDeferred = async(ThreadPool.pool) {
                xDerivative.await().mapImages(yDerivative.await()) { x, y ->
                    Math.sqrt(x.pow(2) + y.pow(2))
                }
            }

            val gradientAnglesDeferred = async(ThreadPool.pool) {
                xDerivative.await().mapImages(yDerivative.await()) { x, y ->
                    normalizeAngle(Math.atan2(x, y))
                }
            }

            val gradientValues = gradientValuesDeferred.await()
            val gradientAngles = gradientAnglesDeferred.await()

            val gaussianFilter = GaussianFilter(windowSize / 2 * 2 + 1)

            val step = Math.PI * 2 / histogramSize

            val lowerBorder = windowSize / 2
            val higherBorder =
                if (windowSize % 2 == 0)
                    abs(lowerBorder) - 1
                else
                    abs(lowerBorder)

            val bordersRange = (-lowerBorder..higherBorder)
            val cellSize = windowSize.toDouble() / histogramRowSize

            val descriptors = featurePoints.map { point ->

                val turnAngles = calculateAreaOrientationAngle(
                    point.x,
                    point.y,
                    bordersRange,
                    gaussianFilter,
                    gradientValues,
                    36
                )

                val histogramsBuilders =
                    (0 until histogramRowSize).map {
                        (0 until histogramRowSize).map {
                            HistogramBuilder(step, histogramSize)
                        }
                    }


                bordersRange.forEach { i ->
                    bordersRange.forEach { j ->
                        turnAngles.forEach { turnAngle ->

                            val newI = i * cos(turnAngle) + j * sin(turnAngle)
                            val newJ = j * cos(turnAngle) - i * sin(turnAngle)

                            val newIRounded = floor(newI).toInt()
                            val newJRounded = floor(newJ).toInt()

                            if (newIRounded in bordersRange && newJRounded in bordersRange) {
                                val gradientValue =
                                    gradientValues.getPixelValue(
                                        point.x + i,
                                        point.y + j
                                    )

                                val gradientValueBlur = gradientValue *
                                        gaussianFilter
                                            .getValue(
                                                newIRounded,
                                                newJRounded
                                            )

                                val gradientAngle = normalizeAngle(
                                    gradientAngles.getPixelValue(
                                        point.x + i,
                                        point.y + j
                                    ) + turnAngle
                                )

                                val x = (newI + abs(lowerBorder)) / cellSize
                                val y = (newJ + abs(lowerBorder)) / cellSize
                                val cellRadius = cellSize / 2.0
                                val cellCenterX = -lowerBorder + floor(x).toInt() * cellSize + cellRadius
                                val cellCenterY = -lowerBorder + floor(y).toInt() * cellSize + cellRadius

                                val xDif = newI - cellCenterX
                                val cx1 = if (xDif > 0) xDif else (cellSize - abs(xDif))
                                val cx2 = cellSize - cx1
                                val yDif = newJ - cellCenterY
                                val cy1 = if (yDif > 0) yDif else (cellSize - abs(yDif))
                                val cy2 = cellSize - cy1

                                val distance =
                                    sqrt(cx2.pow(2) + cy1.pow(2)) +
                                            sqrt(cx2.pow(2) + cy2.pow(2)) +
                                            sqrt(cx1.pow(2) + cy2.pow(2)) +
                                            sqrt(
                                                cx1.pow(2) + cy1.pow(2)
                                            )

                                val hIndexI = floor((newIRounded + abs(lowerBorder)) / cellSize).toInt()
                                val hIndexJ = floor((newJRounded + abs(lowerBorder)) / cellSize).toInt()

                                val rightBottom = sqrt(cx1.pow(2) + cy1.pow(2)) / distance
                                val leftBottom = sqrt(cx2.pow(2) + cy1.pow(2)) / distance
                                val topLeft = sqrt(cx2.pow(2) + cy2.pow(2)) / distance
                                val topRight = sqrt(cx1.pow(2) + cy2.pow(2)) / distance

                                val values = listOf(topLeft, leftBottom, topRight, rightBottom)

                                val rangeX = mutableListOf<Int>()
                                val rangeY = mutableListOf<Int>()

                                if (newI < cellCenterX && newJ < cellCenterY) {
                                    rangeX.add(-1)
                                    rangeX.add(0)
                                    rangeY.add(-1)
                                    rangeY.add(0)
                                } else if (newI > cellCenterX && newJ < cellCenterY) {
                                    rangeX.add(0)
                                    rangeX.add(1)
                                    rangeY.add(-1)
                                    rangeY.add(0)
                                } else if (newI > cellCenterX && newJ > cellCenterY) {
                                    rangeX.add(0)
                                    rangeX.add(1)
                                    rangeY.add(0)
                                    rangeY.add(1)
                                } else {
                                    rangeX.add(-1)
                                    rangeX.add(0)
                                    rangeY.add(0)
                                    rangeY.add(1)
                                }
                                var iter = 0
                                for (i1 in rangeX) {
                                    for (j1 in rangeY) {
                                        try {
                                            val coef = values[iter]
                                            iter++
                                            histogramsBuilders[hIndexI + i1][hIndexJ + j1]
                                                .addGradient(
                                                    gradientValueBlur * coef,
                                                    gradientAngle
                                                )
                                        } catch (ex: IndexOutOfBoundsException) {

                                        }
                                    }
                                }

                                /*data class Distribution(
                                    val x: Int,
                                    val y: Int,
                                    val distance: Double
                                )

                                val distributions = mutableListOf<Distribution>()
                                var sum = 0.0
                                for (dx in (-1 .. 1)) {
                                    for (dy in  (-1 .. 1)) {
                                        if (x + dx < 0 || x + dx >= histogramRowSize || y + dy < 0 || y + dy >= histogramRowSize)
                                            continue
                                        if (dx * pointXSign >= 0 && dy * pointYSign >= 0) {
                                            val neighbourX = -lowerBorder + floor(x + dx).toInt() * cellSize + cellRadius
                                            val neighbourY = -lowerBorder + floor(y + dy).toInt() * cellSize + cellRadius
                                            val distance = sqrt((neighbourX - newI).pow(2) + (neighbourY - newJ).pow(2))
                                            distributions.add(Distribution(floor(x + dx).roundToInt(), floor(y + dy).roundToInt(), distance))
                                            sum += distance
                                        }
                                    }
                                }

                                distributions.forEach {
                                    histogramsBuilders[it.x][it.y]
                                        .addGradient(gradientValueBlur * (1 - it.distance / sum), gradientAngle)
                                }*/
                            }
                        }

                    }
                }


                Descriptor(
                    histogramsBuilders
                        .flatMap { it.map { h -> h.build() } },
                    point
                ).normalize()
            }
            ImageDescriptors(image, descriptors)
        }


        fun calculateAreaOrientationAngle(
            i: Int,
            j: Int,
            bordersRange: IntRange,
            gaussianFilter: GaussianFilter,
            gradientValues: GrayScaledImage,
            histogramSize: Int
        ): List<Double> {
            val step = Math.PI * 2 / histogramSize
            val histogramBuilder = HistogramBuilder(step, histogramSize)

            val pairs = bordersRange.flatMap { x ->
                bordersRange.map { y ->
                    val xDerivativeSubtraction =
                        gradientValues.getPixelValue(i + x, j + y + 1) -
                                gradientValues.getPixelValue(i + x, j + y - 1)

                    val yDerivativeSubtraction =
                        gradientValues.getPixelValue(i + x + 1, j + y) -
                                gradientValues.getPixelValue(i + x - 1, j + y)

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
                            xDerivativeSubtraction,
                            yDerivativeSubtraction
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
    var point: Point

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

