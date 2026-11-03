// Reproducible vanilla structure NBT, shared by both Ponder versions. No dependencies.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const int = n => { const b = Buffer.alloc(4); b.writeInt32BE(n); return b; };
const str = s => { const b = Buffer.from(s); const n = Buffer.alloc(2); n.writeUInt16BE(b.length); return Buffer.concat([n,b]); };
const tag = (type,name,body) => Buffer.concat([Buffer.from([type]),str(name),body]);
const list = (type,entries) => Buffer.concat([Buffer.from([type]),int(entries.length),...entries]);
const compound = tags => Buffer.concat([...tags,Buffer.from([0])]);
const blocks = [];
for (let x=0;x<5;x++) for (let z=0;z<5;z++) blocks.push([x,0,z,0]);
for (let x=1;x<=3;x++) blocks.push([x,1,2,1]);
const root=compound([
 tag(3,'DataVersion',int(3465)),
 tag(9,'size',list(3,[5,2,5].map(int))),
 tag(9,'palette',list(10,['minecraft:stone','create:copycat_step'].map(name=>compound([tag(8,'Name',str(name))])))),
 tag(9,'blocks',list(10,blocks.map(([x,y,z,state])=>compound([tag(9,'pos',list(3,[x,y,z].map(int))),tag(3,'state',int(state))])))),
 tag(9,'entities',list(10,[]))
]);
const target=path.resolve(__dirname,'../common/src/main/resources/assets/copycat_replace/ponder/material_actions.nbt');
fs.mkdirSync(path.dirname(target),{recursive:true});
fs.writeFileSync(target,zlib.gzipSync(tag(10,'',root)));
