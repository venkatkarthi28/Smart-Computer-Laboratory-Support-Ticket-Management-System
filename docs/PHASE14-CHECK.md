# Phase 14 - SLA monitoring (background check)

Before this phase a ticket was marked "SLA breached" only when a technician resolved it late.
A ticket nobody touched was never flagged and nobody was notified. This phase adds a background job.

## Merge
Unzip into the project root, keep the folder structure (it only ADDS 5 new files, nothing is overwritten):
  src/main/java/.../service/SlaService.java
  src/main/java/.../service/SlaScheduler.java
  src/main/java/.../config/SchedulingConfig.java
  src/main/java/.../controller/AdminSlaController.java
  src/test/java/.../service/SlaServiceTest.java

## Check 1 - compile
    .\mvnw.cmd clean compile
Expected: BUILD SUCCESS

## Check 2 - automated test (stop the running app first with Ctrl+C)
    .\mvnw.cmd -Dtest=SlaServiceTest test
Expected: Tests run: 3, Failures: 0, Errors: 0

## Check 3 - live (optional)
1. Start the app. As the student, create a ticket (priority HIGH).
2. In your MySQL tool run (replace 5 with the ticket id):
     UPDATE tickets SET sla_deadline = NOW() - INTERVAL 10 MINUTE WHERE id = 5;
3. As admin: POST /api/admin/sla/check  -> {"flagged":1}
4. GET /api/tickets/5 as admin -> "slaBreached": true
5. GET /api/notifications as admin and as technician -> a message starting "SLA breached"
6. POST /api/admin/sla/check again -> {"flagged":0}
7. POST /api/admin/sla/check with the student token -> 403
