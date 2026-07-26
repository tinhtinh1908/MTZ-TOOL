package root.dtinh.mtzimporter;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ThemeImporter {
    private static final String TARGET_ROOT =
            "/storage/emulated/0/Android/data/"
                    + "com.android.thememanager/files/MIUI/theme/.data";

    private final Context context;
    private final ProgressCallback callback;

    ThemeImporter(Context context, ProgressCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    ImportResult importFromUri(Uri uri) throws Exception {
        ensureRoot();

        File workingRoot = new File(
                context.getCacheDir(),
                "imports/" + UUID.randomUUID().toString()
        );
        File archive = new File(workingRoot, "source.mtz");
        File staging = new File(workingRoot, "remote");

        if (!workingRoot.mkdirs()) {
            throw new IllegalStateException(
                    "Không tạo được thư mục làm việc."
            );
        }

        try {
            progress("Đang sao chép file vào bộ nhớ tạm…");
            IoUtils.copyUri(
                    context.getContentResolver(),
                    uri,
                    archive
            );

            String sourceName = IoUtils.displayName(
                    context.getContentResolver(),
                    uri
            );
            String fallbackTitle =
                    IoUtils.stripMtzExtension(sourceName);

            progress("Đang phân tích description.xml và thành phần MTZ…");
            try (ZipFile zip = new ZipFile(archive)) {
                ThemeDescription description =
                        ThemeDescription.parse(zip);
                String title = description.titleOr(fallbackTitle);
                description.titles.put("fallback", title);

                ArchiveScan scan = ArchiveScanner.scan(zip);
                if (scan.resources.isEmpty()) {
                    throw new IllegalStateException(
                            "Không tìm thấy thành phần theme hợp lệ "
                                    + "trong MTZ."
                    );
                }

                String rootThemeId = UUID.randomUUID().toString();
                File rootPreviewDir = new File(
                        staging,
                        "preview/theme/" + rootThemeId
                );
                if (!rootPreviewDir.mkdirs()) {
                    throw new IllegalStateException(
                            "Không tạo được thư mục preview."
                    );
                }

                progress("Đang trích xuất ảnh xem trước…");
                extractPreviews(
                        zip,
                        scan,
                        rootPreviewDir
                );

                LinkedHashMap<String, String> subResources =
                        new LinkedHashMap<>();

                int done = 0;
                for (ArchiveResource resource : scan.resources) {
                    done++;
                    progress(
                            "Đang xử lý thành phần "
                                    + done
                                    + "/"
                                    + scan.resources.size()
                                    + ": "
                                    + resource.resourceCode
                    );

                    String localId = UUID.randomUUID().toString();
                    File content = new File(
                            staging,
                            "content/"
                                    + resource.resourceCode
                                    + "/"
                                    + localId
                                    + ".mrc"
                    );
                    extractEntry(
                            zip,
                            resource.entryName,
                            content
                    );

                    if ("fonts".equals(resource.resourceCode)) {
                        File fontPreview = new File(
                                rootPreviewDir,
                                "preview_fonts_small_0.jpg"
                        );
                        FontPreview.create(
                                content,
                                fontPreview,
                                title
                        );
                        if (fontPreview.exists()
                                && !scan.previewNames.contains(
                                "preview_fonts_small_0.jpg")) {
                            scan.previewNames.add(
                                    "preview_fonts_small_0.jpg"
                            );
                        }
                    }

                    JSONObject metadata =
                            MetadataFactory.component(
                                    localId,
                                    rootThemeId,
                                    resource.resourceCode,
                                    content,
                                    scan.previewNames,
                                    description
                            );
                    File meta = new File(
                            staging,
                            "meta/"
                                    + resource.resourceCode
                                    + "/"
                                    + localId
                                    + ".mrm"
                    );
                    IoUtils.writeUtf8(
                            meta,
                            metadata.toString()
                    );
                    subResources.put(
                            resource.resourceCode,
                            localId
                    );
                }

                progress("Đang tạo metadata chủ đề gốc…");
                File rootContent = new File(
                        staging,
                        "content/theme/"
                                + rootThemeId
                                + ".mrc"
                );
                File rootContentParent = rootContent.getParentFile();
                if (rootContentParent != null) {
                    rootContentParent.mkdirs();
                }
                if (!rootContent.createNewFile()) {
                    throw new IllegalStateException(
                            "Không tạo được content gốc."
                    );
                }

                JSONObject rootMetadata =
                        MetadataFactory.rootTheme(
                                rootThemeId,
                                IoUtils.sha1(archive),
                                archive.length(),
                                scan.previewNames,
                                subResources,
                                description
                        );
                File rootMeta = new File(
                        staging,
                        "meta/theme/"
                                + rootThemeId
                                + ".mrm"
                );
                IoUtils.writeUtf8(
                        rootMeta,
                        rootMetadata.toString()
                );

                progress("Đang nhập dữ liệu bằng quyền root…");
                copyStagingWithRoot(
                        staging,
                        archive,
                        title
                );

                progress("Đang xác minh dữ liệu sau khi nhập…");
                verifyImported(
                        rootThemeId,
                        subResources
                );

                return new ImportResult(
                        rootThemeId,
                        title,
                        subResources.size(),
                        scan.previewNames.size(),
                        subResources.get("fonts")
                );
            }
        } finally {
            IoUtils.deleteRecursively(workingRoot);
        }
    }

    private void ensureRoot() throws Exception {
        RootShell.Result result = RootShell.run("id\n");
        if (result.code != 0 || !result.output.contains("uid=0")) {
            throw new IllegalStateException(
                    "Không được cấp quyền root.\n" + result.output
            );
        }
    }

    private void extractPreviews(
            ZipFile zip,
            ArchiveScan scan,
            File targetDir
    ) throws Exception {
        for (Map.Entry<String, String> preview :
                scan.previewEntries.entrySet()) {
            ZipEntry entry = zip.getEntry(preview.getValue());
            if (entry == null) {
                continue;
            }

            File output = safeChild(
                    targetDir,
                    preview.getKey()
            );
            try (InputStream input = zip.getInputStream(entry)) {
                IoUtils.copy(input, output);
            }
        }
    }

    private void extractEntry(
            ZipFile zip,
            String entryName,
            File destination
    ) throws Exception {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new IllegalStateException(
                    "Không tìm thấy mục trong MTZ: " + entryName
            );
        }

        try (InputStream input = zip.getInputStream(entry)) {
            IoUtils.copy(input, destination);
        }
    }

    private File safeChild(File root, String relative)
            throws Exception {
        File child = new File(root, relative);
        String rootPath = root.getCanonicalPath() + File.separator;
        String childPath = child.getCanonicalPath();

        if (!childPath.startsWith(rootPath)) {
            throw new IllegalStateException(
                    "Đường dẫn ZIP không an toàn: " + relative
            );
        }
        return child;
    }

    private void copyStagingWithRoot(
            File staging,
            File originalArchive,
            String title
    ) throws Exception {
        String safeTitle = IoUtils.safeFileName(title);
        String backupPath =
                "/storage/emulated/0/MIUI/theme/"
                        + safeTitle
                        + ".mtz";

        String script =
                "set -e\n"
                        + "SRC="
                        + RootShell.quote(staging.getAbsolutePath())
                        + "\n"
                        + "DST="
                        + RootShell.quote(TARGET_ROOT)
                        + "\n"
                        + "mkdir -p \"$DST\"\n"
                        + "cp -Rf \"$SRC\"/. \"$DST\"/\n"
                        + "mkdir -p /storage/emulated/0/MIUI/theme\n"
                        + "cp -f "
                        + RootShell.quote(originalArchive.getAbsolutePath())
                        + " "
                        + RootShell.quote(backupPath)
                        + "\n"
                        + "find \"$DST\" -type d -exec chmod 0775 {} \\; "
                        + "2>/dev/null || true\n"
                        + "find \"$DST\" -type f -exec chmod 0664 {} \\; "
                        + "2>/dev/null || true\n"
                        + "restorecon -RF \"$DST\" 2>/dev/null || true\n"
                        + "am force-stop com.android.thememanager "
                        + "2>/dev/null || true\n";

        RootShell.Result result = RootShell.run(script);
        if (result.code != 0) {
            throw new IllegalStateException(
                    "Lệnh root nhập dữ liệu thất bại ("
                            + result.code
                            + "):\n"
                            + result.output
            );
        }
    }

    private void verifyImported(
            String rootThemeId,
            LinkedHashMap<String, String> subResources
    ) throws Exception {
        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        script.append("test -s ")
                .append(RootShell.quote(
                        TARGET_ROOT
                                + "/meta/theme/"
                                + rootThemeId
                                + ".mrm"
                ))
                .append("\n");
        script.append("test -f ")
                .append(RootShell.quote(
                        TARGET_ROOT
                                + "/content/theme/"
                                + rootThemeId
                                + ".mrc"
                ))
                .append("\n");

        for (Map.Entry<String, String> entry :
                subResources.entrySet()) {
            script.append("test -s ")
                    .append(RootShell.quote(
                            TARGET_ROOT
                                    + "/meta/"
                                    + entry.getKey()
                                    + "/"
                                    + entry.getValue()
                                    + ".mrm"
                    ))
                    .append("\n");
            script.append("test -s ")
                    .append(RootShell.quote(
                            TARGET_ROOT
                                    + "/content/"
                                    + entry.getKey()
                                    + "/"
                                    + entry.getValue()
                                    + ".mrc"
                    ))
                    .append("\n");
        }

        RootShell.Result result = RootShell.run(script.toString());
        if (result.code != 0) {
            throw new IllegalStateException(
                    "Dữ liệu đã chép nhưng kiểm tra không đạt:\n"
                            + result.output
            );
        }
    }

    private void progress(String message) {
        if (callback != null) {
            callback.onProgress(message);
        }
    }
}
