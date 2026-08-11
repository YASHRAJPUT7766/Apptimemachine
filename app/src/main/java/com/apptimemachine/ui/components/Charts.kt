package com.apptimemachine.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Part 2.8 Statistics Engine graphs, implemented with plain Compose Canvas
 * rather than a third-party charting library — keeps the dependency
 * surface small and every chart trivially themeable with Material 3
 * colors (Part 1.4A: charts should feel native, not bolted-on).
 *
 * Every chart here ALWAYS renders its shape — line, bars, donut ring, and
 * progress ring are all drawn even with zero/empty data (a flat zero line,
 * empty-height bars, an unfilled ring), and every fill/sweep animates in
 * on first composition. The screen should never look "unfinished" just
 * because a metric hasn't collected data yet; a visible zero communicates
 * "tracking, nothing yet" far better than a blank space or vanishing
 * component, and it means every tab (Overview/Storage/Version/Permissions)
 * has the same polished shell whether or not data exists behind it.
 */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
) {
    // A flat zero-line reads as "no movement yet" rather than nothing at
    // all — same treatment a single real data point would need anyway.
    val safeValues = if (values.size < 2) listOf(0f, 0f) else values
    val maxValue = (safeValues.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val minValue = (safeValues.minOrNull() ?: 0f).coerceAtMost(0f)
    val range = (maxValue - minValue).coerceAtLeast(1f)

    var animatedIn by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animatedIn) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label = "lineChartProgress"
    )
    androidx.compose.runtime.LaunchedEffect(safeValues) { animatedIn = true }

    Canvas(modifier = modifier.height(120.dp)) {
        val stepX = size.width / (safeValues.size - 1).coerceAtLeast(1)
        val points = safeValues.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - minValue) / range
            val y = size.height - (normalized * size.height * progress)
            Offset(x, y)
        }

        if (points.size > 1) {
            val fillPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(fillPath, color = fillColor)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
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
    // Zero-height placeholder bars (drawn as a thin baseline sliver) keep
    // the chart's shape and label row present instead of collapsing away.
    val safeValues = values.ifEmpty { List(7) { 0f } }
    val maxValue = (safeValues.maxOrNull() ?: 1f).coerceAtLeast(1f)

    var animatedIn by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animatedIn) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label = "barChartProgress"
    )
    androidx.compose.runtime.LaunchedEffect(safeValues) { animatedIn = true }

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val barWidth = size.width / safeValues.size
            val gap = barWidth * 0.25f
            val minBarPx = 3.dp.toPx() // baseline sliver so a 0-value bar is still visible
            safeValues.forEachIndexed { index, value ->
                val targetHeight = (value / maxValue) * size.height
                val barHeight = (targetHeight * progress).coerceAtLeast(minBarPx)
                drawRoundRect(
                    color = if (value <= 0f) barColor.copy(alpha = 0.25f) else barColor,
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
 * With no data (all slices 0), draws a soft neutral ring rather than
 * nothing, so the chart shell is always present.
 */
@Composable
fun DonutChart(
    slices: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    strokeWidthDp: androidx.compose.ui.unit.Dp = 22.dp,
    centerContent: (@Composable () -> Unit)? = null
) {
    val total = slices.sumOf { it.first.toDouble() }.toFloat()

    var animatedIn by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animatedIn) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "donutProgress"
    )
    androidx.compose.runtime.LaunchedEffect(slices) { animatedIn = true }

    Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidthDp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Track (full ring) — always drawn, doubles as the "empty
            // state" ring when there's no data yet.
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
                    val sweep = (value / total) * 360f * progress
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
            } else {
                // Zero-data state: a faint full-ring accent so it still
                // reads as "a donut chart, currently empty" rather than a
                // plain gray circle.
                drawArc(
                    color = (slices.firstOrNull()?.second ?: Color.Gray).copy(alpha = 0.18f * progress),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                )
            }
        }
        centerContent?.invoke()
    }
}

/**
 * Circular ring progress — used for "today's usage vs typical" style dials
 * (App Details Overview: a clock-like ring showing today's foreground time
 * against the app's own recent daily average, so the ring is meaningful
 * per-app rather than an arbitrary fixed target). Always draws the track;
 * a 0 progress simply animates in as an unfilled ring.
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

    var animatedIn by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animatedIn) clamped else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "ringProgress"
    )
    androidx.compose.runtime.LaunchedEffect(clamped) { animatedIn = true }

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
            // Always draw at least a sliver so the progress color is
            // visible even at 0% — reads as "active, currently empty"
            // rather than a component that silently did nothing.
            val minSweep = 4f
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = (360f * animatedProgress).coerceAtLeast(minSweep),
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
