package com.example.ocean.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;

@Mapper
public interface MemberTransactionMapper {

    // WORKSPACE_ENTRANCE_DATE 조회
    Timestamp getEntranceTime(@Param("workspaceCd") String workspaceCd, @Param("userId") String userId);

    // 누적시간 조회
    Long getAccumulatedTime(@Param("workspaceCd") String workspaceCd, @Param("userId") String userId);

    // 누적 insert
    void insertAccumulatedTime(@Param("workspaceCd") String workspaceCd,
                               @Param("userId") String userId,
                               @Param("duration") Long duration);

    // 누적 update
    void updateAccumulatedTime(@Param("workspaceCd") String workspaceCd,
                               @Param("userId") String userId,
                               @Param("duration") Long duration);

    void truncateTransactionTable();

}
