package root.dtinh.mtzimporter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class RootShell {
    private RootShell() {
    }

    static Result run(String script) throws Exception {
        Process process = new ProcessBuilder(
                "su",
                "-c",
                script
        ).redirectErrorStream(true).start();

        String output = readAll(process.getInputStream());
        int code = process.waitFor();
        return new Result(code, output);
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
