package webserver;

import http.Request;
import http.RequestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RequestReader {
    private static final Logger logger = LoggerFactory.getLogger(RequestReader.class);

    private final BufferedInputStream in;

    public RequestReader(InputStream in) {
        this.in = new BufferedInputStream(in);
    }

    public Request parseSingleMessage() throws Exception {

        // TODO 불완전한 메시지가 들어온 경우 처리 (헤더가 완성되지 않음)
        // 헤더 첫 번째 라인 파싱
        String requestLine = readLine();
        String[] requestLineTokens = requestLine.split(" ");

        if (requestLineTokens.length != 3) {
            throw new IllegalArgumentException("Invalid request line: " + requestLine);
        }

        RequestMethod method = RequestMethod.valueOf(requestLineTokens[0]);
        String requestTarget = requestLineTokens[1];
        String httpVersion = requestLineTokens[2];

        Request msg = new Request(method, requestTarget, httpVersion);

        // 남은 헤더를 파싱, 빈 라인(\r\n\r\n) 만날 때 까지
        while (true) {
            String line = readLine();
            if (line == null) break;
            if (line.isEmpty()) break;

            // 첫 ":"를 기준으로 앞은 fieldName, 뒤는 fieldValue
            int colon = line.indexOf(':');
            if (colon <= 0) {
                logger.error("Invalid header line: {}", line);
                continue;
            }

            String fieldName = line.substring(0, colon).toLowerCase();
            String fieldValue = line.substring(colon + 1).trim();
            msg.putHeader(fieldName, fieldValue);
        }

        int contentLength = 0;
        String strLen = msg.getHeader("content-length");
        if (strLen != null && !strLen.isBlank()) {
            contentLength = Integer.parseInt(strLen.trim());
        }

        if (contentLength > 0) {
            msg.body = new byte[contentLength];
            int offset = 0;

            while (offset < contentLength) {
                int r = in.read(msg.body, offset, contentLength - offset);
                if (r == -1) throw new EOFException("Stream ended before reading Content-Length bytes");
                offset += r;
            }
        }

        return msg;
    }

    private String readLine() throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);

        while (true) {
            int b = in.read();
            if (b == -1) {
                if (baos.size() == 0) return null;
                break;
            }
            if (b == '\n') break;
            baos.write(b);
        }

        byte[] lineBytes = baos.toByteArray();
        int len = lineBytes.length;
        if (len > 0 && lineBytes[len - 1] == '\r') len--;

        return new String(lineBytes, 0, len, StandardCharsets.UTF_8);
    }
}
