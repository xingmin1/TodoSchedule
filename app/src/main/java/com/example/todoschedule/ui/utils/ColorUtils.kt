package com.example.todoschedule.ui.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import kotlin.math.pow

object ColorUtils {
    fun calculateTimeSlotColors(
        displayColor: ColorSchemeEnum?,
        scheduleId: Int,
        isDarkTheme: Boolean,
        colorScheme: ColorScheme
    ): Pair<Color, Color> {
        val backgroundColor = displayColor?.toColor(colorScheme) ?: run {
            generateAdaptiveCourseColor(scheduleId, isDarkTheme)
        }

        val contentColor = if (backgroundColor.calculateLuminance() < 0.5f) {
            Color.White.copy(alpha = 0.95f)
        } else {
            Color.Black.copy(alpha = 0.87f)
        }

        return backgroundColor to contentColor
    }

    /**
     * 根据输入种子生成自适应颜色
     */
    private fun generateAdaptiveCourseColor(seed: Int, isDarkTheme: Boolean): Color {
        val random = kotlin.random.Random(seed)
        val hue = random.nextFloat() * 360f

        val saturation = random.nextFloat() * 0.2f + 0.3f

        val lightness = if (isDarkTheme) {
            random.nextFloat() * 0.2f + 0.4f
        } else {
            random.nextFloat() * 0.2f + 0.65f
        }

        val c = (1f - kotlin.math.abs(2 * lightness - 1)) * saturation
        val x = c * (1f - kotlin.math.abs((hue / 60f) % 2 - 1))
        val m = lightness - c / 2f

        val (r, g, b) = when {
            hue < 60 -> Triple(c, x, 0f)
            hue < 120 -> Triple(x, c, 0f)
            hue < 180 -> Triple(0f, c, x)
            hue < 240 -> Triple(0f, x, c)
            hue < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (r + m).coerceIn(0f, 1f),
            green = (g + m).coerceIn(0f, 1f),
            blue = (b + m).coerceIn(0f, 1f)
        )
    }

    /**
     * 计算颜色的相对亮度
     */
    fun Color.calculateLuminance(): Float {
        fun adjust(component: Float): Float {
            return if (component <= 0.03928f) {
                component / 12.92f
            } else {
                ((component + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
            }
        }

        val r = adjust(red)
        val g = adjust(green)
        val b = adjust(blue)

        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
} 