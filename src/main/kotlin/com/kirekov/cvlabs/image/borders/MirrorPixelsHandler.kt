package com.kirekov.cvlabs.image.borders

import com.kirekov.cvlabs.image.GrayScaledImage

class MirrorPixelsHandler :
    ImagePixelsHandler {

    private fun getMirrorCoordinates(i: Int, j: Int, width: Int, height: Int): Pair<Int, Int> {
        var (newI, newJ) = Pair(i, j)
        if (i < 0)
            newI = width + i
        else if (i >= width)
            newI = i - width

        if (j < 0)
            newJ = height + j
        else if (j >= height)
            newJ = j - height
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