package com.employee.management.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on @Scheduled jobs (e.g. the daily birthday email in BirthdayService).
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
