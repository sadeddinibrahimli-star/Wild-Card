package com.wildcard.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class ContentSanitizer {

    private static final Safelist BASIC = Safelist.basic()
            .addTags("p", "br", "pre", "code", "blockquote", "ul", "ol", "li", "strong", "em")
            .removeTags("img")
            .removeAttributes(":all", "style", "onerror", "onclick", "onload")
            .removeProtocols("a", "href", "javascript", "data");

    private ContentSanitizer() {
    }

    public static String text(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? "" : Jsoup.clean(trimmed, Safelist.none());
    }

    public static String richText(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return Jsoup.clean(trimmed, BASIC).trim();
    }

    public static String line(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        String cleaned = text(input).replaceAll("\\s+", " ");
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}