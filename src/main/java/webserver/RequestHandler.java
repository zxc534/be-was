package webserver;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Util;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;
    private final ActionMap actionMap;

    public RequestHandler(Socket connectionSocket, ActionMap actionMap) {
        this.connection = connectionSocket;
        this.actionMap = actionMap;
    }

    public void run() {
        logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            // 파싱
            InputStreamDecoder inputStreamDecoder = new InputStreamDecoder(in);
            RequestMessage requestMessage = inputStreamDecoder.parseSingleMessage();
            logger.debug(requestMessage.toString());

            // 처리 순서: 정의된 Action->정적 파일->404 Not Found
            Response response =
                    handleAction(requestMessage)
                    .or(() -> handleStaticFile(requestMessage))
                    .orElseGet(() -> new Response(ResultCode.NOT_FOUND));

            // TODO 파일 복사하지 않고 바로 흘려보내기
            // 생성된 Response를 내보냄
            DataOutputStream dos = new DataOutputStream(out);
            response.streamOutResponse(dos);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private Optional<Response> handleAction(RequestMessage req) {
        try {
            String[] splitted = Util.splitOnce(req.requestTarget, '?');
            String path = splitted[0];
            String paramChunk = splitted[1];

            Map<String, String> params = Util.parseParams(paramChunk);

            Function<Map<String, String>, ResultCode> action = null;
            if (req.method == RequestMethod.GET) {
                action = actionMap.GET(path);
            } else if (req.method == RequestMethod.POST) {
                action = actionMap.POST(path);
            }

            // Action이 MAP에 없음
            if (action == null) {
                // path에 해당하는 action이 정의되어 있지 않음
                // null 반환 => 처리를 위임
                return Optional.empty();
            } else {
                // action을 실행하고 결과 반환
                ResultCode code = action.apply(params);
                return Optional.of(new Response(code));
            }
        } catch (Exception e) {
            // TODO 500이 아닌 적절한 코드 반환
            //파싱 실패 (올바르지 않은 요청 형식 등) 적절한 response 반환
            logger.error(e.getMessage());
            return Optional.of(new Response(ResultCode.INTERNAL_SERVER_ERROR));
        }
    }

    private Optional<Response> handleStaticFile(RequestMessage requestMessage) {
        // 기본 처리
        URL resource;
        if (requestMessage.requestTarget.contains(".")) {
            // 정적 파일
            resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestMessage.requestTarget);
        } else {
            // 디렉토리 => 경로/index.html
            resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestMessage.requestTarget + "/index.html");
            requestMessage.requestTarget = "index.html";
        }

        // 파일을 찾음
        if (resource != null) {
            try {
                Response rspWithFile = new Response();
                rspWithFile.resultCode = ResultCode.OK;
                rspWithFile.contentType = ContentType.fromFileName(requestMessage.requestTarget);
                if (rspWithFile.contentType == null) {
                    // TODO 적절한 처리 필요
                    // 파일은 찾았는데 확장자명에 대한 content type이 존재하지 않는 경우
                    return Optional.of(new Response(ResultCode.INTERNAL_SERVER_ERROR));
                }
                InputStream is = resource.openStream();
                rspWithFile.body = is.readAllBytes();
                is.close();

                return Optional.of(rspWithFile);
            } catch (IOException e) {
                // 파일 읽기 중 예외 발생 => 404 반환
                logger.debug(e.getMessage());
            }
        }

        // 파일을 찾지 못함
        return Optional.empty();
    }
}
