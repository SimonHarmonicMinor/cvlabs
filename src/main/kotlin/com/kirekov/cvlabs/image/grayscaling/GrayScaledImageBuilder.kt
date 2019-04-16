package com.kirekov.cvlabs.image.grayscaling


import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.borders.ImagePixelsHandler
import com.kirekov.cvlabs.image.grayscaling.method.RgbToGrayScale
import com.kirekov.cvlabs.image.normalization.normalize
import java.awt.image.BufferedImage


fun bufferedImageToGrayScaledImage(
    image: BufferedImage,
    rgbToGrayScale: RgbToGrayScale,
    imagePixelsHandler: ImagePixelsHandler
): GrayScaledImage {

    val pixels =
        (0 until image.width).flatMap { i ->
            (0 until image.height).map { j ->
                rgbToGrayScale.convert(image.getRGB(i, j))
            }
        }.normalize(0.0, 1.0)


    return GrayScaledImage(
        image.width,
        image.height,
        pixels,
        imagePixelsHandler
    )
}