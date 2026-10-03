# Deployment guide

The application is one runnable JAR plus a MySQL 8 database. Secrets come from environment variables,
nothing secret is stored in the code or in Git.

## 1. Build the JAR (on your PC)
```powershell
.\mvnw.cmd clean package -DskipTests
```
Result: `target\lab-support-0.0.1-SNAPSHOT.jar`. (Run `.\mvnw.cmd test` first if MySQL is running locally.)

## 2. Prepare MySQL 8 on the server
```sql
CREATE DATABASE lab_support_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'labsupport_user'@'%' IDENTIFIED BY 'a-strong-password';
GRANT ALL PRIVILEGES ON lab_support_db.* TO 'labsupport_user'@'%';
FLUSH PRIVILEGES;
```
Use the host name instead of `%` when the app and database run on the same machine (`'localhost'`).

## 3. Environment variables
| Variable | Required | Meaning |
|---|---|---|
| `DB_PASSWORD` | yes | MySQL password |
| `JWT_SECRET` | yes | random string, at least 32 characters. Generate one: `openssl rand -base64 48` or PowerShell `-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 48 \| % {[char]$_})` |
| `DB_USERNAME` | no | default `labsupport_user` |
| `DB_URL` | no | default is the local database. Example: `jdbc:mysql://db-host:3306/lab_support_db?useSSL=true` |
| `PORT` | no | default 8080 |
| `SEED_PASSWORD` | first start only | password for the 3 demo accounts, see step 5 |
| `SPRING_PROFILES_ACTIVE` | yes | set to `prod` |

Use a **different** `JWT_SECRET` and database password than on your PC. Never commit `.env`.

## 4. First start creates the tables, later starts only validate them
The `prod` profile uses `ddl-auto=validate`, which fails on an empty database. So the very first start is run once with an override:

Linux:
```bash
export SPRING_PROFILES_ACTIVE=prod DB_PASSWORD=... JWT_SECRET=... SEED_PASSWORD=...
SPRING_JPA_HIBERNATE_DDL_AUTO=update java -jar lab-support-0.0.1-SNAPSHOT.jar
```
Windows PowerShell:
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"; $env:DB_PASSWORD="..."; $env:JWT_SECRET="..."; $env:SEED_PASSWORD="..."
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"; java -jar target\lab-support-0.0.1-SNAPSHOT.jar
```
When you see `Started LabSupportApplication`, stop it (Ctrl+C). From now on start **without** `SPRING_JPA_HIBERNATE_DDL_AUTO`
(the profile then validates). Keep that rule whenever you add a new column or table: run once with `update`, then go back.

## 5. Replace the demo accounts
The first start created `admin@college.com`, `technician@college.com`, `student@college.com` (password = `SEED_PASSWORD`).
1. Log in as the admin, create your real administrator and technicians (Admin pages).
2. Deactivate the three demo accounts (or reset their passwords to long random values).
3. Remove `SEED_PASSWORD` from the environment for all later starts.

## 6. Run
```bash
java -jar lab-support-0.0.1-SNAPSHOT.jar        # with SPRING_PROFILES_ACTIVE=prod, DB_PASSWORD, JWT_SECRET set
```
Open `http://SERVER:8080/`. To keep it running after logout use a service manager (systemd on Linux, NSSM on Windows).

## 7. HTTPS is required in production
Login sends the password and then a JWT in every request. Put the app behind HTTPS: a reverse proxy
(Nginx or Caddy with a free Let's Encrypt certificate) or the TLS of your hosting platform. Do not expose port 8080 directly.

## 8. Smoke test after deployment
1. `GET https://your-domain/api/health` returns `UP`.
2. Log in on the website with your real admin account.
3. Import `postman/LabSupport.postman_collection.json`, change `baseUrl`, run the folders 1, 3, 4, 5 and 11 with a test account.
4. Make a backup plan: nightly `mysqldump lab_support_db > backup.sql`.
