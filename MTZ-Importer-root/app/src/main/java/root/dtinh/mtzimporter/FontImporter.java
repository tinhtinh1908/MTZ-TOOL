package root.dtinh.mtzimporter;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

final class FontImporter {
    private static final long MAX_FONT_SIZE = 128L * 1024L * 1024L;

    private final Context context;
    private final ProgressCallback callback;

    FontImporter(Context context, ProgressCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    ImportResult importFromUri(Uri uri) throws Exception {
        File workingRoot = new File(
                context.getCacheDir(),
                "font-imports/" + UUID.randomUUID().toString()
        );
        File source = new File(workingRoot, "selected");
        if (!workingRoot.mkdirs()) {
            throw new IllegalStateException(
                    "Không tạo được thư mục xử lý font."
            );
        }

        try {
            progress("Đang sao chép tệp font vào bộ nhớ tạm…");
            IoUtils.copyUri(
                    context.getContentResolver(),
                    uri,
                    source
            );

            String sourceName = IoUtils.displayName(
                    context.getContentResolver(),
                    uri
            );
            String title = stripExtension(sourceName);
            File mtz;

            if (isZip(source)) {
                progress("Đang kiểm tra gói font MTZ…");
                validateFontMtz(source);
                mtz = new File(workingRoot, safeTitle(title) + ".mtz");
                copyFile(source, mtz);
            } else {
                progress("Đang kiểm tra dữ liệu TTF/OTF…");
                validateFont(source);
                mtz = new File(workingRoot, safeTitle(title) + ".mtz");
                progress("Đang đóng gói font theo cấu trúc Xiaomi…");
                createXiaomiMtz(source, mtz, title);
            }

            progress("Đang đăng ký font vào Chủ đề Xiaomi…");
            return new ThemeImporter(
                    context,
                    callback
            ).importFromUri(Uri.fromFile(mtz));
        } finally {
            IoUtils.deleteRecursively(workingRoot);
        }
    }

    private void validateFontMtz(File archive) throws Exception {
        try (ZipFile zip = new ZipFile(archive)) {
            ArchiveScan scan = ArchiveScanner.scan(zip);
            for (ArchiveResource resource : scan.resources) {
                if ("fonts".equals(resource.resourceCode)) {
                    return;
                }
            }
        }
        throw new IllegalStateException(
                "MTZ này không chứa thành phần font Xiaomi."
        );
    }

    private void validateFont(File font) throws Exception {
        if (!font.isFile() || font.length() < 12) {
            throw new IllegalStateException("Tệp font bị trống hoặc hỏng.");
        }
        if (font.length() > MAX_FONT_SIZE) {
            throw new IllegalStateException("Font lớn hơn 128 MB.");
        }

        byte[] header = new byte[4];
        try (InputStream input = new FileInputStream(font)) {
            if (input.read(header) != header.length) {
                throw new IllegalStateException("Không đọc được đầu tệp font.");
            }
        }

        boolean sfnt = header[0] == 0
                && header[1] == 1
                && header[2] == 0
                && header[3] == 0;
        String tag = new String(header, "US-ASCII");
        if (!sfnt
                && !"OTTO".equals(tag)
                && !"true".equals(tag)
                && !"typ1".equals(tag)
                && !"ttcf".equals(tag)) {
            throw new IllegalStateException(
                    "Tệp đã chọn không phải TTF, OTF hoặc TTC hợp lệ."
            );
        }

        try {
            if (Typeface.createFromFile(font) == null) {
                throw new IllegalStateException("Android không đọc được font.");
            }
        } catch (RuntimeException error) {
            throw new IllegalStateException(
                    "Android không đọc được font đã chọn.",
                    error
            );
        }
    }

    private void createXiaomiMtz(
            File font,
            File destination,
            String title
    ) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(destination)
        )) {
            writeTextEntry(
                    zip,
                    "description.xml",
                    descriptionXml(title)
            );

            zip.putNextEntry(
                    new ZipEntry("fonts/Roboto-Regular.ttf")
            );
            try (InputStream input = new FileInputStream(font)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = input.read(buffer)) > 0) {
                    zip.write(buffer, 0, count);
                }
            }
            zip.closeEntry();
        }
    }

    private void writeTextEntry(
            ZipOutputStream zip,
            String name,
            String value
    ) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes("UTF-8"));
        zip.closeEntry();
    }

    private String descriptionXml(String title) {
        String safe = cdata(
                title == null || title.trim().isEmpty()
                        ? "Font tùy chỉnh"
                        : title.trim()
        );
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" "
                + "standalone=\"no\"?>\n"
                + "<theme>\n"
                + "    <version><![CDATA[1.0.0]]></version>\n"
                + "    <uiVersion>8</uiVersion>\n"
                + "    <author><![CDATA[MTZ Tool]]></author>\n"
                + "    <designer><![CDATA[MTZ Tool]]></designer>\n"
                + "    <title><![CDATA[" + safe + "]]></title>\n"
                + "    <description><![CDATA[Font được đóng gói "
                + "bằng MTZ Tool]]></description>\n"
                + "    <authors><author locale=\"vi_VN\"><![CDATA["
                + "MTZ Tool]]></author></authors>\n"
                + "    <designers><designer locale=\"vi_VN\"><![CDATA["
                + "MTZ Tool]]></designer></designers>\n"
                + "    <titles><title locale=\"vi_VN\"><![CDATA["
                + safe + "]]></title></titles>\n"
                + "</theme>";
    }

    private String cdata(String value) {
        return value.replace("]]>", "]]]]><![CDATA[>");
    }

    private boolean isZip(File file) throws Exception {
        byte[] header = new byte[4];
        try (InputStream input = new FileInputStream(file)) {
            if (input.read(header) < 4) {
                return false;
            }
        }
        return header[0] == 'P'
                && header[1] == 'K'
                && ((header[2] == 3 && header[3] == 4)
                || (header[2] == 5 && header[3] == 6)
                || (header[2] == 7 && header[3] == 8));
    }

    private void copyFile(File source, File destination) throws Exception {
        try (InputStream input = new FileInputStream(source)) {
            IoUtils.copy(input, destination);
        }
    }

    private String stripExtension(String value) {
        if (value == null) {
            return "Font tùy chỉnh";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String extension : new String[]{
                ".ttf", ".otf", ".ttc", ".mtz", ".zip"
        }) {
            if (lower.endsWith(extension)) {
                return value.substring(
                        0,
                        value.length() - extension.length()
                );
            }
        }
        return value;
    }

    private String safeTitle(String value) {
        String safe = IoUtils.safeFileName(value);
        return safe.isEmpty() ? "font" : safe;
    }

    private void progress(String message) {
        if (callback != null) {
            callback.onProgress(message);
        }
    }
}
