package webserver;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import db.Database;
import http.RequestMessage;
import http.RequestMethod;
import http.ResultCode;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    private Map<String, String> contentTypeMap = new HashMap<>();

    // Action Map
    private Map<String, Function<Map<String, String>, ResultCode>> getMap = new HashMap<>();
    private Map<String, Function<Map<String, String>, ResultCode>> postMap = new HashMap<>();

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;

        // Path를 Action과 연결
        getMap.put("/create", this::handleCreate);

        // Content Type 해시 맵 초기화
        contentTypeMap.put("html", "text/html;charset=utf-8");
        contentTypeMap.put("css",  "text/css;charset=utf-8");
        contentTypeMap.put("js",   "application/javascript;charset=utf-8");
        contentTypeMap.put("json", "application/json;charset=utf-8");
        contentTypeMap.put("xml",  "application/xml;charset=utf-8");
        contentTypeMap.put("txt",  "text/plain;charset=utf-8");
        contentTypeMap.put("csv",  "text/csv;charset=utf-8");
        contentTypeMap.put("md",   "text/markdown;charset=utf-8");

        contentTypeMap.put("svg",  "image/svg+xml");
        contentTypeMap.put("png",  "image/png");
        contentTypeMap.put("jpg",  "image/jpeg");
        contentTypeMap.put("jpeg", "image/jpeg");
        contentTypeMap.put("gif",  "image/gif");
        contentTypeMap.put("webp", "image/webp");
        contentTypeMap.put("ico",  "image/x-icon");

        contentTypeMap.put("woff",  "font/woff");
        contentTypeMap.put("woff2", "font/woff2");
        contentTypeMap.put("ttf",   "font/ttf");
        contentTypeMap.put("otf",   "font/otf");
        contentTypeMap.put("eot",   "application/vnd.ms-fontobject");

        contentTypeMap.put("pdf", "application/pdf");
        contentTypeMap.put("wasm","application/wasm");
        contentTypeMap.put("map", "application/json;charset=utf-8");
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
            byte[] body = null;
            ResultCode resultCode = handleRequest(requestMessage).orElse(() -> {
                // 기본 처리
                URL resource;
                if (requestMessage.requestTarget.contains(".")) {
                    resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestMessage.requestTarget);
                } else {
                    resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestMessage.requestTarget + "/index.html");
                    requestMessage.requestTarget = "index.html";
                }
                // 파일을 찾음 => body에 데이터 복사 => stream에 흘려보냄
                if (resource != null) {
                    InputStream is = resource.openStream();
                    body = is.readAllBytes();
                    is.close();
                    DataOutputStream dos = new DataOutputStream(out);

                    String fileType = requestMessage.requestTarget.split("\\.")[1];
                    String contentType = contentTypeMap.get(fileType);
                    if (contentType != null) {
                        response200Header(dos, contentType, body.length);
                        responseBody(dos, body);
                    } else {
                        logger.debug("Unknown file type");
                    }
                }
            });
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, String contentType, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + contentType + "\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private Optional<ResultCode> handleRequest(RequestMessage req) {
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
                return Optional.of(code);
            }
        } catch (Exception e) {
            // TODO 500이 아닌 적절한 코드 반환
            //파싱 실패 (올바르지 않은 요청 형식 등) 적절한 response 반환
            logger.error(e.getMessage());
            return Optional.of(ResultCode.INTERNAL_SERVER_ERROR);
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
