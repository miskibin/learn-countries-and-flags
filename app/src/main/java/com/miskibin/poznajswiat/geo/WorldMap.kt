package com.miskibin.poznajswiat.geo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive

class CountryShape(val rings: List<FloatArray>) {
    val bounds: Rect = run {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (r in rings) {
            var i = 0
            while (i < r.size) {
                val x = r[i]; val y = r[i + 1]
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                i += 2
            }
        }
        Rect(minX, minY, maxX, maxY)
    }

    fun contains(x: Float, y: Float): Boolean {
        if (x < bounds.left || x > bounds.right || y < bounds.top || y > bounds.bottom) return false
        return rings.any { ringContains(it, x, y) }
    }

    private fun ringContains(r: FloatArray, x: Float, y: Float): Boolean {
        var inside = false
        var j = r.size - 2
        var i = 0
        while (i < r.size) {
            val xi = r[i]; val yi = r[i + 1]
            val xj = r[j]; val yj = r[j + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside
            }
            j = i
            i += 2
        }
        return inside
    }
}

class WorldMap(
    val width: Float,
    val height: Float,
    val shapes: Map<String, CountryShape>,
    val background: List<FloatArray>,
) {
    /** Projection used when preprocessing the GeoJSON — must stay in sync with the asset. */
    fun project(lat: Double, lng: Double): Offset {
        val x = (lng + 180.0) / 360.0 * width
        val y = (LAT_TOP - lat) / (LAT_TOP - LAT_BOT) * height
        return Offset(x.toFloat(), y.toFloat())
    }

    fun hitTest(x: Float, y: Float): String? {
        for ((code, shape) in shapes) {
            if (shape.contains(x, y)) return code
        }
        return null
    }

    companion object {
        private const val LAT_TOP = 85.0
        private const val LAT_BOT = -60.0

        fun parse(text: String): WorldMap {
            val root = Json.parseToJsonElement(text).jsonObject
            val w = root.getValue("w").jsonPrimitive.float
            val h = root.getValue("h").jsonPrimitive.float
            val shapes = root.getValue("countries").jsonObject.mapValues { (_, v) ->
                CountryShape(v.jsonArray.map { ring ->
                    val arr = ring.jsonArray
                    FloatArray(arr.size) { arr[it].jsonPrimitive.float }
                })
            }
            val background = root.getValue("background").jsonArray.map { ring ->
                val arr = ring.jsonArray
                FloatArray(arr.size) { arr[it].jsonPrimitive.float }
            }
            return WorldMap(w, h, shapes, background)
        }
    }
}
