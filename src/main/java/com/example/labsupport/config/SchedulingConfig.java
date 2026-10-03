package com.example.labsupport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled methods (used by SlaScheduler). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
