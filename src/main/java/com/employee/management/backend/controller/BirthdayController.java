package com.employee.management.backend.controller;

import com.employee.management.backend.dto.BirthdayDTO;
import com.employee.management.backend.security.AuthenticatedUser;
import com.employee.management.backend.service.BirthdayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/birthdays")
public class BirthdayController {

    private final BirthdayService birthdayService;

    public BirthdayController(BirthdayService birthdayService) {
        this.birthdayService = birthdayService;
    }

    private AuthenticatedUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            throw new RuntimeException("Not authenticated");
        }
        return authenticatedUser;
    }

    // Colleagues (same client only) whose birthday is today - for the employee dashboard card.
    @GetMapping("/today")
    public ResponseEntity<List<BirthdayDTO>> getTodaysBirthdays() {
        return ResponseEntity.ok(birthdayService.getTodaysBirthdays(currentUser().clientId()));
    }
}
