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
