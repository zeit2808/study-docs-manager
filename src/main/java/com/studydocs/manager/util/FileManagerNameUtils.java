package com.studydocs.manager.util;

import java.util.Locale;
import java.util.function.Predicate;

public final class FileManagerNameUtils {

    private FileManagerNameUtils() {
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String normalize(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String buildCopyName(String sourceName, int copyIndex) {
        String safeSourceName = hasText(sourceName) ? sourceName.trim() : "Untitled";
        int extensionIndex = extensionIndex(safeSourceName);
        String baseName = extensionIndex >= 0 ? safeSourceName.substring(0, extensionIndex) : safeSourceName;
        String extension = extensionIndex >= 0 ? safeSourceName.substring(extensionIndex) : "";
        String suffix = copyIndex <= 1 ? " - Copy" : " - Copy " + copyIndex;
        return baseName + suffix + extension;
    }

    public static String resolveCopyName(String sourceName, Predicate<String> existsPredicate) {
        int copyIndex = 1;
        String candidate = buildCopyName(sourceName, copyIndex);
        while (existsPredicate.test(candidate)) {
            copyIndex++;
            candidate = buildCopyName(sourceName, copyIndex);
        }
        return candidate;
    }

    private static int extensionIndex(String value) {
        int lastDot = value.lastIndexOf('.');
        return lastDot > 0 ? lastDot : -1;
    }
}
