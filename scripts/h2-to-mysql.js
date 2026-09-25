const fs = require('fs')

const input = process.argv[2]
const output = process.argv[3]
if (!input || !output) {
  console.error('Usage: node h2-to-mysql.js <h2-raw.sql> <tender.sql>')
  process.exit(1)
}

const raw = fs.readFileSync(input, 'utf8')
const skipTables = new Set(['flyway_schema_history'])

const columns = new Map()
const restarts = new Map()
for (const match of raw.matchAll(/CREATE CACHED TABLE "public"\."([^"]+)"\(([\s\S]*?)\);/g)) {
  const table = match[1]
  const body = match[2]
  const cols = []
  for (const col of body.matchAll(/^\s*"([^"]+)"\s+/gm)) {
    cols.push(col[1])
  }
  columns.set(table, cols)
  const restart = body.match(/RESTART WITH (\d+)/)
  if (restart) restarts.set(table, Number(restart[1]))
}

const statements = []
const insertRe = /INSERT INTO "public"\."([^"]+)" VALUES\s*([\s\S]*?);/g
let match
while ((match = insertRe.exec(raw)) !== null) {
  const table = match[1]
  if (skipTables.has(table)) continue
  let values = match[2]
    .replace(/TIMESTAMP '([^']+)'/g, "'$1'")
    .replace(/DATE '([^']+)'/g, "'$1'")
    .replace(/TIME '([^']+)'/g, "'$1'")
    .replace(/X'([0-9A-Fa-f]+)'/g, '0x$1')
    .replace(/U&'((?:[^']|'')*)'/g, (_, body) => {
      const decoded = body
        .replace(/''/g, "'")
        .replace(/\\([0-9a-fA-F]{4})/g, (__, hex) => String.fromCharCode(parseInt(hex, 16)))
      return `'${decoded.replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
    })
  const colList = (columns.get(table) || []).map((c) => `\`${c}\``).join(', ')
  const into = colList ? `INSERT INTO \`${table}\` (${colList})` : `INSERT INTO \`${table}\``
  statements.push(`${into} VALUES\n${values};`)
}

const alters = [...restarts.entries()]
  .filter(([table]) => !skipTables.has(table))
  .map(([table, next]) => `ALTER TABLE \`${table}\` AUTO_INCREMENT = ${next};`)

const sql = [
  'SET NAMES utf8mb4;',
  'SET FOREIGN_KEY_CHECKS = 0;',
  'SET UNIQUE_CHECKS = 0;',
  ...statements,
  ...alters,
  'SET UNIQUE_CHECKS = 1;',
  'SET FOREIGN_KEY_CHECKS = 1;',
  '',
].join('\n')

fs.writeFileSync(output, sql)
console.log(`Wrote ${statements.length} inserts and ${alters.length} auto-increment resets to ${output}`)
