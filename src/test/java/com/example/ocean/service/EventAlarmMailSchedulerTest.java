package com.example.ocean.service;

import com.example.ocean.EventAlarmMailScheduler;
import com.example.ocean.dto.response.MailInfo;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@MapperScan({
        "com.example.ocean.mapper",
        "com.example.ocean.repository"
})
@ComponentScan(basePackages = {"com.example.ocean", "com.example.ocean.util"})
class EventAlarmMailSchedulerTest {
    @Autowired
    EventAlarmMailScheduler scheduler;

    @Test
    public void test1() throws MessagingException {
        List<String> attendencdEmails= new ArrayList<>();
        attendencdEmails.add("dltnalssumin@gmail.com");
        attendencdEmails.add("victoai999@gmail.com");

        MailInfo m = new MailInfo();
        m.setEventCd("evnt_abc123456789");
        m.setAttendencdEmails(attendencdEmails);
        m.setWorkspaceName("Test용 워크스페이스");
        m.setDescription("이벤트 1 설명");
        m.setTitle("이벤트 1");
        m.setWorkspaceCd("8a151dac-0baa-4938-9379-746219ea1e4c");

        scheduler.sendEmail(m);
    }

}