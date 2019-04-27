package com.kirekov.cvlabs.features.points

import kotlin.math.pow

data class Point(val x: Int, val y: Int, var value: Double) {
    var angle: Double = 0.0
    var scale: Double = 0.0

    fun distance(point: Point): Double {
        return Math.sqrt((x - point.x.toDouble()).pow(2) + (y - point.y.toDouble()).pow(2))
    }
}