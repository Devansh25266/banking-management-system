# Banking Management System

Educational banking **simulation** (not a real bank; no regulatory compliance, no real money).
Java 17 + Spring Boot 3 (JdbcTemplate) + MySQL 8 backend, React + TypeScript + Tailwind frontend.

## Features
- Customer registration, login, logout (server sessions, HTTP-only cookie, CSRF protection, BCrypt)
- Savings / Current accounts with generated account numbers, balance starts at 0.00 INR
- Deposit and withdraw: `BigDecimal` / `DECIMAL(19,2)`, `SELECT ... FOR UPDATE` row locking, one DB transaction per operation
- Balance inquiry, transaction history (date + type filters, sorting, pagination)
- Admin console: dashboard, account search, freeze / unfreeze / close, transaction monitoring, audit log
- Customers can only see their own accounts and transactions (other customers' IDs return 404)

## Layout
```
backend/    Spring Boot app (pom.xml, src/main, src/test, schema.sql)
frontend/   React + Vite + TypeScript + Tailwind
.env.example, API_DOCUMENTATION.md
```

## Verification status (please read)
The code was written in an environment **without** a Java compiler, Maven, MySQL or network access.
**Nothing here has been compiled or executed, and no test has been run.** Expect to fix small compile
errors on first build. Run the commands below, and the tests, on your machine to verify.

## Windows setup (PowerShell)

### 1. Install prerequisites
```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
winget install OpenJS.NodeJS.LTS
winget install Oracle.MySQL          # or use the MySQL 8 installer from mysql.com
```
Close and reopen PowerShell, then check:
```powershell
java -version      # use JDK 21 for this project (avoid JDK 25 with the current test dependencies)
mvn -version
node -v; npm -v
mysql --version    # if not found, add "C:\Program Files\MySQL\MySQL Server 8.0\bin" to PATH
```
Choose a root password during the MySQL installation and keep it private.

### 2. Create the database and a dedicated user
```powershell
mysql -u root -p -e "CREATE DATABASE banking_db CHARACTER SET utf8mb4; CREATE USER 'bank_user'@'localhost' IDENTIFIED BY '<choose-a-password>'; GRANT ALL PRIVILEGES ON banking_db.* TO 'bank_user'@'localhost'; FLUSH PRIVILEGES;"
```

### 3. Create the tables
From the project root:
```powershell
mysql -u bank_user -p banking_db -e "source backend/src/main/resources/schema.sql"
```

### 4. Configure environment variables (current PowerShell window)
```powershell
$env:DB_HOST="localhost"; $env:DB_PORT="3306"; $env:DB_NAME="banking_db"
$env:DB_USERNAME="bank_user"; $env:DB_PASSWORD="<your-db-password>"
$env:FRONTEND_ORIGIN="http://localhost:5173"
# Optional: creates ONE administrator on first start if this email does not exist (password >= 12 chars)
$env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="<a-long-admin-password>"
```
See `.env.example` for the full list. Never commit real values.

### 5. Start the backend (port 8080)
```powershell
cd backend
mvn spring-boot:run
```

### 6. Start the frontend (port 5173) in a second PowerShell window
```powershell
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. In development Vite proxies `/api` to `http://localhost:8080`.

### 7. Try it
1. Register at `/register`, log in, open an account, deposit, withdraw, view history.
2. Administrator: log in at `/admin/login` with `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

### 8. Tests and builds
```powershell
cd backend;  mvn test                 # unit + web-layer tests
cd frontend; npm run build            # type-check and production build
```
`FinancialIntegrationTest` (real MySQL, concurrent withdrawals, rollback) uses Testcontainers and needs
Docker Desktop running; without Docker it is skipped automatically.

## Troubleshooting
| Problem | Fix |
|---|---|
| `Access denied for user` | Check `DB_USERNAME` / `DB_PASSWORD` and that the user has rights on `banking_db`. |
| `Communications link failure` | MySQL service not running: `Get-Service MySQL*`, then `Start-Service <name>`. Check `DB_HOST`/`DB_PORT`. |
| `Unknown database` / `Table doesn't exist` | Redo steps 2-3. |
| Port 8080 or 5173 in use | `netstat -ano | findstr :8080`, stop that process, or set `$env:SERVER_PORT` (backend) / `npm run dev -- --port 5174` (then update `FRONTEND_ORIGIN`). |
| CORS errors | `FRONTEND_ORIGIN` must exactly equal the browser origin (scheme + host + port). |
| 403 on POST/PATCH | Security token cookie missing: reload the page; ensure cookies are enabled. |
| `npm install` fails | Check Node 18+, delete `node_modules`, retry; check proxy/network. |
| `mvn` not recognized | Reopen PowerShell after installing, or add Maven's `bin` folder to PATH. |

## Known limitations
- Not compiled or tested yet (see above). No frontend automated tests.
- No transfers between accounts (optional in the spec), no password reset, no login-attempt throttling.
- Production use would need HTTPS (`COOKIE_SECURE=true`), a proper secrets store and a security review.
- Dates are shown using the server database time zone; configure MySQL/JVM time zones consistently.
- No development seed script is included; create data through the UI.
