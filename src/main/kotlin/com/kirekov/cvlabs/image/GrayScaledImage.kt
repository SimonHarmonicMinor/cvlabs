package com.kirekov.cvlabs.image

import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.Match
import com.kirekov.cvlabs.features.points.Point
import com.kirekov.cvlabs.features.points.descriptors.DescriptorsDistance
import com.kirekov.cvlabs.features.points.descriptors.ImageDescriptors
import com.kirekov.cvlabs.image.borders.ImagePixelsHandler
import com.kirekov.cvlabs.image.filter.Filter
import com.kirekov.cvlabs.image.filter.SeparableFilter
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import com.kirekov.cvlabs.image.normalization.normalize
import com.kirekov.cvlabs.octaves.Blob
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.lang.Math.pow
import kotlin.math.*

class GrayScaledImage(
    val width: Int,
    val height: Int,
    private val imgArray: List<Double>,
    private val imagePixelsHandler: ImagePixelsHandler
) {

    fun getPixelValue(x: Int, y: Int): Double {
        return imagePixelsHandler.getPixelValue(x, y, this)
    }

    fun getPixelValue(index: Int): Double {
        return imgArray[index]
    }

    fun applyFilter(filter: Filter): GrayScaledImage {
        val arr = (0 until width).flatMap { i ->
            (0 until height).map { j ->
                applyFilterToPoint(filter, i, j)
            }
        }

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

        val arr = (0 until width).flatMap { i ->
            (0 until height).map { j ->
                (startPos..endPos).mapIndexed { indexV, x ->
                    val horizontalSum = (startPos..endPos).mapIndexed { indexH, y ->
                        val imgValue = getPixelValue(i - x, j - y)
                        imgValue * separableFilter.getHorizontalFilter1D()[indexH]
                    }.sum()
                    horizontalSum * separableFilter.getVerticalFilter1D()[indexV]
                }.sum()
            }
        }

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
        val arr = (0 until width * height).map { i ->
            function(image1.getPixelValue(i), image2.getPixelValue(i))
        }

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
            newImageArr,
            imagePixelsHandler
        )
    }

    fun getBufferedImage(): BufferedImage {
        val bufferedImage = BufferedImage(width, height, TYPE_INT_RGB)

        val normalizedImgArray = imgArray.normalize(0.0, 255.0)

        (0 until width).forEach { i ->
            (0 until height).forEach { j ->
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
                    } catch (ex: Exception) {

                    }
                }
            }
        }

        return bufferedImage
    }

    fun getBufferedImage(blobs: List<Blob>): BufferedImage {
        val bufferedImage = getBufferedImage()
        val graphics = bufferedImage.graphics

        blobs.forEach { blob ->
            graphics.color = Color.RED
            val blobRadius = blob.radius * pow(2.0, blob.octaveIndex.toDouble())
            graphics.drawOval(
                (blob.x - blobRadius).toInt(),
                (blob.y - blobRadius).toInt(),
                (blobRadius * 2).roundToInt(),
                (blobRadius * 2).roundToInt()
            )
            val descriptorSize =
                ((blob.descriptorSizeRatio * 16) * pow(2.0, blob.octaveIndex.toDouble()))
                    .roundToInt()
            graphics.color = Color.BLUE
            graphics.drawRect(
                (blob.x - descriptorSize / 2),
                (blob.y - descriptorSize / 2),
                descriptorSize,
                descriptorSize
            )
        }


        /*val size = (Math.max(width, height) / 200) * 2 + 1
        val endPos = (size - 1) / 2
        val startPos = endPos * -1;


        for (i in 0 until blobs.size) {
            (startPos..endPos).forEach { x ->
                (startPos..endPos).forEach { y ->
                    try {
                        bufferedImage.setRGB(blobs[i].x + x, blobs[i].y + y, Color.RED.rgb)
                    } catch (ex: ArrayIndexOutOfBoundsException) {

                    }
                }
            }
        }*/

        return bufferedImage
    }

    companion object {

        fun drawAngles(
            image: GrayScaledImage,
            featurePoints: FeaturePoints
        ): Pair<BufferedImage, List<Double>> {
            val bufferedImage = image.getBufferedImage(featurePoints)
            val xDerivative = image.applyFilter(SobelFilter(SobelType.X))
            val yDerivative = image.applyFilter(SobelFilter(SobelType.Y))

            val gradientValues = xDerivative.mapImages(yDerivative) { x, y ->
                Math.sqrt(x.pow(2) + y.pow(2))
            }

            data class DescriptorLine(val point1: Point, val point2: Point, val value: Double)

            val d = 70
            val windowSize = 15
            val lowerBorder = windowSize / 2
            val higherBorder =
                if (windowSize % 2 == 0)
                    abs(lowerBorder) - 1
                else
                    abs(lowerBorder)

            val bordersRange = (-lowerBorder..higherBorder)

            val gaussianFilter = GaussianFilter(windowSize)

            val angles = mutableListOf<Double>()

            val descriptorLines = featurePoints.map { point ->

                val angle = ImageDescriptors.calculateAreaOrientationAngle(
                    point.x,
                    point.y,
                    bordersRange,
                    gaussianFilter,
                    gradientValues,
                    36
                )[0]

                angles.add(angle)
                DescriptorLine(
                    point,
                    Point(
                        point.x + (d * cos(angle))
                            .roundToInt(),
                        point.y + (d * sin(angle))
                            .roundToInt(),
                        10.0
                    ),
                    10.0
                )
            }

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
                Color.YELLOW,
                Color.DARK_GRAY,
                Color.GRAY,
                Color.LIGHT_GRAY
            )

            var index = 0

            val graphics = bufferedImage.graphics

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
                    descriptorLine.point2.x,
                    descriptorLine.point2.y
                )
            }

            return Pair(bufferedImage, angles)
        }

        fun getMatches(
            descriptors1: ImageDescriptors,
            descriptors2: ImageDescriptors,
            descriptorsDistance: DescriptorsDistance
        ): List<Match> {
            val matches = mutableListOf<Match>()
            descriptors1.descriptors.forEach { descriptor ->
                val (minimum, nextAfter) =
                    descriptors2.findClosestTo(descriptor, descriptorsDistance, 2)

                if ((minimum.distance / nextAfter.distance) < 0.8) {
                    descriptor.point.value = descriptor.scale
                    descriptor.point.scale = descriptor.scale
                    descriptor.point.angle = descriptor.angle

                    minimum.descriptor.point.value = minimum.descriptor.scale
                    minimum.descriptor.point.scale = minimum.descriptor.scale
                    minimum.descriptor.point.angle = minimum.descriptor.angle
                    matches.add(Match(descriptor.point, minimum.descriptor.point))
                }

            }
            return matches
        }

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

            val descriptorLines = descriptors1.descriptors.map { descriptor ->
                val (minimum, nextAfter) =
                    descriptors2.findClosestTo(descriptor, descriptorsDistance, 2)
                DescriptorLine(
                    descriptor.point,
                    minimum.descriptor.point,
                    minimum.distance / nextAfter.distance
                )
            }.filter { it.value < 0.8 }


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
                Color.YELLOW,
                Color.DARK_GRAY,
                Color.GRAY,
                Color.LIGHT_GRAY
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





