# Activity is created by Android from the manifest.
-keep public class root.dtinh.mtzimporter.MainActivity {
    public <init>();
    protected <methods>;
}

# Keep source information out of the release APK.
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,EnclosingMethod
