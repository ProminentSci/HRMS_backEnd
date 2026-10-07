package com.employee.management.backend.dto;

// One colleague celebrating a birthday today, as shown on the employee dashboard card.
public record BirthdayDTO(
        Long empId,
        String name,
        String designation,
        String department,
        String profilePhoto,
        String companyName
) {
}
