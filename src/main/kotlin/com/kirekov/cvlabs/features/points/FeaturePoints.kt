package com.kirekov.cvlabs.features.points

class FeaturePoints(val imageWidth: Int, val imageHeight: Int, private val coordinates: Array<Point>) {

    val size = coordinates.size

    fun getPoint(index: Int): Point {
        return coordinates[index]
    }

    fun forEach(transform: (point: Point) -> Unit) {
        coordinates.forEach(transform)
    }

}