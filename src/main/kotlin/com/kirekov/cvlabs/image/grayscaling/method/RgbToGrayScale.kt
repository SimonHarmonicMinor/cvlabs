package com.kirekov.cvlabs.image.grayscaling.method

interface RgbToGrayScale {
    fun convert(rgb: Int): Double
}