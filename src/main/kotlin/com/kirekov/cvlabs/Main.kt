package com.kirekov.cvlabs

import com.kirekov.cvlabs.features.points.EigenValuesMethod
import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.descriptors.EuclidDistance
import com.kirekov.cvlabs.features.points.descriptors.ImageDescriptors
import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.borders.MirrorPixelsHandler
import com.kirekov.cvlabs.image.grayscaling.bufferedImageToGrayScaledImage
import com.kirekov.cvlabs.image.grayscaling.method.HdtvScaling
import com.kirekov.cvlabs.octaves.generateOctavesFrom
import com.kirekov.cvlabs.octaves.getPixelValueFromOctaves
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

fun main() {
    //octaves()
    //featurePointsCalc()
    val time = System.currentTimeMillis()
    val bufferedImage1 = ImageIO.read(File("input/lena.jpg"))
    val grayScaledImage1 = bufferedImageToGrayScaledImage(
        bufferedImage1,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val bufferedImage2 = ImageIO.read(File("input/lena_angle.jpg"))
    val grayScaledImage2 = bufferedImageToGrayScaledImage(
        bufferedImage2,
        HdtvScaling(),
        MirrorPixelsHandler()
    )
    val image = descriptors(grayScaledImage1, grayScaledImage2)

    ImageIO
        .write(
            image,
            "jpg",
            File("output3_1.jpg")
        )

    println(System.currentTimeMillis() - time)
}

fun descriptors(
    grayScaledImage1: GrayScaledImage,
    grayScaledImage2: GrayScaledImage
): BufferedImage {

    val featurePoints1 =
        FeaturePoints.ofHarris(
            7,
            0.3,
            grayScaledImage1,
            EigenValuesMethod()
        ).calculate()
            .filterByAdaptiveNonMaximumSuppression(100)

    val featurePoints2 =
        FeaturePoints.ofHarris(
            7,
            0.3,
            grayScaledImage2,
            EigenValuesMethod()
        ).calculate()
            .filterByAdaptiveNonMaximumSuppression(100)


    val imageDescriptors1 = ImageDescriptors.of(
        grayScaledImage1,
        featurePoints1,
        3,
        9,
        8
    )

    val imageDescriptors2 = ImageDescriptors.of(
        grayScaledImage2,
        featurePoints2,
        3,
        9,
        8
    )

    val image = GrayScaledImage
        .combineImagesByDescriptors(
            grayScaledImage1,
            imageDescriptors1,
            grayScaledImage2,
            imageDescriptors2,
            EuclidDistance()
        )

    return image
}

fun featurePointsCalc() {
    val bufferedImage = ImageIO.read(File("input/img.jpg"))
    val grayScaledImage = bufferedImageToGrayScaledImage(
        bufferedImage,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val featurePoints =
        FeaturePoints.ofHarris(7, 0.3, grayScaledImage, EigenValuesMethod())
            .calculate()

    ImageIO
        .write(
            grayScaledImage
                .getBufferedImage(featurePoints.filterByAdaptiveNonMaximumSuppression(50)),
            "jpg",
            File("output3_harris_max_1.jpg")
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
