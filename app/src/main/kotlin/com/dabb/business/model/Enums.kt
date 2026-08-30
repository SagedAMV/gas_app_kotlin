package com.dabb.business.model

/** حالة الأسطوانة في المخزون — تُخزَّن في القاعدة كاسم الحالة (AVAILABLE/SOLD). */
enum class CylinderStatus { AVAILABLE, SOLD }

/** حالة سداد البيع — تُخزَّن كاسم الحالة (PAID/CREDIT). */
enum class SaleStatus { PAID, CREDIT }
