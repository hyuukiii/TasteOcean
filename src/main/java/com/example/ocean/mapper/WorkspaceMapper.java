package com.example.ocean.mapper;

import com.example.ocean.domain.Workspace;
import com.example.ocean.domain.WorkspaceDept;
import com.example.ocean.domain.WorkspaceMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkspaceMapper {

    // 사용자 ID로 참여 중인 모든 워크스페이스 조회
    List<Workspace> findWorkspacesByUserId(@Param("userId") String userId);

    // 워크스페이스에 사용자 추가
    void addUserToWorkspace(@Param("userId") String userId,
                            @Param("workspaceCd") String workspaceCd,
                            @Param("userRole") String userRole,
                            @Param("activeState") String activeState);

    // 초대 코드로 워크스페이스 정보 조회
    Workspace findWorkspaceByInviteCd(@Param("inviteCd") String inviteCd);

    // ⭐ 워크스페이스 사용자 프로필 정보 삽입 (이미지 포함)
    void insertUserProfile(@Param("workspaceCd") String workspaceCd,
                           @Param("userId") String userId,
                           @Param("userNickname") String userNickname,
                           @Param("statusMsg") String statusMsg,
                           @Param("email") String email,
                           @Param("phoneNum") String phoneNum,
                           @Param("userRole") String userRole,
                           @Param("userImg") String userImg);

    // ⭐ 워크스페이스 멤버 프로필 정보 업데이트 (이미지 포함)
    void updateWorkspaceProfile(@Param("workspaceCd") String workspaceCd,
                                @Param("userId") String userId,
                                @Param("userNickname") String userNickname,
                                @Param("statusMsg") String statusMsg,
                                @Param("email") String email,
                                @Param("phoneNum") String phoneNum,
                                @Param("userImg") String userImg);

    // 프로필 이미지만 업데이트
    void updateProfileImageOnly(@Param("workspaceCd") String workspaceCd,
                                @Param("userId") String userId,
                                @Param("userImg") String userImg);

    // ⭐ 부서 및 직급 정보 업데이트
    void updateDeptAndPosition(@Param("workspaceCd") String workspaceCd,
                               @Param("userId") String userId,
                               @Param("deptCd") String deptCd,
                               @Param("position") String position);

    // ⭐ 특정 워크스페이스와 사용자에 대한 멤버 상세 정보 조회
    WorkspaceMember findMemberByWorkspaceAndUser(@Param("workspaceCd") String workspaceCd,
                                                 @Param("userId") String userId);

    // 사용자 상태 업데이트
    void updateUserState(Map<String, Object> param);

    // 워크스페이스 생성
    void insertWorkspace(Workspace workspace);

    // 워크스페이스 조회
    Workspace findWorkspaceByCd(@Param("workspaceCd") String workspaceCd);

    // 초대 코드로 워크스페이스 조회 (대소문자 무시)
    Workspace findByInviteCode(@Param("inviteCd") String inviteCd);

    // 중복 초대 여부 확인
    int countInvitation(@Param("workspaceCd") String workspaceCd,
                        @Param("invitedUserId") String invitedUserId);

    // 초대 요청 삽입
    void insertInvitation(@Param("workspaceCd") String workspaceCd,
                          @Param("invitedUserId") String invitedUserId,
                          @Param("inviteCd") String inviteCd);

    // ⭐ 모든 워크스페이스의 대기 중인 초대 목록 조회
    List<Map<String, Object>> getAllPendingInvitations();

    // 초대 상태 업데이트
    void updateInvitationStatus(@Param("workspaceCd") String workspaceCd,
                                @Param("invitedUserId") String invitedUserId,
                                @Param("status") String status);

    // 워크스페이스 멤버 등록
    void insertWorkspaceMember(@Param("workspaceCd") String workspaceCd,
                               @Param("userId") String userId);

    // 초대 거절 처리
    void rejectInvitation(@Param("workspaceCd") String workspaceCd,
                          @Param("invitedUserId") String invitedUserId);

    // 부서 정보 등록
    void insertDepartment(WorkspaceDept dept);

    // 멤버 추가
    void insertMember(WorkspaceMember member);

    // 전체 부서 중 가장 큰 번호 조회
    Integer findGlobalMaxDeptNumber();

    // 워크스페이스에 속한 부서 목록 조회
    List<WorkspaceDept> selectDepartmentsByWorkspace(@Param("workspaceCd") String workspaceCd);

    // 찜하기 상태 업데이트
    void updateFavorite(@Param("userId") String userId,
                        @Param("workspaceCd") String workspaceCd,
                        @Param("favorite") int favorite);

    // 워크스페이스 입장 시간 업데이트
    void updateEntranceTime(@Param("workspaceCd") String workspaceCd,
                            @Param("userId") String userId,
                            @Param("timestamp") Timestamp timestamp);

    // 워크스페이스 퇴장 시간 업데이트
    void updateQuitTime(@Param("workspaceCd") String workspaceCd,
                        @Param("userId") String userId,
                        @Param("timestamp") Timestamp timestamp);

    // ⭐ 워크스페이스 멤버 조회
    List<WorkspaceMember> findMembersByWorkspaceCd(@Param("workspaceCd") String workspaceCd);
}