package nonroot.dtinh.mtzimporter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

final class ProtectionState {
    private static final String PREFS = "theme_protection";
    private static final String KEY_ENABLED = "enabled";

    private ProtectionState() {
    }

    static boolean isEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context context, boolean enabled) {
        preferences(context)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        Context storageContext = context;
        if (Build.VERSION.SDK_INT >= 24) {
            storageContext =
                    context.createDeviceProtectedStorageContext();
        }
        return storageContext.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
    }
}
