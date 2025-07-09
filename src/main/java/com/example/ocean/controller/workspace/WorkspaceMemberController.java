package com.example.ocean.controller.workspace;

import com.example.ocean.domain.WorkspaceMember;
import com.example.ocean.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceService workspaceService;

    @GetMapping("/api/getMembers")
    public ResponseEntity<List<WorkspaceMember>> getWorkspaceMembers(@RequestParam("workspaceCd") String workspaceCd){
        List<WorkspaceMember> memberList = workspaceService.getWorkspaceMembers(workspaceCd);
        return ResponseEntity.ok(memberList);
    }
}
