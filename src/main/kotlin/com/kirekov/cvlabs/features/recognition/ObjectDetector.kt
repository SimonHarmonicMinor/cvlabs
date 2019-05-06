package com.kirekov.cvlabs.features.recognition

import com.kirekov.cvlabs.features.pano.Panorama
import com.kirekov.cvlabs.features.points.Match
import com.kirekov.cvlabs.image.GrayScaledImage
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.*

class ObjectDetector {

    private var sample: GrayScaledImage? = null
    private var image: GrayScaledImage? = null

    var votesImage: BufferedImage? = null
        private set

    private var votes: Array<Array<Array<DoubleArray>>>? = null
    private var voters: Array<Array<Array<Array<LinkedList<Match?>>>>>? = null
    private var cellsX: Int = 0
    private var cellsY: Int = 0

    private val sampleCorners: List<Pair<Int, Int>>
        get() = listOf(
            Pair(0, 0),
            Pair(sample!!.width, 0),
            Pair(sample!!.width, sample!!.height),
            Pair(0, sample!!.height)
        )

    private fun find(matching: List<Match>): BufferedImage {
        cellsX = Math.ceil(image!!.width * 1.0 / COORDINATE_STEP).toInt()
        cellsY = Math.ceil(image!!.height * 1.0 / COORDINATE_STEP).toInt()

        votesImage = image!!.getBufferedImage()

        val sampleCenterX = sample!!.width / 2.0
        val sampleCenterY = sample!!.height / 2.0

        votes = Array(cellsX) { Array(cellsY) { Array(ANGLE_CELLS) { DoubleArray(SCALE_CELLS) } } }
        voters =
            Array(cellsX) {
                Array(cellsY) {
                    Array(ANGLE_CELLS) {
                        (0 until SCALE_CELLS).map { LinkedList<Match?>() }.toTypedArray()
                    }
                }
            }
        for (match in matching) {
            val pointOnSample = match.point2
            val pointOnImage = match.point1

            val scale = pointOnImage.scale / pointOnSample.scale
            val angle = pointOnSample.angle - pointOnImage.angle

            val vX = (sampleCenterX - pointOnSample.x) * scale
            val vY = (sampleCenterY - pointOnSample.y) * scale

            val centerX = pointOnImage.x + vX * Math.cos(angle) - vY * Math.sin(angle)
            val centerY = pointOnImage.y + vX * Math.sin(angle) + vY * Math.cos(angle)

            vote(centerX, centerY, scale, angle, match)
        }
        val g = votesImage!!.createGraphics()
        g.color = Color.BLUE
        val candidates = maximums()

        val w1 = image!!.width
        val h1 = image!!.height

        for (matches in candidates) {
            val inliners = Panorama.getInliners(image!!, sample!!, matches as List<Match>)
            val reversePerspective = Panorama.getReversePerspective(inliners)
            var leftTop = Panorama.PanoramaPoint(-1.0, -1.0)
            leftTop = reversePerspective.apply(leftTop)
            val convertedLeftTop = Panorama.PanoramaPoint(
                Panorama.convertFrom(leftTop.x, w1).toDouble(),
                Panorama.convertFrom(leftTop.y, h1).toDouble()
            )
            var rightTop = Panorama.PanoramaPoint(1.0, -1.0)
            rightTop = reversePerspective.apply(rightTop)
            val convertedRightTop = Panorama.PanoramaPoint(
                Panorama.convertFrom(rightTop.x, w1).toDouble(),
                Panorama.convertFrom(rightTop.y, h1).toDouble()
            )
            var leftBottom = Panorama.PanoramaPoint(-1.0, 1.0)
            leftBottom = reversePerspective.apply(leftBottom)
            val convertedLeftBottom = Panorama.PanoramaPoint(
                Panorama.convertFrom(leftBottom.x, w1).toDouble(),
                Panorama.convertFrom(leftBottom.y, h1).toDouble()
            )
            var rightBottom = Panorama.PanoramaPoint(1.0, 1.0)
            rightBottom = reversePerspective.apply(rightBottom)
            val convertedRightBottom = Panorama.PanoramaPoint(
                Panorama.convertFrom(rightBottom.x, w1).toDouble(),
                Panorama.convertFrom(rightBottom.y, h1).toDouble()
            )
            drawRect(
                g,
                convertedLeftBottom,
                convertedLeftTop,
                convertedRightTop,
                convertedRightBottom,
                convertedLeftBottom
            )

        }

        return votesImage!!
    }

    private fun drawRect(graphics: Graphics2D, vararg points: Panorama.PanoramaPoint) {
        for (i in 0 until points.size - 1) {
            val j = i + 1
            graphics.drawLine(
                points[i].x.toInt(),
                points[i].y.toInt(),
                points[j].x.toInt(),
                points[j].y.toInt()
            )
        }
    }

