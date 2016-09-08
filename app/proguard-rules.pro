#基本指令
################################################################################################################################################################
-optimizationpasses 5	# 指定代码的压缩级别
-dontusemixedcaseclassnames	# 表示混淆时不使用大小写混合类名
-dontskipnonpubliclibraryclasses	# 表示不跳过library中的非public的类
-dontskipnonpubliclibraryclassmembers	# 指定不去忽略包可见的库类的成员
-dontoptimize	# 表示不进行优化，建议使用此选项，因为根据proguard-android-optimize.txt中的描述，优化可能会造成一些潜在风险，不能保证在所有版本的Dalvik上都正常运行
-dontpreverify  # 表示不进行预校验,这个预校验是作用在Java平台上的，Android平台上不需要这项功能，去掉之后还可以加快混淆速度
-verbose	# 表示打印混淆的详细信息
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*	# 混淆时所采用的算法

-dump dump.txt	# 描述apk内所有class文件的内部结构
-printseeds seeds.txt	# 列出了没有被混淆的类和成员
-printusage unused.txt	# 列出了源代码中被删除在apk中不存在的代码
-printmapping mapping.txt	# 表示混淆前后代码的对照表
################################################################################################################################################################

#公共组件
################################################################################################################################################################
-keep public class * extends android.app.Activity	# 保留继承自Activity类不被混淆
-keep public class * extends android.app.Application	# 保留继承自Application类不被混淆
-keep public class * extends android.support.multidex.MultiDexApplication	# 保留继承自MultiDexApplication类不被混淆
-keep public class * extends android.app.Service	# 保留继承自Service类不被混淆
-keep public class * extends android.content.BroadcastReceiver	# 保留继承自BroadcastReceiver类不被混淆
-keep public class * extends android.content.ContentProvider	# 保留继承自ContentProvider类不被混淆
-keep public class * extends android.app.backup.BackupAgentHelper	# 保留继承自BackupAgentHelper类不被混淆
-keep public class * extends android.preference.Preference	# 保留继承自Preference类不被混淆
-keep public class com.google.vending.licensing.ILicensingService	# 保留Google包下ILicensingService类不被混淆
-keep public class com.android.vending.licensing.ILicensingService	# 保留Android包下ILicensingService类不被混淆

-keepattributes *Annotation*,InnerClasses,Signature,SourceFile,LineNumberTable	# 保留相关属性

-keepclasseswithmembernames class * {	# 保持native方法不被混淆
    native <methods>;
}

-keepclassmembers public class * extends android.view.View{	# 保持自定义控件类不被混淆
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclasseswithmembers class * {	# 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {	# 表示不混淆Activity中参数是View的方法，主要针对在xml中配置onClick事件
   public void *(android.view.View);
}

-keepclassmembers enum * {	# 保持枚举类不被混淆
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {	# 保持Parcelable不被混淆
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {	# 保持Serializable不被混淆
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepclassmembers class **.R$* {	# 表示不混淆R文件下的静态字段
    public static <fields>;
}

-keepclassmembers class * extends android.webkit.webViewClient {	# 保留WebView
	public void *(android.webkit.webView, jav.lang.String);
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
################################################################################################################################################################

#实体类
################################################################################################################################################################
#-keep class 你的实体类包名.** { *; }
################################################################################################################################################################

#反射相关
################################################################################################################################################################
#-keep class 你的类所在的包.** { *; }
################################################################################################################################################################

#JS调用相关
################################################################################################################################################################
-keepattributes *JavascriptInterface*
-keep class **.Webview2JsInterface { *; }  # 保持WebView对HTML页面的API不被混淆
-keepclassmembers class fqcn.of.javascript.interface.for.webview {	# 保留WebView
   public *;
}
#-keep class 你的类所在的包.** { *; }
#如果是内部类则使用如下方式
#-keepclasseswithmembers class 你的类所在的包.父类$子类 { <methods>; }
################################################################################################################################################################