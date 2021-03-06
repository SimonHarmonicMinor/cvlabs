package com.kirekov.cvlabs.octaves

import com.kirekov.cvlabs.image.GrayScaledImage

data class OctaveElement(val image: GrayScaledImage, val localSigma: Double, val globalSigma: Double)