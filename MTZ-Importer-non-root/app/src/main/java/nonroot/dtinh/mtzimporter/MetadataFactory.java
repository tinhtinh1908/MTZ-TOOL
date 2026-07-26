package nonroot.dtinh.mtzimporter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MetadataFactory {
    private static final Pattern PREVIEW_PATTERN = Pattern.compile(
            "^(?:preview_)?([a-z][a-z_]*[a-z])"
                    + "(?:_([0-9]+[a-z0-9]*))?"
                    + "_(?:small_)?(\\d+)\\.(jpg|png|webp)$",
            Pattern.CASE_INSENSITIVE
    );

    private MetadataFactory() {
    }

    static JSONObject component(
            String localId,
            String rootThemeId,
            String resourceCode,
            java.io.File contentFile,
            List<String> allPreviews,
            ThemeDescription description
    ) throws Exception {
        JSONObject object = base(
                localId,
                IoUtils.sha1(contentFile),
                description.platformOrDefault(),
                contentFile.length(),
                description
        );

        List<String> previews = previewsFor(
                resourceCode,
                allPreviews
        );
        object.put("builtInThumbnails", previewObject(previews));
        object.put("builtInPreviews", previewObject(previews));
        object.put("thumbnails", new JSONArray());
        object.put("previews", new JSONArray());

        JSONArray parents = new JSONArray();
        JSONObject parent = new JSONObject();
        parent.put("localId", rootThemeId);
        parent.put("resourceCode", "theme");
        parent.put("extraMeta", new JSONObject());
        parent.put("metaPath", JSONObject.NULL);
        parent.put("contentPath", JSONObject.NULL);
        parents.put(parent);
        object.put("parentResources", parents);

        object.put("subResources", new JSONArray());
        object.put("extraMeta", new JSONObject());
        object.put("metaPath", JSONObject.NULL);
        object.put("contentPath", JSONObject.NULL);
        object.put("rightsPath", JSONObject.NULL);
        object.put("wallpaperStyle", description.wallpaperStyle);
        object.put("isSingleResource", description.singleResource);
        object.put("iconsCount", JSONObject.NULL);
        return object;
    }

    static JSONObject rootTheme(
            String rootThemeId,
            String archiveHash,
            long archiveSize,
            List<String> allPreviews,
            LinkedHashMap<String, String> subResources,
            ThemeDescription description
    ) throws Exception {
        JSONObject object = base(
                rootThemeId,
                archiveHash,
                description.platformOrDefault(),
                archiveSize,
                description
        );

        ArrayList<String> nonSmall = new ArrayList<>();
        for (String preview : allPreviews) {
            if (!preview.toLowerCase(Locale.ROOT).contains("small")) {
                nonSmall.add(preview);
            }
        }

        object.put("builtInThumbnails", previewObject(nonSmall));
        object.put("builtInPreviews", previewObject(nonSmall));
        object.put("thumbnails", new JSONArray());
        object.put("previews", new JSONArray());
        object.put("parentResources", new JSONArray());

        JSONArray children = new JSONArray();
        for (Map.Entry<String, String> entry :
                subResources.entrySet()) {
            JSONObject child = new JSONObject();
            child.put("localId", entry.getValue());
            child.put("resourceCode", entry.getKey());
            child.put("extraMeta", new JSONObject());
            child.put("metaPath", JSONObject.NULL);
            child.put("contentPath", JSONObject.NULL);
            children.put(child);
        }
        object.put("subResources", children);
        object.put("extraMeta", new JSONObject());
        object.put("metaPath", JSONObject.NULL);
        object.put(
                "contentPath",
                "/system/../storage/emulated/0/Android/data/"
                        + "com.android.thememanager/files/MIUI/theme/"
                        + ".data/content/theme/"
                        + rootThemeId
                        + ".mrc"
        );
        object.put("rightsPath", JSONObject.NULL);
        object.put("iconsCount", JSONObject.NULL);
        return object;
    }

    private static JSONObject base(
            String localId,
            String hash,
            int platform,
            long size,
            ThemeDescription description
    ) throws Exception {
        JSONObject object = new JSONObject();
        object.put("localId", localId);
        object.put("onlineId", JSONObject.NULL);
        object.put("assemblyId", JSONObject.NULL);
        object.put("productId", JSONObject.NULL);
        object.put("hash", hash);
        object.put("platform", platform);
        object.put("size", size);
        object.put("updatedTime", 0);
        object.put("version",
                description.version == null
                        || description.version.trim().isEmpty()
                        ? "1"
                        : description.version);
        object.put("authors", localized(description.authors));
        object.put("designers", localized(description.designers));
        object.put("titles", localized(description.titles));
        object.put("descriptions", localized(description.descriptions));
        object.put("screenRatio",
                nullableString(description.screenRatio));
        object.put(
                "supportHomeSearchBar",
                description.supportHomeSearchBar
        );
        object.put("packageVersion", JSONObject.NULL);
        object.put("packageName", JSONObject.NULL);
        object.put("officialIcons", description.officialIcons);
        object.put("fontWeight",
                nullableString(description.fontWeight));
        object.put("price", -1);
        object.put("isBackUpVersion", description.backupVersion);
        object.put("themeType", description.themeType);
        object.put(
                "miuiAdapterVersion",
                nullableString(description.miuiAdapterVersion)
        );
        return object;
    }

    private static JSONObject localized(
            LinkedHashMap<String, String> map
    ) throws Exception {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() != null
                    && !entry.getValue().trim().isEmpty()) {
                object.put(entry.getKey(), entry.getValue());
            }
        }

        if (!object.has("fallback")) {
            String fallback = "";
            if (!map.isEmpty()) {
                fallback = map.values().iterator().next();
            }
            object.put("fallback", fallback);
        }
        return object;
    }

    private static JSONObject previewObject(List<String> values)
            throws Exception {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value == null ? JSONObject.NULL : value);
        }
        object.put("fallback", array);
        return object;
    }

    private static Object nullableString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return JSONObject.NULL;
        }
        return value;
    }

    private static List<String> previewsFor(
            String resourceCode,
            List<String> allPreviews
    ) {
        if ("fonts".equals(resourceCode)) {
            ArrayList<String> fontPreview = new ArrayList<>();
            fontPreview.add("preview_fonts_small_0.jpg");
            return fontPreview;
        }

        String wanted = resourceCode;
        if ("bootanimation".equals(wanted)) {
            wanted = "animation";
        } else if ("lockstyle".equals(wanted)) {
            wanted = "lockscreen";
        }

        ArrayList<String> matched = new ArrayList<>();
        for (String value : allPreviews) {
            String fileName = value;
            int slash = fileName.lastIndexOf('/');
            if (slash >= 0) {
                fileName = fileName.substring(slash + 1);
            }

            Matcher matcher = PREVIEW_PATTERN.matcher(fileName);
            if (matcher.matches()
                    && wanted.equalsIgnoreCase(matcher.group(1))) {
                matched.add(value);
            }
        }

        if (!matched.isEmpty()) {
            return matched;
        }

        return new ArrayList<>(allPreviews);
    }
}
