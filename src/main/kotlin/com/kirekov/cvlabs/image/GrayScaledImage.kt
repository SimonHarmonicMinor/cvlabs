package com.kirekov.cvlabs.image

import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.Point
import com.kirekov.cvlabs.features.points.descriptors.DescriptorsDistance
import com.kirekov.cvlabs.features.points.descriptors.ImageDescriptors
import com.kirekov.cvlabs.image.borders.ImagePixelsHandler
import com.kirekov.cvlabs.image.filter.Filter
import com.kirekov.cvlabs.image.filter.SeparableFilter
import com.kirekov.cvlabs.image.normalization.normalize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.util.*
import kotlin.math.max
import kotlin.streams.toList

class GrayScaledImage(
    val width: Int,
    val height: Int,
    private val imgArray: DoubleArray,
    private val imagePixelsHandler: ImagePixelsHandler
) {

    fun getPixelValue(x: Int, y: Int): Double {
        try {
            return imagePixelsHandler.getPixelValue(x, y, this)
        } catch (ex: Exception) {
            throw Exception()
        }

    }

    fun getPixelValue(index: Int): Double {
        return imgArray[index]
    }

    fun applyFilter(filter: Filter): GrayScaledImage {
        val arr = (0 until width).toList().parallelStream().map { i ->
            (0 until height).toList().stream().map { j ->
                applyFilterToPoint(filter, i, j)
            }.toList()
        }.toList().flatMap { it.asIterable() }.toDoubleArray()

        return GrayScaledImage(width, height, arr, imagePixelsHandler)
    }

    fun applyFilterToPoint(filter: Filter, i: Int, j: Int): Double {
        val endPos = (filter.size - 1) / 2
        val startPos = endPos * -1

        return (startPos..endPos).flatMap { x ->
            (startPos..endPos).map { y ->
                val imgValue = getPixelValue(i + x, j + y)
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

    fun mapImages(
        image: GrayScaledImage,
        function: (Double, Double) -> Double
    ): GrayScaledImage {
        if (image.width != this.width || image.height != image.height)
            throw IllegalArgumentException("Изображения должны быть одинакового размера")
        return mapImages(this, image, function)
    }

    private fun mapImages(
        image1: GrayScaledImage,
        image2: GrayScaledImage,
        function: (Double, Double) -> Double
    ): GrayScaledImage {
        val arr = (0 until width * height).toList().parallelStream().map { i ->
            function(image1.getPixelValue(i), image2.getPixelValue(i))
        }.toList().toDoubleArray()

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

        val size = (Math.max(width, height) / 200) * 2 + 1
        val endPos = (size - 1) / 2
        val startPos = endPos * -1;


        val bufferedImage = getBufferedImage()
        for (i in 0 until featurePoints.size) {
            val point = featurePoints.getPoint(i)
            (startPos..endPos).forEach { x ->
                (startPos..endPos).forEach { y ->
                    try {
                        bufferedImage.setRGB(point.x + x, point.y + y, Color.RED.rgb)
                    } catch (ex: ArrayIndexOutOfBoundsException) {

                    }
                }
            }
        }

        return bufferedImage
    }

    companion object {
        fun combineImagesByDescriptors(
            image1: GrayScaledImage,
            descriptors1: ImageDescriptors,
            image2: GrayScaledImage,
            descriptors2: ImageDescriptors,
            descriptorsDistance: DescriptorsDistance
        ): BufferedImage = runBlocking {

            val (bufferedImage1, bufferedImage2) =
                awaitAll(
                    GlobalScope.async { image1.getBufferedImage(descriptors1.featurePoints) },
                    GlobalScope.async { image2.getBufferedImage(descriptors2.featurePoints) }
                )
            val newWidth = image1.width + image2.width
            val newHeight = max(image1.height, image2.height)

            val newBufferedImage = BufferedImage(newWidth, newHeight, TYPE_INT_RGB)

            (0 until image1.width).forEach { x ->
                (0 until image1.height).forEach { y ->
                    newBufferedImage.setRGB(x, y, bufferedImage1.getRGB(x, y))
                }
            }

            (0 until image2.width).forEach { x ->
                (0 until image2.height).forEach { y ->
                    newBufferedImage.setRGB(
                        x + image1.width,
                        y,
                        bufferedImage2.getRGB(x, y)
                    )
                }
            }

            data class DescriptorLine(val point1: Point, val point2: Point, val value: Double)

            val descriptorLines = descriptors1.descriptors.parallelStream().map { descriptor ->
                val (minimum, nextAfter) =
                    descriptors2.findClosestTo(descriptor, descriptorsDistance, 2)
                DescriptorLine(
                    descriptor.point,
                    minimum.descriptor.point,
                    minimum.distance / nextAfter.distance
                )
            }.filter { it.value < 0.9 }
                .map { Optional.ofNullable(it).get() }
                .toList()


            val graphics = newBufferedImage.graphics

            val colors = arrayOf(
                Color.BLACK,
                Color.BLUE,
                Color.CYAN,
                Color.GREEN,
                Color.MAGENTA,
                Color.ORANGE,
                Color.PINK,
                Color.RED,
                Color.WHITE,
                Color.YELLOW
            )

            var index = 0

            descriptorLines.forEach { descriptorLine ->
                var color = colors.getOrNull(index++)
                if (color == null) {
                    index = 0
                    color = colors.get(index++)
                }

                graphics.color = color

                graphics.drawLine(
                    descriptorLine.point1.x,
                    descriptorLine.point1.y,
                    descriptorLine.point2.x + image1.width,
                    descriptorLine.point2.y
                )
            }

            newBufferedImage
        }
    }
}





