'use strict';

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { localConfig, baseUrl } = require('./qq-config.cjs');

async function start() {
  const config = localConfig();
  fs.mkdirSync(config.state, { recursive: true });
  fs.mkdirSync(config.runtime, { recursive: true });

  // Create the encryption key once; keep it and the session file across restarts.
  // Never put the key in output, URLs, or the process manifest.
  if (!process.env.QQ_SESSION_SECRET) {
    const keyFile = path.join(config.state, 'qq-local.key');
    try { fs.writeFileSync(keyFile, crypto.randomBytes(32).toString('hex'), { flag: 'wx', mode: 0o600 }); }
    catch (error) { if (error.code !== 'EEXIST') throw error; }
    process.env.QQ_SESSION_SECRET = fs.readFileSync(keyFile, 'utf8').trim();
    if (!process.env.QQ_SESSION_SECRET) throw new Error('The saved local session key is empty.');
  }
  process.env.QQ_AUTH_STATE_PATH = path.join(config.state, 'qq-local-device.json');
  process.env.QQ_AUTH_SESSION_PATH = path.join(config.state, 'qq-local-sessions.json');
  process.env.QQ_DISABLE_UPDATE_CHECK = 'true';
  process.env.AUTO_OPEN_EXPLORER = 'false';
  process.env.NODE_ENV = 'production';

  // The package starts a server on import. Close its initial listener
  // synchronously, before it can accept requests, then bind the selected interface.
  process.env.PORT = String(config.port);
  const backend = require('@yakult-green-tea/qq-music-api');
  const server = backend.server;
  if (!server) throw new Error('QQ Music API did not create its HTTP server.');
  server.removeAllListeners('listening');
  const instance = crypto.randomUUID();
  const startedAt = new Date().toISOString();
  backend.default.middleware.unshift(async (ctx, next) => {
    if (ctx.path !== config.healthPath) return next();
    ctx.set('Cache-Control', 'no-store');
    ctx.body = { service: 'jaychou-local-qq', pid: process.pid, instance, startedAt };
  });

  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.close(() => {
      server.listen(config.port, config.host, () => {
        server.removeListener('error', reject);
        resolve();
      });
    });
  });

  const manifest = { pid: process.pid, instance, host: config.host, port: config.port, startedAt };
  fs.writeFileSync(config.manifest, JSON.stringify(manifest, null, 2) + '\n', { mode: 0o600 });
  const url = baseUrl(config.connectHost, config.port);
  console.log(`QQ Music API: ${url}`);
  console.log(`API Explorer: ${url}/explorer`);
  console.log('Login sessions are encrypted and stored locally.');

  const cleanManifest = () => {
    try {
      const saved = JSON.parse(fs.readFileSync(config.manifest, 'utf8'));
      if (saved.instance === instance) fs.unlinkSync(config.manifest);
    } catch (error) { if (error.code !== 'ENOENT') console.error('Could not clean up the process manifest.'); }
  };
  process.on('exit', cleanManifest);
  let closing = false;
  const shutdown = () => {
    if (closing) return;
    closing = true;
    server.close(() => process.exit(0));
    setTimeout(() => { server.closeAllConnections(); process.exit(0); }, 2_000).unref();
  };
  process.once('SIGINT', shutdown);
  process.once('SIGTERM', shutdown);
  server.on('error', error => { console.error(`HTTP server error: ${error.code || 'unknown'}`); process.exit(1); });
}

start().catch(error => {
  console.error(`QQ Music API failed to start: ${error.code || error.message}`);
  process.exit(1);
});
