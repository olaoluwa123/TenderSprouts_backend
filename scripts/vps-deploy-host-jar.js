/**
 * Upload a locally built JAR to the host school backend.
 * Does not use Docker, does not enable Flyway, does not touch EstateFlow.
 */
const { Client } = require('ssh2')
const fs = require('fs')
const path = require('path')
const os = require('os')
const { loadEnv } = require('./load-env')

loadEnv()
const host = process.env.VPS_HOST || '68.169.55.92'
const username = process.env.VPS_USER || 'solaolu'
const password = process.env.VPS_PASS
const jarLocal = path.resolve(__dirname, '..', 'target', 'backend-0.0.1-SNAPSHOT.jar')
const remoteJar = '/home/solaolu/supersoft/tender/app.jar'
const remoteTmpJar = '/tmp/school-backend-new.jar'

if (!password) {
  console.error('VPS_PASS required')
  process.exit(2)
}
if (!fs.existsSync(jarLocal)) {
  console.error('Missing JAR:', jarLocal)
  process.exit(2)
}

const schemaSql = `
CREATE TABLE IF NOT EXISTS email_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    attachments_json TEXT NULL,
    attempt INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    last_error VARCHAR(500) NULL,
    next_attempt_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

SET @idx := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_outbox' AND INDEX_NAME = 'idx_email_outbox_status_next'
);
SET @sql := IF(@idx = 0,
  'CREATE INDEX idx_email_outbox_status_next ON email_outbox (status, next_attempt_at)',
  'SELECT ''idx_email_outbox_exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preschool_reports' AND COLUMN_NAME = 'month_number'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE preschool_reports ADD COLUMN month_number INT NOT NULL DEFAULT 0',
  'SELECT ''month_number_exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uk := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preschool_reports' AND INDEX_NAME = 'uk_preschool_report'
);
SET @uk_month := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preschool_reports'
    AND INDEX_NAME = 'uk_preschool_report' AND COLUMN_NAME = 'month_number'
);
SET @sql := IF(@uk > 0 AND @uk_month = 0,
  'ALTER TABLE preschool_reports DROP INDEX uk_preschool_report',
  'SELECT ''uk_preschool_keep''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uk2 := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preschool_reports' AND INDEX_NAME = 'uk_preschool_report'
);
SET @sql := IF(@uk2 = 0,
  'ALTER TABLE preschool_reports ADD CONSTRAINT uk_preschool_report UNIQUE (class_id, term_id, month_number)',
  'SELECT ''uk_preschool_exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS assessment_sheet_publications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    published_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    UNIQUE KEY uk_assessment_sheet_publication (sheet_id, student_id),
    CONSTRAINT fk_asp_sheet FOREIGN KEY (sheet_id) REFERENCES assessment_sheets (id) ON DELETE CASCADE,
    CONSTRAINT fk_asp_student FOREIGN KEY (student_id) REFERENCES students (id)
);

SET @idx2 := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assessment_sheet_publications' AND INDEX_NAME = 'idx_asp_student'
);
SET @sql := IF(@idx2 = 0,
  'CREATE INDEX idx_asp_student ON assessment_sheet_publications (student_id)',
  'SELECT ''idx_asp_exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sub := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assessment_sheets' AND COLUMN_NAME = 'submitted_at'
);
SET @sql := IF(@sub = 0,
  'ALTER TABLE assessment_sheets ADD COLUMN submitted_at TIMESTAMP NULL',
  'SELECT ''submitted_at_exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sub_by := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assessment_sheets' AND COLUMN_NAME = 'submitted_by_teacher_id'
);
SET @sql := IF(@sub_by = 0,
  'ALTER TABLE assessment_sheets ADD COLUMN submitted_by_teacher_id BIGINT NULL',
  'SELECT ''submitted_by_exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS school_calendar_poster (
    id BIGINT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_bytes MEDIUMBLOB NOT NULL,
    uploaded_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);
`

function redact(text) {
  return String(text)
    .replaceAll(password, '***')
    .replace(/password=[^ \n]*/gi, 'password=***')
    .replace(/-p[^ \n]*/g, '-p***')
}

function connect() {
  return new Promise((resolve, reject) => {
    const conn = new Client()
    conn.on('ready', () => resolve(conn))
    conn.on('error', reject)
    conn.connect({
      host,
      username,
      password,
      readyTimeout: 120000,
      keepaliveInterval: 5000,
      hostVerifier: () => true,
    })
  })
}

function exec(conn, command, timeoutMs = 180000) {
  return new Promise((resolve, reject) => {
    conn.exec(command, { pty: true }, (err, stream) => {
      if (err) return reject(err)
      let stdout = ''
      let sentPw = false
      const timer = setTimeout(() => {
        stream.close()
        reject(new Error(`Timeout after ${timeoutMs}ms\n${redact(stdout).slice(-2000)}`))
      }, timeoutMs)
      stream.on('data', (d) => {
        const text = d.toString()
        stdout += text
        process.stdout.write(redact(text))
        if (!sentPw && (/\[sudo\] password/i.test(text) || /password for/i.test(text))) {
          sentPw = true
          stream.write(`${password}\n`)
        }
      })
      stream.stderr.on('data', (d) => process.stderr.write(redact(d.toString())))
      stream.on('close', (code) => {
        clearTimeout(timer)
        if (code === 0) resolve(stdout)
        else reject(new Error(`Exit ${code}\n${redact(stdout).slice(-4000)}`))
      })
    })
  })
}

