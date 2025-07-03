package com.example.ocean.controller.workspace;

import com.example.ocean.domain.Workspace;
import com.example.ocean.domain.WorkspaceDept;
import com.example.ocean.domain.WorkspaceMember;
import com.example.ocean.service.WorkspaceService;
import com.example.ocean.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

// REST API 전용 컨트롤러 (JSON 응답)
@Slf4j
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    // 워크스페이스 목록 조회
    @GetMapping
    public ResponseEntity<List<Workspace>> getMyWorkspaces(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<Workspace> workspaces = workspaceService
                .getWorkspacesByUserId(userPrincipal.getId());

        return ResponseEntity.ok(workspaces);
    }

    // 1. @InitBinder 수정: 'endDate' 필드를 자동 변환 대상에서 제외.
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 스프링이 'endDate' 필드를 자동으로 변환하려는 시도를 막기.
        binder.setDisallowedFields("endDate");
    }

    // 워크스페이스 생성 메서드
    @PostMapping
    public ResponseEntity<?> createWorkspace(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @ModelAttribute Workspace workspace,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "departments", required = false) List<String> departments,
            @RequestParam(value = "workspaceImageFile", required = false) MultipartFile workspaceImg) {

        log.info("워크스페이스 생성 요청 - 사용자: {}", userPrincipal.getId());
        log.info("워크스페이스명: {}, 이미지 파일: {}",
                workspace.getWorkspaceNm(),
                workspaceImg != null ? workspaceImg.getOriginalFilename() : "없음");

        try {
            // 2. 수동으로 endDate 파싱
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    java.util.Date parsedDate = sdf.parse(endDateStr);
                    workspace.setEndDate(new Timestamp(parsedDate.getTime()));
                } catch (ParseException e) {
                    log.error("날짜 파싱 에러: {}", endDateStr, e);
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "유효하지 않은 날짜 형식입니다."));
                }
            }

            // ⭐ 3. 올바른 메서드명과 파라미터 순서로 수정
            Workspace createdWorkspace = workspaceService.createWorkspace(
                    userPrincipal,    // 첫 번째 파라미터
                    workspace,        // 두 번째 파라미터
                    departments,      // 세 번째 파라미터
                    workspaceImg      // 네 번째 파라미터
            );

            log.info("워크스페이스 생성 완료 - 워크스페이스 코드: {}", createdWorkspace.getWorkspaceCd());

            return ResponseEntity.ok(createdWorkspace);

        } catch (Exception e) {
            log.error("워크스페이스 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "워크스페이스 생성 중 오류가 발생했습니다."));
        }
    }

    // 워크스페이스 멤버 조회
    @GetMapping("/{workspaceCd}/members")
    public ResponseEntity<Map<String, Object>> getWorkspaceMembers(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceCd);

        Map<String, Object> response = new HashMap<>();
        response.put("workspaceCd", workspaceCd);
        response.put("members", members);

        return ResponseEntity.ok(response);
    }

    // 워크스페이스 프로필 조회
    @GetMapping("/{workspaceCd}/profile")
    public ResponseEntity<WorkspaceMember> getProfile(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        WorkspaceMember profile = workspaceService
                .findMemberByWorkspaceAndUser(workspaceCd, userPrincipal.getId());

        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(profile);
    }

    // 워크스페이스 사용자 프로필 수정
    @PutMapping("/{workspaceCd}/profile")
    public ResponseEntity<WorkspaceMember> updateProfile(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody WorkspaceMember profileData) {

        try {
            // 멤버 존재 여부 확인
            WorkspaceMember existingMember = workspaceService
                    .findMemberByWorkspaceAndUser(workspaceCd, userPrincipal.getId());

            if (existingMember == null) {
                // 새 멤버 추가
                workspaceService.insertUserProfileToWorkspace(
                        workspaceCd,
                        userPrincipal.getId(),
                        profileData.getUserNickname(),
                        profileData.getStatusMsg(),
                        profileData.getEmail(),
                        profileData.getPhoneNum(),
                        "MEMBER",
                        profileData.getUserImg()
                );
            } else {
                // 기존 멤버 업데이트
                workspaceService.updateWorkspaceProfile(
                        workspaceCd,
                        userPrincipal.getId(),
                        profileData.getUserNickname(),
                        profileData.getStatusMsg(),
                        profileData.getEmail(),
                        profileData.getPhoneNum(),
                        profileData.getUserRole(),
                        profileData.getUserImg()
                );
            }

            // 부서 및 직급 정보 업데이트
            workspaceService.updateDeptAndPosition(
                    workspaceCd,
                    userPrincipal.getId(),
                    profileData.getDeptCd(),
                    profileData.getPosition()
            );

            WorkspaceMember updatedProfile = workspaceService
                    .findMemberByWorkspaceAndUser(workspaceCd, userPrincipal.getId());

            return ResponseEntity.ok(updatedProfile);

        } catch (Exception e) {
            log.error("프로필 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 부서 목록 조회
    @GetMapping("/{workspaceCd}/departments")
    public ResponseEntity<List<WorkspaceDept>> getDepartments(
            @PathVariable String workspaceCd) {

        List<WorkspaceDept> departments = workspaceService.getDepartments(workspaceCd);
        return ResponseEntity.ok(departments);
    }

    // 즐겨찾기 토글
    @PatchMapping("/{workspaceCd}/favorite")
    public ResponseEntity<Void> toggleFavorite(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Boolean> request) {

        boolean isFavorite = request.get("favorite");
        workspaceService.updateFavoriteStatus(
                userPrincipal.getId(),
                Arrays.asList(workspaceCd),
                isFavorite
        );

        return ResponseEntity.ok().build();
    }

    // 워크스페이스 입장 시간 업데이트
    @PostMapping("/{workspaceCd}/enter")
    public ResponseEntity<Void> enterWorkspace(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        workspaceService.updateEntranceTime(workspaceCd, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    // 워크스페이스 퇴장 시간 업데이트
    @PostMapping("/{workspaceCd}/exit")
    public ResponseEntity<Void> exitWorkspace(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        workspaceService.updateQuitTime(workspaceCd, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    // 사용자 상태 업데이트
    @PatchMapping("/{workspaceCd}/user-state")
    public ResponseEntity<Void> updateUserState(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> request) {

        String userState = request.get("userState");
        workspaceService.updateUserState(workspaceCd, userPrincipal.getId(), userState);

        return ResponseEntity.ok().build();
    }

    // ⚠️ 불필요한 별도 이미지 업로드 엔드포인트 제거됨
    // 이제 프로필 설정은 WorkspacePageController의 handleSetProfile 메서드에서 통합 처리
}