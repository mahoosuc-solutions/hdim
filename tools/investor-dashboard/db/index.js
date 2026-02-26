const Database = require('better-sqlite3');
const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, 'investor.db');
const SCHEMA_PATH = path.join(__dirname, 'schema.sql');

function getDb() {
  const db = new Database(DB_PATH);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  // Initialize schema using better-sqlite3's exec (not child_process)
  const schema = fs.readFileSync(SCHEMA_PATH, 'utf-8');
  db.exec(schema);

  return db;
}

module.exports = { getDb, DB_PATH };
