package com.example.labsupport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "ticket_priorities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_priorities_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_priorities_level", columnNames = "severity_level")
        }
)
public class TicketPriority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LOW, MEDIUM, HIGH, CRITICAL
    @Column(nullable = false, length = 20)
    private String name;

    // Resolution target in hours: 72, 24, 4, 1
    @Column(nullable = false)
    private int slaHours;

    // 1 (lowest) to 4 (highest) - used for sorting
    @Column(nullable = false)
    private int severityLevel;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSlaHours() { return slaHours; }
    public void setSlaHours(int slaHours) { this.slaHours = slaHours; }

    public int getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(int severityLevel) { this.severityLevel = severityLevel; }
}