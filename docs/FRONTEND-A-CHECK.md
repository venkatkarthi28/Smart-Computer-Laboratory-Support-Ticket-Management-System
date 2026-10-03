# Frontend Zip A - shared files, login, register, 3 dashboards

## Merge
Unzip into the project root. It only ADDS files under src/main/resources/static/ (nothing is overwritten):
  css/app.css, js/api.js, js/auth.js, js/ui.js,
  index.html, login.html, register.html,
  student/dashboard.html, technician/dashboard.html, admin/dashboard.html

## Check (no compile needed - these are static files)
1. Stop the app (Ctrl+C) and start it again:  .\mvnw.cmd spring-boot:run
2. Open http://localhost:8080/  in the browser -> you land on the login page.
3. Login as student@college.com (password = your SEED_PASSWORD)
   -> Student dashboard: 7 number cards, recent tickets table with coloured status badges.
4. Click Logout, login as technician@college.com -> Technician dashboard with 6 cards.
5. Logout, login as admin@college.com -> Admin dashboard: cards, a bar chart, a doughnut chart,
   technician workload table, problematic computers table.
6. Wrong password -> red message "Invalid email or password" (no page reload).
7. Register page: create a new student -> you land on the student dashboard.
   Try password "abc" -> the form lists what is wrong before sending anything.
8. Logged in as the student, type  http://localhost:8080/admin/dashboard.html  in the address bar
   -> you are sent back to the student dashboard.
9. Click the "Notifications" button in the top bar: your notifications appear, the red number is the unread count.
10. Press F12 -> Console tab: there should be no red errors.

Note: links such as "Create Ticket", "My Tickets", "Students" in the menu will show a 404 page until
Zip B (student + technician pages) and Zip C (admin pages) are merged. That is expected.
