package com.example.ocean.controller.mention;

import com.example.ocean.domain.MentionNotification;
import com.example.ocean.service.MentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/userNoti")
@RequiredArgsConstructor
public class UserNotiAPIController {
    private final MentionService mentionService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<MentionNotification>> getUserNoti(@PathVariable String userId){
        List<MentionNotification> result = mentionService.selectUserNoti(userId);
        if (result == null || result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{userId}")
    public void readAllNoti(@PathVariable String userId){
        mentionService.updateAllUserNoti(userId);
    }

    @PutMapping("/{notiCd}")
    public void readNoti(
            @PathVariable String notiCd
    ){
        mentionService.updateUserNoti(notiCd);
    }
}
