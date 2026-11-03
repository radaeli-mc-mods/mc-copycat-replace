"""Builds the Blockbench logo scene of Copycat Replace with the "replace" animation.

Usage: python -I generate_logo_anim.py <create.jar> <minecraft.jar> <out.bbmodel>

Scene (seen from the north, which is Blockbench's "Initial Angle": +X is on the viewer's left):
  - centre: Create copycat frame, with two "skins" (blue / lime wool) that grow out of the
    tapped point to show the copied block;
  - viewer's left: the off-hand block (blue wool, swapped for lime wool halfway through);
  - viewer's right: the Create wrench, pivot fixed at the base of the handle.

The animation `replace` (3.85 s) is a seamless loop: tap -> copycat becomes blue, the off-hand block
spins into lime (already during the wrench's rebound), tap -> copycat becomes lime, the lime block
spins away, tap -> the skins shrink back and the copycat is empty again, a new blue block drops
into the hand once it is empty and the wrench's wind-up of the next cycle follows.
Easing is baked into linear
keyframes (one per frame) so it plays identically in every Blockbench format and exporter.
The break particles reuse the applied wool texture. The textures come from the given jars and are embedded in the project (not meant for git:
Create/Minecraft assets).
"""
import base64, json, math, struct, sys, uuid, zipfile, zlib

FPS = 60
LENGTH = 3.85

# ---------------------------------------------------------------- scene layout
COPY_C = (0.0, 26.0, 0.0)            # copycat centre (16^3 cube)
FRAME, SKIN_BLUE, SKIN_LIME = 8.0, 8.1, 8.2   # half sizes: skins envelop the frame
OFF_C, OFF_HALF = (14.0, 6.0, -10.0), 5.0     # off-hand block
WRENCH_K = 1.35                       # wrench item scale
WRENCH_TIP = (1.0, 15.0)              # item-space point that touches the copycat (x offset from pivot, y)
HIT_X = -4.5                          # where under the copycat the wrench lands
REST, STRIKE = 20.0, -4.0             # wrench angle (deg) at rest / at impact; + tilts head to viewer's right
BLOCK_BOTTOM = COPY_C[1] - FRAME

# ---------------------------------------------------------------- helpers
def uid(): return str(uuid.uuid4())

def fmt(v):
    v = round(v, 4)
    return str(int(v)) if v == int(v) else str(v)

def png_bytes(w, h, rgba):
    def chunk(kind, body):
        c = kind + body
        return struct.pack('>I', len(body)) + c + struct.pack('>I', zlib.crc32(c) & 0xFFFFFFFF)
    raw = b''.join(b'\x00' + bytes(rgba[y * w * 4:(y + 1) * w * 4]) for y in range(h))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

def spark_png(core, edge):
    """16x16 star-ish spark: bright core with a softer rim, so it reads at 1-2 blocks wide."""
    px = bytearray()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            px += bytes(core if d < 4 else edge)
    return png_bytes(16, 16, px)

def data_uri(png): return 'data:image/png;base64,' + base64.b64encode(png).decode()

def texture(name, png, tid, emissive=False):
    return {"path": "", "name": name, "folder": "", "namespace": "", "id": str(tid), "group": "",
            "width": 16, "height": 16, "uv_width": 16, "uv_height": 16, "particle": False,
            "use_as_default": False, "layers_enabled": False, "sync_to_project": "",
            "render_mode": "emissive" if emissive else "default", "render_sides": "auto", "frame_time": 1,
            "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True,
            "internal": True, "saved": False, "uuid": uid(), "source": data_uri(png)}

def full_faces(tex):
    return {f: {"uv": [0, 0, 16, 16], "texture": tex} for f in ("north", "east", "south", "west", "up", "down")}

def patch_faces(tex, u, v, size=4):
    """Every face shows the same size x size patch of the texture: a vanilla-style break particle."""
    return {f: {"uv": [u, v, u + size, v + size], "texture": tex} for f in ("north", "east", "south", "west", "up", "down")}

