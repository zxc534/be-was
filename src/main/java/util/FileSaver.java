package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public final class FileSaver {
    private FileSaver() {
    }

    public static class Result {
        private final boolean success;
        private final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static Result ok(String message) {
            return new Result(true, message);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }

        public boolean isSuccess() {
            return this.success;
        }

        public String getMessage() {
            return this.message;
        }
    }

    /**
     * 이미지 파일을 ./img/{dir} 경로에 저장합니다. png, jpg 확장자의 이미지 파일만 허용합니다.
     *
     * @param imgBytes 이미지 파일 데이터
     * @param fileName 이미지 파일명
     * @param dir      저장될 디렉토리
     * @return 파일 저장 결과
     */
    public static Result saveImg(byte[] imgBytes, String fileName, String dir) {// 방어 로직
        if (imgBytes == null || imgBytes.length == 0) {
            return Result.fail("imgBytes is null or empty");
        }
        if (fileName == null || fileName.isBlank()) {
            return Result.fail("fileName is null or blank");
        }
        if (dir == null || dir.isBlank()) {
            return Result.fail("dir is null or blank");
        }

        String fileExtension = "";
        if (fileName.endsWith(".png")) {
            fileExtension = ".png";
        } else if (fileName.endsWith(".jpg")) {
            fileExtension = ".jpg";
        } else {
            return Result.fail("file extension is wrong");
        }

        try {
            Path root = Paths.get(System.getProperty("user.dir"));
            Path saveDir = root.resolve("img").resolve(dir);
            Files.createDirectories(saveDir);
            String saveFileName = UUID.randomUUID() + fileExtension;

            Path target = saveDir.resolve(saveFileName);
            Files.write(target, imgBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 파일명을 성공 결과로 반환
            return Result.ok(saveFileName);

        } catch (Exception e) {
            return Result.fail("Failed to save image: " + e.getMessage());
        }
    }
}
