-keep public class nonroot.dtinh.mtzimporter.MainActivity {
    public <init>();
    protected <methods>;
    public void onRequestPermissionsResult(int, java.lang.String[], int[]);
}

-keep public class nonroot.dtinh.mtzimporter.BootReceiver { *; }
-keep public class nonroot.dtinh.mtzimporter.BlockService { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,EnclosingMethod
