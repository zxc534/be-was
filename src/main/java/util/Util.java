package util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Util {

    private Util() {}

    public static String[] splitOnce(String text, char delimiter) {
        int idx = text.indexOf(delimiter);
        if (idx < 0) return new String[] { text, ""};

        return new String[] { text.substring(0, idx), text.substring(idx + 1) };
    }

    public static Map<String, String> parseParams(String chunk) {
        return parseParams(chunk, '&', '=');
    }

    public static Map<String, String> parseParams(String chunk, char deli1, char deli2) {
        Map<String, String> params = new HashMap<>();
        if (chunk == null || chunk.isEmpty()) {
            return params;
        }

        String[] pairs = chunk.split(String.valueOf(deli1));
        for (String p : pairs) {
            if (p.isEmpty()) continue;

            String[] nv = splitOnce(p, deli2);
            String name = URLDecoder.decode(nv[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(nv[1], StandardCharsets.UTF_8);

            // "=value" 키 값이 없는 경우 무시
            if (name.isEmpty()) continue;
            params.put(name, value);
        }

        return params;
    }
}
