package com.apptimemachine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Part 2.8 Statistics Engine graphs, implemented with plain Compose
 * Canvas rather than a third-party charting library — keeps the
 * dependency surface small and every chart trivially themeable with
 * Material 3 colors (Part 1.4A: charts should feel native, not bolted-on).
 */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
) {
    if (values.isEmpty()) return
    val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val minValue = (values.minOrNull() ?: 0f).coerceAtMost(0f)
    val range = (maxValue - minValue).coerceAtLeast(1f)

    Canvas(modifier = modifier.height(120.dp)) {
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - minValue) / range
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }

        // Fill under the line
        if (points.size > 1) {
            val fillPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(fillPath, color = fillColor)
        }

        // Line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Dots
        points.forEach { drawCircle(color = lineColor, radius = 4.dp.toPx(), center = it) }
    }
}

@Composable
fun SimpleBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (values.isEmpty()) return
    val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val barWidth = size.width / values.size
            val gap = barWidth * 0.25f
            values.forEachIndexed { index, value ->
                val barHeight = (value / maxValue) * size.height
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(index * barWidth + gap / 2, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth - gap, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        if (labels.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Donut/pie chart for a small set of labeled slices (App Details' Storage
 * breakdown: App / Data / Cache). Draws as a ring rather than a filled
 * pie — reads cleaner at small card sizes and leaves room for a center
 * label (total size) the way the Overview tab's storage summary needs.
 */
@Composable
fun DonutChart(
    slices: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    strokeWidthDp: androidx.compose.ui.unit.Dp = 22.dp,
    centerContent: (@Composable () -> Unit)? = null
) {
    val total = slices.sumOf { it.first.toDouble() }.toFloat()
    Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidthDp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Track (full ring) so the donut reads correctly even if
            // slices don't sum to the visual total.
            drawArc(
                color = Color.Gray.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
            )

            if (total > 0f) {
                var startAngle = -90f
                slices.forEach { (value, color) ->
                    val sweep = (value / total) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep * 0.96f, // tiny gap between slices
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
            }
        }
        centerContent?.invoke()
    }
}

/**
 * Circular ring progress — used for "today's usage vs typical" style dials
 * (App Details Overview: a clock-like ring showing today's foreground time
 * against the app's own recent daily average, so the ring is meaningful
 * per-app rather than an arbitrary fixed target).
 */
@Composable
fun RingProgress(
    progress: Float, // 0f..1f (values above 1 are clamped visually but still shown as overflow color)
    modifier: Modifier = Modifier,
    strokeWidthDp: androidx.compose.ui.unit.Dp = 14.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    centerContent: (@Composable () -> Unit)? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidthDp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        centerContent?.invoke()
    }
}

/** Part 2.8 Calendar Heatmap — simplified grid of colored day-cells by event density. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CalendarHeatmap(
    dailyCounts: List<Int>, // ordered oldest -> newest, one entry per day
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxCount = (dailyCounts.maxOrNull() ?: 1).coerceAtLeast(1)
    val columns = 7

    androidx.compose.foundation.layout.FlowRow(modifier = modifier) {
        dailyCounts.forEach { count ->
            val alpha = if (count == 0) 0.08f else (0.25f + 0.75f * (count.toFloat() / maxCount))
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(16.dp)
                    .background(baseColor.copy(alpha = alpha), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            )
        }
    }
}
