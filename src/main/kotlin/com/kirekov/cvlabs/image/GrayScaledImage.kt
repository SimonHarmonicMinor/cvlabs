package com.kirekov.cvlabs.image

import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.image.borders.ImagePixelsHandler
import com.kirekov.cvlabs.image.filter.Filter
import com.kirekov.cvlabs.image.filter.SeparableFilter
import com.kirekov.cvlabs.image.normalization.normalize
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
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

    fun applyFilterToPoint(filter: Filter, i: Int, j: Int): Double {
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
}





