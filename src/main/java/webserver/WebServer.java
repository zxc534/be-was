package webserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String args[]) throws Exception {
        int port = 0;
        if (args == null || args.length == 0) {
            port = DEFAULT_PORT;
        } else {
            port = Integer.parseInt(args[0]);
        }

        /*
        // 서버소켓을 생성한다. 웹서버는 기본적으로 8080번 포트를 사용한다.
        try (ServerSocket listenSocket = new ServerSocket(port)) {
            logger.info("Web Application Server started {} port.", port);

            // 클라이언트가 연결될때까지 대기한다.
            Socket connection;
            while ((connection = listenSocket.accept()) != null) {
                Thread thread = new Thread(new RequestHandler(connection));
                thread.start();
            }
        }
        */

        final int POOL_SIZE = 16;
        final long KEEPALIVE_TIME = 30L;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1024);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                POOL_SIZE,
                POOL_SIZE,
                KEEPALIVE_TIME,
                TimeUnit.SECONDS,
                workQueue
        );

        try (ServerSocket listenSocket = new ServerSocket(port)) {
            Socket connection;
            while ((connection = listenSocket.accept()) != null) {
                try {
                    // Executor 등록
                    executor.execute(new RequestHandler(connection));
                } catch (RejectedExecutionException exception) {
                    // Executor 등록 실패 => 소켓 닫기
                    connection.close();
                }
            }
        }
    }
}
