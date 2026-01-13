package util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Multipart/form-data parser for fully-buffered request bodies (byte[]).
 * - boundary is already extracted from Content-Type header and passed as String.
 * - Safe for binary files because it operates on bytes, not lines/readers.
 */
public final class MultipartParser {

    private static final Charset HEADER_CHARSET = StandardCharsets.ISO_8859_1; // HTTP header bytes safe decode
    private static final Charset TEXT_CHARSET = StandardCharsets.UTF_8;        // form field decode (usually UTF-8)

    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';

    private MultipartParser() {}

    // ---------- Public API ----------

    public static MultipartForm parse(byte[] body, String boundary) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(boundary, "boundary");
        if (boundary.isBlank()) throw new IllegalArgumentException("boundary is blank");

        byte[] boundaryLine = ("--" + boundary).getBytes(HEADER_CHARSET);
        byte[] finalBoundaryLine = ("--" + boundary + "--").getBytes(HEADER_CHARSET);

        List<Part> parts = splitParts(body, boundaryLine, finalBoundaryLine);

        Map<String, String> fields = new LinkedHashMap<>();
        List<FilePart> files = new ArrayList<>();

        for (Part part : parts) {
            if (part.data.length == 0) continue;

            ParsedPart parsed = parseSinglePart(part.data);
            if (parsed == null) continue;

            String name = parsed.name;
            if (name == null || name.isEmpty()) continue;

            if (parsed.filename != null) {
                // file part
                files.add(new FilePart(
                        name,
                        parsed.filename,
                        parsed.contentType != null ? parsed.contentType : "application/octet-stream",
                        parsed.content
                ));
            } else {
                // text part
                String value = new String(parsed.content, TEXT_CHARSET);
                fields.put(name, value);
            }
        }

