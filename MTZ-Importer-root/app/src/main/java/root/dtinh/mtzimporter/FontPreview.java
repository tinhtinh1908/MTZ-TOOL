package root.dtinh.mtzimporter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;

final class FontPreview {
    private FontPreview() {
    }

    static void create(
            File fontFile,
            File destination,
            String themeTitle
    ) {
        try {
            File parent = destination.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            Bitmap bitmap = Bitmap.createBitmap(
                    1080,
                    480,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.rgb(17, 24, 39));
            titlePaint.setTextSize(62f);
            titlePaint.setTypeface(Typeface.DEFAULT_BOLD);

            Paint samplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            samplePaint.setColor(Color.rgb(31, 41, 55));
            samplePaint.setTextSize(76f);
            try {
                samplePaint.setTypeface(
                        Typeface.createFromFile(fontFile)
                );
            } catch (Exception ignored) {
                samplePaint.setTypeface(Typeface.DEFAULT);
            }

            canvas.drawText(
                    themeTitle == null ? "Phông chữ" : themeTitle,
                    58f,
                    105f,
                    titlePaint
            );
            canvas.drawText(
                    "Tiếng Việt: Ă Â Đ Ê Ô Ơ Ư",
                    58f,
                    225f,
                    samplePaint
            );
            canvas.drawText(
                    "0123456789  Aa Bb Cc",
                    58f,
                    350f,
                    samplePaint
            );

            try (FileOutputStream output =
                         new FileOutputStream(destination)) {
                bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        92,
                        output
                );
            }
            bitmap.recycle();
        } catch (Exception ignored) {
            // Font preview is optional. Import continues without it.
        }
    }
}
