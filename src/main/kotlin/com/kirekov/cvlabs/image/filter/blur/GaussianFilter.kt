package com.kirekov.cvlabs.image.filter.blur

import com.kirekov.cvlabs.image.filter.SeparableFilter
import kotlin.math.pow

class GaussianFilter : SeparableFilter {

    private val sigma: Double
    private val _size: Int

    override val size: Int
        get() = _size

    constructor(sigma: Double) {
        val halfSize = Math.floor(sigma * 3).toInt()
        this.sigma = sigma
        this._size = halfSize * 2 + 1
    }

    constructor(size: Int) {
        if (size % 2 == 0)
            throw IllegalArgumentException("Размер фильтра Гаусса должен быть нечетным")
        this._size = size
        this.sigma = (size - 1).toDouble() / 6
    }

    override fun getVerticalFilter1D(): DoubleArray {
        val endPos = (size - 1) / 2
        val startPos = endPos * -1
        return (startPos..endPos).map { x ->
            getValue(x, endPos)
        }.toDoubleArray()
    }

    override fun getHorizontalFilter1D(): DoubleArray {
        return getVerticalFilter1D()
    }

    override fun getValue(x: Int, y: Int): Double {
        val top = Math.exp(-(x * x + y * y) / (2 * sigma.pow(2)))
        val bottom = 2 * Math.PI * sigma.pow(2)
        return top / bottom
    }

}