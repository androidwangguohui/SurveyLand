package com.example.surveyland.util

import android.location.Location
import android.os.SystemClock
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

/**
 * 企业级农业测亩轨迹管理
 * 1️⃣ 抖动过滤
 * 2️⃣ 异常移动过滤
 * 3️⃣ 精度自适应
 * 4️⃣ 卡尔曼滤波平滑
 * 5️⃣ 闭合判断
 * 6️⃣ 面积计算
 * 7️⃣ 自交检测
 */
class MapboxLandTracker private constructor() {

    companion object {
        val instance: MapboxLandTracker by lazy { MapboxLandTracker() }
    }

    private val points = mutableListOf<Point>()
    private val timestamps = mutableListOf<Long>()

    private val MIN_DISTANCE = 2.0          // 抖动过滤
    private val MAX_SPEED = 5.0             // m/s
    private val MIN_CLOSURE_DISTANCE = 1.0  //收尾闭合最小距离（2米）
    private val AUTO_CLOSE_DISTANCE = 2.0   // 智能闭合阈值
    private val MIN_AREA = 3.0              //最小圈地面积（2平米）

    private var kalmanX: Double? = null
    private var kalmanY: Double? = null
    private var kalmanP: Double = 1.0
    private val kalmanQ = 0.0001
    private val kalmanR = 0.01

    fun reset() {
        points.clear()
        timestamps.clear()
        kalmanX = null
        kalmanY = null
        kalmanP = 1.0
    }

    fun getPoints(): List<Point> = points.toList()

    fun addPoint(lng: Double, lat: Double) {
        val now = SystemClock.elapsedRealtime()
        val newPoint = Point.fromLngLat(lng, lat)

        if (points.isEmpty()) {
            points.add(kalmanFilter(newPoint))
            timestamps.add(now)
            return
        }

        val last = points.last()
        val lastTime = timestamps.last()
        val distance = TurfMeasurement.distance(last, newPoint, TurfConstants.UNIT_METERS)
        // ⭐ 初始点特殊处理
        val threshold = if (points.size == 1) 1.0 else MIN_DISTANCE
        if (distance < threshold) return

        val dt = (now - lastTime) / 1000.0
        val speed = distance / dt
        if (speed > MAX_SPEED) return

        val filtered = kalmanFilter(newPoint)
        points.add(filtered)
        timestamps.add(now)

        // 自动闭合检测
        autoCloseIfNear(filtered)
    }

    private fun kalmanFilter(point: Point): Point {
        val x = point.longitude()
        val y = point.latitude()
        if (kalmanX == null || kalmanY == null) {
            kalmanX = x
            kalmanY = y
            // ⭐ 返回原始点或首点，不刷新地图
            return Point.fromLngLat(x, y)
        }
        kalmanP += kalmanQ
        val K = kalmanP / (kalmanP + kalmanR)
        kalmanX = kalmanX!! + K * (x - kalmanX!!)
        kalmanY = kalmanY!! + K * (y - kalmanY!!)
        kalmanP = (1 - K) * kalmanP
        return Point.fromLngLat(kalmanX!!, kalmanY!!)
    }

    /** 判断首尾闭合 */
    fun isClosed(): Boolean {
        if (points.size < 3) return false
        val first = points.first()
        val last = points.last()
        val distance = TurfMeasurement.distance(first, last, TurfConstants.UNIT_METERS)
        return distance <= MIN_CLOSURE_DISTANCE
    }

    /** 自动闭合逻辑 */
    private fun autoCloseIfNear(currentPoint: Point) {
        if (points.size < 3) return

        // 遍历除最后一个点外的所有点，找最近点
        var minDistance = Double.MAX_VALUE
        var nearestIndex = -1
        for (i in 0 until points.size - 1) {
            val dist = TurfMeasurement.distance(currentPoint, points[i], TurfConstants.UNIT_METERS)
            if (dist < minDistance) {
                minDistance = dist
                nearestIndex = i
            }
        }

        // 如果距离小于阈值，闭合到最近点
        if (minDistance <= AUTO_CLOSE_DISTANCE && nearestIndex != -1) {
            // 把轨迹闭合
            points.add(points[nearestIndex])
        }
    }

    /** 获取闭合轨迹 */
    fun getClosedPoints(): List<Point> =
        if (isClosed() || points.size >= 3 && points.first() == points.last()) points
        else points + points.first()

