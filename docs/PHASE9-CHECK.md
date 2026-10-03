# Phase 9 checking guide (JWT login)

## Step 0 - merge
1. Close the app. Replace your `pom.xml` with the one in this zip.
2. Make sure `.env` (project root, next to pom.xml) has these 4 lines (see .env.example):
   DB_PASSWORD, SEED_PASSWORD, JWT_SECRET (32+ characters), DB_USERNAME (optional).
3. VS Code: Ctrl+Shift+P -> "Java: Clean Java Language Server Workspace" -> Restart.

## Step 1 - compile
    .\mvnw.cmd clean compile
Expected: BUILD SUCCESS. If you see "package org.springframework.security.oauth2... does not exist",
the pom.xml was not replaced or Maven did not reload - run `.\mvnw.cmd -U clean compile`.

## Step 2 - start
    .\mvnw.cmd spring-boot:run
Expected: "Started LabSupportApplication". If it says JWT_SECRET / "Could not resolve placeholder",
your .env line is missing or misspelled.

## Step 3 - Postman (or PowerShell) checks
| # | Request | Expected |
|---|---------|----------|
| 1 | GET  /api/health | 200 |
| 2 | GET  /api/auth/me (no token) | 401 JSON |
| 3 | POST /api/auth/login {"email":"student@college.com","password":"<SEED_PASSWORD>"} | 200, token in response |
| 4 | GET  /api/auth/me with header Authorization: Bearer <token> | 200, role STUDENT |
| 5 | GET  /api/admin/dashboard with the STUDENT token | 403 JSON |
| 6 | POST /api/auth/login as admin@college.com, then GET /api/admin/dashboard | 200 |
| 7 | POST /api/auth/login with a wrong password | 401 "Invalid email or password" |
| 8 | POST /api/auth/register {"fullName":"New One","email":"new1@college.com","password":"Passw0rd1","role":"ADMIN"} | 201, role STUDENT |
| 9 | GET /api/auth/me with token edited by one character | 401 |

PowerShell example for step 3:
    $b = @{email="student@college.com";password="YOUR_SEED_PASSWORD"} | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType "application/json" -Body $b

## Step 4 - automated test (needs MySQL running)
    .\mvnw.cmd -Dtest=AuthServiceTest test
Expected: Tests run: 3, Failures: 0.

If any row fails, send me the row number plus the response body and the first console error.
