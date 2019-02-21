package com.kirekov.cvlabs.image.borders

import com.kirekov.cvlabs.image.GrayScaledImage

/**
 * Интерфейс для получения пикселя изображения
 * с различной обработкой граничных эффектов
 */
interface ImagePixelsHandler {
    fun getPixelValue(
        i: Int,
        j: Int,
        image: GrayScaledImage
    ): Double
}