# Using the Postman collection
1. Start the app (`.\mvnw.cmd spring-boot:run`).
2. Postman -> Import -> choose `LabSupport.postman_collection.json`.
3. Click the collection -> Variables tab -> set `seedPassword` to your SEED_PASSWORD (Current value column) -> Save.
   `baseUrl` is already http://localhost:8080.
4. Click the collection -> Run -> Run Smart Lab Support API. Folder 1 logs in as the three demo users and stores the
   tokens in `studentToken`, `technicianToken`, `adminToken`; later folders reuse them. Every request has a Tests tab check
   (status code); a green result means the behaviour matches the design.
5. Run it from the top each time. The collection creates a new test ticket on every run.

Problems:
- "Could not send request": the app is not running.
- Login returns 401: `seedPassword` is wrong or empty (current value, not just initial value).
- Many 401 errors after login: you ran a single request without running folder 1 first.
