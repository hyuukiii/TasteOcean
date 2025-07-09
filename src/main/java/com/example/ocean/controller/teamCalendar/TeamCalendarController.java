package com.example.ocean.controller.teamCalendar;

import com.example.ocean.dto.response.UserProfileResponse;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
@RequiredArgsConstructor
@Controller
public class TeamCalendarController {

    private final UserService userService;

    @GetMapping("/calendar/team")
    public String teamCalendar(@AuthenticationPrincipal UserPrincipal userPrincipal,
                       @RequestParam(required = false) String workspaceCd,
                       Model model) {

        UserProfileResponse currentUser = userService.getUserProfile(userPrincipal.getId());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("workspaceCd", workspaceCd);

        return "teamCalendar";
    }
}
