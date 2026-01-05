package webserver;

import http.RequestMessage;
import http.RequestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// 메시지 input stream을 읽어서 메시지 인스턴스 생성
public class InputStreamDecoder {
    private static final Logger logger = LoggerFactory.getLogger(InputStreamDecoder.class);

    private final BufferedReader bufferedReader;

    public InputStreamDecoder(InputStream in) {
        bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public RequestMessage parseSingleMessage() {

        // TODO 불완전한 메시지가 들어온 경우 처리 (헤더가 완성되지 않음)
        try {
            // 헤더 첫 번째 라인 파싱
            String firstLine = bufferedReader.readLine();
            String[] firstLineTokens = firstLine.split(" ");

            if (firstLineTokens.length != 3) {
                throw new IllegalArgumentException("The first line of the request message does not match the expected format.");
            }

            String rawMethod = firstLineTokens[0];
            String requestTarget = firstLineTokens[1];
            String httpVersion = firstLineTokens[2];
            RequestMethod method = RequestMethod.valueOf(rawMethod);

            RequestMessage msg = new RequestMessage(method, requestTarget, httpVersion);

            // 남은 헤더를 파싱, 빈 라인(\r\n\r\n) 만날 때 까지
            while (true) {
                String line = bufferedReader.readLine();
                if (line.isEmpty()) break;

                try {
                    // 첫 ":"를 기준으로 앞은 fieldName, 뒤는 fieldValue
                    int colon = line.indexOf(':');
                    if (colon <= 0) throw new IllegalArgumentException("Invalid header line");

                    String fieldName = line.substring(0, colon);
                    String fieldValue = line.substring(colon + 1).trim();

                    msg.putHeader(fieldName, fieldValue);
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage());
                }
            }

            return msg;
        } catch (Exception e) {
            logger.debug("Error occurred while parsing HTTP message");
            logger.error(e.getMessage());
        }

        // TODO 메시지 파싱 실패 처리
        // 메시지만 출력하고 처리는 Handler로 위임해야함.
        return null;

        // 헤더 모아서 한번에 출력
//        StringBuilder sb = new StringBuilder();
//        sb.append("\n====== HTTP Request Header ======\n");
//        for (String s : header) { sb.append(s).append("\n"); }
//        sb.append("=================================\n");
//        logger.debug(sb.toString());
//
//        String[] tokens = header.get(0).split(" ");
//        String requestTarget = tokens[1];
//        logger.debug("Find {}", requestTarget);
    }
}
