package com.kirekov.cvlabs

import com.kirekov.cvlabs.features.points.FeaturePointOperator
import com.kirekov.cvlabs.image.borders.MirrorPixelsHandler
import com.kirekov.cvlabs.image.grayscaling.bufferedImageToGrayScaledImage
import com.kirekov.cvlabs.image.grayscaling.method.HdtvScaling
import com.kirekov.cvlabs.octaves.generateOctavesFrom
import com.kirekov.cvlabs.octaves.getPixelValueFromOctaves
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO


fun main() {
    //octaves()
    featurePoints()
}

fun featurePoints() {
    val bufferedImage = ImageIO.read(File("input/img2.jpg"))
    val grayScaledImage = bufferedImageToGrayScaledImage(
        bufferedImage,
        HdtvScaling(),
        MirrorPixelsHandler()
    )
    val featurePoints =
        grayScaledImage
            .applyHarrisOperator(FeaturePointOperator(5, 1, 0.1))
            .filterByAdaptiveNonMaximumSuppression(1000)
    ImageIO
        .write(
            grayScaledImage
                .getBufferedImage(featurePoints),
            "jpg",
            File("output2.jpg")
        )
}

fun octaves() = runBlocking {
    Files.walk(Paths.get("output"))
        .map { it.toFile() }
        .sorted { o1, o2 -> -o1.compareTo(o2) }
        .forEach { it.delete() }
    Files.createDirectory(Paths.get("output"))

    val bufferedImage = ImageIO.read(File("input/img.jpg"))
    val grayScaledImage = bufferedImageToGrayScaledImage(
        bufferedImage,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val time = System.currentTimeMillis()

    val octaves = generateOctavesFrom(6, 5, 2.0, grayScaledImage)

    val folderPath = "output"
    if (!Files.exists(Paths.get(folderPath)))
        Files.createDirectory(Paths.get(folderPath))

    octaves.forEachIndexed { index, octave ->

        octave.forEachIndexed { elIndex, element ->
            ImageIO.write(
                element.image.getBufferedImage(),
                "jpg",
                File(
                    "$folderPath/octave_${index}_img_${elIndex}_local_${element.localSigma.format(2)}_global_${element.globalSigma.format(
                        2
                    )}.jpg"
                )
            )
        }

    }


    getPixelValueFromOctaves(octaves, 600, 600, 12.0)

    println(System.currentTimeMillis() - time)
}

private fun Double.format(digits: Int): String {
    return java.lang.String.format("%.${digits}f", this)
}
