package com.kirekov.cvlabs.image.grayscaling.method

import java.awt.Color

class HdtvScaling : RgbToGrayScale {
    override fun convert(rgb: Int): Double {
        val color = Color(rgb)
        return 0.213 * color.red + 0.715 * color.green + 0.072 * color.blue
    }
}