def cube(name, frm, to, faces, origin=None, rotation=None):
    el = {"name": name, "box_uv": False, "rescale": False, "locked": False, "light_emission": 0,
          "render_order": "default", "allow_mirror_modeling": True, "from": frm, "to": to, "autouv": 0,
          "color": 0, "origin": origin or [(a + b) / 2 for a, b in zip(frm, to)], "faces": faces,
          "type": "cube", "uuid": uid()}
    if rotation: el["rotation"] = rotation
    return el

def centered(name, c, half, faces):
    return cube(name, [c[i] - half for i in range(3)], [c[i] + half for i in range(3)], faces)

def group(name, origin, children, rotation=None):
    g = {"name": name, "origin": origin, "color": 0, "uuid": uid(), "export": True, "mirror_uv": False,
         "isOpen": True, "locked": False, "visibility": True, "autouv": 0, "children": children}
    if rotation: g["rotation"] = rotation
    return g

# ---------------------------------------------------------------- easing
def ease_out_quad(t): return 1 - (1 - t) ** 2
def ease_in_quad(t): return t * t
def ease_in_cubic(t): return t ** 3
def ease_out_cubic(t): return 1 - (1 - t) ** 3
def ease_in_out_cubic(t): return 4 * t ** 3 if t < 0.5 else 1 - (-2 * t + 2) ** 3 / 2
def ease_out_back(t, s=1.70158): return 1 + (s + 1) * (t - 1) ** 3 + s * (t - 1) ** 2
def ease_in_back(t, s=1.70158): return (s + 1) * t ** 3 - s * t ** 2
def linear(t): return t

# ---------------------------------------------------------------- wrench (Create item model, scaled)
def wrench_elements(wrench_json, pivot_world, tex):
    """Converts create:item/wrench to cubes in world space; pivot_world = base of the handle."""
    item = json.loads(wrench_json)
    px, pz = 8.0, 8.0                  # item-space pivot: base of the handle
    k = WRENCH_K
    to_world = lambda p: [pivot_world[0] + (p[0] - px) * k, pivot_world[1] + p[1] * k,
                          pivot_world[2] + (p[2] - pz) * k]
    out = []
    for e in item["elements"]:
        faces = {}
        for side in ("north", "east", "south", "west", "up", "down"):
            f = e["faces"].get(side)
            if f:
                faces[side] = {"uv": f["uv"], "texture": tex}
                if f.get("rotation"): faces[side]["rotation"] = f["rotation"]
            else:
                faces[side] = {"uv": [0, 0, 0, 0], "texture": None}
        origin = to_world(e["rotation"]["origin"])
        angle = e["rotation"]["angle"]
        out.append(cube(e["name"], to_world(e["from"]), to_world(e["to"]), faces, origin=origin,
                        rotation=[0, angle, 0] if angle else None))
    return out

# ---------------------------------------------------------------- animation baking
class Track:
    """One bone channel built from eased segments that connect end to end (holds are implicit)."""
    def __init__(self, v0): self.keys = {0: v0}; self.t = 0.0; self.v = v0

    def to(self, t1, v1, ease=linear, t0=None):
        t0 = self.t if t0 is None else t0
        if t0 > self.t + 1e-9:                         # hold until t0
            self.keys[round(t0 * FPS)] = self.v
        f0, f1 = round(t0 * FPS), round(t1 * FPS)
        for f in range(f0, f1 + 1):
            a = ease((f - f0) / max(1, f1 - f0))
            self.keys[f] = tuple(self.v[i] + (v1[i] - self.v[i]) * a for i in range(3))
        self.t, self.v = t1, v1
        return self

    def chain(self, t0, steps, ease=ease_out_quad):    # steps: [(duration, value), ...]
        t = t0
        for dur, v in steps:
            self.to(t + dur, v, ease, t0=t); t += dur
        return self

def keyframes(channel, track):
    end = round(LENGTH * FPS)
    keys = dict(track.keys); keys.setdefault(end, track.v)
    return [{"channel": channel, "data_points": [{"x": fmt(v[0]), "y": fmt(v[1]), "z": fmt(v[2])}],
             "uuid": uid(), "time": round(f / FPS, 4), "color": -1, "interpolation": "linear"}
            for f, v in sorted(keys.items())]

V0, V1 = (0, 0, 0), (1, 1, 1)
def s(v): return (v, v, v)

