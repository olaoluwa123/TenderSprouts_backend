const fs = require('fs')
const path = require('path')

function loadEnv(filePath) {
  const envPath = filePath || path.resolve(__dirname, '..', '.env')
  const out = {}
  if (!fs.existsSync(envPath)) return out
  for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    if (!line || line.startsWith('#') || !line.includes('=')) continue
    const key = line.slice(0, line.indexOf('=')).trim()
    const value = line.slice(line.indexOf('=') + 1).trim()
    if (!key) continue
    out[key] = value
    if (process.env[key] === undefined) process.env[key] = value
  }
  return out
}

function requireEnv(keys, env) {
  const src = env || process.env
  const missing = keys.filter((key) => !src[key])
  if (missing.length) {
    console.error(`Missing in .env: ${missing.join(', ')}`)
    process.exit(2)
  }
}

function shQuote(value) {
  return `'${String(value).replace(/'/g, `'\\''`)}'`
}

module.exports = { loadEnv, requireEnv, shQuote }
