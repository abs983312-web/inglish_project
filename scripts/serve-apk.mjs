import { createServer } from 'node:http';
import { createReadStream, statSync } from 'node:fs';
import { resolve } from 'node:path';

const file = resolve(
  process.argv[2] ?? 'android/app/build/outputs/apk/debug/app-debug.apk',
);
const port = Number(process.argv[3] ?? 8080);
const size = statSync(file).size;

createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('ok');
    return;
  }

  res.writeHead(200, {
    'Content-Type': 'application/vnd.android.package-archive',
    'Content-Disposition': 'attachment; filename="inglish-debug.apk"',
    'Content-Length': size,
  });

  if (req.method === 'HEAD') {
    res.end();
    return;
  }

  createReadStream(file).pipe(res);
}).listen(port, '0.0.0.0', () => {
  console.log(`APK server on http://0.0.0.0:${port} -> ${file} (${size} bytes)`);
});
