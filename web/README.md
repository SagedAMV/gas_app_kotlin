# دَبّ للغاز — نسخة الويب (إعادة بناء)

إعادة بناء ويب كاملة لتطبيق `gas_app_kotlin` (Kotlin/Compose/Room) بنفس جوهر المنطق:
كل عملية مالية/مخزونية داخل معاملة قاعدة بيانات واحدة، والأرصدة تُشتق من المعاملات.

## التقنيات
Next.js (App Router) · PostgreSQL · Drizzle ORM · Tailwind CSS v4 · Framer Motion

## التشغيل
```bash
# 1) التبعيات
npm install
# 2) متغير البيئة (قاعدة PostgreSQL محلية)
echo 'DATABASE_URL=postgresql://postgres:postgres@127.0.0.1:5432/app_db' > .env
# 3) تطبيق المخطط ثم البناء والتشغيل
npx drizzle-kit push
npm run build && npm start
```

## عرض التحسينات
افتح `changes.html` (أو `/changes.html` بعد التشغيل) على شاشة هاتف —
عرض متحرك «قبل/بعد» يشرح كل تحسين بشكل إبداعي.

## خريطة المجلد
```
src/
├── db/               ← مخطط قاعدة البيانات (مطابق لمخطط Room v5 + قيود CHECK/FK)
├── lib/
│   ├── logic.ts      ← منطق الأعمال (مرآة AppViewModel.kt)
│   ├── money.ts      ← وحدة المال وأسقفها
│   └── types.ts      ← أنواع مشتركة
├── app/
│   ├── api/          ← نقاط العملية (حالة، كتابة موحدة، زبون، محطة، مبيعات، قفل)
│   └── page.tsx      ← الصدفة + شريط التنقل + قفل الرمز
└── components/       ← الشاشات الست + المكونات المشتركة + مزود الحالة
```
