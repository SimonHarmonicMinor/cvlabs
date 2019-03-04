package com.kirekov.cvlabs.image

import com.kirekov.cvlabs.extension.add
import com.kirekov.cvlabs.features.points.FeaturePointOperator
import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.Point
import com.kirekov.cvlabs.image.borders.ImagePixelsHandler
import com.kirekov.cvlabs.image.filter.Filter
import com.kirekov.cvlabs.image.filter.SeparableFilter
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import com.kirekov.cvlabs.image.normalization.normalize
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.util.*
import kotlin.math.pow
import kotlin.streams.toList

class GrayScaledImage(
    val width: Int,
    val height: Int,
    private val imgArray: DoubleArray,
    private val imagePixelsHandler: ImagePixelsHandler
) {

    fun getPixelValue(x: Int, y: Int): Double {
        return imagePixelsHandler.getPixelValue(x, y, this)
    }

    fun getPixelValue(index: Int): Double {
        return imgArray[index]
    }

    fun applyFilter(filter: Filter): GrayScaledImage {
        val arr = (0 until width).toList().parallelStream().map { i ->
            (0 until height).toList().parallelStream().map { j ->
                applyFilterToPoint(filter, i, j)
            }.toList()
        }.toList().flatMap { it.asIterable() }.toDoubleArray()

        return GrayScaledImage(width, height, arr, imagePixelsHandler)
    }

    private fun applyFilterToPoint(filter: Filter, i: Int, j: Int): Double {
        val endPos = (filter.size - 1) / 2
        val startPos = endPos * -1

        return (startPos..endPos).flatMap { x ->
            (startPos..endPos).map { y ->
                val imgValue = getPixelValue(i - x, j - y)
                imgValue * filter.getValue(x, y)
            }
        }.sum()
    }

    fun applySeparableFilter(separableFilter: SeparableFilter): GrayScaledImage {
        val endPos = (separableFilter.size - 1) / 2
        val startPos = endPos * -1

        val arr = (0 until width).map { i ->
            (0 until height).map { j ->
                (startPos..endPos).mapIndexed { indexV, x ->
                    val horizontalSum = (startPos..endPos).mapIndexed { indexH, y ->
                        val imgValue = getPixelValue(i - x, j - y)
                        imgValue * separableFilter.getHorizontalFilter1D()[indexH]
                    }.sum()
                    horizontalSum * separableFilter.getVerticalFilter1D()[indexV]
                }.sum()
            }
        }.flatMap { x -> x.asIterable() }.toDoubleArray()

        return GrayScaledImage(width, height, arr, imagePixelsHandler)
    }

    fun applyMoravecOperator(featurePointOperator: FeaturePointOperator): FeaturePoints {
        val offsetList = listOf(-featurePointOperator.offset, 0, featurePointOperator.offset)
        val endPos = (featurePointOperator.size - 1) / 2
        val startPos = endPos * -1

        val pointsValues = (0 until width).toList().parallelStream().flatMap { i ->
            (0 until height).toList().stream().map { j ->
                val value = applyMoravecToPoint(i, j, startPos, endPos, offsetList)
                Triple(i, j, value)
            }.filter { x -> x.third > featurePointOperator.threshold }
        }.toList()
            .map { Optional.ofNullable(it).map { v -> Point(v.first, v.second, v.third) }.get() }
            .toTypedArray()
        return FeaturePoints(width, height, pointsValues)
    }

    private fun applyMoravecToPoint(i: Int, j: Int, startPos: Int, endPos: Int, offsetList: List<Int>): Double {
        val res = offsetList.flatMap { offsetX ->
            offsetList.map { offsetY ->
                (startPos..endPos).flatMap { x ->
                    (startPos..endPos).map { y ->
                        val imgValue = getPixelValue(i - x, j - y)
                        val offsetImgValue = getPixelValue(i - x - offsetX, j - y - offsetY)
                        (imgValue - offsetImgValue).pow(2)
                    }
                }.sum()
            }
        }.sorted()[1]
        return res
    }

    fun applyHarrisOperator(featurePointOperator: FeaturePointOperator): FeaturePoints {
        val gaussianFilter = GaussianFilter(featurePointOperator.size)

        val pointsValues = (0 until width).toList().parallelStream().flatMap { i ->
            (0 until height).toList().stream().map { j ->
                val pointResult = applyHarrisToPoint(i, j, gaussianFilter)
                val a = pointResult.first
                val b = pointResult.second
                val c = pointResult.third
                Triple(i, j, a * c - b.pow(2) + 0.05 * (a + c))
            }.filter { x -> x.third > featurePointOperator.threshold }
        }.toList()
            .map { Optional.ofNullable(it).map { v -> Point(v.first, v.second, v.third) }.get() }
            .toTypedArray()

        return FeaturePoints(width, height, pointsValues)
    }

    private fun applyHarrisToPoint(i: Int, j: Int, gaussianFilter: GaussianFilter)
            : Triple<Double, Double, Double> {
        val endPos = (gaussianFilter.size - 1) / 2
        val startPos = endPos * -1

        val result = (startPos..endPos).flatMap { x ->
            (startPos..endPos).map { y ->
                val filterValue = gaussianFilter.getValue(x, y)
                val xDerivative =
                    applyFilterToPoint(SobelFilter(SobelType.X), i - x, j - y)
                val yDerivative =
                    applyFilterToPoint(SobelFilter(SobelType.Y), i - x, j - y)
                Triple(
                    xDerivative.pow(2) * gaussianFilter.getValue(x, y),
                    xDerivative * yDerivative * gaussianFilter.getValue(x, y),
                    yDerivative.pow(2) * gaussianFilter.getValue(x, y)
                )
            }
        }.reduce { x, y -> x.add(y) }

        return result
    }


    fun sumImagesBySquare(image: GrayScaledImage): GrayScaledImage {
        if (image.width != this.width || image.height != image.height)
            throw IllegalArgumentException("Изображения должны быть одинакового размера")

        return sumImagesBySquare(image, this)
    }

    private fun sumImagesBySquare(image1: GrayScaledImage, image2: GrayScaledImage): GrayScaledImage {

        val arr = (0 until width * height).map { i ->
            Math.sqrt(
                image1.getPixelValue(i).pow(2) + image2.getPixelValue(i).pow(2)
            )
        }.toDoubleArray()

        return GrayScaledImage(width, height, arr, imagePixelsHandler)
    }

    fun getHalfSizeImage(): GrayScaledImage {
        val newImageArr = mutableListOf<Double>()

        (0 until width).forEach { i ->
            if (i % 2 != 0) {
                (0 until height).forEach { j ->
                    if (j % 2 != 0)
                        newImageArr.add(getPixelValue(i, j))
                }
            }
        }

        return GrayScaledImage(
            width / 2,
            height / 2,
            newImageArr.toDoubleArray(),
            imagePixelsHandler
        )
    }

    fun getBufferedImage(): BufferedImage {
        val bufferedImage = BufferedImage(width, height, TYPE_INT_RGB)

        val normalizedImgArray = imgArray.normalize(0.0, 255.0)

        (0 until width).map { i ->
            (0 until height).map { j ->
                val pixelValue = normalizedImgArray[height * i + j]
                val color = Color(pixelValue.toInt(), pixelValue.toInt(), pixelValue.toInt())
                bufferedImage.setRGB(i, j, color.rgb)
            }
        }

        return bufferedImage
    }

    fun getBufferedImage(featurePoints: FeaturePoints): BufferedImage {
        if (featurePoints.imageHeight != height || featurePoints.imageWidth != width) {
            throw IllegalArgumentException("Высота и ширина должны совпадать")
        }

        val bufferedImage = getBufferedImage()
        for (i in 0 until featurePoints.size) {
            val point = featurePoints.getPoint(i)
            bufferedImage.setRGB(point.x, point.y, Color.RED.rgb)
        }

        return bufferedImage
    }
}





