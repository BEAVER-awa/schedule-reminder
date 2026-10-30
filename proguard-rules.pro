# 行程提醒 — R8 混淆规则
# 目标：删除无用代码 + 全量优化（内联/类合并/常量折叠）+ 类/方法/字段改名
# 注：已开启 R8 全量优化（不再 -dontoptimize），需要实机验证
-allowaccessmodification

# —— Android 组件必须按类名保留（清单/系统按字符串反射加载）——
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.appwidget.AppWidgetProvider
-keep public class * extends android.app.backup.BackupAgentHelper

# —— 自定义 View 的构造函数（XML / RemoteViews 可能反射实例化）——
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# —— 保留注解与行号，崩溃堆栈仍可读，配合 mapping.txt 反查 ——
-keepattributes *Annotation*, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

