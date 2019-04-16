package com.kirekov.cvlabs.features.points

import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter

class FeaturePoints(
    val imageWidth: Int,
    val imageHeight: Int,
    private val coordinates: List<Point>
) : Iterable<Point> {

    override fun iterator(): Iterator<Point> {
        return coordinates.iterator()
    }

    val size = coordinates.size

    fun getPoint(index: Int): Point {
        return coordinates[index]
    }

    fun filterByAdaptiveNonMaximumSuppression(count: Int): FeaturePoints {
        val newCoordinates = coordinates.sortedByDescending { it.value }.toMutableList()
        var r = 0.9
        while (newCoordinates.size > count) {
            val pointsToRemove = mutableListOf<Point>()
            for (i in 0 until newCoordinates.size) {
                val point = newCoordinates[i]
                for (j in i + 1 until newCoordinates.size) {
                    if (newCoordinates[j].distance(point) <= r) {
                        pointsToRemove.add(point)
                        break
                    }
                }
            }
            newCoordinates.removeAll(pointsToRemove)
            r += 0.9
        }

        return FeaturePoints(imageWidth, imageHeight, newCoordinates)
    }

    companion object {
        fun ofMorravec(
            size: Int,
            offset: Int,
            threshold: Double,
            image: GrayScaledImage
        ): FeaturePointsCalculator {
            return MorravecCalculator(size, offset, threshold, image)
        }

        fun ofHarris(
            size: Int,
            threshold: Double,
            image: GrayScaledImage,
            harrisCalculationMethod: HarrisCalculationMethod
        ): FeaturePointsCalculator {
            return HarrisCalculator(
                size,
                threshold,
                image.applyFilter(GaussianFilter(1.6)),
                harrisCalculationMethod
            )
        }
    }

}
