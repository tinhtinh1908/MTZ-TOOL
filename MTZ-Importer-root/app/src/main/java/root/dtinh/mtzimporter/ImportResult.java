package root.dtinh.mtzimporter;

final class ImportResult {
    final String themeId;
    final String title;
    final int resourceCount;
    final int previewCount;
    final String fontId;

    ImportResult(
            String themeId,
            String title,
            int resourceCount,
            int previewCount,
            String fontId
    ) {
        this.themeId = themeId;
        this.title = title;
        this.resourceCount = resourceCount;
        this.previewCount = previewCount;
        this.fontId = fontId;
    }
}
