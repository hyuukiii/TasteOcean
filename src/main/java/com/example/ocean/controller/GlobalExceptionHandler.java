package com.example.ocean.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ModelAndView handleOAuth2Exception(OAuth2AuthenticationException e, HttpServletRequest request) {
        log.error("OAuth2 인증 오류 발생");
        log.error("요청 URL: {}", request.getRequestURL());
        log.error("에러 메시지: {}", e.getMessage());

        if (e.getError() != null) {
            log.error("OAuth2 에러 코드: {}", e.getError().getErrorCode());
            log.error("OAuth2 에러 설명: {}", e.getError().getDescription());
            log.error("OAuth2 에러 URI: {}", e.getError().getUri());
        }

        log.error("전체 스택 트레이스:", e);

        ModelAndView mav = new ModelAndView();
        mav.setViewName("redirect:/oauth2/redirect");
        mav.addObject("error", e.getMessage());
        mav.addObject("error_type", "OAuth2AuthenticationException");

        if (e.getError() != null) {
            mav.addObject("error_code", e.getError().getErrorCode());
            mav.addObject("error_description", e.getError().getDescription());
        }

        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception e, HttpServletRequest request, Model model) {
        log.error("일반 오류 발생 - URL: {}, 오류: {}", request.getRequestURL(), e.getMessage(), e);

        String accept = request.getHeader("Accept");

        if (accept != null && accept.contains("application/json")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "error", e.getClass().getSimpleName(),
                            "message", e.getMessage(),
                            "path", request.getRequestURI()
                    )
            );
        }

        // HTML 뷰 처리
        model.addAttribute("error", e.getClass().getSimpleName());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("exception", e.toString());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        model.addAttribute("trace", sw.toString());

        if (request.getRequestURI().contains("oauth2") || request.getRequestURI().contains("login")) {
            return "error/oauth-error";
        }

        return "error";
    }
}
