import assert from 'node:assert/strict';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const { mergeTrace } = await import(pathToFileURL(path.join(__dirname, 'mergeTrace.js')).href);

const steps = mergeTrace({
  action: 'EXECUTE',
  viaGateway: false,
  hops: [
    { service: 'order-service', ok: true, detail: 'execute flow' },
    { service: 'risk-service', ok: false, detail: 'over limit' }
  ]
});

assert.equal(steps.length, 3);
assert.equal(steps[0].service, 'frontend');
assert.equal(steps[1].service, 'order-service');
assert.equal(steps[2].ok, false);
assert.match(steps[2].stateHint, /over limit/);

const withGw = mergeTrace({
  action: 'CREATE_ORDER',
  viaGateway: true,
  hops: [{ service: 'gateway', ok: true }, { service: 'order-service', ok: true }]
});
assert.ok(withGw.some((s) => s.service === 'gateway'));

console.log('OK mergeTrace.selftest');
