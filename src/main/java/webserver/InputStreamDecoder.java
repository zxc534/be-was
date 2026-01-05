package webserver;

import http.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// 메시지 input stream을 읽어서 메시지 인스턴스 생성
public class InputStreamDecoder {
    private static final Logger logger = LoggerFactory.getLogger(InputStreamDecoder.class);

    private InputStream inputStream;
    private BufferedReader bufferedReader;

    public InputStreamDecoder(InputStream in) {
        bufferedReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            // TODO 불완전한 메시지가 들어온 경우 처리 (헤더가 완성되지 않음)
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            ArrayList<String> header = new ArrayList<>();
            while (true) {
                String line = br.readLine();
                header.add(line);
                if (line.isEmpty()) break;
            }

            // 헤더 모아서 한번에 출력
            StringBuilder sb = new StringBuilder();
            sb.append("\n====== HTTP Request Header ======\n");
            for (String s : header) { sb.append(s).append("\n"); }
            sb.append("=================================\n");
            logger.debug(sb.toString());

            String[] tokens = header.get(0).split(" ");
            String requestTarget = tokens[1];
            logger.debug("Find {}", requestTarget);
    }

    public RequestMessage parseSingleMessage() {
        return null;
    }
}
