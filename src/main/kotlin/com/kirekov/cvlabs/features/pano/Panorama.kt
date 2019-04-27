package com.kirekov.cvlabs.features.pano

import com.kirekov.cvlabs.features.points.Match
import com.kirekov.cvlabs.image.GrayScaledImage
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.SingularValueDecomposition
import java.awt.image.BufferedImage
import java.util.*
import kotlin.collections.ArrayList

class Panorama {
    companion object {
        private val random = Random()
        private const val EPS = 10
        fun create(
            image1: GrayScaledImage,
            image2: GrayScaledImage,
            matches: List<Match>
        ): BufferedImage {
            val w1 = image1.width
            val h1 = image1.height
            val w2 = image2.width
            val h2 = image2.height
            val inliners = getInliners(image1, image2, matches)

            val foundPerspective = getPerspective(inliners)
            val foundReversePerspective = getReversePerspective(inliners)

            var minX = -1.0
            var maxX = 1.0
            var minY = -1.0
            var maxY = 1.0
            val angles = arrayOf(
                intArrayOf(0, 0),
                intArrayOf(0, image2.height),
                intArrayOf(image1.width, 0),
                intArrayOf(image2.width, image2.height)
            )

            for (angle in angles) {
                val point = foundReversePerspective.apply(
                    PanoramaPoint(
                        convertTo(angle[0].toDouble(), image1.width),
                        convertTo(angle[1].toDouble(), image2.height)
                    )
                )
                minX = Math.min(minX, point.x)
                minY = Math.min(minY, point.y)
                maxX = Math.max(maxX, point.x)
                maxY = Math.max(maxY, point.y)
            }
            val minI = convertFrom(minX, w1)
            val maxI = convertFrom(maxX, w1)
            val minJ = convertFrom(minY, h1)
            val maxJ = convertFrom(maxY, h1)

            val nw = maxI - minI + 1
            val nh = maxJ - minJ + 1

            val result = BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB)

            println("$minX $minY")

            val imageA = image1.getBufferedImage()
            val imageB = image2.getBufferedImage()

            for (i in minI..maxI) {
                for (j in minJ..maxJ) {
                    val x = (1.0 * i - minI) * (maxX - minX) / (maxI - minI) + minX
                    val y = (1.0 * j - minJ) * (maxY - minY) / (maxJ - minJ) + minY
                    val xy = PanoramaPoint(x, y)

                    val aX = convertFrom(xy.x, w1)
                    val aY = convertFrom(xy.y, h1)
                    if (aX >= 0 && aX < w1 && aY >= 0 && aY < h1) {
                        result.setRGB(i - minI, j - minJ, imageA.getRGB(aX, aY))
                    }

                    val nxt = foundPerspective.apply(xy)
                    val nx = convertFrom(nxt.x, imageB.width)
                    val ny = convertFrom(nxt.y, imageB.height)
                    if (nx >= 0 && nx < imageB.getWidth() && ny >= 0 && ny < imageB.getHeight()) {
                        result.setRGB(i - minI, j - minJ, imageB.getRGB(nx, ny))
                    }
                }
            }

            return result
        }

        fun getInliners(
            image1: GrayScaledImage,
            image2: GrayScaledImage,
            matches: List<Match>
        ): List<Pair<PanoramaPoint, PanoramaPoint>> {
            val n = matches.size
            val indices = ArrayList<Int>(n)
            for (i in 0 until n) indices.add(i)
            val cnt = 1000

            var inliners: List<Pair<PanoramaPoint, PanoramaPoint>> = LinkedList()

            val pairs = ArrayList<Pair<PanoramaPoint, PanoramaPoint>>()
            val w1 = image1.width
            val h1 = image1.height
            val w2 = image2.width
            val h2 = image2.height
            for (i in 0 until n) {
                val cur = matches[i]
                pairs.add(
                    Pair(
                        PanoramaPoint(convertTo(cur.point1.x.toDouble(), w1), convertTo(cur.point1.y.toDouble(), h1)),
                        PanoramaPoint(convertTo(cur.point2.x.toDouble(), w2), convertTo(cur.point2.y.toDouble(), h2))
                    )
                )
            }

            for (voting in 0 until cnt) {
                indices.shuffle(random)
                val currentMatch = java.util.ArrayList<Pair<PanoramaPoint, PanoramaPoint>>(4)
                for (i in 0..3) currentMatch.add(pairs[indices[i]])
                val perspective = getPerspective(currentMatch)
                val curOk = LinkedList<Pair<PanoramaPoint, PanoramaPoint>>()
                for (i in 0 until n) {
                    val pair = pairs[i]
                    val a = pair.first
                    val b = pair.second
                    val conv = perspective.apply(a)
                    val x0 = b.x
                    val y0 = b.y
                    val x1 = conv.x
                    val y1 = conv.y

                    val eps = Math.max(
                        Math.abs(convertFrom(x0, w2) - convertFrom(x1, w2)),
                        Math.abs(convertFrom(y0, h2) - convertFrom(y1, h2))
                    ).toDouble()
                    if (eps < EPS) {
                        curOk.add(Pair(a, b))
                    }
                }
                if (inliners.size < curOk.size) {
                    inliners = java.util.ArrayList(curOk)
                    println("INLINE = " + inliners.size)
                }
            }

            return inliners
        }

