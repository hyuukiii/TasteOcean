package com.example.ocean.service;

import com.example.ocean.dto.ActiveMeetingDto;
import com.example.ocean.dto.UserMeetingPreferences;
import com.example.ocean.dto.request.MeetingCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
     * 진행 중인 회의 목록 조회 (단순화된 버전)
     */
    public List<ActiveMeetingDto> getActiveMeetings(String workspaceId) {
        try {
            // 먼저 간단한 쿼리로 회의 정보만 가져오기
            String query = """
            SELECT 
                mr.ROOM_CD,
                mr.ROOM_NM,
                mr.HOST_ID,
                mr.ACTUAL_START_TIME
            FROM MEETING_ROOMS mr
            WHERE mr.WORKSPACE_CD = :workspaceId 
            AND mr.STATUS = 'IN_PROGRESS'
        """;

            Query nativeQuery = entityManager.createNativeQuery(query);
            nativeQuery.setParameter("workspaceId", workspaceId);

            List<Object[]> results = nativeQuery.getResultList();
            List<ActiveMeetingDto> meetings = new ArrayList<>();

            log.info("진행 중인 회의 수: {}", results.size());

            for (Object[] row : results) {
                String roomId = (String) row[0];
                String title = (String) row[1];
                String hostId = (String) row[2];

                // 호스트 이름 별도 조회
                String hostName = getHostName(hostId);

                // 시작 시간 처리
                LocalDateTime startTime = null;
                if (row[3] != null) {
                    startTime = ((Timestamp) row[3]).toLocalDateTime();
                } else {
                    startTime = LocalDateTime.now();
                }

                // 참가자 목록 조회
                List<ActiveMeetingDto.ParticipantDto> participants = getParticipantsByRoom(roomId);

                // 참가자 수 계산
                int participantCount = participants.size();

                ActiveMeetingDto meeting = new ActiveMeetingDto(
                        roomId,
                        title,
                        hostId,
                        hostName,
                        startTime,
                        participants,
                        participantCount
                );

                log.info("회의 정보: roomId={}, title={}, hostId={}, hostName={}, participantCount={}",
                        roomId, title, hostId, hostName, participantCount);

                meetings.add(meeting);
            }

            return meetings;

        } catch (Exception e) {
            log.error("진행 중인 회의 목록 조회 실패: workspaceId={}", workspaceId, e);
            e.printStackTrace(); // 상세 에러 출력
            return new ArrayList<>();
        }
    }

    /**
     * 호스트 이름 조회
     */
    private String getHostName(String hostId) {
        try {
            String query = "SELECT USER_NAME FROM USERS WHERE USER_ID = :hostId";
            Query nativeQuery = entityManager.createNativeQuery(query);
            nativeQuery.setParameter("hostId", hostId);

            List<String> results = nativeQuery.getResultList();
            if (!results.isEmpty()) {
                return results.get(0);
            }
            return "Unknown User";
        } catch (Exception e) {
            log.error("호스트 이름 조회 실패: hostId={}", hostId, e);
            return "Unknown User";
        }
    }

    /**
     * 특정 회의실의 참가자 목록 조회
     */
    private List<ActiveMeetingDto.ParticipantDto> getParticipantsByRoom(String roomId) {
        try {
            String query = """
            SELECT 
                mp.USER_ID,
                u.USER_NAME,
                wm.USER_IMG,
                CASE WHEN mr.HOST_ID = mp.USER_ID THEN 'Y' ELSE 'N' END as IS_HOST,
                mp.VIDEO_STATE,
                mp.AUDIO_STATE
            FROM MEETING_PARTICIPANTS mp
            JOIN USERS u ON mp.USER_ID = u.USER_ID
            LEFT JOIN MEETING_ROOMS mr ON mp.ROOM_CD = mr.ROOM_CD
            LEFT JOIN WORKSPACE_MEMBERS wm ON mp.USER_ID = wm.USER_ID 
                AND mr.WORKSPACE_CD = wm.WORKSPACE_CD
            WHERE mp.ROOM_CD = :roomId 
            AND mp.ACTIVE_STATE = 'Y'
        """;

            Query nativeQuery = entityManager.createNativeQuery(query);
            nativeQuery.setParameter("roomId", roomId);

            List<Object[]> results = nativeQuery.getResultList();
            List<ActiveMeetingDto.ParticipantDto> participants = new ArrayList<>();

            for (Object[] row : results) {
                String profileImg = (String) row[2];

                // 프로필 이미지 경로 처리
                if (profileImg != null && !profileImg.isEmpty() && !profileImg.startsWith("http")) {
                    profileImg = "/images/profiles" + (profileImg.startsWith("/") ? profileImg : "/" + profileImg);
                }

                ActiveMeetingDto.ParticipantDto participant = ActiveMeetingDto.ParticipantDto.builder()
                        .userId((String) row[0])
                        .displayName((String) row[1])
                        .profileImg(profileImg)
                        .isHost("Y".equals(row[3]))
                        .isVideoOn("ON".equals(row[4]))
                        .isAudioOn("ON".equals(row[5]))
                        .build();

                participants.add(participant);
            }

            return participants;
        } catch (Exception e) {
            log.error("참가자 목록 조회 실패: roomId={}", roomId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 회의 재참가
     */
    @Transactional
    public void rejoinParticipant(String roomId, String userId) {
        try {
            String updateQuery = """
            UPDATE MEETING_PARTICIPANTS 
            SET ACTIVE_STATE = 'Y',
                QUIT_DATE = NULL
            WHERE ROOM_CD = :roomId 
            AND USER_ID = :userId
        """;

            Query query = entityManager.createNativeQuery(updateQuery);
            query.setParameter("roomId", roomId);
            query.setParameter("userId", userId);

            int updated = query.executeUpdate();

            if (updated == 0) {
                // 참가 기록이 없으면 새로 추가
                addParticipant(roomId, userId, "PARTICIPANT");
            }

            log.info("회의 재참가 완료: roomId={}, userId={}", roomId, userId);

        } catch (Exception e) {
            log.error("회의 재참가 실패: roomId={}, userId={}", roomId, userId, e);
            throw new RuntimeException("회의 재참가 실패: " + e.getMessage());
        }
    }

    /**
     * 호스트 여부 확인
     */
    public boolean isHost(String roomId, String userId) {
        try {
            String query = """
            SELECT COUNT(*) FROM MEETING_ROOMS 
            WHERE ROOM_CD = :roomId 
            AND HOST_ID = :userId
        """;

            Query queryObj = entityManager.createNativeQuery(query);
            queryObj.setParameter("roomId", roomId);
            queryObj.setParameter("userId", userId);

            Number count = (Number) queryObj.getSingleResult();
            return count.intValue() > 0;

        } catch (Exception e) {
            log.error("호스트 확인 실패: roomId={}, userId={}", roomId, userId, e);
            return false;
        }
    }

    /**
     * 회의에서 나가기 (일시적)
     */
    @Transactional
    public void leaveParticipant(String roomId, String userId) {
        try {
            String updateQuery = """
            UPDATE MEETING_PARTICIPANTS 
            SET ACTIVE_STATE = 'N',
                QUIT_DATE = NOW()
            WHERE ROOM_CD = :roomId 
            AND USER_ID = :userId
        """;

            Query query = entityManager.createNativeQuery(updateQuery);
            query.setParameter("roomId", roomId);
            query.setParameter("userId", userId);

            query.executeUpdate();

            log.info("회의 나가기 완료: roomId={}, userId={}", roomId, userId);

        } catch (Exception e) {
            log.error("회의 나가기 실패: roomId={}, userId={}", roomId, userId, e);
            throw new RuntimeException("회의 나가기 실패: " + e.getMessage());
        }
    }

    /**
     * 호스트 ID 조회
     */
    public String getHostId(String roomId) {
        try {
            String query = "SELECT HOST_ID FROM MEETING_ROOMS WHERE ROOM_CD = :roomId";

            Query queryObj = entityManager.createNativeQuery(query);
            queryObj.setParameter("roomId", roomId);

            return (String) queryObj.getSingleResult();

        } catch (Exception e) {
            log.error("호스트 ID 조회 실패: roomId={}", roomId, e);
            return null;
        }
    }

    /**
     * 호스트 권한 이전
     */
    @Transactional
    public void transferHost(String roomId, String newHostId) {
        try {
            // 새 호스트가 참가자인지 확인
            String checkQuery = """
            SELECT COUNT(*) FROM MEETING_PARTICIPANTS 
            WHERE ROOM_CD = :roomId 
            AND USER_ID = :userId
            AND ACTIVE_STATE = 'Y'
        """;

            Query check = entityManager.createNativeQuery(checkQuery);
            check.setParameter("roomId", roomId);
            check.setParameter("userId", newHostId);

            Number count = (Number) check.getSingleResult();
            if (count.intValue() == 0) {
                throw new RuntimeException("새 호스트가 회의 참가자가 아닙니다");
            }

            // 기존 호스트를 일반 참가자로 변경
            String oldHostId = getHostId(roomId);
            if (oldHostId != null) {
                String updateOldHost = """
                UPDATE MEETING_PARTICIPANTS 
                SET ROLE = 'PARTICIPANT'
                WHERE ROOM_CD = :roomId 
                AND USER_ID = :userId
            """;

                Query updateOld = entityManager.createNativeQuery(updateOldHost);
                updateOld.setParameter("roomId", roomId);
                updateOld.setParameter("userId", oldHostId);
                updateOld.executeUpdate();
            }

            // 새 호스트 설정
            String updateRoom = """
            UPDATE MEETING_ROOMS 
            SET HOST_ID = :newHostId
            WHERE ROOM_CD = :roomId
        """;

            Query updateRoomQuery = entityManager.createNativeQuery(updateRoom);
            updateRoomQuery.setParameter("roomId", roomId);
            updateRoomQuery.setParameter("newHostId", newHostId);
            updateRoomQuery.executeUpdate();

            // 새 호스트의 역할 업데이트
            String updateNewHost = """
            UPDATE MEETING_PARTICIPANTS 
            SET ROLE = 'HOST'
            WHERE ROOM_CD = :roomId 
            AND USER_ID = :userId
        """;

            Query updateNew = entityManager.createNativeQuery(updateNewHost);
            updateNew.setParameter("roomId", roomId);
            updateNew.setParameter("userId", newHostId);
            updateNew.executeUpdate();

            log.info("호스트 권한 이전 완료: roomId={}, {} -> {}", roomId, oldHostId, newHostId);

        } catch (Exception e) {
            log.error("호스트 권한 이전 실패: roomId={}, newHostId={}", roomId, newHostId, e);
            throw new RuntimeException("호스트 권한 이전 실패: " + e.getMessage());
        }
    }

}
