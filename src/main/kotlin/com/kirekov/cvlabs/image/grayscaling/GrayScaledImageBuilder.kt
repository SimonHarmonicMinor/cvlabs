package com.kirekov.cvlabs.image.grayscaling


import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.borders.ImagePixelsHandler
import com.kirekov.cvlabs.image.grayscaling.method.RgbToGrayScale
import com.kirekov.cvlabs.image.normalization.normalize
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage


fun bufferedImageToGrayScaledImage(
    image: BufferedImage,
    rgbToGrayScale: RgbToGrayScale,
    imagePixelsHandler: ImagePixelsHandler
): GrayScaledImage {

    val pixels = runBlocking {
        (0 until image.width).map { i ->
            (0 until image.height).map { j ->
                rgbToGrayScale.convert(image.getRGB(i, j))
            }
        }.flatMap { x -> x.asIterable() }
            .normalize(0.0, 1.0)
            .toDoubleArray()
    }

    return GrayScaledImage(
        image.width,
        image.height,
        pixels,
        imagePixelsHandler
    )
}