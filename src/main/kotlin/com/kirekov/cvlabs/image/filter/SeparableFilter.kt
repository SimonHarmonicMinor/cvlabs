package com.kirekov.cvlabs.image.filter

interface SeparableFilter : Filter {
    fun getVerticalFilter1D(): DoubleArray
    fun getHorizontalFilter1D(): DoubleArray
}