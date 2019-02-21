package com.kirekov.cvlabs

import com.kirekov.cvlabs.image.borders.MirrorPixelsHandler
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelFilter
import com.kirekov.cvlabs.image.filter.derivative.sobel.SobelType
import com.kirekov.cvlabs.image.grayscaling.bufferedImageToGrayScaledImage
import com.kirekov.cvlabs.image.grayscaling.method.HdtvScaling
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO

fun main() = runBlocking<Unit> {
    val bufferedImage = ImageIO.read(File("img.jpg"))
    val grayScaledImage = bufferedImageToGrayScaledImage(
        bufferedImage,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val sobelXCoroutine = async {
        grayScaledImage.applyFilter(SobelFilter(SobelType.X))
    }

    val sobelYCoroutine = async {
        grayScaledImage.applyFilter(SobelFilter(SobelType.Y))
    }

    val gaussianCoroutine = async {
        grayScaledImage.applyFilter(GaussianFilter(7))
    }

    val sobelX = sobelXCoroutine.await()
    val sobelY = sobelYCoroutine.await()
    val newImage = sobelX
        .sumImagesBySquare(sobelY)

    ImageIO.write(newImage.getBufferedImage(), "jpg", File("outGradient.jpg"))
    ImageIO.write(sobelX.getBufferedImage(), "jpg", File("outX.jpg"))
    ImageIO.write(sobelY.getBufferedImage(), "jpg", File("outY.jpg"))
    ImageIO.write(
        gaussianCoroutine.await().getBufferedImage(),
        "jpg", File("gaussian2.jpg")
    )
}