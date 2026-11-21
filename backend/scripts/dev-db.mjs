import { existsSync, readFileSync, rmSync } from 'node:fs';
import EmbeddedPostgres from 'embedded-postgres';

const dataDir = './.pgdata';

const pg = new EmbeddedPostgres({
  databaseDir: dataDir,
  user: 'inglish',
  password: 'inglish',
  port: 5433,
  persistent: true,
});

// Re-running the script must reuse the existing cluster instead of failing with
// "directory exists but is not empty".
if (!existsSync(`${dataDir}/PG_VERSION`)) {
  await pg.initialise();
}

// A crashed previous run can leave a stale postmaster.pid that blocks startup.
const pidFile = `${dataDir}/postmaster.pid`;
if (existsSync(pidFile)) {
  const pid = Number.parseInt(readFileSync(pidFile, 'utf8').split('\n')[0] ?? '', 10);
  let running = false;

  if (Number.isInteger(pid) && pid > 0) {
    try {
      process.kill(pid, 0);
      running = true;
    } catch {
      running = false;
    }
  }

  if (!running) {
    rmSync(pidFile);
  }
}

await pg.start();

try {
  await pg.createDatabase('inglish');
} catch {
  // Database already exists — safe to ignore.
}

console.log('Postgres ready: postgresql://inglish:inglish@localhost:5433/inglish');

const shutdown = async () => {
  await pg.stop();
  process.exit(0);
};

process.on('SIGINT', () => void shutdown());
process.on('SIGTERM', () => void shutdown());
