package com.example.ocean.controller.workspace;

import com.example.ocean.domain.Workspace;
import com.example.ocean.service.WorkspaceService;
import com.example.ocean.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final WorkspaceService workspaceService;

    // 초대 코드로 워크스페이스 조회
    @GetMapping("/workspace/{inviteCode}")
    public ResponseEntity<Workspace> getWorkspaceByInviteCode(
            @PathVariable String inviteCode) {

        Workspace workspace = workspaceService.findByInviteCode(inviteCode);
        if (workspace == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(workspace);
    }

    // 초대 요청
    @PostMapping
    public ResponseEntity<Map<String, String>> requestInvitation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> request) {

        String inviteCode = request.get("inviteCode");
        Workspace workspace = workspaceService.findByInviteCode(inviteCode);

        Map<String, String> response = new HashMap<>();

        if (workspace == null) {
            response.put("message", "유효하지 않은 초대 코드입니다.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean already = workspaceService.existsInvitation(
                workspace.getWorkspaceCd(),
                userPrincipal.getId()
        );

        if (already) {
            response.put("message", "이미 참가 요청을 보냈습니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        workspaceService.requestInvitation(
                workspace.getWorkspaceCd(),
                userPrincipal.getId(),
                inviteCode
        );

        response.put("message", "참가 요청이 전송되었습니다.");
        response.put("workspaceCode", workspace.getWorkspaceCd());

        return ResponseEntity.ok(response);
    }

    // 대기 중인 초대 목록 조회
    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingInvitations(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<Map<String, Object>> invitations =
                workspaceService.getAllPendingInvitations();

        return ResponseEntity.ok(invitations);
    }

    // 초대 승인
    @PatchMapping("/{workspaceCd}/approve/{userId}")
    public ResponseEntity<Void> approveInvitation(
            @PathVariable String workspaceCd,
            @PathVariable String userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 🔐 현재 로그인한 사용자가 owner인지 확인해야 하므로 ID 전달
        workspaceService.approveInvitation(workspaceCd, userId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    // 초대 거절
    @PatchMapping("/{workspaceCd}/reject/{userId}")
    public ResponseEntity<Void> rejectInvitation(
            @PathVariable String workspaceCd,
            @PathVariable String userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        workspaceService.rejectInvitation(workspaceCd, userId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

}