function upload(conn, localFile, remoteFile) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(err)
      sftp.fastPut(localFile, remoteFile, (putErr) => {
        sftp.end()
        if (putErr) reject(putErr)
        else resolve()
      })
    })
  })
}

async function main() {
  const sizeMb = (fs.statSync(jarLocal).size / 1024 / 1024).toFixed(1)
  console.log(`JAR ${jarLocal} (${sizeMb} MB)`)
  const conn = await connect()
  try {
    console.log('\n=== disk / current jar / flyway flag ===')
    await exec(
      conn,
      `
df -h / | tail -1
ls -lh ${remoteJar} 2>/dev/null || echo 'no current jar'
awk -F= '/^SPRING_FLYWAY_ENABLED=/{print}' /home/solaolu/supersoft/tender/.env || true
grep -E '^export DB_URL=' /home/solaolu/supersoft/tender/start-backend.sh || true
ss -lntp | grep -E ':8090|:8080|:2000' || true
`.trim()
    )

    console.log('\n=== apply schema on host MySQL ===')
    const sqlFile = path.join(os.tmpdir(), 'school-schema-v35-v37.sql')
    fs.writeFileSync(sqlFile, schemaSql)
    await upload(conn, sqlFile, '/tmp/school-schema-v35-v37.sql')
    await exec(
      conn,
      `
set -e
cd /home/solaolu/supersoft/tender
DB_USER=$(awk -F= '/^DB_USERNAME=/{print substr($0,index($0,"=")+1)}' .env | tr -d '\\r')
DB_PASS=$(awk -F= '/^DB_PASSWORD=/{print substr($0,index($0,"=")+1)}' .env | tr -d '\\r')
grep -q '^SPRING_FLYWAY_ENABLED=false' .env || printf '\\nSPRING_FLYWAY_ENABLED=false\\n' >> .env
echo "flyway=$(awk -F= '/^SPRING_FLYWAY_ENABLED=/{print $2}' .env | tail -1)"
mysql --protocol=tcp -h 127.0.0.1 -P 3306 -u "$DB_USER" -p"$DB_PASS" school_portal < /tmp/school-schema-v35-v37.sql
mysql --protocol=tcp -h 127.0.0.1 -P 3306 -u "$DB_USER" -p"$DB_PASS" school_portal -N -e "
SHOW TABLES LIKE 'email_outbox';
SHOW TABLES LIKE 'assessment_sheet_publications';
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='school_portal' AND TABLE_NAME='preschool_reports' AND COLUMN_NAME='month_number';
"
rm -f /tmp/school-schema-v35-v37.sql
echo SCHEMA_OK
`.trim()
    )

    console.log('\n=== upload jar ===')
    await upload(conn, jarLocal, remoteTmpJar)
    await exec(conn, `ls -lh ${remoteTmpJar}`)

    console.log('\n=== replace jar and restart school-backend ===')
    await exec(
      conn,
      `
set -e
systemctl --user stop school-backend.service
cp -f ${remoteJar} /tmp/app.jar.bak 2>/dev/null || true
mv -f ${remoteTmpJar} ${remoteJar}
chmod 644 ${remoteJar}
ls -lh ${remoteJar}
systemctl --user start school-backend.service
echo RESTART_ISSUED
`.trim(),
      60000
    )

    console.log('\n=== wait for health ===')
    await exec(
      conn,
      `
set -e
for i in $(seq 1 24); do
  code=$(curl -sS -m 5 -o /tmp/h.txt -w '%{http_code}' http://127.0.0.1:8090/actuator/health || echo 000)
  echo "try $i health=$code $(head -c 120 /tmp/h.txt 2>/dev/null)"
  echo "$code" | grep -q 200 && break
  sleep 5
done
curl -sS -m 8 -o /dev/null -w 'estate8080=%{http_code}\\n' http://127.0.0.1:8080/ || true
curl -sk -m 20 -o /tmp/api.txt -w 'public_login=%{http_code}\\n' https://68.169.55.92.sslip.io/api/auth/login -X POST -H 'Content-Type: application/json' -d '{}' || true
head -c 200 /tmp/api.txt; echo
ss -lntp | grep -E ':8090|:8080|:2000' || true
echo DEPLOY_OK
`.trim(),
      180000
    )
    conn.end()
    try {
      fs.unlinkSync(sqlFile)
    } catch (_) {}
  } catch (err) {
    try {
      conn.end()
    } catch (_) {}
    throw err
  }
}

main().catch((e) => {
  console.error(redact(e.message || e))
  process.exit(1)
})
