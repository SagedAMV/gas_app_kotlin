package com.dabb.business.ui.animation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * دخول وخروج الشاشات — انزلاق من الأسفل + تلاشي + توسع بسيط
 * المهارة: ㊹ (Design Pattern — Navigation Animation) + ㉜ (دفاعي — لا يوجد انقطاع مفاجئ)
 */
fun NavGraphBuilder.animatedComposable(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            slideInVertically(
                animationSpec = AnimationSpecs.SCREEN_ENTER_OFFSET,
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = AnimationSpecs.SCREEN_ENTER)
        },
        exitTransition = {
            slideOutVertically(
                animationSpec = AnimationSpecs.SCREEN_EXIT_OFFSET,
                targetOffsetY = { -it / 3 }
            ) + fadeOut(animationSpec = AnimationSpecs.SCREEN_EXIT)
        },
        popEnterTransition = {
            slideInVertically(
                animationSpec = AnimationSpecs.SCREEN_ENTER_OFFSET,
                initialOffsetY = { -it / 3 }
            ) + fadeIn(animationSpec = AnimationSpecs.SCREEN_ENTER)
        },
        popExitTransition = {
            slideOutVertically(
                animationSpec = AnimationSpecs.SCREEN_EXIT_OFFSET,
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = AnimationSpecs.SCREEN_EXIT)
        }
    ) {
        content(it)
    }
}
