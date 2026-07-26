package nonroot.dtinh.mtzimporter;

import android.content.pm.PackageManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import rikka.shizuku.Shizuku;

final class RootShell {
    private RootShell() {
    }

    static boolean isBinderAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isPermissionGranted() {
        try {
            return isBinderAvailable()
                    && Shizuku.checkSelfPermission()
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void requestPermission(int requestCode) {
        if (!isBinderAvailable()) {
            throw new IllegalStateException(
                    "Shizuku chưa chạy. Hãy khởi động Shizuku trước."
            );
        }
        Shizuku.requestPermission(requestCode);
    }

    static Result run(String script) throws Exception {
        if (!isPermissionGranted()) {
            throw new IllegalStateException(
                    "MTZ Tool chưa được cấp quyền Shizuku."
            );
        }

        String mergedScript = "(\n" + script + "\n) 2>&1";
        Process process = Shizuku.newProcess(
                new String[]{
                        "/system/bin/sh",
                        "-c",
                        mergedScript
                },
                null,
                null
        );

        String output = readAll(process.getInputStream());
        int code = process.waitFor();
        return new Result(code, output);
    }

    static boolean isPrivilegedIdentity(String output) {
        return output != null
                && (output.contains("uid=2000")
                || output.contains("uid=0"));
    }

    static String quote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static String readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) > 0) {
            output.write(buffer, 0, count);
        }
        return new String(
                output.toByteArray(),
                StandardCharsets.UTF_8
        );
    }

    static final class Result {
        final int code;
        final String output;

        Result(int code, String output) {
            this.code = code;
            this.output = output;
        }
    }
}
