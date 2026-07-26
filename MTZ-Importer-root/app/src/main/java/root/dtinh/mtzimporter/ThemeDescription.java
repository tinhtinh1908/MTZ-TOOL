package root.dtinh.mtzimporter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

final class ThemeDescription {
    Integer platform;
    String version = "1";
    final LinkedHashMap<String, String> authors = new LinkedHashMap<>();
    final LinkedHashMap<String, String> designers = new LinkedHashMap<>();
    final LinkedHashMap<String, String> titles = new LinkedHashMap<>();
    final LinkedHashMap<String, String> descriptions = new LinkedHashMap<>();
    String screenRatio;
    boolean supportHomeSearchBar;
    String fontWeight;
    String miuiAdapterVersion;
    boolean backupVersion;
    boolean singleResource;
    int wallpaperStyle;
    boolean officialIcons;
    int themeType;

    static ThemeDescription parse(ZipFile zip) {
        ThemeDescription result = new ThemeDescription();
        ZipEntry descriptionEntry = null;

        for (java.util.Enumeration<? extends ZipEntry> entries =
             zip.entries(); entries.hasMoreElements();) {
            ZipEntry entry = entries.nextElement();
            String normalized = ArchiveScanner.normalize(entry.getName());
            if ("description.xml".equals(normalized)
                    || normalized.endsWith("/description.xml")) {
                descriptionEntry = entry;
                break;
            }
        }

        if (descriptionEntry == null) {
            applyDefaults(result);
            return result;
        }

        try (InputStream input = zip.getInputStream(descriptionEntry)) {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            try {
                factory.setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true
                );
            } catch (Exception ignored) {
            }

            Document document = factory.newDocumentBuilder().parse(input);
            Element root = document.getDocumentElement();
            if (root != null) {
                walk(root, result);
            }
        } catch (Exception ignored) {
            // Description metadata is optional. Import still continues.
        }

        applyDefaults(result);
        return result;
    }


    private static void applyDefaults(ThemeDescription result) {
        ensureFallback(result.authors, "Nhập cục bộ");
        ensureFallback(result.designers, "Nhập cục bộ");
        ensureFallback(result.titles, "Chủ đề MTZ");
        ensureFallback(result.descriptions, "");
    }

    private static void walk(Element element, ThemeDescription out) {
        String name = element.getTagName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
        String value = directText(element);

        if (!value.isEmpty()) {
            if ("platform".equals(name) || "uiversion".equals(name)) {
                Integer integer = parseInteger(value);
                if (integer != null) {
                    out.platform = integer;
                }
            } else if ("version".equals(name)) {
                out.version = value;
            } else if ("author".equals(name) || "authors".equals(name)) {
                putLocalized(out.authors, element, value);
            } else if ("designer".equals(name) || "designers".equals(name)) {
                putLocalized(out.designers, element, value);
            } else if ("name".equals(name)
                    || "title".equals(name)
                    || "titles".equals(name)) {
                putLocalized(out.titles, element, value);
            } else if ("description".equals(name)
                    || "descriptions".equals(name)) {
                putLocalized(out.descriptions, element, value);
            } else if ("screenratio".equals(name)) {
                out.screenRatio = value;
            } else if ("supporthomesearchbar".equals(name)) {
                out.supportHomeSearchBar = parseBoolean(value);
            } else if ("fontweight".equals(name)) {
                out.fontWeight = value;
            } else if ("miuiadapterversion".equals(name)) {
                out.miuiAdapterVersion = value;
            } else if ("isbackupversion".equals(name)) {
                out.backupVersion = parseBoolean(value);
            } else if ("issingleresource".equals(name)) {
                out.singleResource = parseBoolean(value);
            } else if ("wallpaperstyle".equals(name)) {
                Integer integer = parseInteger(value);
                out.wallpaperStyle = integer == null ? 0 : integer;
            } else if ("officialicons".equals(name)) {
                out.officialIcons = parseBoolean(value);
            } else if ("themetype".equals(name)) {
                Integer integer = parseInteger(value);
                out.themeType = integer == null ? 0 : integer;
            }
        }

        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element) {
                walk((Element) child, out);
            }
        }
    }

    private static String directText(Element element) {
        StringBuilder value = new StringBuilder();
        NodeList children = element.getChildNodes();

        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.TEXT_NODE
                    || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                value.append(child.getNodeValue());
            }
        }
        return value.toString().trim();
    }

    private static void putLocalized(
            LinkedHashMap<String, String> target,
            Element element,
            String value
    ) {
        String locale = attribute(
                element,
                "locale",
                "lang",
                "language",
                "key",
                "name"
        );

        if (locale == null || locale.trim().isEmpty()) {
            locale = "fallback";
        }

        target.put(locale, value);
        if (!target.containsKey("fallback")) {
            target.put("fallback", value);
        }
    }

    private static String attribute(Element element, String... names) {
        NamedNodeMap attributes = element.getAttributes();
        for (String name : names) {
            Node node = attributes.getNamedItem(name);
            if (node != null) {
                return node.getNodeValue();
            }
        }
        return null;
    }

    private static Integer parseInteger(String value) {
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("-?\\d+").matcher(value);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean parseBoolean(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "on".equals(normalized);
    }

    static void ensureFallback(
            LinkedHashMap<String, String> map,
            String defaultValue
    ) {
        removeEmpty(map);
        if (map.isEmpty()) {
            map.put("fallback", defaultValue);
        } else if (!map.containsKey("fallback")) {
            map.put("fallback", map.values().iterator().next());
        }
    }

    private static void removeEmpty(LinkedHashMap<String, String> map) {
        java.util.Iterator<Map.Entry<String, String>> iterator =
                map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue() == null
                    || entry.getValue().trim().isEmpty()) {
                iterator.remove();
            }
        }
    }

    String titleOr(String fallback) {
        String title = titles.get("fallback");
        if (title == null || title.trim().isEmpty()
                || "Chủ đề MTZ".equals(title)) {
            return fallback;
        }
        return title;
    }

    int platformOrDefault() {
        return platform == null ? 17 : platform;
    }
}
