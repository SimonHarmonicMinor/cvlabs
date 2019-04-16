package com.kirekov.cvlabs.image.filter.derivative.sobel

import com.kirekov.cvlabs.image.filter.SeparableFilter

class SobelFilter(sobelType: SobelType) : SeparableFilter {

    private val filterArray: Array<Array<Int>> =
        if (sobelType == SobelType.Y)
            arrayOf(
                arrayOf(-1, 0, 1),
                arrayOf(-2, 0, 2),
                arrayOf(-1, 0, 1)
            )
        else
            arrayOf(
                arrayOf(-1, -2, -1),
                arrayOf(0, 0, 0),
                arrayOf(1, 2, 1)
            )
    private val verticalFilter: DoubleArray
    private val horizontalFilter: DoubleArray

    override val size: Int
        get() = 3

    init {
        verticalFilter = arrayOf(filterArray[0][0], filterArray[1][0], filterArray[2][0])
            .map { item -> item.toDouble() }
            .toDoubleArray()
        horizontalFilter = arrayOf(filterArray[0][0], filterArray[0][1], filterArray[0][2])
            .map { item -> item.toDouble() }
            .toDoubleArray()
    }

    override fun getVerticalFilter1D(): DoubleArray {
        return verticalFilter
    }

    override fun getHorizontalFilter1D(): DoubleArray {
        return horizontalFilter
    }


    override fun getValue(x: Int, y: Int): Double {
        return filterArray[x + 1][y + 1].toDouble()
    }
}