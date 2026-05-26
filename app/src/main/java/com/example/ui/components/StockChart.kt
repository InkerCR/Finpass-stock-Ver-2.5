package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
fun StockChart(
    points: List<Double>,
    changePercent: Double,
    modifier: Modifier = Modifier,
    onPointSelected: (Double?, Int?) -> Unit = { _, _ -> }
) {
    if (points.isEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            Text("No chart data available.", modifier = Modifier.padding(16.dp))
        }
        return
    }

    val isPositive = changePercent >= 0.0
    val stockColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    val minPrice = points.minOrNull() ?: 0.0
    val maxPrice = points.maxOrNull() ?: 10.0
    val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

    // Animation progress for drawing the line
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(points) {
        startAnim = true
    }
    val animProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(1200),
        label = "chartAnim"
    )

    // Touch interaction states
    var activeX by remember { mutableStateOf<Float?>(null) }
    var activeIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("interactive_stock_chart")
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val colWidth = size.width / (points.size - 1).toFloat()
                            val idx = (offset.x / colWidth).toInt().coerceIn(0, points.size - 1)
                            activeX = offset.x
                            activeIndex = idx
                            onPointSelected(points[idx], idx)
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val colWidth = size.width / (points.size - 1).toFloat()
                            val idx = (offset.x / colWidth).toInt().coerceIn(0, points.size - 1)
                            activeX = offset.x
                            activeIndex = idx
                            onPointSelected(points[idx], idx)
                        },
                        onDragEnd = {
                            activeX = null
                            activeIndex = null
                            onPointSelected(null, null)
                        },
                        onDragCancel = {
                            activeX = null
                            activeIndex = null
                            onPointSelected(null, null)
                        },
                        onDrag = { change, _ ->
                            val colWidth = size.width / (points.size - 1).toFloat()
                            val x = change.position.x
                            val idx = (x / colWidth).toInt().coerceIn(0, points.size - 1)
                            activeX = x
                            activeIndex = idx
                            onPointSelected(points[idx], idx)
                        }
                    )
                }
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val spacing = 20f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val drawWidth = width
                val drawHeight = height - (spacing * 2)

                // Grid divisions
                val gridLinesCount = 4
                for (i in 0..gridLinesCount) {
                    val y = spacing + (drawHeight * (i.toFloat() / gridLinesCount))
                    // Horizontal lines
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }

                // Compile Points to Canvas Paths
                val pointsCount = points.size
                val stepX = drawWidth / (pointsCount - 1)
                
                val chartPath = Path()
                val fillPath = Path()

                points.forEachIndexed { i, price ->
                    // Normalize position backwards: higher price is LOWER on screen coordinates
                    val ratio = (price - minPrice) / priceRange
                    val x = i * stepX
                    val y = spacing + drawHeight - (drawHeight * ratio).toFloat()

                    // Apply layout animation progress
                    val animY = lerp(spacing + drawHeight, y, animProgress)

                    if (i == 0) {
                        chartPath.moveTo(x, animY)
                        fillPath.moveTo(x, spacing + drawHeight)
                        fillPath.lineTo(x, animY)
                    } else {
                        chartPath.lineTo(x, animY)
                        fillPath.lineTo(x, animY)
                    }

                    if (i == pointsCount - 1) {
                        fillPath.lineTo(x, spacing + drawHeight)
                        fillPath.close()
                    }
                }

                // Draw solid shaded gradients
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(stockColor.copy(alpha = 0.35f), stockColor.copy(alpha = 0.00f)),
                    startY = spacing,
                    endY = spacing + drawHeight
                )
                drawPath(
                    path = fillPath,
                    brush = fillBrush
                )

                // Draw main trend curve path
                drawPath(
                    path = chartPath,
                    color = stockColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Render dynamic tactile crosshair marker
                val activeIdx = activeIndex
                val actX = activeX
                if (activeIdx != null && actX != null) {
                    val ratioSelected = (points[activeIdx] - minPrice) / priceRange
                    val cursorX = activeIdx * stepX
                    val cursorY = spacing + drawHeight - (drawHeight * ratioSelected).toFloat()

                    // Vertical alignment cursor line
                    drawLine(
                        color = stockColor.copy(alpha = 0.6f),
                        start = Offset(cursorX, spacing),
                        end = Offset(cursorX, spacing + drawHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                    )

                    // Draw outer glowing halo indicator
                    drawCircle(
                        color = stockColor.copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = Offset(cursorX, cursorY)
                    )

                    // Draw inner tracking solid locator dot
                    drawCircle(
                        color = stockColor,
                        radius = 4.dp.toPx(),
                        center = Offset(cursorX, cursorY)
                    )
                }
            }
        }

        // Timeline labels below chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            Text("Past", color = labelColor, fontSize = 10.sp)
            Text("Historical Performance", color = labelColor, fontSize = 10.sp)
            Text("Now", color = labelColor, fontSize = 10.sp)
        }
    }
}

// Linear interpolation utility helper
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
