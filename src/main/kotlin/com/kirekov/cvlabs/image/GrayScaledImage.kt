package com.kirekov.cvlabs.image

import com.kirekov.cvlabs.image.borders.ImagePixelsHandler
import com.kirekov.cvlabs.image.filter.Filter
import com.kirekov.cvlabs.image.filter.SeparableFilter
import com.kirekov.cvlabs.image.normalization.normalize
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import kotlin.math.pow

class GrayScaledImage(
    val width: Int,
    val height: Int,
    private val imgArray: DoubleArray,
    var imagePixelsHandler: ImagePixelsHandler
) {

    private fun getPixelValue(x: Int, y: Int): Double {
        return imagePixelsHandler.getPixelValue(x, y, this)
    }

    fun getPixelValue(index: Int): Double {
        return imgArray[index]
    }

    fun applyFilter(filter: Filter): GrayScaledImage {

        val endPos = (filter.size - 1) / 2
        val startPos = endPos * -1

        val arr = (0 until width).map { i ->
            (0 until height).map { j ->

                (startPos..endPos).flatMap { x ->
                    (startPos..endPos).map { y ->
                        val imgValue = getPixelValue(i - x, j - y)
                        imgValue * filter.getValue(x, y)
                    }
                }.sum()

            }
        }.flatMap { x -> x.asIterable() }.toDoubleArray()

        return GrayScaledImage(width, height, arr, imagePixelsHandler)
    }

    fun applyFilter(separableFilter: SeparableFilter): GrayScaledImage {
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
}

