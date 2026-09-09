package com.dabb.business.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * انتقالات الشاشات — دليل إعادة التصميم §5.2:
 *  - tabComposable: تبويبات الشريط السفلي — Shared Axis خفيف (220ms)
 *    يعكس ترتيب التبويب (المخزون→الصرف→الزبائن→المحطة→التقارير):
 *    التقدّم لتبويب لاحق = الدخول من اليسار (RTL: تقدّم للأمام)،
 *    والرجوع لتبويب سابق = المنطق معكوساً.
 *  - pushComposable: شاشات «التوغّل» (تفاصيل زبون/سجل/إعدادات) —
 *    انزلاق أعمق 380ms: تمايز إدراكي بين «تنقّل أفقي» و«تعمّق».
 */

/** ترتيب التبويبات — أساس اتجاه المحور المشترك (RTL). */
private val tabOrder = listOf("inventory", "dispense", "customers", "station", "reports")

/** +1 = تقدّم نحو تبويب لاحق، -1 = رجوع، 0 = غير معروف (fade). */
private fun tabDirection(initial: String?, target: String?): Int {
    val a = tabOrder.indexOf(initial)
    val b = tabOrder.indexOf(target)
    if (a < 0 || b < 0) return 0
    return if (b >= a) 1 else -1
}

/** مدة محترمة لتقليل الحركة (تُقرأ لحظة بناء الانتقال — خارج الـ composition). */
private fun dur(ms: Int): Int = if (MotionPreferences.reducedMotion) 0 else ms

/** تبويب الشريط السفلي — محور مشترك خفيف باتجاه ترتيب التبويب. */
fun NavGraphBuilder.tabComposable(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            val dir = tabDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            if (dir >= 0)
                slideInHorizontally(tween(dur(Motion.TAB_SWITCH))) { -it } +
                    fadeIn(tween(dur(Motion.TAB_SWITCH)))
            else
                slideInHorizontally(tween(dur(Motion.TAB_SWITCH))) { it } +
                    fadeIn(tween(dur(Motion.TAB_SWITCH)))
        },
        exitTransition = {
            val dir = tabDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            if (dir >= 0)
                slideOutHorizontally(tween(dur(Motion.TAB_SWITCH))) { it } +
                    fadeOut(tween(dur(Motion.TAB_SWITCH))) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(dur(Motion.TAB_SWITCH)))
            else
                slideOutHorizontally(tween(dur(Motion.TAB_SWITCH))) { -it } +
                    fadeOut(tween(dur(Motion.TAB_SWITCH))) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(dur(Motion.TAB_SWITCH)))
        },
        popEnterTransition = {
            val dir = tabDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            if (dir <= 0)
                slideInHorizontally(tween(dur(Motion.TAB_SWITCH))) { it } +
                    fadeIn(tween(dur(Motion.TAB_SWITCH)))
            else
                slideInHorizontally(tween(dur(Motion.TAB_SWITCH))) { -it } +
                    fadeIn(tween(dur(Motion.TAB_SWITCH)))
        },
        popExitTransition = {
            val dir = tabDirection(
                initialState.destination.route,
                targetState.destination.route
            )
            if (dir <= 0)
                slideOutHorizontally(tween(dur(Motion.TAB_SWITCH))) { -it } +
                    fadeOut(tween(dur(Motion.TAB_SWITCH))) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(dur(Motion.TAB_SWITCH)))
            else
                slideOutHorizontally(tween(dur(Motion.TAB_SWITCH))) { it } +
                    fadeOut(tween(dur(Motion.TAB_SWITCH))) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(dur(Motion.TAB_SWITCH)))
        }
    ) {
        content(it)
    }
}

/** شاشة توغّل (تفاصيل/سجل/إعدادات) — انزلاق أعمق 380ms. */
fun NavGraphBuilder.pushComposable(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            slideInHorizontally(tween(dur(Motion.SCREEN_PUSH_IN), easing = Motion.EaseOutQuint)) { -it } +
                fadeIn(tween(dur(Motion.SCREEN_PUSH_IN)))
        },
        exitTransition = {
            fadeOut(tween(dur(Motion.SCREEN_PUSH_OUT))) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(dur(Motion.SCREEN_PUSH_OUT)))
        },
        popEnterTransition = {
            fadeIn(tween(dur(Motion.SCREEN_PUSH_IN))) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(dur(Motion.SCREEN_PUSH_IN)))
        },
        popExitTransition = {
            slideOutHorizontally(tween(dur(Motion.SCREEN_PUSH_OUT), easing = Motion.EaseInQuint)) { -it } +
                fadeOut(tween(dur(Motion.SCREEN_PUSH_OUT)))
        }
    ) {
        content(it)
    }
}

/** التوافق القديم — يحوَّل تلقائياً إلى سلوك التوغّل الجديد. */
@Deprecated("استخدم tabComposable للتبويبات أو pushComposable لشاشات التوغّل (§5.2)")
fun NavGraphBuilder.animatedComposable(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    pushComposable(route, content)
}
