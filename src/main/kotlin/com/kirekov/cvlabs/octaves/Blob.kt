package com.kirekov.cvlabs.octaves

import com.kirekov.cvlabs.features.points.EigenValuesMethod
import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.HarrisCalculator
import com.kirekov.cvlabs.features.points.Point
import com.kirekov.cvlabs.features.points.descriptors.Descriptor
import com.kirekov.cvlabs.features.points.descriptors.ImageDescriptors
import java.lang.Double.max
import java.lang.Double.min
import java.lang.Math.abs
import java.lang.Math.pow
import kotlin.math.round
import kotlin.math.roundToInt

class Blob(
    val radius: Double,
    val x: Int,
    val y: Int,
    val intensity: Double,
    val descriptorSizeRatio: Double,
    val octaveIndex: Int,
    val imageIndex: Int,
    val imageX: Int,
    val imageY: Int
) {
    companion object {

        private fun almostEqual(num1: Double, num2: Double, eps: Double = 0.000000000001): Boolean {
            return abs(num1 - num2) <= eps
        }

        fun calculateDescriptors(
            blobs: List<Blob>,
            octaves: List<Octave>
        ): List<Descriptor> {
            val threshold = 0.005

            val descriptors = mutableListOf<Descriptor>()

            for (i in 0 until blobs.size) {
                val blob = blobs[i]
                val octaveElement = octaves[blob.octaveIndex].getOctaveElement(blob.imageIndex)
                val point = Point(
                    blob.imageX, blob.imageY, HarrisCalculator(
                        (5 * (octaveElement.localSigma / octaves[blob.octaveIndex].getOctaveElement(0).localSigma))
                            .roundToInt(),
                        threshold,
                        octaveElement.image,
                        EigenValuesMethod()
                    ).calculatePoint(blob.imageX, blob.imageY)
                )
                if (point.value < threshold)
                    continue

                val res = ImageDescriptors.of(
                    octaveElement.image,
                    FeaturePoints(
                        octaveElement.image.width,
                        octaveElement.image.height,
                        listOf(point)
                    ),
                    (16 * blob.descriptorSizeRatio).roundToInt(),
                    4,
                    10
                ).descriptors[0]
                res.scale = octaveElement.globalSigma
                res.angle = res.angle
                val actualPoint = Point(
                    (point.x * pow(2.0, blob.octaveIndex.toDouble())).roundToInt(),
                    (point.y * pow(2.0, blob.octaveIndex.toDouble())).roundToInt(),
                    res.point.value
                )

                res.point = actualPoint

                descriptors.add(res)
            }

            return descriptors
        }

        fun of(laplassians: List<Octave>, threshold: Double = 0.03): List<Blob> {

            val actualWidth = laplassians[0].first().image.width
            val actualHeight = laplassians[0].first().image.height

            val result = mutableListOf<Blob>()
            laplassians.forEachIndexed { octaveIndex, octave ->
                for (k in 1 until octave.size - 1) {
                    val startSigma = octave.getOctaveElement(0).localSigma
                    val prevImage = octave.getOctaveElementThroughAll(k - 1).image
                    val curEl = octave.getOctaveElementThroughAll(k)
                    val curSigmaLocal = curEl.localSigma
                    val curImage = curEl.image
                    val nextImage = octave.getOctaveElementThroughAll(k + 1).image

                    (1 until curImage.width - 1).forEach { i ->
                        (1 until curImage.height - 1).forEach { j ->
                            val centerIntensity = curImage.getPixelValue(i, j)
                            var minValue = Double.MAX_VALUE
                            var maxValue = Double.MIN_VALUE

                            (i - 1..i + 1).forEach { x ->
                                (j - 1..j + 1).forEach { y ->
                                    val prevImageValue =
                                        prevImage.getPixelValue(x, y)
                                    val curImageValue =
                                        curImage.getPixelValue(x, y)
                                    val nextImageValue =
                                        nextImage.getPixelValue(x, y)
                                    minValue = min(prevImageValue, minValue)
                                    minValue = min(curImageValue, minValue)
                                    minValue = min(nextImageValue, minValue)

                                    maxValue = max(prevImageValue, maxValue)
                                    maxValue = max(curImageValue, maxValue)
                                    maxValue = max(nextImageValue, maxValue)
                                }
                            }
                            if ((almostEqual(centerIntensity, minValue) || almostEqual(centerIntensity, maxValue))
                                && centerIntensity >= threshold
                            ) {
                                val newX = round(i * (actualWidth.toDouble() / curImage.width)).toInt()
                                val newY = round(j * (actualHeight.toDouble() / curImage.height)).toInt()
                                result.add(
                                    Blob(
                                        curSigmaLocal * Math.sqrt(2.0),
                                        newX,
                                        newY,
                                        centerIntensity,
                                        curSigmaLocal / startSigma,
                                        octaveIndex,
                                        k,
                                        i,
                                        j
                                    )
                                )
                            }
                        }
                    }

                }
            }

            return result
        }
    }
}