package com.kirekov.cvlabs

import com.kirekov.cvlabs.extension.ThreadPool
import com.kirekov.cvlabs.features.pano.Panorama
import com.kirekov.cvlabs.features.points.EigenValuesMethod
import com.kirekov.cvlabs.features.points.FeaturePoints
import com.kirekov.cvlabs.features.points.descriptors.EuclidDistance
import com.kirekov.cvlabs.features.points.descriptors.ImageDescriptors
import com.kirekov.cvlabs.features.recognition.ObjectDetector
import com.kirekov.cvlabs.image.GrayScaledImage
import com.kirekov.cvlabs.image.borders.MirrorPixelsHandler
import com.kirekov.cvlabs.image.grayscaling.bufferedImageToGrayScaledImage
import com.kirekov.cvlabs.image.grayscaling.method.HdtvScaling
import com.kirekov.cvlabs.octaves.Blob
import com.kirekov.cvlabs.octaves.generateOctavesFrom
import com.kirekov.cvlabs.octaves.octavesToLaplassian
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

fun main() {
    val time = System.currentTimeMillis()

    /*val bufferedImage1 = ImageIO.read(File("input/lena.jpg"))
    val bufferedImage2 = ImageIO.read(File("input/lena_90.jpg"))

    val grayScaledImage1 = bufferedImageToGrayScaledImage(
        bufferedImage1,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val grayScaledImage2 = bufferedImageToGrayScaledImage(
        bufferedImage2,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val buf = descriptors(grayScaledImage1, grayScaledImage2)

    ImageIO.write(buf, "jpg", File("test.jpg"))*/

    octaves()
    //descriptors(grayScaledImage1, grayScaledImage2)
    println(System.currentTimeMillis() - time)
}

fun descriptors(
    grayScaledImage1: GrayScaledImage,
    grayScaledImage2: GrayScaledImage
): BufferedImage {

    val featurePoints1 =
        FeaturePoints.ofHarris(
            5,
            0.01,
            grayScaledImage1,
            EigenValuesMethod()
        ).calculate()
    //.filterByAdaptiveNonMaximumSuppression(200)

    val featurePoints2 =
        FeaturePoints.ofHarris(
            5,
            0.01,
            grayScaledImage2,
            EigenValuesMethod()
        ).calculate()
    //.filterByAdaptiveNonMaximumSuppression(200)


    val imageDescriptors1 = ImageDescriptors.of(
        grayScaledImage1,
        featurePoints1,
        16,
        4,
        10
    )

    val imageDescriptors2 = ImageDescriptors.of(
        grayScaledImage2,
        featurePoints2,
        16,
        4,
        10
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

    val time = System.currentTimeMillis()

    Files.walk(Paths.get("output"))
        .map { it.toFile() }
        .sorted { o1, o2 -> -o1.compareTo(o2) }
        .forEach { it.delete() }
    Files.createDirectory(Paths.get("output"))

    val bufferedImage1 = ImageIO.read(File("input/1.jpg"))
    val grayScaledImage1 = bufferedImageToGrayScaledImage(
        bufferedImage1,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val bufferedImage2 = ImageIO.read(File("input/3.jpg"))
    val grayScaledImage2 = bufferedImageToGrayScaledImage(
        bufferedImage2,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val descriptors1 = async(ThreadPool.pool) {
        println("enter image1 thread")
        val octaves1 = generateOctavesFrom(
            3,
            4,
            1.6,
            grayScaledImage1,
            overheadSize = 3
        )

        val laplassianOctaves1 = octavesToLaplassian(octaves1)

        val blobs1 = Blob.of(laplassianOctaves1)

        /*ImageIO
            .write(
                grayScaledImage1.getBufferedImage(blobs1),
                "jpg",
                File("blobs1.jpg")
            )*/
        Pair(blobs1, laplassianOctaves1)
        val descriptors1 = Blob.calculateDescriptors(
            blobs1,
            octaves1.map { it.overheadToOctaveElements() }
        )
        println("exit image1 thread")
        Pair(descriptors1, blobs1)
    }


    val descriptors2 = async(ThreadPool.pool) {
        println("enter image2 thread")
        val octaves2 = generateOctavesFrom(
            3,
            4,
            1.6,
            grayScaledImage2,
            overheadSize = 3
        )

        val laplassianOctaves2 = octavesToLaplassian(octaves2)

        val blobs2 = Blob.of(laplassianOctaves2)

        /*ImageIO
            .write(
                grayScaledImage2.getBufferedImage(blobs2),
                "jpg",
                File("blobs2.jpg")
            )*/
        blobs2
        val descriptors2 = Blob.calculateDescriptors(
            blobs2,
            octaves2.map { it.overheadToOctaveElements() }
        )
        println("exit image2 thread")
        Pair(descriptors2, blobs2)
    }

    val imageDescriptors1 = ImageDescriptors(grayScaledImage1, descriptors1.await().first)
    val imageDescriptors2 = ImageDescriptors(grayScaledImage2, descriptors2.await().first)

    val matches = GrayScaledImage.getMatches(
        imageDescriptors1,
        imageDescriptors2,
        EuclidDistance()
    )


    val result = Panorama.create(grayScaledImage1, grayScaledImage2, matches)

    // val result = ObjectDetector.detect(grayScaledImage1, grayScaledImage2, matches)

    ImageIO.write(
        result, "jpg", File("pano3.jpg")
    )

/*
    val folderPath = "output"
    if (!Files.exists(Paths.get(folderPath)))
        Files.createDirectory(Paths.get(folderPath))

    descriptors1.await().second.forEachIndexed { index, octave ->

        octave.forEachIndexed { elIndex, element ->
            ImageIO.write(
                element.image.getBufferedImage(),
                "jpg",
                File(
                    "$folderPath/octave_${index}_img_${elIndex}_local_${element.localSigma}_global_${element.globalSigma}.jpg"
                )
            )
        }

    }*/


    println(System.currentTimeMillis() - time)
}

