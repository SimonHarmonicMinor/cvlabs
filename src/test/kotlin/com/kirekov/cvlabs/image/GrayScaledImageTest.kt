package com.kirekov.cvlabs.image

import com.kirekov.cvlabs.image.borders.MirrorPixelsHandler
import com.kirekov.cvlabs.image.grayscaling.bufferedImageToGrayScaledImage
import com.kirekov.cvlabs.image.grayscaling.method.HdtvScaling
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage

class GrayScaledImageTest {

    @Test
    fun getPixelValue() {
        val width = 9
        val height = 7

        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        (0 until width).forEach { i ->
            (0 until height).forEach { j ->
                bufferedImage.setRGB(i, j, Color.WHITE.rgb)
            }
        }

        bufferedImage.setRGB(2, 1, Color.BLACK.rgb)
        bufferedImage.setRGB(2, 2, Color.BLACK.rgb)
        bufferedImage.setRGB(2, 3, Color.BLACK.rgb)

        val image = bufferedImageToGrayScaledImage(
            bufferedImage,
            HdtvScaling(),
            MirrorPixelsHandler()
        )

        assert(image.getPixelValue(2, 1).toInt() == 0)
        assert(image.getPixelValue(2, 2).toInt() == 0)
        assert(image.getPixelValue(2, 3).toInt() == 0)

    }
}