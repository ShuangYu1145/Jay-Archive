'use strict';

const fs = require('node:fs');
const path = require('node:path');

function localConfig() {
  const envFile = path.join(__dirname, '.env.qq.local');
  if (fs.existsSync(envFile)) process.loadEnvFile(envFile);
  const host = process.env.QQ_API_HOST || '127.0.0.1';
  const port = Number(process.env.QQ_API_PORT || '3200');
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error('QQ_API_PORT must be an integer between 1 and 65535.');
  }
  const runtime = path.join(__dirname, '.local-qq');
  return {
    host, port, runtime,
    state: path.join(__dirname, '.auth-state'),
    manifest: path.join(runtime, 'process.json'),
    healthPath: '/__jaychou_qq__/health',
    connectHost: host === '0.0.0.0' ? '127.0.0.1' : host === '::' ? '::1' : host,
  };
}

function baseUrl(host, port) {
  return `http://${host.includes(':') ? `[${host}]` : host}:${port}`;
}

module.exports = { localConfig, baseUrl };
