package webserver;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import db.Database;
import http.RequestMessage;
import http.RequestMethod;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    private HashMap<String, String> contentTypeMap = new HashMap<>();

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;

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

            InputStreamDecoder inputStreamDecoder = new InputStreamDecoder(in);
            RequestMessage requestMessage = inputStreamDecoder.parseSingleMessage();
            logger.debug(requestMessage.toString());

            // TODO 파일 복사하지 않고 바로 흘려보내기
            // request target 분기
            // 1) 요청 (/create?userId=zxc534)
            // 2) 정적 파일 (/global.css) 
            // 3) 디렉토리 (/registration => registration/index.html)

            // TODO 스프링처럼 매핑하는 로직을 만들어야할 듯
            if (requestTarget.split("\\?")[0].equals("/create")) {
                try {
                    String[] parameters = requestTarget.split("\\?")[1].split("&");
                    Map<String, String> paramMap = new HashMap<>(4);

                    for (String param : parameters) {
                        String[] token = param.split("=");
                        paramMap.put(token[0], token[1]);
                    }

                    // TODO NULL 체크
                    // TODO handle 결과를 받아서 response 전송
                    handleCreate(
                            paramMap.get("userId"),
                            paramMap.get("name"),
                            paramMap.get("email"),
                            paramMap.get("password")
                            );
                    printAllUsers();
                } catch (Exception e) {
                    //파싱 실패 (올바르지 않은 요청 형식 등) 적절한 response 반환
                }
            } else {
                URL resource;
                if (requestTarget.contains(".")) {
                    resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestTarget);
                } else {
                    resource = Thread.currentThread().getContextClassLoader().getResource("./static" + requestTarget + "/index.html");
                    requestTarget = "index.html";
                }

                // 파일을 찾음 => body에 데이터 복사 => stream에 흘려보냄
                if (resource != null) {
                    InputStream is = resource.openStream();
                    byte[] body = is.readAllBytes();
                    is.close();
                    DataOutputStream dos = new DataOutputStream(out);

                    String fileType = requestTarget.split("\\.")[1];
                    String contentType = contentTypeMap.get(fileType);
                    if (contentType != null) {
                        response200Header(dos, contentType, body.length);
                        responseBody(dos, body);
                    } else {
                        logger.debug("Unknown file type");
                    }
                }
            }


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

    private void handleCreate(String userId, String name, String email, String password) {
        User user = new User(userId, password, name, email);
        Database.addUser(user);
    }

    private void printAllUsers() {
        logger.debug("==== USERS ====");
        for (User user : Database.findAll()) {
            logger.debug("{} {} {} {}", user.getUserId(), user.getPassword(), user.getName(), user.getEmail());
        }
    }
}