        fun getPerspective(currentMatches: List<Pair<PanoramaPoint, PanoramaPoint>>): Perspective {
            val matrix = Array(currentMatches.size * 2) { DoubleArray(9) }
            for (i in currentMatches.indices) {
                val a = currentMatches[i].first
                val b = currentMatches[i].second
                matrix[i * 2][0] = a.x
                matrix[i * 2][1] = a.y
                matrix[i * 2][2] = 1.0
                matrix[i * 2 + 1][3] = a.x
                matrix[i * 2 + 1][4] = a.y
                matrix[i * 2 + 1][5] = 1.0
                matrix[i * 2][6] = -a.x * b.x
                matrix[i * 2][7] = -a.y * b.x
                matrix[i * 2][8] = -b.x
                matrix[i * 2 + 1][6] = -a.x * b.y
                matrix[i * 2 + 1][7] = -a.y * b.y
                matrix[i * 2 + 1][8] = -b.y
            }

            val AMatrix = MatrixUtils.createRealMatrix(matrix)
            val transposed = AMatrix.transpose()
            val M = transposed.multiply(AMatrix)

            val svd = SingularValueDecomposition(M)

            val singularValues = svd.singularValues
            val minIndex = getMinIndex(singularValues)

            val U = svd.u
            val h = U.getColumn(minIndex)

            val h22 = h[8]
            for (i in 0..8) h[i] /= h22
            return Perspective(h)
        }

        fun getReversePerspective(currentMatch: List<Pair<PanoramaPoint, PanoramaPoint>>): Perspective {
            val reversed = java.util.ArrayList<Pair<PanoramaPoint, PanoramaPoint>>(currentMatch.size)
            for (cur in currentMatch) {
                reversed.add(Pair(cur.second, cur.first))
            }
            return getPerspective(reversed)
        }

        fun convertTo(coord: Double, size: Int): Double {
            return (2.0 * coord - size) / size
        }

        fun convertFrom(coord: Double, size: Int): Int {
            return ((coord * size + size) / 2).toInt()
        }

        private fun getMinIndex(doubles: DoubleArray): Int {
            var min = Double.MAX_VALUE
            var index = -1
            doubles.forEachIndexed { i, value ->
                if (value < min) {
                    min = value
                    index = i
                }
            }
            return index
        }
    }

    data class PanoramaPoint(val x: Double, val y: Double)

    class Perspective(buffer: DoubleArray) {
        private val matrix: RealMatrix

        init {
            val h = Array(3) { DoubleArray(3) }
            var ptr = 0
            for (i in 0..2) {
                for (j in 0..2) {
                    h[i][j] = buffer[ptr++]
                }
            }
            matrix = MatrixUtils.createRealMatrix(h)
        }

        fun apply(point: PanoramaPoint): PanoramaPoint {
            val coords = Array(3) { DoubleArray(1) }
            coords[0][0] = point.x
            coords[1][0] = point.y
            coords[2][0] = 1.0
            var realMatrix = MatrixUtils.createRealMatrix(coords)
            realMatrix = matrix.multiply(realMatrix)
            val buf = realMatrix.data
            val scale = buf[2][0]
            return PanoramaPoint(buf[0][0] / scale, buf[1][0] / scale)
        }
    }
}