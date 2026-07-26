package root.dtinh.mtzimporter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class ThemeRepository {
    private static final String DATA_ROOT =
            "/storage/emulated/0/Android/data/"
                    + "com.android.thememanager/files/MIUI/theme/.data";
    private static final String THEME_META_DIR =
            DATA_ROOT + "/meta/theme";
    private static final Pattern SAFE_SEGMENT =
            Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final char RECORD_SEPARATOR = 30;
    private static final char FIELD_SEPARATOR = 31;

    List<ThemeRecord> listThemes() throws Exception {
        ensureRoot();

        String script =
                "DIR=" + RootShell.quote(THEME_META_DIR) + "\n"
                        + "[ -d \"$DIR\" ] || exit 0\n"
                        + "for f in \"$DIR\"/*.mrm; do\n"
                        + "  [ -f \"$f\" ] || continue\n"
                        + "  n=${f##*/}\n"
                        + "  n=${n%.mrm}\n"
                        + "  printf '\\036%s\\037' \"$n\"\n"
                        + "  cat \"$f\"\n"
                        + "done\n";

        RootShell.Result result = RootShell.run(script);
        if (result.code != 0) {
            throw new IllegalStateException(
                    "Không đọc được kho chủ đề:\n" + result.output
            );
        }

        ArrayList<ThemeRecord> themes = new ArrayList<>();
        String[] records = result.output.split(
                String.valueOf(RECORD_SEPARATOR)
        );
        for (String record : records) {
            if (record == null || record.isEmpty()) {
                continue;
            }
            int separator = record.indexOf(FIELD_SEPARATOR);
            if (separator <= 0) {
                continue;
            }

            String themeId = record.substring(0, separator).trim();
            String json = record.substring(separator + 1).trim();
            if (!isSafeSegment(themeId) || json.isEmpty()) {
                continue;
            }

            try {
                themes.add(parseTheme(themeId, new JSONObject(json)));
            } catch (Throwable ignored) {
            }
        }

        Collections.sort(
                themes,
                Comparator.comparing(
                        value -> value.title.toLowerCase(Locale.ROOT)
                )
        );
        return themes;
    }

    int deleteTheme(ThemeRecord theme) throws Exception {
        ensureRoot();
        requireSafeSegment(theme.themeId, "ID chủ đề");

        ArrayList<String> paths = new ArrayList<>();
        for (ThemeRecord.ComponentRecord component :
                theme.components) {
            requireSafeSegment(
                    component.resourceCode,
                    "loại thành phần"
            );
            requireSafeSegment(
                    component.localId,
                    "ID thành phần"
            );
            paths.add(
                    DATA_ROOT
                            + "/meta/"
                            + component.resourceCode
                            + "/"
                            + component.localId
                            + ".mrm"
            );
            paths.add(
                    DATA_ROOT
                            + "/content/"
                            + component.resourceCode
                            + "/"
                            + component.localId
                            + ".mrc"
            );
        }

        paths.add(
                DATA_ROOT
                        + "/preview/theme/"
                        + theme.themeId
        );
        paths.add(
                DATA_ROOT
                        + "/meta/theme/"
                        + theme.themeId
                        + ".mrm"
        );
        paths.add(
                DATA_ROOT
                        + "/content/theme/"
                        + theme.themeId
                        + ".mrc"
        );

        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        for (String path : paths) {
            script.append("rm -rf ")
                    .append(RootShell.quote(path))
                    .append("\n");
        }
        script.append("am force-stop com.android.thememanager ")
                .append("2>/dev/null || true\n");
        for (String path : paths) {
            script.append("test ! -e ")
                    .append(RootShell.quote(path))
                    .append("\n");
        }

        RootShell.Result result = RootShell.run(script.toString());
        if (result.code != 0) {
            throw new IllegalStateException(
                    "Xóa chưa hoàn tất:\n" + result.output
            );
        }
        return paths.size();
    }

    private ThemeRecord parseTheme(
            String themeId,
            JSONObject metadata
    ) throws Exception {
        String title = localizedValue(metadata.optJSONObject("titles"));
        if (title.isEmpty()) {
            title = themeId;
        }

        String author = localizedValue(
                metadata.optJSONObject("authors")
        );

        ArrayList<ThemeRecord.ComponentRecord> components =
                new ArrayList<>();
        JSONArray array = metadata.optJSONArray("subResources");
        if (array != null) {
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                String resourceCode =
                        item.optString("resourceCode", "").trim();
                String localId =
                        item.optString("localId", "").trim();
                if (isSafeSegment(resourceCode)
                        && isSafeSegment(localId)) {
                    components.add(
                            new ThemeRecord.ComponentRecord(
                                    resourceCode,
                                    localId
                            )
                    );
                }
            }
        }

        return new ThemeRecord(
                themeId,
                title,
                author,
                components
        );
    }

    private String localizedValue(JSONObject object) {
        if (object == null) {
            return "";
        }
        String fallback = object.optString("fallback", "").trim();
        if (!fallback.isEmpty()) {
            return fallback;
        }
        JSONArray names = object.names();
        if (names == null) {
            return "";
        }
        for (int index = 0; index < names.length(); index++) {
            String key = names.optString(index, "");
            String value = object.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private void ensureRoot() throws Exception {
        RootShell.Result result = RootShell.run("id\n");
        if (result.code != 0 || !result.output.contains("uid=0")) {
            throw new IllegalStateException(
                    "Không được cấp quyền root.\n" + result.output
            );
        }
    }

    private boolean isSafeSegment(String value) {
        return value != null
                && SAFE_SEGMENT.matcher(value).matches()
                && !".".equals(value)
                && !"..".equals(value);
    }

    private void requireSafeSegment(
            String value,
            String label
    ) {
        if (!isSafeSegment(value)) {
            throw new IllegalArgumentException(
                    label + " không hợp lệ."
            );
        }
    }
}
