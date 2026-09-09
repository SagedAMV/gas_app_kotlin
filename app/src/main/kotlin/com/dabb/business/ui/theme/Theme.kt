package com.dabb.business.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Application
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.dabb.business.data.local.SettingsStore
import com.dabb.business.ui.animation.MotionPreferences

/**
 * ثيم متكامل (Material 3) بهوية دبب البترول:
 * - نظام ألوان كامل (فاving/داكن) — لا ألوان افتراضية مبعثرة
 * - خطوط بخريطة وزن واضحة (أرقام ثقيلة، عناوين بارزة)
 * - أشكال بطاقات دائرية ناعمة موحّدة
 */
private val LightColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = WarmAmber,
    onSecondary = OnAmberContainer,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = BlueAccent,
    onTertiary = Color.White,
    tertiaryContainer = BlueContainer,
    onTertiaryContainer = OnBlueContainer,
    background = CanvasBg,
    onBackground = InkText,
    surface = Color.White,
    onSurface = InkText,
    surfaceVariant = SurfaceMid,
    onSurfaceVariant = SlateText,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceMid,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    outline = OutLine,
    outlineVariant = OutLineSoft,
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFADBD8),
    onErrorContainer = Color(0xFF5E1610)
)

private val DarkColors = darkColorScheme(
    primary = DarkTealLight,
    onPrimary = OnDarkTeal,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = OnDarkTealContainer,
    secondary = DarkAmberLight,
    onSecondary = OnDarkAmber,
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = OnDarkAmberContainer,
    tertiary = DarkBlueLight,
    onTertiary = OnDarkBlue,
    tertiaryContainer = DarkBlueContainer,
    onTertiaryContainer = OnDarkBlueContainer,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkContainer,
    onSurfaceVariant = Color(0xFFA7B4C8),
    surfaceContainerLowest = Color(0xFF0B1016),
    surfaceContainerLow = Color(0xFF131A23),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF33404E),
    outline = DarkOutLine,
    outlineVariant = Color(0xFF3A4655),
    error = Color(0xFFF28B82),
    onError = Color(0xFF3D0A05),
    errorContainer = Color(0xFF6B1F16),
    onErrorContainer = Color(0xFFFBDAD4)
)

/** خطوط بخريطة وزن واضحة — عناوين أثقل من الافتراضي */
private val AppTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.5.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp)
)

/** أشكال موحّدة: بطاقات 16-20 دائرة، حقول 12، أزرار 14 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // الخطوة 28 (دليل إعادة التصميم): تهيئة تفضيل تقليل الحركة من الإعدادات —
    // قراءة الحالة التفاعلية تضمن انعكاس تبديل المفتاح فوراً في كل الشاشات.
    val ctx = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        MotionPreferences.reducedMotion = SettingsStore(ctx).reducedMotion
    }
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
