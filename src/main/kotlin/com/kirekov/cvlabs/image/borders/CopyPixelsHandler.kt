package com.kirekov.cvlabs.image.borders

import com.kirekov.cvlabs.image.GrayScaledImage

class CopyPixelsHandler : ImagePixelsHandler {

    private fun getNearestPixelCellCoordinates(
        i: Int,
        j: Int,
        width: Int,
        height: Int
    ): Pair<Int, Int> {
        var (newI, newJ) = Pair(i, j)
        if (i < 0)
            newI = 0
        else if (i >= width)
            newI = width - 1

        if (j < 0)
            newJ = 0
        else if (j >= height)
            newJ = height - 1
        return Pair(newI, newJ)
    }

    override fun getPixelValue(
        i: Int,
        j: Int,
        image: GrayScaledImage
    ): Double {
        val (newI, newJ) = getNearestPixelCellCoordinates(i, j, image.width, image.height)
        return image.getPixelValue(newI * image.height + newJ)
    }

}