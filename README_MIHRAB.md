# مِحراب — Android TV Native / Phase 1

المشروع الأصلي الجديد هو `app/` وإعدادات Gradle في **جذر هذا المجلد**. اللغة Java والواجهة Android Views/XML. مجلد `launcher/` الموروث مرجع CleanYourTV ولا يدخل في APK مِحراب.

## البناء المحلي

JDK 17 وAndroid SDK في `G:\Android\SDK`، مع Platform 35 وBuild Tools 35.0.0. يمكن تعديل مسار SDK في `local.properties` المحلي غير المتعقب.

```powershell
.\tools\build-local.ps1
```

هذا السكربت ينفذ الاختبارات وLint والبناء فقط. الناتج:
`app/build/outputs/apk/debug/app-debug.apk`.

## نتيجة المرحلة

- [تقرير التنفيذ](NATIVE_IMPLEMENTATION_REPORT.md)
- [المقارنة البصرية](VISUAL_FIDELITY_REPORT.md)
- [البناء والاختبارات](ANDROID_BUILD_REPORT.md)
- [المراحل التالية](NEXT_PHASES.md)
- [نسبة المصدر والأصول](THIRD_PARTY_NOTICES.md)

الصور في `validation/` لقطات محلية من Chrome ومحاكي Android؛ لا توجد لقطات من Mi Box. مواقيت الصلاة وعدّاد رمضان نماذج عرض موسومة بذلك. تبويب القرآن وشاشة السكون مؤجلان.

## الأدوات

`tools/prepare_native_assets.py` يستورد الأصول المعتمدة للموارد الأصلية دون تشغيل HTML داخل التطبيق؛ راجع متطلباته أعلى الملف. `tools/audit_native.py` يفحص سلامة الأصول والمحتوى وAPK وعدم تغير المصدرين.

`tools/start-local-emulator.ps1` و`tools/local_emulator_qa.py` خاصان بمحاكي الكمبيوتر `Mihrab_Phase1` على منفذ ADB مستقل `5041` وبالرقم الثابت `emulator-5580`. أداة QA ترفض أي جهاز آخر. ليست هذه أدوات نشر على التلفزيون.

المستندات والأدوات الموروثة أدناه وفي `launcher/` تخص CleanYourTV. لا تستخدم سكربتات التثبيت الموروثة لهذه المرحلة.
