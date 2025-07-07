package com.example.ocean.service;

import com.example.ocean.dto.MeetingInfoDto;
import com.example.ocean.dto.MeetingRoomDto;
import com.example.ocean.dto.UserMeetingPreferences;
import com.example.ocean.dto.request.MeetingCreateRequest;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final EntityManager entityManager;

    /**
     * 미팅룸 생성
     */
    @Transactional
    public void createMeetingRoom(String roomId, String roomName, String workspaceId, String hostId, String meetingType) {
        try {
            // 1. 룸 중복 체크
            if (isMeetingRoomExists(roomId)) {
                log.info("미팅룸이 이미 존재함: roomId={}", roomId);
                return;
            }

            // 2. 워크스페이스 존재 확인
            if (!isWorkspaceExists(workspaceId)) {
                log.error("워크스페이스가 존재하지 않음: workspaceId={}", workspaceId);
                throw new RuntimeException("워크스페이스를 찾을 수 없습니다: " + workspaceId);
            }

            // 3. 사용자 존재 확인
            if (!isUserExists(hostId)) {
                log.error("사용자가 존재하지 않음: userId={}", hostId);
                throw new RuntimeException("사용자를 찾을 수 없습니다: " + hostId);
            }

            // 4. 미팅룸 생성
            String insertQuery = """
                INSERT INTO MEETING_ROOMS (
                    ROOM_CD, 
                    ROOM_NM, 
                    WORKSPACE_CD, 
                    HOST_ID,
                    STATUS, 
                    RECORDING_ENABLE, 
                    ACTUAL_START_TIME,
                    DESCRIPTION
                ) VALUES (
                    :roomId, 
                    :roomName, 
                    :workspaceId, 
                    :hostId,
                    'IN_PROGRESS', 
                    'Y', 
                    NOW(),
                    :description
                )
            """;

            Query query = entityManager.createNativeQuery(insertQuery);
            query.setParameter("roomId", roomId);
            query.setParameter("roomName", roomName != null ? roomName : "회의실-" + roomId);
            query.setParameter("workspaceId", workspaceId);
            query.setParameter("hostId", hostId);
            query.setParameter("description", meetingType + " 회의");

            query.executeUpdate();

            log.info("미팅룸 생성 완료: roomId={}, workspaceId={}, hostId={}, type={}",
                    roomId, workspaceId, hostId, meetingType);

            // 5. 호스트를 참가자로 추가
            addParticipant(roomId, hostId, "HOST");

        } catch (Exception e) {
            log.error("미팅룸 생성 실패: roomId={}", roomId, e);
            throw new RuntimeException("미팅룸 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 미팅 참가자 추가
     */
    @Transactional
    public void addParticipant(String roomId, String userId, String role) {
        try {
            // 이미 참가자인지 확인
            String checkQuery = """
                SELECT COUNT(*) FROM MEETING_PARTICIPANTS 
                WHERE ROOM_CD = :roomId AND USER_ID = :userId
            """;

            Query query = entityManager.createNativeQuery(checkQuery);
            query.setParameter("roomId", roomId);
            query.setParameter("userId", userId);

            Number count = (Number) query.getSingleResult();

            if (count.intValue() > 0) {
                log.info("이미 참가 중인 사용자: roomId={}, userId={}", roomId, userId);
                return;
            }

            // 참가자 추가
            String insertQuery = """
                INSERT INTO MEETING_PARTICIPANTS (
                    ROOM_CD, 
                    USER_ID, 
                    ROLE,
                    ACTIVE_STATE
                ) VALUES (
                    :roomId, 
                    :userId, 
                    :role,
                    'Y'
                )
            """;

            Query insertQueryObj = entityManager.createNativeQuery(insertQuery);
            insertQueryObj.setParameter("roomId", roomId);
            insertQueryObj.setParameter("userId", userId);
            insertQueryObj.setParameter("role", role != null ? role : "PARTICIPANT");

            insertQueryObj.executeUpdate();

            log.info("참가자 추가 완료: roomId={}, userId={}, role={}", roomId, userId, role);

        } catch (Exception e) {
            log.error("참가자 추가 실패: roomId={}, userId={}", roomId, userId, e);
            throw new RuntimeException("참가자 추가 실패: " + e.getMessage());
        }
    }

    /**
     * 미팅룸 종료
     */
    @Transactional
    public void endMeeting(String roomId) {
        try {
            String updateQuery = """
                UPDATE MEETING_ROOMS 
                SET STATUS = 'ENDED', 
                    ACTUAL_END_TIME = NOW() 
                WHERE ROOM_CD = :roomId
            """;

            Query query = entityManager.createNativeQuery(updateQuery);
            query.setParameter("roomId", roomId);

            int updated = query.executeUpdate();

            if (updated > 0) {
                log.info("미팅룸 종료: roomId={}", roomId);
            } else {
                log.warn("종료할 미팅룸을 찾을 수 없음: roomId={}", roomId);
            }

        } catch (Exception e) {
            log.error("미팅룸 종료 실패: roomId={}", roomId, e);
            throw new RuntimeException("미팅룸 종료 실패: " + e.getMessage());
        }
    }

    /**
     * 미팅룸 존재 여부 확인
     */
    private boolean isMeetingRoomExists(String roomId) {
        String query = "SELECT COUNT(*) FROM MEETING_ROOMS WHERE ROOM_CD = :roomId";
        Query queryObj = entityManager.createNativeQuery(query);
        queryObj.setParameter("roomId", roomId);
        Number count = (Number) queryObj.getSingleResult();
        return count.intValue() > 0;
    }

    /**
     * 워크스페이스 존재 여부 확인
     */
    private boolean isWorkspaceExists(String workspaceId) {
        String query = "SELECT COUNT(*) FROM WORKSPACE WHERE WORKSPACE_CD = :workspaceId";
        Query queryObj = entityManager.createNativeQuery(query);
        queryObj.setParameter("workspaceId", workspaceId);
        Number count = (Number) queryObj.getSingleResult();
        return count.intValue() > 0;
    }

    /**
     * 사용자 존재 여부 확인
     */
    private boolean isUserExists(String userId) {
        String query = "SELECT COUNT(*) FROM USERS WHERE USER_ID = :userId";
        Query queryObj = entityManager.createNativeQuery(query);
        queryObj.setParameter("userId", userId);
        Number count = (Number) queryObj.getSingleResult();
        return count.intValue() > 0;
    }

    /**
     * 미팅룸 정보 조회
     */
    public boolean isMeetingActive(String roomId) {
        String query = """
            SELECT COUNT(*) FROM MEETING_ROOMS 
            WHERE ROOM_CD = :roomId AND STATUS = 'IN_PROGRESS'
        """;
        Query queryObj = entityManager.createNativeQuery(query);
        queryObj.setParameter("roomId", roomId);
        Number count = (Number) queryObj.getSingleResult();
        return count.intValue() > 0;
    }

    /**
     * 회의 옵션 저장
     */
    @Transactional
    public void saveMeetingOptions(String roomId, MeetingCreateRequest request) {
        try {
            // MEETING_OPTIONS 테이블이 있다고 가정
            String insertQuery = """
            INSERT INTO MEETING_OPTIONS (
                ROOM_CD,
                AUTO_RECORD,
                MUTE_ON_JOIN,
                WAITING_ROOM,
                VIDEO_QUALITY,
                CREATED_DATE
            ) VALUES (
                :roomId,
                :autoRecord,
                :muteOnJoin,
                :waitingRoom,
                :videoQuality,
                NOW()
            )
        """;

            Query query = entityManager.createNativeQuery(insertQuery);
            query.setParameter("roomId", roomId);
            query.setParameter("autoRecord", request.isAutoRecord() ? "Y" : "N");
            query.setParameter("muteOnJoin", request.isMuteOnJoin() ? "Y" : "N");
            query.setParameter("waitingRoom", request.isWaitingRoom() ? "Y" : "N");
            query.setParameter("videoQuality", request.getVideoQuality());

            query.executeUpdate();

            log.info("회의 옵션 저장 완료: roomId={}", roomId);

        } catch (Exception e) {
            log.error("회의 옵션 저장 실패: roomId={}", roomId, e);
            // 옵션 저장 실패해도 회의는 진행 가능하므로 로그만 남김
        }
    }

    /**
     * 사용자 회의 설정 저장
     */
    @Transactional
    public void saveUserPreferences(String userId, MeetingCreateRequest request) {
        try {
            // 기존 설정이 있는지 확인
            String checkQuery = "SELECT COUNT(*) FROM USER_MEETING_PREFERENCES WHERE USER_ID = :userId";
            Query check = entityManager.createNativeQuery(checkQuery);
            check.setParameter("userId", userId);
            Number count = (Number) check.getSingleResult();

            if (count.intValue() > 0) {
                // UPDATE
                String updateQuery = """
                UPDATE USER_MEETING_PREFERENCES SET
                    DEFAULT_AUTO_RECORD = :autoRecord,
                    DEFAULT_MUTE_ON_JOIN = :muteOnJoin,
                    DEFAULT_VIDEO_QUALITY = :videoQuality,
                    DEFAULT_DURATION = :duration,
                    UPDATED_DATE = NOW()
                WHERE USER_ID = :userId
            """;

                Query query = entityManager.createNativeQuery(updateQuery);
                query.setParameter("userId", userId);
                query.setParameter("autoRecord", request.isAutoRecord() ? "Y" : "N");
                query.setParameter("muteOnJoin", request.isMuteOnJoin() ? "Y" : "N");
                query.setParameter("videoQuality", request.getVideoQuality());
                query.setParameter("duration", request.getDuration());

                query.executeUpdate();
            } else {
                // INSERT
                String insertQuery = """
                INSERT INTO USER_MEETING_PREFERENCES (
                    USER_ID,
                    DEFAULT_AUTO_RECORD,
                    DEFAULT_MUTE_ON_JOIN,
                    DEFAULT_VIDEO_QUALITY,
                    DEFAULT_DURATION,
                    UPDATED_DATE
                ) VALUES (
                    :userId,
                    :autoRecord,
                    :muteOnJoin,
                    :videoQuality,
                    :duration,
                    NOW()
                )
            """;

                Query query = entityManager.createNativeQuery(insertQuery);
                query.setParameter("userId", userId);
                query.setParameter("autoRecord", request.isAutoRecord() ? "Y" : "N");
                query.setParameter("muteOnJoin", request.isMuteOnJoin() ? "Y" : "N");
                query.setParameter("videoQuality", request.getVideoQuality());
                query.setParameter("duration", request.getDuration());

                query.executeUpdate();
            }

            log.info("사용자 회의 설정 저장 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("사용자 회의 설정 저장 실패: userId={}", userId, e);
            // 설정 저장 실패해도 회의는 진행 가능
        }
    }

    /**
     * 사용자 회의 설정 조회
     */
    public UserMeetingPreferences getUserPreferences(String userId) {
        try {
            String query = """
            SELECT 
                USER_ID,
                DEFAULT_AUTO_RECORD,
                DEFAULT_MUTE_ON_JOIN,
                DEFAULT_VIDEO_QUALITY,
                DEFAULT_DURATION,
                UPDATED_DATE
            FROM USER_MEETING_PREFERENCES
            WHERE USER_ID = :userId
        """;

            Query nativeQuery = entityManager.createNativeQuery(query);
            nativeQuery.setParameter("userId", userId);

            List<Object[]> results = nativeQuery.getResultList();

            if (!results.isEmpty()) {
                Object[] row = results.get(0);
                return UserMeetingPreferences.builder()
                        .userId((String) row[0])
                        .defaultAutoRecord("Y".equals(row[1]))
                        .defaultMuteOnJoin("Y".equals(row[2]))
                        .defaultVideoQuality((String) row[3])
                        .defaultDuration((Integer) row[4])
                        .updatedDate(((Timestamp) row[5]).toLocalDateTime())
                        .build();
            }

            return null;

        } catch (Exception e) {
            log.error("사용자 회의 설정 조회 실패: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 회의 멤버 초대
     */
    @Transactional
    public void inviteMember(String roomId, String memberId, String invitedBy) {
        try {
            // 회의 초대 기록 저장 (MEETING_INVITATIONS 테이블이 있다고 가정)
            String insertQuery = """
            INSERT INTO MEETING_INVITATIONS (
                ROOM_CD,
                INVITED_USER_ID,
                INVITED_BY,
                INVITE_DATE,
                STATUS
            ) VALUES (
                :roomId,
                :memberId,
                :invitedBy,
                NOW(),
                'PENDING'
            )
        """;

            Query query = entityManager.createNativeQuery(insertQuery);
            query.setParameter("roomId", roomId);
            query.setParameter("memberId", memberId);
            query.setParameter("invitedBy", invitedBy);

            query.executeUpdate();

            log.info("멤버 초대 완료: roomId={}, memberId={}", roomId, memberId);

            // TODO: 실시간 알림 전송 로직 추가

        } catch (Exception e) {
            log.error("멤버 초대 실패: roomId={}, memberId={}", roomId, memberId, e);
        }
    }

    /**
     * 캘린더 일정 생성
     */
    @Transactional
    public void createCalendarEvent(String roomId, String title,
                                    LocalDateTime scheduledTime, Integer duration, String userId) {
        try {
            // CALENDAR_EVENTS 테이블이 있다고 가정
            String insertQuery = """
                        INSERT INTO CALENDAR_EVENTS (
                            EVENT_ID,
                            USER_ID,
                            EVENT_TYPE,
                            EVENT_TITLE,
                            EVENT_DATE,
                            START_TIME,
                            END_TIME,
                            ROOM_CD,
                            CREATED_DATE
                        ) VALUES (
                            :eventId,
                            :userId,
                            'MEETING',
                            :title,
                            :eventDate,
                            :startTime,
                            :endTime,
                            :roomId,
                            NOW()
                        )
                    """;

            Query query = entityManager.createNativeQuery(insertQuery);
            query.setParameter("eventId", UUID.randomUUID().toString());
            query.setParameter("userId", userId);
            query.setParameter("title", title);
            query.setParameter("eventDate", scheduledTime.toLocalDate());
            query.setParameter("startTime", scheduledTime);
            query.setParameter("endTime", scheduledTime.plusMinutes(duration != null ? duration : 60));
            query.setParameter("roomId", roomId);

            query.executeUpdate();

            log.info("캘린더 일정 생성 완료: roomId={}, scheduledTime={}", roomId, scheduledTime);

        } catch (Exception e) {
            log.error("캘린더 일정 생성 실패: roomId={}", roomId, e);
            // 캘린더 일정 생성 실패해도 회의는 진행 가능
        }
    }

    /**
     * 사용자의 진행 중인 회의 조회
     */
    public MeetingRoomDto getActiveMeetingForUser(String workspaceCd, String userId) {
        String query = """
        SELECT mr.*, 
               (SELECT COUNT(*) FROM MEETING_PARTICIPANTS mp 
                WHERE mp.ROOM_CD = mr.ROOM_CD 
                AND mp.ACTIVE_STATE = 'Y') as participantCount
        FROM MEETING_ROOMS mr
        JOIN MEETING_PARTICIPANTS mp ON mr.ROOM_CD = mp.ROOM_CD
        WHERE mr.WORKSPACE_CD = :workspaceCd
        AND mp.USER_ID = :userId
        AND mr.STATUS = 'IN_PROGRESS'
        AND mp.ACTIVE_STATE = 'Y'
        ORDER BY mr.ACTUAL_START_TIME DESC
        LIMIT 1
    """;

        Query queryObj = entityManager.createNativeQuery(query);
        queryObj.setParameter("workspaceCd", workspaceCd);
        queryObj.setParameter("userId", userId);

        try {
            Object[] result = (Object[]) queryObj.getSingleResult();

            return MeetingRoomDto.builder()
                    .roomId((String) result[0])
                    .title((String) result[1])
                    .workspaceCd((String) result[3])
                    .hostId((String) result[4])
                    .status((String) result[5])
                    .startTime((Timestamp) result[8])
                    .participantCount(((Number) result[10]).intValue())
                    .build();

        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * 회의 정보 조회
     */
    public MeetingInfoDto getMeetingInfo(String roomId) {
        String query = """
        SELECT ROOM_CD, ROOM_NM, HOST_ID, STATUS, WORKSPACE_CD
        FROM MEETING_ROOMS
        WHERE ROOM_CD = :roomId
    """;

        Query queryObj = entityManager.createNativeQuery(query);
        queryObj.setParameter("roomId", roomId);

        try {
            Object[] result = (Object[]) queryObj.getSingleResult();

            return MeetingInfoDto.builder()
                    .roomId((String) result[0])
                    .title((String) result[1])
                    .hostId((String) result[2])
                    .status((String) result[3])
                    .workspaceCd((String) result[4])
                    .build();

        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * 회의 상태 업데이트
     */
    @Transactional
    public void updateMeetingStatus(String roomId, String status) {
        String updateQuery = """
        UPDATE MEETING_ROOMS 
        SET STATUS = :status,
            ACTUAL_END_TIME = CASE 
                WHEN :status = 'ENDED' THEN NOW() 
                ELSE ACTUAL_END_TIME 
            END
        WHERE ROOM_CD = :roomId
    """;

        Query query = entityManager.createNativeQuery(updateQuery);
        query.setParameter("status", status);
        query.setParameter("roomId", roomId);
        query.executeUpdate();

        // 종료 시 모든 참가자 비활성화
        if ("ENDED".equals(status)) {
            String updateParticipants = """
            UPDATE MEETING_PARTICIPANTS
            SET ACTIVE_STATE = 'N',
                QUIT_DATE = NOW()
            WHERE ROOM_CD = :roomId
            AND ACTIVE_STATE = 'Y'
        """;

            Query participantQuery = entityManager.createNativeQuery(updateParticipants);
            participantQuery.setParameter("roomId", roomId);
            participantQuery.executeUpdate();
        }
    }

}
