package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

public class DynamicHtmlLoader {
    private static final Logger logger = LoggerFactory.getLogger(DynamicHtmlLoader.class);

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

    public static byte[] load(String filePath, Map<String, String> variable) {
        try {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(filePath);
            InputStream is = resource.openStream();
            byte[] body = is.readAllBytes();
            is.close();
            return body;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new byte[]{};
        }
    }
}
