package it.thcs.fse.fsersaservice.support.util;

public final class XmlEscaper {

    private XmlEscaper() {
    }

    public static String escapeText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String escapeAttribute(String value) {
        if (value == null) {
            return "";
        }
        return escapeText(value)
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}