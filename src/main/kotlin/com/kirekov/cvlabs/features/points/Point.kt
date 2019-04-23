package com.kirekov.cvlabs.features.points

import kotlin.math.pow

data class Point(val x: Int, val y: Int, val value: Double) {
    fun distance(point: Point): Double {
        return Math.sqrt((x - point.x.toDouble()).pow(2) + (y - point.y.toDouble()).pow(2))
    }

    constructor(x: Int, y: Int) : this(x, y, 0.0)
}