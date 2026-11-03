// Offline frame sheet of a .bbmodel animation: samples the clip at the given times, rasterizes
// the saved cuboids with their embedded textures and lays the frames out in a labeled grid.
// Same idea as pets/blockbench/tools/render_cururu.mjs: it is a QA preview, not a Blockbench
// screenshot or Minecraft runtime evidence (no perspective, simple lighting).
//
// Usage: node render_frames.mjs <model.bbmodel> <out.png> [times=0,0.45,...] [view=3q|front] [cols=4]
// Times are seconds in the first animation; the default list is the key moments of the
// `replace` clip (three impacts, swap, vanish, drop-in and the loop seam at the clip length).
import fs from 'node:fs/promises';
import {createRequire} from 'node:module';
const require = createRequire(import.meta.url);
let sharp;
try { sharp = require('sharp'); }
catch { sharp = require('C:/Users/lucca/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp'); }

const [modelPath, outPath, timesArg, viewArg = '3q', colsArg = '4'] = process.argv.slice(2);
if (!modelPath || !outPath) { console.error('usage: node render_frames.mjs <model.bbmodel> <out.png> [times] [3q|front] [cols]'); process.exit(1); }
const m = JSON.parse(await fs.readFile(modelPath, 'utf8'));
const clip = m.animations[0];
const times = timesArg && timesArg !== 'default' ? timesArg.split(',').map(Number)
  : [0, 0.55, 1.35, 2.3, 2.65, 2.8, 3.0, 3.15, 3.3, 3.5, 3.7, clip.length];

// ---- textures (embedded PNG data URIs)
const textures = [];
for (const t of m.textures) {
  const buf = Buffer.from(t.source.split(',')[1], 'base64');
  const {data, info} = await sharp(buf).ensureAlpha().raw().toBuffer({resolveWithObject: true});
  textures.push({data, w: info.width, h: info.height, uw: t.uv_width ?? info.width, uh: t.uv_height ?? info.height,
    emissive: t.render_mode === 'emissive'});
}

// ---- scene graph
const els = new Map(m.elements.map(e => [e.uuid, e]));
const groups = new Map(), parent = new Map(), owner = new Map();
(function walk(nodes, p = null) {
  for (const n of nodes) {
    if (typeof n === 'string') owner.set(n, p);
    else { groups.set(n.uuid, n); parent.set(n.uuid, p); walk(n.children ?? [], n.uuid); }
  }
})(m.outliner);

const mv = (a, v) => [0, 1, 2].map(i => a[i * 3] * v[0] + a[i * 3 + 1] * v[1] + a[i * 3 + 2] * v[2]);
const mm = (a, b) => [0, 1, 2].flatMap(i => [0, 1, 2].map(j => a[i * 3] * b[j] + a[i * 3 + 1] * b[j + 3] + a[i * 3 + 2] * b[j + 6]));
const add = (a, b) => a.map((v, i) => v + b[i]), sub = (a, b) => a.map((v, i) => v - b[i]);
const dot = (a, b) => a.reduce((s, v, i) => s + v * b[i], 0);
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const norm = a => a.map(v => v / Math.hypot(...a));
const I3 = [1, 0, 0, 0, 1, 0, 0, 0, 1];
// Euler 'ZYX' like Blockbench: x is applied first, z last
function eulerZYX(deg) {
  const [x, y, z] = deg.map(d => d * Math.PI / 180), cx = Math.cos(x), sx = Math.sin(x), cy = Math.cos(y), sy = Math.sin(y), cz = Math.cos(z), sz = Math.sin(z);
  return [cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx, sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx, -sy, cy * sx, cy * cx];
}

// ---- animation sampling (keyframes are linear; missing channels = rest)
function sample(time) {
  const pose = {};
  for (const [id, a] of Object.entries(clip.animators)) {
    for (const ch of ['rotation', 'position', 'scale']) {
      const ks = (a.keyframes ?? []).filter(k => k.channel === ch).sort((p, q) => p.time - q.time);
      if (!ks.length) continue;
      const val = k => ['x', 'y', 'z'].map(c => Number(k.data_points[0][c]));
      let j = ks.findIndex(k => k.time >= time);
      let v;
      if (j < 0) v = val(ks[ks.length - 1]);
      else if (j === 0) v = val(ks[0]);
      else { const l = ks[j - 1], r = ks[j], u = (time - l.time) / (r.time - l.time); v = val(l).map((x, i) => x + (val(r)[i] - x) * u); }
      (pose[id] ??= {})[ch] = v;
    }
  }
  return pose;
}

function worldOf(id, pose, cache) {
  if (!id) return {m: I3, p: [0, 0, 0]};
  if (cache.has(id)) return cache.get(id);
  const g = groups.get(id), par = worldOf(parent.get(id), pose, cache), v = pose[id] ?? {};
  const rot = eulerZYX(add(v.rotation ?? [0, 0, 0], g.rotation ?? [0, 0, 0])), s = v.scale ?? [1, 1, 1];
  const local = rot.map((x, i) => x * s[i % 3]);                     // R * S
  const o = g.origin, lp = sub(add(o, v.position ?? [0, 0, 0]), mv(local, o));
  const w = {m: mm(par.m, local), p: add(par.p, mv(par.m, lp))};
  cache.set(id, w); return w;
}

// ---- camera
const views = {'3q': [-0.5, 0.42, -0.85], front: [0, 0.12, -1]};
const cam = norm(views[viewArg] ?? views['3q']);                       // direction towards the camera
const right = norm(cross([0, 1, 0], cam)), up = cross(cam, right);     // right-handed screen basis
const CENTER = [5, 17, -2], SS = 2, CELL_W = 380, CELL_H = 400, PX_PER_UNIT = 7.4, HEAD = 32;