    private fun vote(x: Double, y: Double, scale: Double, angle: Double, match: Match) {
        var angle = angle
        if (x < 0 || x >= image!!.width || y < 0 || y >= image!!.height) return

        while (angle < 0) angle += Math.PI * 2
        while (angle >= Math.PI * 2) angle -= Math.PI * 2

        val xPos = Math.floor(x / COORDINATE_STEP).toInt()
        val yPos = Math.floor(y / COORDINATE_STEP).toInt()

        val angleStep = Math.PI * 2 / ANGLE_CELLS
        val aPos = Math.floor(angle / angleStep).toInt()

        var sPos = nearestGeometricProgressionElement(SCALE_MINIMUM, SCALE_MULTIPLIER, scale)
        if (scale < SCALE_MINIMUM * Math.pow(SCALE_MULTIPLIER, sPos.toDouble())) sPos--

        if (sPos < 0) sPos = 0
        if (sPos >= SCALE_CELLS) sPos = SCALE_CELLS - 1


        val xDelta = x % COORDINATE_STEP / COORDINATE_STEP

        val yDelta = y % COORDINATE_STEP / COORDINATE_STEP

        val aDelta = angle % angleStep / angleStep

        val scaleLeft = SCALE_MINIMUM * Math.pow(SCALE_MULTIPLIER, sPos.toDouble())
        val scaleRight = SCALE_MINIMUM * Math.pow(SCALE_MULTIPLIER, (sPos + 1).toDouble())
        val sDelta = (scale - scaleLeft) / (scaleRight - scaleLeft)


        val d = listOf(
            Pair(xPos, xDelta),
            Pair(yPos, yDelta),
            Pair(aPos, aDelta),
            Pair(sPos, sDelta)
        )

        vote(d, 0, IntArray(d.size), DoubleArray(d.size), match)
    }

    private fun vote(
        list: List<Pair<Int, Double>>, step: Int,
        indices: IntArray, result: DoubleArray, match: Match
    ) {
        if (step == list.size) {
            var value = 1.0
            for (d in result) value *= d

            vote(indices[0], indices[1], indices[2], indices[3], value, match)
        } else {
            val current = list[step]
            val value = 1 - Math.abs(current.second - 0.5)

            indices[step] = current.first
            result[step] = value
            vote(list, step + 1, indices, result, match)

            indices[step] = current.first + if (current.second < 0.5) -1 else 1
            result[step] = 1 - value
            vote(list, step + 1, indices, result, match)
        }
    }

    private fun vote(xPos: Int, yPos: Int, aPos: Int, sPos: Int, value: Double, match: Match) {
        var aPos = aPos
        if (xPos < 0 || xPos >= votes!!.size) return
        if (yPos < 0 || yPos >= votes!![0].size) return
        if (sPos < 0 || sPos >= SCALE_CELLS) return

        if (aPos < 0) aPos += ANGLE_CELLS
        if (aPos >= ANGLE_CELLS) aPos -= ANGLE_CELLS

        votes!![xPos][yPos][aPos][sPos] += value

        voters!![xPos][yPos][aPos][sPos].add(match)
    }

    private fun maximums(): List<List<Match?>> {
        val lists = LinkedList<LinkedList<Match?>>()
        for (i in 0 until cellsX) {
            for (j in 0 until cellsY) {
                for (k in 0 until ANGLE_CELLS) {
                    for (l in 0 until SCALE_CELLS) {
                        if (voters!![i][j][k][l].size < 4)
                            continue
                        val value = votes!![i][j][k][l]
                        var ok = true
                        var di = -1
                        while (di <= 1 && ok) {
                            if (i + di < 0 || i + di >= cellsX) {
                                di++
                                continue
                            }
                            var dj = -1
                            while (dj <= 1 && ok) {
                                if (j + dj < 0 || j + dj >= cellsY) {
                                    dj++
                                    continue
                                }
                                var dk = -1
                                while (dk <= 1 && ok) {
                                    if (k + dk < 0 || k + dk >= ANGLE_CELLS) {
                                        dk++;
                                        continue
                                    }
                                    var dl = -1
                                    while (dl <= 1 && ok) {
                                        if (di == 0 && dj == 0 && dk == 0 && dl == 0 || l + dl < 0 || l + dl >= SCALE_CELLS) {
                                            dl++
                                            continue
                                        }
                                        val ni = i + di
                                        val nj = j + dj
                                        val nk = k + dk
                                        val nl = l + dl
                                        ok = value >= votes!![ni][nj][nk][nl]
                                        dl++
                                    }
                                    dk++
                                }
                                dj++
                            }
                            di++
                        }
                        if (ok) {
                            lists.add(voters!![i][j][k][l])
                        }
                    }
                }
            }
        }
        return lists
    }

    private fun nearestGeometricProgressionElement(a: Double, q: Double, value: Double): Int {
        val y = (Math.log(value) - Math.log(a)) / Math.log(q)
        return Math.round(y).toInt()
    }

    companion object {

        private val COORDINATE_STEP = 30
        private val ANGLE_CELLS = 12

        private val SCALE_MINIMUM = 1 / 4.0
        private val SCALE_MULTIPLIER = 2.0
        private val SCALE_CELLS = 4

        fun find(image: GrayScaledImage, sample: GrayScaledImage, matching: List<Match>): BufferedImage {
            val transform = ObjectDetector()
            transform.image = image
            transform.sample = sample
            return transform.find(matching)
        }
    }

}
