package com.kirekov.cvlabs.features.points.descriptors

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

interface DescriptorsDistance {
    fun calculateDistance(descriptor1: Descriptor, descriptor2: Descriptor): Double
}

class EuclidDistance : DescriptorsDistance {
    override fun calculateDistance(
        descriptor1: Descriptor,
        descriptor2: Descriptor
    ): Double {
        assert(descriptor1.values.size == descriptor2.values.size)

        return sqrt(
            SumOfSquaredDistances()
                .calculateDistance(descriptor1, descriptor2)
        )
    }
}

class ManhattanMetric : DescriptorsDistance {
    override fun calculateDistance(
        descriptor1: Descriptor,
        descriptor2: Descriptor
    ): Double {
        assert(descriptor1.values.size == descriptor2.values.size)

        return descriptor1.values.mapIndexed { index, item ->
            (item - descriptor2.values[index]).absoluteValue
        }.sum()
    }
}

class SumOfSquaredDistances : DescriptorsDistance {
    override fun calculateDistance(
        descriptor1: Descriptor,
        descriptor2: Descriptor
    ): Double {
        assert(descriptor1.values.size == descriptor2.values.size)

        return descriptor1.values.mapIndexed { index, item ->
            (item - descriptor2.values[index]).pow(2)
        }.sum()
    }

}