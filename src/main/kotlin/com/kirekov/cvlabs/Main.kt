package com.kirekov.cvlabs

import com.kirekov.cvlabs.image.borders.MirrorPixelsHandler
import com.kirekov.cvlabs.image.grayscaling.bufferedImageToGrayScaledImage
import com.kirekov.cvlabs.image.grayscaling.method.HdtvScaling
import com.kirekov.cvlabs.octaves.generateOctavesFrom
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

fun main() = runBlocking {
    Files.deleteIfExists(Path.of("output"))
    Files.createDirectory(Path.of("output"))

    val bufferedImage = ImageIO.read(File("input/img.jpg"))
    val grayScaledImage = bufferedImageToGrayScaledImage(
        bufferedImage,
        HdtvScaling(),
        MirrorPixelsHandler()
    )

    val octaves = generateOctavesFrom(4, 6, 1.0, grayScaledImage)

    octaves.forEachIndexed { index, octave ->

        val folderPath = "output/octave$index"
        if (!Files.exists(Path.of(folderPath)))
            Files.createDirectory(Path.of(folderPath))

        octave.forEachIndexed { elIndex, element ->
            ImageIO.write(
                element.image.getBufferedImage(),
                "jpg", File("$folderPath/img$elIndex.jpg")
            )
        }

    }
}
