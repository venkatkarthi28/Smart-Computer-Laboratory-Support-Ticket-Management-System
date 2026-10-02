-- =====================================================================
-- Smart Computer Laboratory IT Support & Ticket Management System
-- REFERENCE DATABASE SCHEMA (design document)
--
-- This script is for understanding and for generating the ER diagram.
-- Do NOT run it in lab_support_db. In Phase 3, Hibernate creates the
-- real tables from our Java entity classes, following this design.
-- Run it in a separate scratch database (created below).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS lab_support_design
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE lab_support_design;

-- ---------------------------------------------------------------------
-- 1. users : students, technicians and admins
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('STUDENT','TECHNICIAN','ADMIN') NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. laboratories
-- ---------------------------------------------------------------------
CREATE TABLE laboratories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    location    VARCHAR(150),
    description VARCHAR(500),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_laboratories_name UNIQUE (name)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. lab_technicians : many-to-many join table (lab <-> technician)
--    The service layer must ensure technician_id belongs to a user
--    whose role is TECHNICIAN (a database cannot check that itself).
-- ---------------------------------------------------------------------
CREATE TABLE lab_technicians (
    lab_id        BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    PRIMARY KEY (lab_id, technician_id),
    CONSTRAINT fk_labtech_lab
        FOREIGN KEY (lab_id) REFERENCES laboratories (id) ON DELETE CASCADE,
    CONSTRAINT fk_labtech_technician
        FOREIGN KEY (technician_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. computers
-- ---------------------------------------------------------------------
CREATE TABLE computers (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    computer_code    VARCHAR(30)  NOT NULL,   -- e.g. LAB1-PC-034
    computer_name    VARCHAR(60)  NOT NULL,   -- e.g. PC 034
    laboratory_id    BIGINT       NOT NULL,
    brand            VARCHAR(50),
    model            VARCHAR(80),
    processor        VARCHAR(80),
    ram              VARCHAR(30),             -- e.g. 16 GB
    storage_capacity VARCHAR(50),             -- e.g. 512 GB SSD
    operating_system VARCHAR(60),
    ip_address       VARCHAR(45),             -- optional, fits IPv4 and IPv6
    status           ENUM('WORKING','UNDER_MAINTENANCE','OUT_OF_SERVICE')
                     NOT NULL DEFAULT 'WORKING',
    purchase_date    DATE,                    -- optional
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_computers_code UNIQUE (computer_code),
    KEY idx_computers_laboratory (laboratory_id),
    KEY idx_computers_status (status),
    CONSTRAINT fk_computers_laboratory
        FOREIGN KEY (laboratory_id) REFERENCES laboratories (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. ticket_categories (admin can add / edit / deactivate)
-- ---------------------------------------------------------------------
CREATE TABLE ticket_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,        -- e.g. NETWORK
    description VARCHAR(200),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. ticket_priorities (each priority carries its SLA target in hours)
-- ---------------------------------------------------------------------
CREATE TABLE ticket_priorities (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    name           VARCHAR(20) NOT NULL,      -- LOW, MEDIUM, HIGH, CRITICAL
    sla_hours      INT         NOT NULL,      -- 72, 24, 4, 1
    severity_level TINYINT     NOT NULL,      -- 1 (lowest) to 4 (highest), for sorting
    PRIMARY KEY (id),
    CONSTRAINT uk_priorities_name UNIQUE (name),
    CONSTRAINT uk_priorities_level UNIQUE (severity_level),
    CONSTRAINT chk_priorities_sla CHECK (sla_hours > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. tickets : the main table
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    student_id             BIGINT        NOT NULL,
    computer_id            BIGINT        NOT NULL,
    category_id            BIGINT        NOT NULL,
    priority_id            BIGINT        NOT NULL,
    assigned_technician_id BIGINT        NULL,        -- empty until assigned
    description            VARCHAR(2000) NOT NULL,
    status                 ENUM('OPEN','ASSIGNED','IN_PROGRESS',
                                'RESOLVED','CLOSED','REOPENED')
                           NOT NULL DEFAULT 'OPEN',
    diagnosis              TEXT,
    resolution_notes       TEXT,
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at            DATETIME      NULL,
    started_at             DATETIME      NULL,
    resolved_at            DATETIME      NULL,
    closed_at              DATETIME      NULL,
    sla_deadline           DATETIME      NOT NULL,    -- created_at + priority sla_hours
    sla_breached           BOOLEAN       NOT NULL DEFAULT FALSE,
    student_confirmed      BOOLEAN       NULL,        -- NULL until student answers
    reopen_count           INT           NOT NULL DEFAULT 0,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tickets_status (status),
    KEY idx_tickets_student (student_id),
    KEY idx_tickets_technician (assigned_technician_id),
    KEY idx_tickets_computer (computer_id),
    KEY idx_tickets_category (category_id),
    KEY idx_tickets_priority (priority_id),
    KEY idx_tickets_sla_deadline (sla_deadline),
    CONSTRAINT fk_tickets_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_technician
        FOREIGN KEY (assigned_technician_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_computer
        FOREIGN KEY (computer_id) REFERENCES computers (id),
    CONSTRAINT fk_tickets_category
        FOREIGN KEY (category_id) REFERENCES ticket_categories (id),
    CONSTRAINT fk_tickets_priority
        FOREIGN KEY (priority_id) REFERENCES ticket_priorities (id),
    CONSTRAINT chk_tickets_resolved_after_created
        CHECK (resolved_at IS NULL OR resolved_at >= created_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. ticket_status_history : audit trail of every status change
-- ---------------------------------------------------------------------
CREATE TABLE ticket_status_history (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_id     BIGINT       NOT NULL,
    from_status   ENUM('OPEN','ASSIGNED','IN_PROGRESS',
                       'RESOLVED','CLOSED','REOPENED') NULL,  -- NULL when created
    to_status     ENUM('OPEN','ASSIGNED','IN_PROGRESS',
                       'RESOLVED','CLOSED','REOPENED') NOT NULL,
    changed_by_id BIGINT       NOT NULL,
    changed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark        VARCHAR(500),
    PRIMARY KEY (id),
    KEY idx_history_ticket (ticket_id),
    CONSTRAINT fk_history_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_history_changed_by
        FOREIGN KEY (changed_by_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 9. ticket_comments
-- ---------------------------------------------------------------------
CREATE TABLE ticket_comments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    ticket_id  BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    message    VARCHAR(1000) NOT NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comments_ticket (ticket_id),
    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 10. feedback : at most one per ticket
-- ---------------------------------------------------------------------
CREATE TABLE feedback (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    ticket_id  BIGINT        NOT NULL,
    rating     TINYINT       NOT NULL,
    comments   VARCHAR(1000),
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_feedback_ticket UNIQUE (ticket_id),
    CONSTRAINT fk_feedback_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 11. maintenance_history : repair log per computer (one row per ticket)
-- ---------------------------------------------------------------------
CREATE TABLE maintenance_history (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    computer_id        BIGINT        NOT NULL,
    ticket_id          BIGINT        NOT NULL,
    category_id        BIGINT        NOT NULL,
    technician_id      BIGINT        NOT NULL,
    issue_summary      VARCHAR(500)  NOT NULL,
    resolution_summary VARCHAR(1000) NOT NULL,
    resolved_at        DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_maintenance_ticket UNIQUE (ticket_id),
    KEY idx_maintenance_computer (computer_id, category_id),
    CONSTRAINT fk_maintenance_computer
        FOREIGN KEY (computer_id) REFERENCES computers (id),
    CONSTRAINT fk_maintenance_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_maintenance_category
        FOREIGN KEY (category_id) REFERENCES ticket_categories (id),
    CONSTRAINT fk_maintenance_technician
        FOREIGN KEY (technician_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 12. notifications
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    ticket_id  BIGINT       NULL,
    message    VARCHAR(255) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,git add docs
git commit -m "Phase 2: database design and ER diagram"
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_user_read (user_id, is_read),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id)
) ENGINE=InnoDB;