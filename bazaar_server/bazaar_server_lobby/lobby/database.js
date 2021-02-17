require('dotenv').config()
// const util = require('util')
const mysql = require('mysql2')
const pool = mysql.createPool({
  connectionLimit: 1000,
  connectTimeout: 40000,
  waitForConnections: true,
  host: 'nodechat',
  user: 'root',
  password: 'smoot',
  database: 'nodechat',
})

const promisePool = pool.promise();

module.exports = {
  pool: pool,
  promisePool: promisePool
};