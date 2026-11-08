'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');
const { localConfig, baseUrl } = require('./qq-config.cjs');
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));

function manifest(config) {
  try { return JSON.parse(fs.readFileSync(config.manifest, 'utf8')); }
  catch (error) { if (error.code === 'ENOENT') return null; throw error; }
}

async function running(config) {
  const saved = manifest(config);
  if (!saved || !Number.isInteger(saved.pid) || saved.pid < 1) return null;
  const host = saved.host === '0.0.0.0' ? '127.0.0.1' : saved.host === '::' ? '::1' : saved.host;
  const url = baseUrl(host, saved.port);
  try {
    const response = await fetch(url + config.healthPath, { signal: AbortSignal.timeout(1_500), redirect: 'error' });
    const body = await response.json();
    if (body.service === 'jaychou-local-qq' && body.pid === saved.pid && body.instance === saved.instance) return { ...saved, url };
  } catch { }
  return null;
}

async function main() {
  const command = process.argv[2];
  if (!['start', 'status', 'stop'].includes(command)) throw new Error('Usage: node qq-control.cjs start|status|stop');
  const config = localConfig();
  const current = await running(config);
  if (command === 'status') {
    console.log(current ? `Running: ${current.url} (PID ${current.pid})` : 'QQ Music API is not running.');
    if (!current) process.exitCode = 1;
    return;
  }
  if (command === 'stop') {
    if (!current) { console.log('QQ Music API is not running.'); return; }
    // Stop only the process whose identity was verified through our health route.
    process.kill(current.pid, 'SIGTERM');
    for (let count = 0; count < 30; count++) {
      if (!await running(config)) {
        const saved = manifest(config);
        if (saved?.instance === current.instance) fs.unlinkSync(config.manifest);
        console.log('QQ Music API stopped.');
        return;
      }
      await pause(100);
    }
    throw new Error('The service has not stopped yet.');
  }
  if (current) { console.log(`Already running: ${current.url}`); return; }
  fs.mkdirSync(config.runtime, { recursive: true });
  const output = fs.openSync(path.join(config.runtime, 'stdout.log'), 'a', 0o600);
  const errors = fs.openSync(path.join(config.runtime, 'stderr.log'), 'a', 0o600);
  let child;
  try {
    child = spawn(process.execPath, [path.join(__dirname, 'qq-local.cjs')], {
      cwd: __dirname, detached: true, windowsHide: true, stdio: ['ignore', output, errors],
    });
    await new Promise((resolve, reject) => { child.once('spawn', resolve); child.once('error', reject); });
    child.unref();
  } finally { fs.closeSync(output); fs.closeSync(errors); }
  for (let count = 0; count < 100; count++) {
    const state = await running(config);
    if (state) { console.log(`Started: ${state.url}`); console.log(`API Explorer: ${state.url}/explorer`); return; }
    if (child.exitCode !== null) break;
    await pause(100);
  }
  throw new Error('Service startup did not complete. Check .local-qq/stderr.log.');
}

main().catch(error => { console.error(error.message); process.exitCode = 1; });
