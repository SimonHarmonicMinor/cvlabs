package com.kirekov.cvlabs.image.borders

import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class MirrorPixelsHandlerTest {

    @Test
    fun getPixelValue() {
        val turnAngle = 0.3
        val x = 1
        val y = 1
        val newX = (x * cos(turnAngle) + y * sin(turnAngle))
        val newY = (y * cos(turnAngle) - x * sin(turnAngle))
        println(newX)
        println(newY)
    }
}