package webserver;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import db.Database;
import http.*;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    // TODO !!! !!! !!! 1순위 !!! !!! !!! 현재는 요청 들어올 때 마다 액션 초기화 => 액션은 한번만 초기화하고 계속 사용하도록 클래스 분리
    // Action Map
    private final Map<String, Function<Map<String, String>, ResultCode>> getMap = new HashMap<>();
    private final Map<String, Function<Map<String, String>, ResultCode>> postMap = new HashMap<>();

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;

        // Path를 Action과 연결
        getMap.put("/create", this::handleCreate);

    }

    public void run() {
        logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            // 파싱
            InputStreamDecoder inputStreamDecoder = new InputStreamDecoder(in);
            RequestMessage requestMessage = inputStreamDecoder.parseSingleMessage();
            logger.debug(requestMessage.toString());

            // TODO 파일 복사하지 않고 바로 흘려보내기
            // request target 분기
            // 1) 요청 (/create?userId=zxc534)
            // 2) 정적 파일 (/global.css) 
            // 3) 디렉토리 (/registration => registration/index.html)

            // 우선 정의된 Action이 있는지 확인
            // 있다면 Action을 실행하고 결과 반환, 없다면 null 반환
            // null이면 정적파일 탐색
            Response response = handleRequest(requestMessage).orElseGet(() -> {
                // 기본 처리
                URL resource;
                if (requestMessage.requestTarget.contains(".")) {
                    resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestMessage.requestTarget);
                } else {
                    resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestMessage.requestTarget + "/index.html");
                    requestMessage.requestTarget = "index.html";
                }

                // 파일을 찾음
                if (resource != null) {
                    try {
                        Response rspWithFile = new Response();
                        rspWithFile.contentType = ContentType.fromFileName(requestMessage.requestTarget);
                        if (rspWithFile.contentType == null) {
                            // TODO 적절한 처리 필요
                            // 파일은 찾았는데 확장자명에 대한 content type이 존재하지 않는 경우
                            return new Response(ResultCode.INTERNAL_SERVER_ERROR);
                        }
                        InputStream is = resource.openStream();
                        rspWithFile.body = is.readAllBytes();
                        is.close();

                        return rspWithFile;
                    } catch (IOException e) {
                        // 파일 읽기 중 예외 발생 => 404 반환
                        logger.debug(e.getMessage());
                    }
                }

                // 파일을 찾지 못함
                return new Response(ResultCode.NOT_FOUND);
            });

            // 생성된 Response를 내보냄
            DataOutputStream dos = new DataOutputStream(out);
            response.streamOutResponse(dos);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private Optional<Response> handleRequest(RequestMessage req) {
        try {
            int qm = req.requestTarget.indexOf('?');
            String path = req.requestTarget.substring(0, qm);
            String query = req.requestTarget.substring(qm + 1);

            // TODO split 파싱 로직 검토 필요
            // 처음 나타나는 char를 기준으로 2개로 나누는 유틸 메소드
            Map<String, String> params = new HashMap<>();
            if (!query.isEmpty()) {
                String[] pairs = query.split("&");
                for (String param : pairs) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        params.put(pair[0], pair[1]);
                    } else {
                        params.put(pair[0], null);
                    }
                }
            }

            Function<Map<String, String>, ResultCode> action = null;
            if (req.method == RequestMethod.GET) {
                action = getMap.get(path);
            } else if (req.method == RequestMethod.POST) {
                action = postMap.get(path);
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

    private ResultCode handleCreate(Map<String, String> params) {
        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email= params.get("email");

        User user = new User(userId, password, name, email);
        Database.addUser(user);

        return ResultCode.OK;
    }

    private void printAllUsers() {
        logger.debug("==== USERS ====");
        for (User user : Database.findAll()) {
            logger.debug("{} {} {} {}", user.getUserId(), user.getPassword(), user.getName(), user.getEmail());
        }
    }
}
