package com.sukanth.resonance.ui.lockscreen

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * A continuous-curvature superellipse shape — Apple's signature "squircle".
 *
 * Mathematical formula: |x/a|^n + |y/b|^n = 1
 * With n = 5 the curvature transitions smoothly from straight edge to corner
 * with no G1 discontinuity (unlike RoundedRectangle).
 *
 * @param n Superellipse exponent. 2 = ellipse, 4–5 = squircle, ∞ = rectangle.
 * @param bevelFraction Fraction of the shortest dimension reserved for the bevel zone
 *   (used for edge lighting / Fresnel calculations by callers).
 */
class SquircleShape(
    private val n: Float = 5f,
    val bevelFraction: Float = 0.06f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = buildSquirclePath(size)
        return Outline.Generic(path)
    }

    /**
     * Returns a normalized distance from the shape edge (0 = center, 1 = edge).
     * Useful for Fresnel and bevel calculations in modifiers.
     */
    fun edgeFactor(x: Float, y: Float, size: Size): Float {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val a = cx
        val b = cy
        if (a <= 0f || b <= 0f) return 0f
        val nx = (x - cx) / a
        val ny = (y - cy) / b
        val s = abs(nx).pow(n) + abs(ny).pow(n)
        return s.coerceIn(0f, 1f)
    }
}

private fun buildSquirclePath(size: Size): Path {
    val path = Path()
    val n = 5f
    val steps = 120
    val hw = size.width / 2f
    val hh = size.height / 2f

    // Build the superellipse in the first quadrant using parametric form,
    // then mirror to all four quadrants.
    // Parametric: x = a * sign(cos(t)) * |cos(t)|^(2/n)
    //             y = b * sign(sin(t)) * |sin(t)|^(2/n)
    // where a = hw, b = hh, t in [0, pi/2] for first quadrant.
    val exponent = 2f / n

    val firstQuadrant = mutableListOf<Offset>()
    for (i in 0..steps) {
        val t = (Math.PI / 2.0) * i / steps
        val cosT = Math.cos(t).toFloat()
        val sinT = Math.sin(t).toFloat()
        val x = hw + hw * cosT.sign * abs(cosT).pow(exponent)
        val y = hh - hh * sinT.sign * abs(sinT).pow(exponent)
        firstQuadrant.add(Offset(x, y))
    }

    // Mirror: Q1 → Q2 → Q3 → Q4 → close
    // Q1: (x, y) as-is
    // Q2: (w-x, y)
    // Q3: (w-x, h-y)
    // Q4: (x, h-y)
    val allPoints = mutableListOf<Offset>()

    // Start at top-center, go clockwise
    // Top edge: Q1 reversed then Q2
    for (pt in firstQuadrant.reversed()) {
        allPoints.add(Offset(pt.x, pt.y))
    }
    for (pt in firstQuadrant) {
        allPoints.add(Offset(size.width - pt.x, pt.y))
    }
    // Right edge: Q2 bottom points → Q3 top points
    // Bottom edge: Q3 reversed then Q4
    for (pt in firstQuadrant.reversed()) {
        allPoints.add(Offset(size.width - pt.x, size.height - pt.y))
    }
    for (pt in firstQuadrant) {
        allPoints.add(Offset(pt.x, size.height - pt.y))
    }

    // Draw path
    if (allPoints.isNotEmpty()) {
        path.moveTo(allPoints[0].x, allPoints[0].y)
        for (i in 1 until allPoints.size) {
            path.lineTo(allPoints[i].x, allPoints[i].y)
        }
        path.close()
    }

    return path
}