// corners listed top-left, top-right, bottom-right, bottom-left as seen from outside the face
function faceCorners(e) {
  const [a, b, d] = e.from, [A, B, D] = e.to;
  return {
    north: {n: [0, 0, -1], p: [[A, B, d], [a, B, d], [a, b, d], [A, b, d]]},
    south: {n: [0, 0, 1], p: [[a, B, D], [A, B, D], [A, b, D], [a, b, D]]},
    east: {n: [1, 0, 0], p: [[A, B, D], [A, B, d], [A, b, d], [A, b, D]]},
    west: {n: [-1, 0, 0], p: [[a, B, d], [a, B, D], [a, b, D], [a, b, d]]},
    up: {n: [0, 1, 0], p: [[a, B, d], [A, B, d], [A, B, D], [a, B, D]]},
    down: {n: [0, -1, 0], p: [[a, b, D], [A, b, D], [A, b, d], [a, b, d]]},
  };
}

function renderFrame(time) {
  const W = CELL_W * SS, H = CELL_H * SS, px = Buffer.alloc(W * H * 4), depth = new Float64Array(W * H).fill(-Infinity);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const c = ((Math.floor(x / (24 * SS)) + Math.floor(y / (24 * SS))) % 2) ? 37 : 40; px.set([c, c + 3, c + 4, 255], (y * W + x) * 4);
  }
  const pose = sample(time), cache = new Map(), scale = PX_PER_UNIT * SS;
  const light = norm([-3, 7, -5]);
  for (const e of m.elements) {
    const w = worldOf(owner.get(e.uuid), pose, cache), rel = eulerZYX(e.rotation ?? [0, 0, 0]), eo = e.origin;
    const toWorld = q => add(w.p, mv(w.m, add(mv(rel, sub(q, eo)), eo)));
    for (const [name, f] of Object.entries(faceCorners(e))) {
      const face = e.faces[name]; if (!face || face.texture == null) continue;
      const nrm = norm(mv(w.m, mv(rel, f.n))); if (dot(nrm, cam) <= 0) continue;
      const tex = textures[face.texture], lit = tex.emissive ? 1 : 0.7 + 0.3 * Math.max(0, dot(nrm, light));
      const [u0, v0, u1, v1] = face.uv, base = [[u0, v0], [u1, v0], [u1, v1], [u0, v1]], k = ((face.rotation ?? 0) / 90) | 0;
      const pts = f.p.map((p, i) => {
        const q = sub(toWorld(p), CENTER), uv = base[((i - k) % 4 + 4) % 4];
        return [W / 2 + dot(q, right) * scale, H / 2 - dot(q, up) * scale, dot(q, cam), uv[0] * tex.w / tex.uw, uv[1] * tex.h / tex.uh];
      });
      for (const ids of [[0, 1, 2], [0, 2, 3]]) {
        const [p, q, r] = ids.map(i => pts[i]);
        const den = (q[1] - r[1]) * (p[0] - r[0]) + (r[0] - q[0]) * (p[1] - r[1]); if (Math.abs(den) < 1e-9) continue;
        const x0 = Math.max(0, Math.floor(Math.min(p[0], q[0], r[0]))), x1 = Math.min(W - 1, Math.ceil(Math.max(p[0], q[0], r[0])));
        const y0 = Math.max(0, Math.floor(Math.min(p[1], q[1], r[1]))), y1 = Math.min(H - 1, Math.ceil(Math.max(p[1], q[1], r[1])));
        for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) {
          const s = ((q[1] - r[1]) * (x + .5 - r[0]) + (r[0] - q[0]) * (y + .5 - r[1])) / den,
                t = ((r[1] - p[1]) * (x + .5 - r[0]) + (p[0] - r[0]) * (y + .5 - r[1])) / den, c = 1 - s - t;
          if (s < -1e-6 || t < -1e-6 || c < -1e-6) continue;
          const z = s * p[2] + t * q[2] + c * r[2], idx = y * W + x; if (z <= depth[idx]) continue;
          const tx = Math.max(0, Math.min(tex.w - 1, Math.floor(s * p[3] + t * q[3] + c * r[3])));
          const ty = Math.max(0, Math.min(tex.h - 1, Math.floor(s * p[4] + t * q[4] + c * r[4])));
          const o = (ty * tex.w + tx) * 4; if (tex.data[o + 3] < 128) continue;
          depth[idx] = z; px.set([Math.min(255, Math.round(tex.data[o] * lit)), Math.min(255, Math.round(tex.data[o + 1] * lit)), Math.min(255, Math.round(tex.data[o + 2] * lit)), 255], idx * 4);
        }
      }
    }
  }
  return sharp(px, {raw: {width: W, height: H, channels: 4}}).resize(CELL_W, CELL_H, {kernel: 'lanczos3'}).png().toBuffer();
}

const cols = Number(colsArg), rows = Math.ceil(times.length / cols);
const comps = [];
for (const [i, t] of times.entries()) {
  const x = (i % cols) * CELL_W, y = Math.floor(i / cols) * (CELL_H + HEAD);
  comps.push({input: await renderFrame(t), left: x, top: y + HEAD});
  comps.push({input: Buffer.from(`<svg width="${CELL_W}" height="${HEAD}"><text x="12" y="22" fill="#dcd5c8" font-family="Arial" font-size="17">${t.toFixed(2)} s${t >= clip.length - 1e-6 ? '  (loop seam)' : ''}</text></svg>`), left: x, top: y});
}
await sharp({create: {width: cols * CELL_W, height: rows * (CELL_H + HEAD), channels: 4, background: {r: 35, g: 40, b: 48, alpha: 1}}})
  .composite(comps).png().toFile(outPath);
console.log(`Saved ${outPath} (${times.length} frames, offline preview, view ${viewArg})`);
