package root.dtinh.mtzimporter;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Locale;

final class IoUtils {
    private IoUtils() {
    }

    static void copyUri(
            ContentResolver resolver,
            Uri uri,
            File destination
    ) throws Exception {
        InputStream raw = resolver.openInputStream(uri);
        if (raw == null) {
            throw new IllegalStateException("Không mở được file đã chọn.");
        }

        try (InputStream input = raw;
             OutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[65536];
            long total = 0;
            int count;

            while ((count = input.read(buffer)) > 0) {
                total += count;
                if (total > 1024L * 1024L * 1024L) {
                    throw new IllegalStateException("File lớn hơn 1 GB.");
                }
                output.write(buffer, 0, count);
            }
        }
    }

    static void copy(InputStream input, File destination) throws Exception {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Không tạo được thư mục: " + parent
            );
        }

        try (OutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
            }
        }
    }

    static void writeUtf8(File file, String content) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Không tạo được thư mục: " + parent
            );
        }

        try (OutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes("UTF-8"));
        }
    }

    static String sha1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format(Locale.ROOT, "%02x", value));
        }
        return result.toString();
    }

    static String displayName(
            ContentResolver resolver,
            Uri uri
    ) {
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = resolver.query(
                        uri,
                        new String[]{OpenableColumns.DISPLAY_NAME},
                        null,
                        null,
                        null
                );
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                    );
                    if (index >= 0) {
                        String name = cursor.getString(index);
                        if (name != null && !name.trim().isEmpty()) {
                            return name;
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        String last = uri.getLastPathSegment();
        if (last == null || last.trim().isEmpty()) {
            return "Chủ đề";
        }
        int slash = last.lastIndexOf('/');
        return slash >= 0 ? last.substring(slash + 1) : last;
    }

    static String stripMtzExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mtz")) {
            return name.substring(0, name.length() - 4);
        }
        if (lower.endsWith(".zip")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    static String safeFileName(String value) {
        String result = value.replaceAll(
                "[^a-zA-Z0-9._ -]",
                "_"
        ).trim();
        return result.isEmpty() ? "theme" : result;
    }

    static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
