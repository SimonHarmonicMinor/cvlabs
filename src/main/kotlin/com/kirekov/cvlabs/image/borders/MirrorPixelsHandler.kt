package com.kirekov.cvlabs.image.borders

import com.kirekov.cvlabs.image.GrayScaledImage
import kotlin.math.absoluteValue

class MirrorPixelsHandler :
    ImagePixelsHandler {

    private fun getMirrorCoordinates(i: Int, j: Int, width: Int, height: Int): Pair<Int, Int> {
        var (newI, newJ) = Pair(i, j)
        if (i < 0)
            newI = (width - 1) - (i.absoluteValue % width)

        else if (i >= width)
            newI = i % width


        if (j < 0)
            newJ = (height - 1) - (j.absoluteValue % height)
        else if (j >= height)
            newJ = j % height

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