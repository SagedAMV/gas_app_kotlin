package com.dabb.business.ui.animation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** تبويب لشريط التنقل السفلي. */
data class LiquidTab(val route: String, val label: String, val icon: ImageVector)

/**
 * شريط تنقل سفلي بمؤشر «بلوب» سائل — دليل إعادة التصميم §7.6:
 *  - البلوب ينزلق بين الأيقونات مع تمدد مطاطي لحظي أثناء الحركة
 *  -الأيقونة المختارة تكبر (1→1.12) ولون التسمية يتدرّج
 *  -يحل محل مؤشر Material الافتراضي
 */
@Composable
fun LiquidBottomBar(
    tabs: List<LiquidTab>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
        ) {
            val tabWidth = maxWidth / tabs.size.coerceAtLeast(1)
            val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
            val blobWidth = 52.dp

            // البلوب المنزلق — إزاحة اتجاهية (RTL-aware)
            val x by animateDpAsState(
                targetValue = tabWidth * selectedIndex + (tabWidth - blobWidth) / 2,
                animationSpec = tween(motionDuration(Motion.TAB_SWITCH), easing = Motion.EaseOutQuint),
                label = "navBlobX"
            )
            // تمدد مطاطي لحظي أثناء الانتقال فقط (scaleX يتجاوز 1 ثم يعود — §7.6)
            val stretch = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(selectedIndex) {
                if (!MotionPreferences.reducedMotion) {
                    stretch.snapTo(1f)
                    stretch.animateTo(1.28f, tween(90))
                    stretch.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f))
                }
            }
            Box(
                Modifier
                    .offset(x = x)
                    .width(blobWidth)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .graphicsLayer { scaleX = stretch.value }
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
            )

            tabs.forEachIndexed { i, tab ->
                val selected = i == selectedIndex
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "iconScale"
                )
                val tint by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(motionDuration(Motion.TAB_SWITCH)),
                    label = "navTint"
                )
                Box(
                    Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(tab.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            tab.icon, tab.label,
                            tint = tint,
                            modifier = Modifier
                                .size(23.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint
                        )
                    }
                }
            }
        }
    }
}
