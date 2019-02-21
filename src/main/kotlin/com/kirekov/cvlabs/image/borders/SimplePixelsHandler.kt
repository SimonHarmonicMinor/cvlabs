package com.kirekov.cvlabs.image.borders

import com.kirekov.cvlabs.image.GrayScaledImage

class SimplePixelsHandler : ImagePixelsHandler {

    override fun getPixelValue(
        i: Int,
        j: Int,
        image: GrayScaledImage
    ): Double {
        return try {
            image.getPixelValue(i * image.height + j)
        } catch (ex: IndexOutOfBoundsException) {
            0.0
        }
    }

}