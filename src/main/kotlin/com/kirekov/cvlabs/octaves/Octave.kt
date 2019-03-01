package com.kirekov.cvlabs.octaves

import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import kotlin.math.pow
import kotlin.streams.toList

class Octave(private val octaveElements: Array<OctaveElement>) : Iterable<OctaveElement> {

    override fun iterator(): Iterator<OctaveElement> {
        return octaveElements.iterator()
    }

    val size = octaveElements.size

    fun getOctaveElement(index: Int) = octaveElements[index]

}

fun getPixelValueFromOctaves(octaves: Array<Octave>, x: Int, y: Int, sigma: Double): Double {
    val globalSigmaMin = octaves.first().first().globalSigma

    var octaveIndex =
        Math.floor((Math.log(sigma) - Math.log(globalSigmaMin)) / Math.log(2.0)).toInt()

    if (octaveIndex >= octaves.size)
        octaveIndex = octaves.size - 1

    val q = Math.pow(2.0, 1.0 / (octaves[0].size - 1))

    val min = octaves[octaveIndex].first().globalSigma

    val elementIndexRaw =
        (Math.log(sigma) - Math.log(min)) / Math.log(q)

    val elementIndex: Int

    if (elementIndexRaw >= octaves[octaveIndex].size - 1)
        elementIndex = Math.floor(elementIndexRaw).toInt()
    else if (elementIndexRaw <= 0) {
        elementIndex = 0
    } else {
        val upperIndex = Math.ceil(elementIndexRaw).toInt()
        val belowIndex = Math.floor(elementIndexRaw).toInt()
        val upperSigma = octaves[octaveIndex].getOctaveElement(upperIndex).globalSigma
        val belowSigma = octaves[octaveIndex].getOctaveElement(belowIndex).globalSigma
        if (Math.abs(upperSigma - sigma) < Math.abs(belowSigma - sigma)) {
            elementIndex = upperIndex
        } else {
            elementIndex = belowIndex
        }
    }

    val newX = x / Math.pow(2.0, octaveIndex.toDouble()).toInt()
    val newY = y / Math.pow(2.0, octaveIndex.toDouble()).toInt()

    println("asked: sigma = ${sigma}, x = ${x}, y = ${y}")
    println("returned: x = $newX, y = $newY, octaveIndex = ${octaveIndex}, elementIndex = ${elementIndexRaw}")

    return octaves[octaveIndex].getOctaveElement(elementIndex).image.getPixelValue(newX, newY)
}

fun generateOctavesFrom(
    octavesCount: Int, shrinksCount: Int,
    sigma0: Double, image: GrayScaledImage, imageSigma: Double = 0.5
): Array<Octave> {
    val octaves = mutableListOf<Octave>()

    val k = 2.0.pow(1 / shrinksCount.toDouble())
    val deltaSigma = calculateDeltaSigma(imageSigma, sigma0)
    var startImage = image.applyFilter(GaussianFilter(deltaSigma))
    var globalSigma = sigma0
    (0 until octavesCount).forEach { i ->
        val octave = generateOneOctaveParallel(shrinksCount, sigma0, startImage, k, globalSigma)
        octaves.add(octave)
        globalSigma *= 2
        startImage = octave.last().image.getHalfSizeImage()
    }

    return octaves.toTypedArray()
}

private fun generateOneOctaveParallel(
    shrinksCount: Int,
    sigma0: Double,
    startImage: GrayScaledImage,
    k: Double,
    globalSigma: Double
): Octave {

    val startElement = OctaveElement(startImage, sigma0, globalSigma)

    val oldSigma = startElement.localSigma

    val octavesDeferred =
        (1..shrinksCount).toList().parallelStream().map { i ->

            val newSigma = oldSigma * k.pow(i)
            val deltaSigma = calculateDeltaSigma(oldSigma, newSigma)

            val newImage = startElement.image.applyFilter(GaussianFilter(sigma = deltaSigma))

            OctaveElement(newImage, newSigma, globalSigma * k.pow(i))
        }

    val octaveElements = mutableListOf<OctaveElement>()
    octaveElements.add(startElement)
    octaveElements.addAll(octavesDeferred.toList())

    return Octave(octaveElements.toTypedArray())
}

private fun calculateDeltaSigma(oldSigma: Double, newSigma: Double): Double {
    return Math.sqrt(newSigma.pow(2) - oldSigma.pow(2))
}