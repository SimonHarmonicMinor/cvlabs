package com.kirekov.cvlabs.features.points.descriptors

import org.junit.Test

class HistogramBuilderTest {

    @Test
    fun getValues() {
        val histogramSize = 2
        val step = 10.0 * 2 / histogramSize
        val builder = HistogramBuilder(step, histogramSize)
        builder.addGradient(1.0, 12.0)
        val histogram = builder.build()
    }
}