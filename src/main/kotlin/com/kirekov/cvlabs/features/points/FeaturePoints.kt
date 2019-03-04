package com.kirekov.cvlabs.features.points

import java.util.*
import java.util.stream.Collectors
import kotlin.streams.toList

class FeaturePoints(val imageWidth: Int, val imageHeight: Int, private val coordinates: Array<Point>) {

    val size = coordinates.size

    fun getPoint(index: Int): Point {
        return coordinates[index]
    }

    fun filterByAdaptiveNonMaximumSuppression(count: Int): FeaturePoints {
        val newCoordinates = coordinates.toMutableList()
        var r = 0.9

        fun getNearbyPoints(coordinates: MutableList<Point>, index: Int, r: Double)
                : Collection<Point> {
            val point = coordinates[index]
            val nearByPoints = (0 until coordinates.size).toList().parallelStream().map { i ->
                if (coordinates[i].distance(point) <= r && coordinates[i].value > point.value) {
                    coordinates[i]
                } else {
                    null
                }
            }
                .filter { it != null }
                .map { Optional.ofNullable(it).map { x -> x as Point }.get() }
                .toList()
            return nearByPoints
        }

        while (newCoordinates.size > count) {
            val pointsToRemove = (0 until newCoordinates.size).toList().parallelStream().flatMap { i ->
                getNearbyPoints(newCoordinates, i, r).toList().stream()
            }.collect(Collectors.toSet())

            newCoordinates.removeAll(pointsToRemove)
            r += 0.9
        }

        return FeaturePoints(imageWidth, imageHeight, newCoordinates.toTypedArray())
    }

}