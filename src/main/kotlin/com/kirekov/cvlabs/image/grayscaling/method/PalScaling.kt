package com.kirekov.cvlabs.image.grayscaling.method

import java.awt.Color

class PalScaling : RgbToGrayScale {
    override fun convert(rgb: Int): Double {
        val color = Color(rgb)
        return 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    }
}