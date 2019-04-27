package com.kirekov.cvlabs.features.recognition

import com.kirekov.cvlabs.features.pano.Panorama
import com.kirekov.cvlabs.features.pano.Panorama.Companion.convertFrom
import com.kirekov.cvlabs.features.pano.Panorama.Companion.getReversePerspective
import com.kirekov.cvlabs.features.points.Match
import com.kirekov.cvlabs.image.GrayScaledImage
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.*

class ObjectDetector {
    companion object {
        fun detect(image: GrayScaledImage, obj: GrayScaledImage, matches: List<Match>): BufferedImage {
            val objectCenterX = obj.width / 2.0
            val objectCenterY = obj.height / 2.0

            val voting = Voting(
                image.width,
                30,
                image.height,
                30,
                image.width.toDouble(),
                30.0,
                Math.PI / 6
            )
            for (match in matches) {
                val atImage = match.point1
                val atObject = match.point2

                val x = objectCenterX - atObject.x
                val y = objectCenterY - atObject.y
                val angle = atObject.angle
                var cos = Math.cos(-angle)
                var sin = Math.sin(-angle)

                val rotatedY = y * cos - x * sin
                val rotatedX = y * sin + x * cos
                val scaledX = rotatedX / atObject.scale
                val scaledY = rotatedY / atObject.scale

                val objectScale = obj.width / atObject.scale

                val resultUnscaledX = scaledX * atImage.scale
                val resultUnscaledY = scaledY * atImage.scale
                cos = Math.cos(atImage.angle)
                sin = Math.sin(atImage.angle)
                val resultY = atImage.x + resultUnscaledY * cos - resultUnscaledX * sin
                val resultX = atImage.y + resultUnscaledY * sin + resultUnscaledX * cos

                val votingAngle = atImage.angle - atObject.angle
                val votingScale = objectScale * atImage.scale

                voting.vote(resultX, resultY, votingScale, votingAngle, match)
            }

            val candidates = voting.maximums(obj.width, obj.height)
            val w1 = image.width
            val h1 = image.height
            val w2 = obj.width
            val h2 = obj.height
            val bufferedImage = image.getBufferedImage()
            val graphics = bufferedImage.createGraphics()
            graphics.color = Color.ORANGE
            graphics.stroke = BasicStroke(2f)
            var rects = 0
            for (match in candidates) {
                val inliners = Panorama.getInliners(image, obj, matches)
                if (inliners.size < 10) continue
                println(inliners.size)
                val reversePerspective = getReversePerspective(inliners)
                var leftTop = Panorama.PanoramaPoint(-1.0, -1.0)
                leftTop = reversePerspective.apply(leftTop)
                val convertedLeftTop = Panorama.PanoramaPoint(
                    convertFrom(leftTop.x, w1).toDouble(),
                    convertFrom(leftTop.y, h1).toDouble()
                )
                var rightTop = Panorama.PanoramaPoint(1.0, -1.0)
                rightTop = reversePerspective.apply(rightTop)
                val convertedRightTop = Panorama.PanoramaPoint(
                    convertFrom(rightTop.x, w1).toDouble(),
                    convertFrom(rightTop.y, h1).toDouble()
                )
                var leftBottom = Panorama.PanoramaPoint(-1.0, 1.0)
                leftBottom = reversePerspective.apply(leftBottom)
                val convertedLeftBottom = Panorama.PanoramaPoint(
                    convertFrom(leftBottom.x, w1).toDouble(),
                    convertFrom(leftBottom.y, h1).toDouble()
                )
                var rightBottom = Panorama.PanoramaPoint(1.0, 1.0)
                rightBottom = reversePerspective.apply(rightBottom)
                val convertedRightBottom = Panorama.PanoramaPoint(
                    convertFrom(rightBottom.x, w1).toDouble(),
                    convertFrom(rightBottom.y, h1).toDouble()
                )
                rects++
                drawRect(graphics, convertedLeftBottom, convertedLeftTop, convertedRightTop, convertedRightBottom)

            }

            return bufferedImage

        }

        private fun drawRect(graphics: Graphics2D, vararg points: Panorama.PanoramaPoint) {
            for (i in points.indices) {
                val j = (i + 1) % points.size
                graphics.drawLine(
                    points[i].x.toInt(),
                    points[i].y.toInt(),
                    points[j].x.toInt(),
                    points[j].y.toInt()
                )
            }
        }


        internal class Voting(
            width: Int,
            private val widthBin: Int,
            height: Int,
            private val heightBin: Int,
            maxScale: Double,
            private val scaleBin: Double,
            private val angleBin: Double
        ) {
            private val votes: Array<Array<Array<DoubleArray>>>
            private val votedPairs: Array<Array<Array<Array<MutableList<Match>?>>>>
            private val n: Int
            private val m: Int
            private val k: Int
            private val l: Int

            init {
                n = width / widthBin + 1
                m = height / heightBin + 1
                k = (maxScale / scaleBin + 1).toInt()
                l = Math.round(2 * Math.PI / angleBin).toInt()
                println("$n $m $k $l")
                votes = Array(n) { Array(m) { Array(k) { DoubleArray(l) } } }
                votedPairs =
                    Array(n) { Array(m) { Array<Array<MutableList<Match>?>>(k) { arrayOfNulls(l) } } }
            }

            fun vote(x: Double, y: Double, scale: Double, angle: Double, pointsPair: Match) {
                var angle = angle
                angle = normalize(angle)
                val _x = x * 1.0 / widthBin
                val _y = y * 1.0 / heightBin
                val _scale = scale / scaleBin
                val _angle = angle / angleBin
                if (_x < 0 || _y < 0 || _scale < 0 || _angle < 0) return
                if (_x >= n || _y >= m || _scale >= k || _angle >= l) return
                for (i in 0..1) {
                    val curX = (_x.toInt() + i) % n
                    for (j in 0..1) {
                        val curY = (_y.toInt() + j) % m
                        for (k in 0..1) {
                            val curScale = (_scale.toInt() + k) % this.k
                            for (l in 0..1) {
                                val curAngle = (_angle.toInt() + l) % this.l
                                votes[curX][curY][curScale][curAngle] += Math.abs(_x - curX) *
                                        Math.abs(_y - curY) *
                                        Math.abs(_scale - curScale) *
                                        Math.abs(_angle - curAngle)
                                var list: MutableList<Match>? = votedPairs[curX][curY][curScale][curAngle]
                                if (list == null) {
                                    list = LinkedList()
                                    votedPairs[curX][curY][curScale][curAngle] = list
                                }
                                list.add(pointsPair)
                            }
                        }
                    }
                }
            }

            private fun normalize(angle: Double): Double {
                var angle = angle
                while (angle < 0)
                    angle += Math.PI * 2
                while (angle >= Math.PI * 2)
                    angle -= Math.PI * 2
                return angle
            }


            fun maximums(objWidth: Int, objHeight: Int): List<List<Match>?> {
                val lists = LinkedList<List<Match>?>()
                for (i in 0 until n) {
                    for (j in 0 until m) {
                        for (k in 0 until this.k) {
                            for (l in 0 until this.l) {
                                val `val` = votes[i][j][k][l]
                                if (votedPairs[i][j][k][l] == null || votedPairs[i][j][k][l]?.size!! < 4)
                                    continue
                                var ok = true
                                var di = -1
                                while (di <= 1 && ok) {
                                    var dj = -1
                                    while (dj <= 1 && ok) {
                                        var dk = -1
                                        while (dk <= 1 && ok) {
                                            var dl = -1
                                            while (dl <= 1 && ok) {
                                                if (di == 0 && dj == 0 && dk == 0 && dl == 0) {
                                                    dl++
                                                    continue
                                                }
                                                val ni = (i + di + this.n) % this.n
                                                val nj = (j + dj + this.m) % this.m
                                                val nk = (k + dk + this.k) % this.k
                                                val nl = (l + dl + this.l) % this.l
                                                ok = `val` > votes[ni][nj][nk][nl]
                                                dl++
                                            }
                                            dk++
                                        }
                                        dj++
                                    }
                                    di++
                                }
                                if (ok) {
                                    println("$i $j $k $l")
                                    val x = (i + 0.5) * widthBin
                                    val y = (j + 0.5) * heightBin
                                    val scale = (k + 0.5) * scaleBin
                                    val coef = scale / objWidth
                                    val w = objWidth * coef
                                    val h = objHeight * coef
                                    val angle = l * angleBin
                                    lists.add(votedPairs[i][j][k][l])
                                }
                            }
                        }
                    }
                }
                return lists
            }


            private fun rotate(
                point: Pair<Double, Double>,
                center: Pair<Double, Double>,
                angle: Double
            ): Pair<Double, Double> {
                val cos = Math.cos(angle)
                val sin = Math.sin(angle)
                val x = point.first - center.first
                val y = point.second - center.second
                val rx = x * cos - y * sin
                val ry = x * sin + y * cos
                return Pair(rx + center.first, ry + center.second)
            }
        }
    }
}