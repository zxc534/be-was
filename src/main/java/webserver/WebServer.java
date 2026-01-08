package webserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import db.Database;
import model.User;
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

        // 테스트용 데이터 추가
        Database.addUser(new User("zxc534", "qwe123", "ybsong", "zxc534@naver.com"));

        ActionMap actionMap = new ActionMap();

        try (ServerSocket listenSocket = new ServerSocket(port)) {
            Socket connection;
            while ((connection = listenSocket.accept()) != null) {
                try {
                    // Executor 등록
                    executor.execute(new RequestHandler(connection, actionMap));
                } catch (RejectedExecutionException exception) {
                    // Executor 등록 실패 => 소켓 닫기
                    connection.close();
                }
            }
        }
    }
}
