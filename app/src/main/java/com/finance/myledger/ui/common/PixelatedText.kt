package com.finance.myledger.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renders [text] as a mosaic: the text is drawn into a tiny bitmap (one pixel per block, so
 * antialiasing averages each block) and scaled back up with nearest-neighbour filtering.
 * Works on every API level, unlike RenderEffect blur (API 31+).
 *
 * Callers hiding sensitive values should pass a fixed placeholder, not the real value — even a
 * mosaic leaks the value's length.
 */
@Composable
fun PixelatedText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    blockSize: Dp = 7.dp
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val layout = remember(text, style) { measurer.measure(text, style) }
    val blockPx = with(density) { blockSize.toPx() }

    val mosaic = remember(layout, color, blockPx) {
        val w = layout.size.width.toFloat()
        val h = layout.size.height.toFloat()
        val cols = max(1, (w / blockPx).roundToInt())
        val rows = max(1, (h / blockPx).roundToInt())
        ImageBitmap(cols, rows).also { bitmap ->
            CanvasDrawScope().draw(density, layoutDirection, Canvas(bitmap), Size(cols.toFloat(), rows.toFloat())) {
                scale(cols / w, rows / h, pivot = Offset.Zero) {
                    drawText(layout, color = color)
                }
            }
        }
    }

    Canvas(
        modifier.size(
            width = with(density) { layout.size.width.toDp() },
            height = with(density) { layout.size.height.toDp() }
        )
    ) {
        drawImage(
            image = mosaic,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(mosaic.width, mosaic.height),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}
