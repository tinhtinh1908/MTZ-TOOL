package nonroot.dtinh.mtzimporter;

import java.util.ArrayList;
import java.util.List;

final class ThemeRecord {
    final String themeId;
    final String title;
    final String author;
    final List<ComponentRecord> components;

    ThemeRecord(
            String themeId,
            String title,
            String author,
            List<ComponentRecord> components
    ) {
        this.themeId = themeId;
        this.title = title;
        this.author = author;
        this.components = new ArrayList<>(components);
    }

    static final class ComponentRecord {
        final String resourceCode;
        final String localId;

        ComponentRecord(String resourceCode, String localId) {
            this.resourceCode = resourceCode;
            this.localId = localId;
        }
    }
}