        return new MultipartForm(fields, files);
    }

    // ---------- Result Types ----------

    public static final class MultipartForm {
        public final Map<String, String> fields;
        public final List<FilePart> files;

        public MultipartForm(Map<String, String> fields, List<FilePart> files) {
            this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
            this.files = Collections.unmodifiableList(new ArrayList<>(files));
        }
    }

    public static final class FilePart {
        public final String fieldName;     // e.g. "image"
        public final String filename;      // original filename (do not trust for paths)
        public final String contentType;   // claimed content-type
        public final byte[] bytes;         // file bytes

        public FilePart(String fieldName, String filename, String contentType, byte[] bytes) {
            this.fieldName = fieldName;
            this.filename = filename;
            this.contentType = contentType;
            this.bytes = bytes;
        }
    }

    // ---------- Internal Types ----------

    private static final class Part {
        final byte[] data; // contains headers + \r\n\r\n + content (no boundary lines)
        Part(byte[] data) { this.data = data; }
    }

    private static final class ParsedPart {
        String name;
        String filename;     // if present -> file part
        String contentType;
        byte[] content;
    }

    // ---------- Core Parsing ----------

    /**
     * Splits body into raw part payloads (excluding boundary lines).
     * Handles preamble/epilogue. Assumes fully-buffered body.
     */
    private static List<Part> splitParts(byte[] body, byte[] boundaryLine, byte[] finalBoundaryLine) {
        List<Part> result = new ArrayList<>();

        int i = 0;

        // The body should start with boundary line, but some implementations allow a preamble.
        // Find first boundary line occurrence.
        int first = indexOf(body, boundaryLine, 0);
        if (first < 0) return result;
        i = first;

        while (i >= 0 && i < body.length) {
            // We are at "--boundary" or "--boundary--"
            boolean isFinal = startsWith(body, i, finalBoundaryLine);

            // Move i to end of boundary line + CRLF
            int lineEnd = indexOfCrlf(body, i);
            if (lineEnd < 0) break;
            i = lineEnd + 2; // skip CRLF

            if (isFinal) {
                // nothing after final boundary (epilogue ignored)
                break;
            }

            // Next boundary begins with CRLF--boundary (often), but could be at very start of remaining bytes.
            // We search for "\r\n--boundary" first to avoid matching in file bytes.
            byte[] nextDelimiter = concat(new byte[]{CR, LF}, boundaryLine);
            int next = indexOf(body, nextDelimiter, i);

            if (next < 0) {
                // As a fallback, search plain boundary line (less strict)
                next = indexOf(body, boundaryLine, i);
                if (next < 0) {
                    // No more boundaries; treat remaining as part (rare)
                    byte[] partBytes = Arrays.copyOfRange(body, i, body.length);
                    partBytes = trimTrailingCrlf(partBytes);
                    result.add(new Part(partBytes));
                    break;
                }
            }

            // Part bytes are from i to next (exclusive)
            byte[] partBytes = Arrays.copyOfRange(body, i, next);
            // partBytes typically ends with CRLF before delimiter; trim it
            partBytes = trimTrailingCrlf(partBytes);

            if (partBytes.length > 0) {
                result.add(new Part(partBytes));
            }

            // Move i to the boundaryLine start (if we found CRLF--boundary, skip the CRLF)
            if (startsWith(body, next, nextDelimiter)) {
                i = next + 2; // skip CRLF, now at "--boundary"
            } else {
                i = next; // already at "--boundary"
            }
        }

        return result;
    }

    /**
     * Parse one part: headers + CRLFCRLF + content.
     * Returns ParsedPart or null if invalid.
     */
    private static ParsedPart parseSinglePart(byte[] partBytes) {
        // Find header/content separator
        byte[] sep = new byte[]{CR, LF, CR, LF};
        int sepIdx = indexOf(partBytes, sep, 0);
        if (sepIdx < 0) return null;

        byte[] headerBytes = Arrays.copyOfRange(partBytes, 0, sepIdx);
        byte[] contentBytes = Arrays.copyOfRange(partBytes, sepIdx + 4, partBytes.length);

        // Parse headers (CRLF separated)
        String headersStr = new String(headerBytes, HEADER_CHARSET);
        Map<String, String> headers = parseHeaderLines(headersStr);

        String cd = headers.get("content-disposition");
        if (cd == null) return null;

        ParsedPart parsed = new ParsedPart();

        // Content-Disposition: form-data; name="title"; filename="a.png"
        Map<String, String> cdParams = parseSemicolonParams(cd);
        parsed.name = stripQuotes(cdParams.get("name"));
        parsed.filename = stripQuotes(cdParams.get("filename"));

        String ct = headers.get("content-type");
        if (ct != null) parsed.contentType = ct.trim();

        parsed.content = contentBytes;
        return parsed;
    }

    // ---------- Header Helpers ----------

    private static Map<String, String> parseHeaderLines(String headersStr) {
        Map<String, String> map = new HashMap<>();
        // Split by CRLF
        String[] lines = headersStr.split("\r\n");
        for (String line : lines) {
            int idx = line.indexOf(':');
            if (idx <= 0) continue;
            String name = line.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(idx + 1).trim();
            map.put(name, value);
        }
        return map;
    }

    /**
     * Parses "form-data; name=\"title\"; filename=\"a.png\"" into:
     * { "" : "form-data", "name" : "\"title\"", "filename" : "\"a.png\"" }
     */
    private static Map<String, String> parseSemicolonParams(String value) {
        Map<String, String> out = new HashMap<>();
        String[] tokens = value.split(";");
        if (tokens.length > 0) out.put("", tokens[0].trim().toLowerCase(Locale.ROOT));

        for (int i = 1; i < tokens.length; i++) {
            String t = tokens[i].trim();
            if (t.isEmpty()) continue;
            int eq = t.indexOf('=');
            if (eq < 0) {
                out.put(t.toLowerCase(Locale.ROOT), "");
            } else {
                String k = t.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String v = t.substring(eq + 1).trim();
                out.put(k, v);
            }
        }
        return out;
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    // ---------- Byte Utilities ----------

    private static boolean startsWith(byte[] haystack, int offset, byte[] needle) {
        if (offset < 0) return false;
        if (offset + needle.length > haystack.length) return false;
        for (int i = 0; i < needle.length; i++) {
            if (haystack[offset + i] != needle[i]) return false;
        }
        return true;
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        if (needle.length == 0) return from;
        outer:
        for (int i = Math.max(0, from); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    // Find CRLF line end starting at pos (returns index of '\r', or -1)
    private static int indexOfCrlf(byte[] data, int from) {
        for (int i = Math.max(0, from); i < data.length - 1; i++) {
            if (data[i] == CR && data[i + 1] == LF) return i;
        }
        return -1;
    }

    private static byte[] trimTrailingCrlf(byte[] bytes) {
        int end = bytes.length;
        // Remove single trailing CRLF if present
        if (end >= 2 && bytes[end - 2] == CR && bytes[end - 1] == LF) {
            end -= 2;
        }
        return (end == bytes.length) ? bytes : Arrays.copyOf(bytes, end);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
