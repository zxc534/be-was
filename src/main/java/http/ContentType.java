package http;

import java.util.Locale;
import java.util.Optional;

public enum ContentType {
    // Text / structured text
    HTML("text/html;charset=utf-8"),
    CSS("text/css;charset=utf-8"),
    JS("application/javascript;charset=utf-8"),
    JSON("application/json;charset=utf-8"),
    XML("application/xml;charset=utf-8"),
    TXT("text/plain;charset=utf-8"),
    CSV("text/csv;charset=utf-8"),
    MD("text/markdown;charset=utf-8"),

    // Images
    SVG("image/svg+xml"),
    PNG("image/png"),
    JPG("image/jpeg"),
    JPEG("image/jpeg"),
    GIF("image/gif"),
    WEBP("image/webp"),
    ICO("image/x-icon"),

    // Fonts
    WOFF("font/woff"),
    WOFF2("font/woff2"),
    TTF("font/ttf"),
    OTF("font/otf"),
    EOT("application/vnd.ms-fontobject"),

    // Others
    PDF("application/pdf"),
    WASM("application/wasm"),
    MAP("application/json;charset=utf-8");

    private final String mimeType;

    ContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }

    //확장자(점 없이 "html" 또는 ".html") -> ContentType
    public static ContentType fromExtension(String ext) {
        if (ext == null || ext.isBlank()) return null;

        String normalized = ext.startsWith(".") ? ext.substring(1) : ext;
        normalized = normalized.toUpperCase(Locale.ROOT);

        return ContentType.valueOf(normalized);
    }

    //파일명("index.html") -> mimeType
    public static ContentType fromFileName(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return null;

        String ext = fileName.substring(dot + 1);
        return fromExtension(ext);
    }
}
