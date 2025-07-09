package com.example.ocean.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkspaceMemberRepository {
    String selectMemberEmail(@Param("workspaceCd")String workspaceCd, @Param("userId")String userId);
    List<String> getWorkspaceMemberId(@Param("workspaceCd") String workspaceCd);
}
