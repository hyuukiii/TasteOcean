package com.example.ocean.controller.recording;

import com.example.ocean.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PathValidationController {

    @PostMapping("/validate-path")
    public ResponseEntity<Map<String, Object>> validatePath(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal user) {

        String path = request.get("path");
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 보안 검증 - 위험한 경로 차단
            if (isDangerousPath(path)) {
                response.put("valid", false);
                response.put("reason", "시스템 경로는 사용할 수 없습니다");
                return ResponseEntity.ok(response);
            }

            // 2. 경로 존재 여부 확인
            Path dirPath = Paths.get(path);
            File directory = dirPath.toFile();

            if (!directory.exists()) {
                // 디렉토리가 없으면 생성 시도
                boolean created = directory.mkdirs();
                if (!created) {
                    response.put("valid", false);
                    response.put("reason", "디렉토리를 생성할 수 없습니다");
                    return ResponseEntity.ok(response);
                }
            }

            // 3. 쓰기 권한 확인
            if (!directory.canWrite()) {
                response.put("valid", false);
                response.put("reason", "쓰기 권한이 없습니다");
                return ResponseEntity.ok(response);
            }

            // 4. 사용자별 경로 저장 (선택사항)
            saveUserRecordingPath(user.getId(), path);

            response.put("valid", true);
            response.put("path", path);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("valid", false);
            response.put("reason", "경로 검증 중 오류 발생");
            return ResponseEntity.ok(response);
        }
    }

    private boolean isDangerousPath(String path) {
        // 시스템 경로 차단
        List<String> dangerousPaths = Arrays.asList(
                "/System", "/Windows", "/etc", "/bin", "/usr/bin",
                "/Program Files", "/Applications", "/Library"
        );

        String normalizedPath = path.toLowerCase();
        return dangerousPaths.stream()
                .anyMatch(dangerous -> normalizedPath.startsWith(dangerous.toLowerCase()));
    }

    private void saveUserRecordingPath(String userId, String path) {
        // DB에 사용자별 기본 녹화 경로 저장
        // user_preferences 테이블에 recording_path 필드 추가 필요
    }
}
