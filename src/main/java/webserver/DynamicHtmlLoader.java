package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicHtmlLoader {
    private static final Logger logger = LoggerFactory.getLogger(DynamicHtmlLoader.class);

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

    public static byte[] load(String filePath, Map<String, String> variable) {
        try {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(filePath);
            if (resource == null) {
                logger.error("Resource not found: {}", filePath);
                return new byte[]{};
            }

            String html;
            try (InputStream is = resource.openStream()) {
                html = new String(is.readAllBytes(), DEFAULT_CHARSET);
            }
            
            String rendered = replaceTokens(html, variable);
            return rendered.getBytes(DEFAULT_CHARSET);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new byte[]{};
        }
    }

    private static String replaceTokens(String input, Map<String, String> variables) {
        if (input == null || input.isEmpty()) return input;
        if (variables == null || variables.isEmpty()) return input;

        Matcher matcher = TOKEN_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tokenName = matcher.group(1).trim();
            String replacement = variables.get(tokenName);

            // 치환값이 없으면 공백
            if (replacement == null) {
                replacement = "";
            }

            // 정규식 치환 안전 처리
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
