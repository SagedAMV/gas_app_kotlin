package com.dabb.business.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.ui.viewmodel.AppViewModel

/**
 * إصلاح مزامنة التبويبات (تقرير مستخدم 2026-09-09):
 *
 * كانت كل شاشة داخل NavHost تستدعي viewModel() فتحصل على نسخة AppViewModel
 * خاصة بمدخل التنقل (NavBackStackEntry) — لا بال نشاط. النتيجة: كل نسخة
 * تحمل لقطة بياناتها لحظة إنشائها فقط (لا Flow/LiveData يربطها بالقاعدة)،
 * فالشراء من المحطة عبر تبويب المخزون كان يحدّث نسخته وحده، وتبويب الصرف
 * يظل يعرض الكمية القديمة (0) ويحجب البيع — وكذلك شريط الأخطاء العام
 * والسعر الافتراضي وقوائم الزبائن بين التبويبات.
 *
 * هذه الدالة تربط كل الشاشات بنسخة واحدة على مستوى النشاط:
 * أي عملية (شراء/بيع/تحصيل/تعديل) تنعكس فوراً في كل التبويبات،
 * وشريط الأخطاء العام يستقبل أخطاء جميع الشاشات.
 *
 * ⚠️ أي شاشة جديدة يجب أن تستخدم sharedAppViewModel() — لا viewModel() —
 * وإلا عادت مشكلة الحالة المتجمدة في تلك الشاشة.
 */
@Composable
fun sharedAppViewModel(): AppViewModel {
    // فك تغليف ContextWrapper وصولاً إلى النشاط — بأمان وبدون افتراضات قاسية
    val owner = generateSequence<Context>(LocalContext.current) { c ->
        (c as? ContextWrapper)?.baseContext
    }.filterIsInstance<ComponentActivity>().firstOrNull()
        ?: error("sharedAppViewModel: لم يُعثر على ComponentActivity في سياق التركيب")
    return viewModel(viewModelStoreOwner = owner)
}
