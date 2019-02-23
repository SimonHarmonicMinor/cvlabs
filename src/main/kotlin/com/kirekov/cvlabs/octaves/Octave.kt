package com.kirekov.cvlabs.octaves

import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.filter.blur.GaussianFilter
import kotlin.math.pow

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

    (0 until octavesCount).forEach { i ->
        val octave = generateOneOctave(shrinksCount, sigma0, startImage, k)
        octaves.add(octave)
        startImage = octave.last().image.getHalfSizeImage()
    }

    return octaves.toTypedArray()
}

private fun generateOneOctave(
    shrinksCount: Int,
    sigma0: Double,
    startImage: GrayScaledImage,
    k: Double
): Octave {

    val elementsList = mutableListOf<OctaveElement>()
    elementsList.add(OctaveElement(startImage, sigma0))

    repeat((1..shrinksCount).count()) {
        val last = elementsList.last()

        val oldSigma = last.sigma
        val newSigma = last.sigma * k
        val deltaSigma = calculateDeltaSigma(oldSigma, newSigma)

        val newImage = last.image.applyFilter(GaussianFilter(sigma = deltaSigma))

        elementsList.add(OctaveElement(newImage, newSigma))
    }

    return Octave(elementsList.toTypedArray())
}

private fun calculateDeltaSigma(oldSigma: Double, newSigma: Double): Double {
    return Math.sqrt(newSigma.pow(2) - oldSigma.pow(2))
}