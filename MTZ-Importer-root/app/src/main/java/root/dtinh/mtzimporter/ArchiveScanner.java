package root.dtinh.mtzimporter;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ArchiveScanner {
    private static final Map<String, String> EXACT = new HashMap<>();
    private static final Set<String> DIRECT_CODES = new HashSet<>();
    private static final Set<String> IGNORED = new HashSet<>();
    private static final Set<String> PREVIEW_EXTENSIONS =
            new HashSet<>(Arrays.asList("png", "webp", "jpg", "jpeg"));

    static {
        EXACT.put("com.miui.home", "launcher");
        EXACT.put("com.android.systemui", "statusbar");
        EXACT.put("com.android.contacts", "contact");
        EXACT.put("com.android.mms", "mms");
        EXACT.put("framework-res", "framework");
        EXACT.put("fonts/roboto-regular.ttf", "fonts");
        EXACT.put("fonts/droidsansfallback.ttf", "fonts_fallback");
        EXACT.put("wallpaper/default_wallpaper.jpg", "wallpaper");
        EXACT.put("wallpaper/default_lock_wallpaper.jpg", "wallpaper");
        EXACT.put("ringtones/alarm.mp3", "alarm");
        EXACT.put("ringtones/ringtone.mp3", "ringtone");
        EXACT.put("ringtones/notification.mp3", "notification");
        EXACT.put("lockscreen", "lockstyle");
        EXACT.put("boots/bootanimation.zip", "bootanimation");
        EXACT.put("boots/bootaudio.mp3", "bootaudio");
        EXACT.put("com.android.settings", "com.android.settings");
        EXACT.put("largeicons", "largeicons");
        EXACT.put("rearscreen", "rearscreen");

        DIRECT_CODES.addAll(Arrays.asList(
                "largeicons", "rearscreen", "alarm", "audioeffect",
                "bootanimation", "bootaudio", "contact", "fonts",
                "framework", "framework-miui-res", "icons", "launcher",
                "lockscreen", "lockstyle", "mms", "ringtone",
                "notification", "statusbar", "wallpaper", "miwallpaper",
                "alarmscreen", "clock_1x2", "clock_2x2", "clock_2x4",
                "photoframe_2x2", "photoframe_2x4", "photoframe_4x4",
                "com.android.settings", "aod", "spaod", "splockscreen",
                "spwallpaper", "miui.systemui.plugin",
                "com.miui.securitycenter", "tkle"
        ));

        IGNORED.addAll(Arrays.asList(
                "updates.xml", "description.xml", "plugin_config.xml",
                "package.xml", "module_config.xml"
        ));
    }

    private ArchiveScanner() {
    }

    static ArchiveScan scan(ZipFile zip) {
        ArchiveScan result = new ArchiveScan();
        LinkedHashSet<String> seenCodes = new LinkedHashSet<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            String actualName = entry.getName();
            String normalized = normalize(actualName);
            if (!isSafe(normalized)) {
                continue;
            }

            if (isPreview(normalized)) {
                String relative = normalized.substring("preview/".length());
                if (!relative.isEmpty()) {
                    result.previewNames.add(relative);
                    result.previewEntries.put(relative, actualName);
                }
                continue;
            }

            ArchiveResource resource = classify(actualName, normalized);
            if (resource != null && seenCodes.add(resource.resourceCode)) {
                result.resources.add(resource);
            }
        }

        return result;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .toLowerCase(Locale.ROOT);
        return normalized;
    }

    private static boolean isSafe(String value) {
        return !value.isEmpty()
                && !value.startsWith("/")
                && !value.contains("../")
                && Pattern.matches("^[a-zA-Z0-9\\s._\\-/]+$", value);
    }

    private static boolean isPreview(String normalized) {
        if (!normalized.startsWith("preview/")) {
            return false;
        }

        int dot = normalized.lastIndexOf('.');
        if (dot < 0 || dot == normalized.length() - 1) {
            return false;
        }

        return PREVIEW_EXTENSIONS.contains(
                normalized.substring(dot + 1).toLowerCase(Locale.ROOT)
        );
    }

    private static ArchiveResource classify(
            String actualName,
            String normalized
    ) {
        if (IGNORED.contains(normalized)) {
            return null;
        }

        String mapped = EXACT.get(normalized);
        if (mapped != null) {
            return new ArchiveResource(actualName, mapped);
        }

        if (normalized.startsWith("fonts/")
                && (normalized.endsWith(".ttf")
                || normalized.endsWith(".otf")
                || normalized.endsWith(".ttc"))) {
            return new ArchiveResource(actualName, "fonts");
        }

        if (normalized.contains("/")) {
            return null;
        }

        if (DIRECT_CODES.contains(normalized)) {
            return new ArchiveResource(actualName, normalized);
        }

        if (normalized.contains(".")) {
            // ThemeKit treats unknown top-level package-like names as
            // resource codes, for example com.vendor.app.
            return new ArchiveResource(actualName, normalized);
        }

        return null;
    }
}
