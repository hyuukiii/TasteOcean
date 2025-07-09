package com.example.ocean.service;

import com.example.ocean.mapper.MemberTransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerService {

    @Autowired
    private MemberTransactionMapper transactionMapper;

    @Scheduled(cron = "0 0 0 * * MON") // 매주 월요일 자정
    public void resetWeeklyUsageTime() {
        transactionMapper.truncateTransactionTable();
        System.out.println("[SCHEDULED] MEMBERS_TRANSACTION 테이블 초기화 완료");
    }
}
