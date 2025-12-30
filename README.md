# be-was-2025
코드스쿼드 백엔드 교육용 WAS 2025 개정판

# 학습 내용
## Java Concurrent 패키지
https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html  
자바에서 스레드 생성, 관리, 작업 실행을 지원하는 패키지.
직접 new Thread()를 만들지 않고, Executor가 작업(Runnable)과 실행(스레드)를 분리하여 운영해준다.

- ExecutorService: 위 기능의 interface
- ThreadPoolExecutor
  - ExecutorService의 대표 구현체
  - 스레드를 미리 만들어 재사용
  - 큐로 작업 버퍼링
  - 거부 정책 설정 가능
  - 서버에 적합하여 이번 프로젝트에 사용
- 다른 구현체
  - FixedThreadPool
  - CachedThreadPool
  - SingleThreadExecutor
  - ScheduledThreadPool

## Java ServerSocket
- 서버가 특정 포트에 바인딩해서 클라이언트의 TCP 연결 요청을 대기
- 요청이 오면 accept()로 연결된 Socket을 반환

## HTTP
클라이언트가 Request로 요청하면 서버가 Response로 응답하는 구조  
헤더는 메시지의 메타데이터를 담아 전송/처리 방식을 결정
  
### Persistent vs Non-persistent
Non-persistent는 요청/응답 1번마다 TCP 연결을 새로 맺고 끊는 방식이고, Persistent(Keep-Alive) 는 한 TCP 연결을 유지한 채 여러 요청/응답을 연속으로 처리해서 연결 오버헤드를 줄이는 방식이다.