def main(create_jar, mc_jar, out):
    with zipfile.ZipFile(create_jar) as z:
        copycat_png = z.read('assets/create/textures/block/copycat_base.png')
        wrench_png = z.read('assets/create/textures/item/wrench.png')
        wrench_json = z.read('assets/create/models/item/wrench/item.json').decode()
    with zipfile.ZipFile(mc_jar) as z:
        blue_png = z.read('assets/minecraft/textures/block/blue_wool.png')
        lime_png = z.read('assets/minecraft/textures/block/lime_wool.png')
    textures = [texture('copycat_base.png', copycat_png, 0), texture('blue_wool.png', blue_png, 1),
                texture('lime_wool.png', lime_png, 2), texture('wrench.png', wrench_png, 3)]

    # --- wrench pivot, solved so the tip lands on HIT_X at the underside of the copycat at STRIKE
    ox, oy = WRENCH_TIP[0] * WRENCH_K, WRENCH_TIP[1] * WRENCH_K
    a = math.radians(STRIKE)
    rx, ry = ox * math.cos(a) - oy * math.sin(a), ox * math.sin(a) + oy * math.cos(a)
    pivot = [HIT_X - rx, BLOCK_BOTTOM - 0.3 - ry, 0.0]
    hit = [HIT_X, BLOCK_BOTTOM, 0.0]

    # --- geometry
    elements = []
    def add(el): elements.append(el); return el["uuid"]
    frame = add(centered('frame', COPY_C, FRAME, full_faces(0)))
    skin_blue = add(centered('skin_blue', COPY_C, SKIN_BLUE, full_faces(1)))
    skin_lime = add(centered('skin_lime', COPY_C, SKIN_LIME, full_faces(2)))
    off_blue = add(centered('offhand_blue', OFF_C, OFF_HALF, full_faces(1)))
    off_lime = add(centered('offhand_lime', OFF_C, OFF_HALF + 0.05, full_faces(2)))
    wrench_ids = [add(e) for e in wrench_elements(wrench_json, pivot, 3)]

    spark_count, sparks = 8, []
    for tex, label in ((1, 'blue'), (2, 'lime')):
        for i in range(spark_count):
            size = 0.6 if i % 2 else 0.85
            c = [hit[0], hit[1], -9.0]
            u, v = (i * 5) % 12, (i * 7 + 3) % 12         # a different 4x4 patch of the wool per chunk
            sparks.append((label, i, add(centered(f'spark_{label}_{i}', c, size, patch_faces(tex, u, v))), c))

    # --- hierarchy
    g_skin_blue = group('skin_blue', hit[:], [skin_blue])
    g_skin_lime = group('skin_lime', hit[:], [skin_lime])
    g_copycat = group('copycat', list(COPY_C), [frame, g_skin_blue, g_skin_lime])
    g_off_blue = group('offhand_blue', list(OFF_C), [off_blue])
    g_off_lime = group('offhand_lime', list(OFF_C), [off_lime])
    g_offhand = group('offhand', list(OFF_C), [g_off_blue, g_off_lime], rotation=[0, -20, 0])
    g_wrench = group('wrench', pivot, wrench_ids, rotation=[0, 0, REST])
    g_sparks = []
    for label, i, uid_, c in sparks:
        g_sparks.append((label, i, group(f'spark_{label}_{i}', c, [uid_])))
    outliner = [g_copycat, g_offhand, g_wrench,
                group('sparks_blue', hit[:], [g for l, _, g in g_sparks if l == 'blue']),
                group('sparks_lime', hit[:], [g for l, _, g in g_sparks if l == 'lime'])]

    # --- animation (times in seconds). One cycle = tap -> blue, swap to lime, tap -> lime, the lime
    # block spins away, tap -> back to the empty copycat, a new blue block drops into the hand.
    # Everything overlaps: the swap starts right after the first impact, the wrench is already
    # winding up for the second tap during it, and the new block drops in while the wrench settles.
    # The clip ends where it starts, so the loop has no seam.
    I1, I2, I3 = 0.55, 1.35, 2.65     # impacts: apply blue, apply lime, remove everything
    SWAP = 0.37                       # off-hand flip / vanish / drop-in duration
    anim = {}                         # group uuid -> {channel: Track}
    def tr(g, channel, v0):
        t = Track(v0); anim.setdefault(g['uuid'], {'name': g['name'], 'ch': {}})['ch'][channel] = t; return t

    # wrench: angle offset from REST around Z (+ = head towards viewer's right)
    w = tr(g_wrench, 'rotation', V0)
    w.to(I1 - 0.1, (0, 0, 10), ease_in_out_cubic, t0=I1 - 0.45)       # wind-up
    w.to(I1, (0, 0, STRIKE - REST), ease_in_quad)                      # strike
    w.to(I1 + 0.15, (0, 0, -8), ease_out_cubic)                        # rebound
    w.to(I2 - 0.1, (0, 0, 10), ease_in_out_cubic)                      # wind-up straight out of the rebound
    w.to(I2, (0, 0, STRIKE - REST), ease_in_quad)
    w.to(I2 + 0.15, (0, 0, -8), ease_out_cubic)
    w.to(I2 + 0.65, V0, ease_out_back)                                 # settle while the lime block vanishes
    w.to(I3 - 0.1, (0, 0, 10), ease_in_out_cubic, t0=I3 - 0.45)        # third tap
    w.to(I3, (0, 0, STRIKE - REST), ease_in_quad)
    w.to(I3 + 0.15, (0, 0, -8), ease_out_cubic)
    w.to(I3 + 0.65, V0, ease_out_back)

    # copycat reaction: squash & stretch, wobble and a small shove upwards at every impact
    c_scale, c_rot, c_pos = tr(g_copycat, 'scale', V1), tr(g_copycat, 'rotation', V0), tr(g_copycat, 'position', V0)
    for impact in (I1, I2, I3):
        c_scale.chain(impact, [(0.07, (1.07, 0.88, 1.07)), (0.12, (0.97, 1.06, 0.97)),
                               (0.13, (1.015, 0.99, 1.015)), (0.15, V1)])
        c_rot.chain(impact, [(0.09, (0, 0, -3.5)), (0.14, (0, 0, 1.8)), (0.14, (0, 0, -0.6)), (0.15, V0)])
        c_pos.chain(impact, [(0.07, (0, 1.0, 0)), (0.2, (0, -0.2, 0)), (0.2, V0)])

    # skins: grow out of the tapped point; the third tap shrinks lime, then blue, back into it
    sb = tr(g_skin_blue, 'scale', V0)
    sb.to(I1 + 0.35, V1, ease_out_back, t0=I1).to(I3 + 0.08, V1).to(I3 + 0.38, V0, ease_in_back)
    sl = tr(g_skin_lime, 'scale', V0)
    sl.to(I2 + 0.35, V1, ease_out_back, t0=I2).to(I3, V1).to(I3 + 0.32, V0, ease_in_back)

    # off-hand block: blue -> lime flip after the first impact, the lime block spins away before the
    # third tap, and a fresh blue block drops in right after it
    sw1 = I1 + 0.05
    van = I2 + 0.45                    # vanish: spin and shrink to nothing, rising a little
    sw3 = I3+ 0.5                     # blue drops back in only once the copycat is empty again
    sw1_mid, sw1_end, van_end, sw3_end = sw1 + SWAP / 2, sw1 + SWAP, van + SWAP, sw3 + SWAP
    ob_rot, ob_scale = tr(g_off_blue, 'rotation', V0), tr(g_off_blue, 'scale', V1)
    ol_rot, ol_scale = tr(g_off_lime, 'rotation', (0, -180, 0)), tr(g_off_lime, 'scale', V0)
    ob_rot.to(sw1_mid, (0, 180, 0), ease_in_cubic, t0=sw1).to(sw3, (0, 180, 0)).to(sw3_end, (0, 360, 0), ease_out_back)
    ob_scale.to(sw1_mid, V0, ease_in_cubic, t0=sw1).to(sw3, V0).to(sw3_end, V1, ease_out_back)
    ol_rot.to(sw1_mid, (0, -180, 0), linear, t0=sw1).to(sw1_end, V0, ease_out_back).to(van, V0).to(van_end, (0, 540, 0), ease_in_cubic)
    ol_scale.to(sw1_mid, V0, linear, t0=sw1).to(sw1_end, V1, ease_out_back).to(van, V1).to(van_end, V0, ease_in_quad)
    hop, squash = tr(g_offhand, 'position', V0), tr(g_offhand, 'scale', V1)
    hop.to(sw1_mid, (0, 4.0, 0), ease_out_cubic, t0=sw1).to(sw1_end, V0, ease_in_quad)
    squash.to(sw1 + 0.06, (0.94, 1.1, 0.94), ease_out_quad, t0=sw1).to(sw1_mid, (0.97, 1.04, 0.97)).to(sw1_end, (1.08, 0.88, 1.08), ease_in_quad)
    squash.chain(sw1_end, [(0.07, (0.98, 1.04, 0.98)), (0.08, V1)])
    hop.to(van_end, (0, 5.0, 0), ease_out_quad, t0=van)                # the vanishing block floats up...
    hop.to(sw3_end, V0, ease_in_quad, t0=sw3)                          # ...and the new one drops from there
    squash.to(sw3_end, (1.08, 0.88, 1.08), ease_in_quad, t0=sw3)
    squash.chain(sw3_end, [(0.07, (0.98, 1.04, 0.98)), (0.08, V1)])

    # break particles: small patches of the applied wool, bursting from the tapped point and falling
    bursts = {'blue': (I1, I3 + 0.12), 'lime': (I2, I3)}           # the 3rd tap also breaks both blocks
    for label, i, g in g_sparks:
        ang = math.radians(22.5 + 45 * i + (8 if i % 2 else -6))
        dist = 7.0 if i % 2 else 4.8
        d = (math.cos(ang) * dist, math.sin(ang) * dist, -1.5)
        p, sc, r = tr(g, 'position', V0), tr(g, 'scale', V0), tr(g, 'rotation', V0)
        for impact in bursts[label]:
            f0, f1 = round(impact * FPS), round((impact + 0.4) * FPS)
            for f in range(f0, f1 + 1):                    # out with ease-out, down with gravity
                k = (f - f0) / (f1 - f0)
                p.keys[f] = tuple(d[j] * ease_out_cubic(k) - (4.5 * k * k if j == 1 else 0) for j in range(3))
                r.keys[f] = tuple(a * ease_out_quad(k) for a in ((180, 0, 0) if i % 2 else (0, 0, 180)))
            p.t = r.t = impact + 0.4
            p.v, r.v = p.keys[f1], r.keys[f1]
            sc.to(impact, V0, t0=impact).to(impact + 0.04, V1, ease_out_quad).to(impact + 0.4, V0, ease_in_quad, t0=impact + 0.08)

    # loop check: every visible channel must end where it started (rotations modulo a full turn)
    for gid, d in anim.items():
        for channel, t in d['ch'].items():
            if d['name'].startswith('spark') and channel != 'scale': continue   # hidden at scale 0
            end = [(round(x) % 360 if channel == 'rotation' else round(x, 3)) for x in t.v]
            start = [(round(x) % 360 if channel == 'rotation' else round(x, 3)) for x in t.keys[0]]
            assert end == start, (d['name'], channel, t.v, t.keys[0])
        assert max(t.t for t in d['ch'].values()) < LENGTH, d['name']

    anim_groups = {}
    for gid, d in anim.items():
        kfs = []
        for channel, track in d['ch'].items(): kfs += keyframes(channel, track)
        anim_groups[gid] = {"name": d['name'], "type": "bone", "keyframes": kfs}

    animation = {"uuid": uid(), "name": "replace", "loop": "loop", "override": False, "length": LENGTH,
                 "snapping": FPS, "selected": True, "anim_time_update": "", "blend_weight": "",
                 "start_delay": "", "loop_delay": "", "animators": anim_groups}

    project = {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
               "name": "copycat_replace_logo", "model_identifier": "", "visible_box": [1, 1, 0],
               "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
               "unhandled_root_fields": {}, "resolution": {"width": 16, "height": 16},
               "elements": elements, "outliner": outliner, "textures": textures, "animations": [animation]}
    with open(out, 'w', encoding='utf-8') as f: json.dump(project, f, indent='\t')
    print('wrote', out, '| pivot', [round(v, 2) for v in pivot], '| keyframes',
          sum(len(d['keyframes']) for d in anim_groups.values()))

if __name__ == '__main__':
    main(*sys.argv[1:4])
