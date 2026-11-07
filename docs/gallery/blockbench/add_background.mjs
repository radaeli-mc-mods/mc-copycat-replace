// Puts an image behind rendered frames, like adding a new layer under the render in Photoshop.
// The background is the bottom layer: it is scaled to cover the frame, then the frame is composited
// over it with its alpha, so the transparent parts of the render (and the cavities of the copycat)
// show the background, while opaque pixels keep the model untouched.
//
// Usage: node add_background.mjs <background.png> <frame.png|frames dir> [output.png|output dir]
// A directory input processes every *.png in it. Default output: <name>_bg.png next to the frame
// (for a directory: <dir>_bg next to it). An input is never overwritten.
import fs from 'node:fs/promises';
import path from 'node:path';
import {createRequire} from 'node:module';
const require = createRequire(import.meta.url);
let sharp;
try { sharp = require('sharp'); }
catch { sharp = require('C:/Users/lucca/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp'); }

const [bgPath, inputPath, outArg] = process.argv.slice(2);
if (!bgPath || !inputPath) { console.error('usage: node add_background.mjs <background.png> <frame.png|frames dir> [output]'); process.exit(1); }

// [input, output] pairs
const pairs = [];
if ((await fs.stat(inputPath)).isDirectory()) {
  const dir = path.resolve(inputPath);
  const outDir = outArg ?? `${dir.replace(/[\\/]+$/, '')}_bg`;
  await fs.mkdir(outDir, {recursive: true});
  const names = (await fs.readdir(dir)).filter(n => n.toLowerCase().endsWith('.png')).sort();
  for (const name of names) pairs.push([path.join(dir, name), path.join(outDir, name)]);
} else {
  const parsed = path.parse(path.resolve(inputPath));
  pairs.push([path.resolve(inputPath), outArg ? path.resolve(outArg) : path.join(parsed.dir, `${parsed.name}_bg.png`)]);
}
if (pairs.length === 0) { console.error(`no .png frames in ${inputPath}`); process.exit(1); }

const bg = await fs.readFile(bgPath);
for (const [input, output] of pairs) {
  if (path.resolve(input) === path.resolve(output)) throw new Error(`refusing to overwrite ${input}`);
  const frame = await fs.readFile(input);
  const {width, height} = await sharp(frame).metadata();
  // Bottom layer: the background, scaled to cover the whole frame (like a Photoshop fill).
  const back = await sharp(bg).resize(width, height, {fit: 'cover', position: 'centre'}).ensureAlpha().png().toBuffer();
  // Top layer: the render. Its alpha decides where the background shows through.
  await sharp(back).composite([{input: frame, blend: 'over'}]).png().toFile(output);
  console.log(output);
}