    /** 面积计算 */
    fun getLandArea(): Double {
        val closedPoints = getClosedPoints()
        if (closedPoints.size < 4) return 0.0
        return TurfMeasurement.area(Polygon.fromLngLats(listOf(closedPoints)))
    }
    /** 周长（米） */
    fun getPerimeter(): Double {
        val closedPoints = getClosedPoints()
        if (closedPoints.size < 2) return 0.0
        var perimeter = 0.0
        for (i in 0 until closedPoints.size - 1) {
            perimeter += TurfMeasurement.distance(closedPoints[i], closedPoints[i + 1], TurfConstants.UNIT_METERS)
        }
        return perimeter
    }
    /** 是否有效地块 */
    fun isValidLand(): Boolean {
        val closedPoints = getClosedPoints()
        if (closedPoints.size < 4) return false
        if (getLandArea() < MIN_AREA) return false
//        if (isSelfIntersecting(closedPoints)) return false
//        if (isPolygonSelfIntersecting(closedPoints)) return false
        if (isSelfIntersectingImproved(closedPoints)) return false
        return true
    }



    /**
     * 多边形自交检测（只要点能连成一圈即可）
     */
    private fun isPolygonSelfIntersecting(points: List<Point>): Boolean {

        if (points.size < 4) return false  // 少于4个点不可能自交

        val size = points.size

        // 遍历每一条边
        for (i in 0 until size) {

            val a1 = points[i]
            val a2 = points[(i + 1) % size]  // 自动闭合

            // 和后面的非相邻边比较
            for (j in i + 1 until size) {

                val b1 = points[j]
                val b2 = points[(j + 1) % size]

                // ❌ 跳过同一条边
                if (i == j) continue

                // ❌ 跳过相邻边
                if ((i + 1) % size == j) continue
                if (i == (j + 1) % size) continue

                if (linesIntersect2(a1, a2, b1, b2)) {
                    return true
                }
            }
        }

        return false
    }

    private fun linesIntersect2(p1: Point, p2: Point, q1: Point, q2: Point): Boolean {

        fun direction(a: Point, b: Point, c: Point): Double {
            return (b.longitude() - a.longitude()) * (c.latitude() - a.latitude()) -
                    (b.latitude() - a.latitude()) * (c.longitude() - a.longitude())
        }

        val d1 = direction(p1, p2, q1)
        val d2 = direction(p1, p2, q2)
        val d3 = direction(q1, q2, p1)
        val d4 = direction(q1, q2, p2)

        return (d1 * d2 < 0) && (d3 * d4 < 0)
    }
    private fun isSelfIntersectingImproved(points: List<Point>, thresholdMeters: Double = 0.5): Boolean {
        val closedPoints = if (points.first() == points.last()) points else points + points.first()

        for (i in 0 until closedPoints.size - 1) {
            val a1 = closedPoints[i]
            val a2 = closedPoints[i + 1]
            for (j in i + 1 until closedPoints.size - 1) {
                val b1 = closedPoints[j]
                val b2 = closedPoints[j + 1]

                // 忽略相邻线段
                if (i == j || i + 1 == j) continue

                // 如果点非常近，认为线段重合，不算交叉
                if (pointsAlmostEqual(a1, b1, thresholdMeters) ||
                    pointsAlmostEqual(a1, b2, thresholdMeters) ||
                    pointsAlmostEqual(a2, b1, thresholdMeters) ||
                    pointsAlmostEqual(a2, b2, thresholdMeters)
                ) continue

                if (linesIntersect(a1, a2, b1, b2)) return true
            }
        }
        return false
    }
    fun pointsAlmostEqual(p1: Point, p2: Point, thresholdMeters: Double = 1.0): Boolean {
        val d = TurfMeasurement.distance(p1, p2)
        return d < thresholdMeters
    }

    /** 自交检测 */
    private fun isSelfIntersecting(points: List<Point>): Boolean {
        for (i in 0 until points.size - 1) {
            val a1 = points[i]
            val a2 = points[i + 1]
            for (j in i + 1 until points.size - 1) {
                val b1 = points[j]
                val b2 = points[j + 1]
                if (linesIntersect(a1, a2, b1, b2)) return true
            }
        }
        return false
    }

    private fun linesIntersect(p1: Point, p2: Point, q1: Point, q2: Point): Boolean {
        fun ccw(a: Point, b: Point, c: Point) =
            (c.latitude() - a.latitude()) * (b.longitude() - a.longitude()) >
                    (b.latitude() - a.latitude()) * (c.longitude() - a.longitude())
        return ccw(p1, q1, q2) != ccw(p2, q1, q2) &&
                ccw(p1, p2, q1) != ccw(p1, p2, q2)
    }
}