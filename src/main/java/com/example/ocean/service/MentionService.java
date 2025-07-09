package com.example.ocean.service;

import com.example.ocean.domain.MentionNotification;
import com.example.ocean.repository.MentionNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MentionService {
    private final MentionNotificationRepository mentionNotificationRepository;

    public List<MentionNotification> selectUserNoti(String userId) {
        return mentionNotificationRepository.selectUserNoti(userId);
    }

    public void updateAllUserNoti(String userId) {
        mentionNotificationRepository.updateAllNoti(userId);
    }

    public void updateUserNoti(String notiCd) {
        mentionNotificationRepository.updateNoti(notiCd);
    }
}
