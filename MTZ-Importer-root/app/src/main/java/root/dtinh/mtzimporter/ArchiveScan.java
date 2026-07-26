package root.dtinh.mtzimporter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

final class ArchiveScan {
    final ArrayList<String> previewNames = new ArrayList<>();
    final LinkedHashMap<String, String> previewEntries =
            new LinkedHashMap<>();
    final ArrayList<ArchiveResource> resources = new ArrayList<>();
}
