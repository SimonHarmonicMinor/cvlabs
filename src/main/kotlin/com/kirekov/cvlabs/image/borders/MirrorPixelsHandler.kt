package com.kirekov.cvlabs.image.borders

import com.kirekov.cvlabs.image.GrayScaledImage
import kotlin.math.abs

class MirrorPixelsHandler :
    ImagePixelsHandler {

    private fun getMirrorCoordinates(i: Int, j: Int, width: Int, height: Int): Pair<Int, Int> {
        var (newI, newJ) = Pair(i, j)
        if (i < 0)
            newI = abs(i) % width
        else if (i >= width)
            newI = (i - width) % width


        if (j < 0)
            newJ = abs(j) % height
        else if (j >= height)
            newJ = (j - height) % height

        return Pair(newI, newJ)
    }

    override fun getPixelValue(
        i: Int,
        j: Int,
        image: GrayScaledImage
    ): Double {
        val (newI, newJ) = getMirrorCoordinates(i, j, image.width, image.height)
        return image.getPixelValue(newI * image.height + newJ)
    }

}