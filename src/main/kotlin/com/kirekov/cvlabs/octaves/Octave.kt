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


fun generateOctavesFrom(
    octavesCount: Int, shrinksCount: Int,
    sigma0: Double, image: GrayScaledImage, imageSigma: Double = 0.5
): Array<Octave> {
    val octaves = mutableListOf<Octave>()

    val k = 2.0.pow(1 / shrinksCount.toDouble())
    val deltaSigma = calculateDeltaSigma(imageSigma, sigma0)
    var startImage = image.applyFilter(GaussianFilter(deltaSigma))

    repeat((0 until octavesCount).count()) {
        val octave = generateOneOctaveParallel(shrinksCount, sigma0, startImage, k)
        octaves.add(octave)
        startImage = octave.last().image.getHalfSizeImage()
    }

    return octaves.toTypedArray()
}

private fun generateOneOctaveParallel(
    shrinksCount: Int,
    sigma0: Double,
    startImage: GrayScaledImage,
    k: Double
): Octave {

    val startElement = OctaveElement(startImage, sigma0)

    val oldSigma = startElement.sigma

    val octavesDeferred =
        (1..shrinksCount).toList().parallelStream().map { i ->

            val newSigma = oldSigma * k.pow(i)
            val deltaSigma = calculateDeltaSigma(oldSigma, newSigma)

            val newImage = startElement.image.applyFilter(GaussianFilter(sigma = deltaSigma))

            OctaveElement(newImage, newSigma)
        }

    val octaveElements = mutableListOf<OctaveElement>()
    octaveElements.add(startElement)
    octaveElements.addAll(octavesDeferred.toList())

    return Octave(octaveElements.toTypedArray())
}

private fun calculateDeltaSigma(oldSigma: Double, newSigma: Double): Double {
    return Math.sqrt(newSigma.pow(2) - oldSigma.pow(2))
}