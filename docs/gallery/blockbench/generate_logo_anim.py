"""Builds the Blockbench logo scene of Copycat Replace with the "replace" animation.

Usage: python -I generate_logo_anim.py <create.jar> <minecraft.jar> <out.bbmodel>
(also writes pov_camera.json next to the model: the camera the scene is framed for)

The scene is the in-game first-person view (Minecraft 1.21.1, FOV 70, no arms): the Create copycat
frame sits in front of the player, the wrench is held in the right hand and a wool block in the
left. Hand items are placed exactly like ItemInHandRenderer does it (arm offset (+-0.56, -0.52, -0.72)
plus the model's own firstperson display transform), expressed in world space for a camera at
CAM_POS looking CAM_PITCH degrees upwards along +Z. Open the project from that camera (see
pov_camera.json) to get the player's view.

The animation `replace` (3.85 s) is a seamless loop: tap -> copycat becomes blue, the off-hand block
spins into lime (already during the wrench's rebound), tap -> copycat becomes lime, the lime block
spins away, tap -> the skins shrink back and the copycat is empty again, a new blue block drops
into the hand once it is empty and the wrench's wind-up of the next cycle follows.
Easing is baked into linear keyframes (one per frame) so it plays identically in every Blockbench
format and exporter. The break particles reuse the applied wool texture. The textures come from the
given jars and are embedded in the project (not meant for git: Create/Minecraft assets).
"""
import base64, json, math, os, struct, sys, uuid, zipfile, zlib

FPS = 60
LENGTH = 3.85

# ---------------------------------------------------------------- scene layout
COPY_C = (0.0, 26.0, 0.0)            # copycat centre (16^3 cube)
FRAME, SKIN_BLUE, SKIN_LIME = 8.0, 8.1, 8.2   # half sizes: skins envelop the frame
BLOCK_BOTTOM = COPY_C[1] - FRAME

# camera: measured on an in-game capture (copycat 1.7 blocks away, 78 px below the crosshair, horizon
# 130 px below it -> looking ~10 degrees up). 16 world units = 1 block.
FOV_V, ASPECT = 70.0, 1920 / 1000
CAM_PITCH = 10.0
COPY_FRONT_DEPTH, COPY_FRONT_DROP = 24.0, 3.0   # front face of the copycat: distance along the view axis / below the crosshair

# hand items (camera space: x right, y up, z towards the viewer; blocks -> x16 units)
ARM_R, ARM_L = (0.56, -0.52, -0.72), (-0.56, -0.52, -0.72)
WRENCH_DISPLAY = dict(rotation=(-4.5, 100.25, 10.0), translation=(1.0, 4.0, 1.0), scale=1.0)   # create:item/wrench firstperson_righthand
BLOCK_DISPLAY = dict(rotation=(0.0, -225.0, 0.0), translation=(0.0, 0.0, 0.0), scale=0.4)       # block/block firstperson_lefthand (y mirrored for the left hand)
WRENCH_TIP = (9.0, 15.0, 8.0)         # item-space point that touches the copycat (top of the head)
WRENCH_BASE = (8.0, 0.0, 8.0)         # item-space pivot: base of the handle
HIT_X, HIT_GAP = -3.0, 0.3     # the tip lands on the front face, right of centre; y follows from the reach of the wrench

# ---------------------------------------------------------------- 3x3 matrices (row major, lists of 9)
def mm(a, b): return [sum(a[i * 3 + k] * b[k * 3 + j] for k in range(3)) for i in range(3) for j in range(3)]
def mv(a, v): return [sum(a[i * 3 + k] * v[k] for k in range(3)) for i in range(3)]
def rot_x(d):
    c, s_ = math.cos(math.radians(d)), math.sin(math.radians(d)); return [1, 0, 0, 0, c, -s_, 0, s_, c]
def rot_y(d):
    c, s_ = math.cos(math.radians(d)), math.sin(math.radians(d)); return [c, 0, s_, 0, 1, 0, -s_, 0, c]
def rot_z(d):
    c, s_ = math.cos(math.radians(d)), math.sin(math.radians(d)); return [c, -s_, 0, s_, c, 0, 0, 0, 1]
def vadd(a, b): return [x + y for x, y in zip(a, b)]
def vsub(a, b): return [x - y for x, y in zip(a, b)]
def vscale(a, k): return [x * k for x in a]

R_PITCH = rot_x(-CAM_PITCH)          # camera orientation: forward (0, sin p, cos p)
R_FLIP = rot_y(180.0)                # Minecraft camera space (+z towards the viewer) -> scene (+Z away from the viewer)
FWD, UP = mv(R_PITCH, [0, 0, 1]), mv(R_PITCH, [0, 1, 0])
CAM_POS = vsub(vadd([COPY_C[0], COPY_C[1], COPY_C[2] - FRAME], vscale(UP, COPY_FRONT_DROP)), vscale(FWD, COPY_FRONT_DEPTH))
def cam_to_world(c): return vadd(CAM_POS, mv(R_PITCH, mv(R_FLIP, c)))

def display_matrix(rotation, left=False):
    """ItemTransform.apply: rotationXYZ(rx, ry, rz) = Rx * Ry * Rz (y and z mirrored for the left hand)."""
    rx, ry, rz = rotation
    if left: ry, rz = -ry, -rz
    return mm(rot_x(rx), mm(rot_y(ry), rot_z(rz)))

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

def texture(name, png, tid, emissive=False, size=16):
    return {"path": "", "name": name, "folder": "", "namespace": "", "id": str(tid), "group": "",
            "width": size, "height": size, "uv_width": size, "uv_height": size, "particle": False,
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

# ---------------------------------------------------------------- wrench (Create item model)
def wrench_elements(wrench_json, centre, scale, tex):
    """create:item/wrench as cubes around `centre` (where the model's centre (8,8,8) lands), unrotated."""
    item = json.loads(wrench_json)
    to_local = lambda p: [centre[i] + (p[i] - 8.0) * scale for i in range(3)]
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
        angle = e["rotation"]["angle"]
        out.append(cube(e["name"], to_local(e["from"]), to_local(e["to"]), faces,
                        origin=to_local(e["rotation"]["origin"]), rotation=[0, angle, 0] if angle else None))
    return out

def nest(name, origin, rotations, children):
    """Chain of groups around the same origin: rotations are listed outermost (applied last) first."""
    for i, rot in reversed(list(enumerate(rotations))):
        children = [group(f'{name}_{i}', list(origin), children, rotation=list(rot))]
    return children[0]

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
    # Blockbench plays animated rotations with x and y inverted (BoneAnimator.displayRotation: x -=, y -=,
    # z +=), so the standard-axis values built above are stored with those two signs flipped.
    sign = (-1, -1, 1) if channel == 'rotation' else (1, 1, 1)
    return [{"channel": channel, "data_points": [{"x": fmt(sign[0] * v[0]), "y": fmt(sign[1] * v[1]), "z": fmt(sign[2] * v[2])}],
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

    # --- hand items, placed like ItemInHandRenderer: arm offset + display transform (centre of the model
    # at the offset), then camera pitch/position
    w_disp = display_matrix(WRENCH_DISPLAY['rotation'])
    w_centre = cam_to_world(vadd(vscale(ARM_R, 16), WRENCH_DISPLAY['translation']))
    w_rot = mm(R_PITCH, mm(R_FLIP, w_disp))                          # model space -> scene
    w_scale = WRENCH_DISPLAY['scale']
    to_scene = lambda p: vadd(w_centre, mv(w_rot, vscale(vsub(p, [8, 8, 8]), w_scale)))
    pivot, tip_rest = to_scene(WRENCH_BASE), to_scene(WRENCH_TIP)

    OFF_C = cam_to_world(vscale(ARM_L, 16))
    OFF_HALF = 8 * BLOCK_DISPLAY['scale']

    # --- swing: pure rotation about the pivot (pitch x, then roll z). The tip travels on a sphere around the
    # pivot, so the contact height on the front face is where that sphere cuts it; then solve the angles.
    reach = math.dist(tip_rest, pivot)
    hit_z = COPY_C[2] - FRAME - HIT_GAP
    dy2 = reach ** 2 - (HIT_X - pivot[0]) ** 2 - (hit_z - pivot[2]) ** 2
    assert dy2 > 0, 'wrench cannot reach the copycat: move it closer'
    HIT = (HIT_X, pivot[1] + math.sqrt(dy2), hit_z)
    assert BLOCK_BOTTOM < HIT[1] < BLOCK_BOTTOM + 10, ('contact point off the lower part of the face', HIT)
    err = lambda ax, az: sum(x * x for x in vsub(vadd(pivot, mv(mm(rot_z(az), rot_x(ax)), vsub(tip_rest, pivot))), HIT))
    _, ax, az = min((err(ax, az), ax, az) for ax in range(0, 91, 2) for az in range(-70, 71, 2))
    for step in (0.5, 0.1, 0.02):
        improved = True
        while improved:
            improved = False
            for dx, dz in ((step, 0), (-step, 0), (0, step), (0, -step)):
                if err(ax + dx, az + dz) < err(ax, az): ax, az, improved = ax + dx, az + dz, True
    assert err(ax, az) < 0.01, ('rotation does not reach the contact point', ax, az, err(ax, az))
    STRIKE_ROT, thrust = (ax, 0.0, az), [0.0, 0.0, 0.0]

    # --- geometry
    elements = []
    def add(el): elements.append(el); return el["uuid"]
    frame = add(centered('frame', COPY_C, FRAME, full_faces(0)))
    skin_blue = add(centered('skin_blue', COPY_C, SKIN_BLUE, full_faces(1)))
    skin_lime = add(centered('skin_lime', COPY_C, SKIN_LIME, full_faces(2)))
    off_blue = add(centered('offhand_blue', OFF_C, OFF_HALF, full_faces(1)))
    off_lime = add(centered('offhand_lime', OFF_C, OFF_HALF + 0.05, full_faces(2)))
    wrench_ids = [add(e) for e in wrench_elements(wrench_json, w_centre, w_scale, 3)]

    hit = list(HIT)
    spark_count, sparks = 8, []
    for tex, label in ((1, 'blue'), (2, 'lime')):
        for i in range(spark_count):
            size = 0.8 if i % 2 else 1.1
            c = [hit[0], hit[1], hit[2] - 0.6]
            u, v = (i * 5) % 12, (i * 7 + 3) % 12         # a different 4x4 patch of the wool per chunk
            sparks.append((label, i, add(centered(f'spark_{label}_{i}', c, size, patch_faces(tex, u, v))), c))

    # --- hierarchy
    skin_origin = [hit[0], hit[1], COPY_C[2]]
    g_skin_blue = group('skin_blue', skin_origin, [skin_blue])
    g_skin_lime = group('skin_lime', skin_origin, [skin_lime])
    g_copycat = group('copycat', list(COPY_C), [frame, g_skin_blue, g_skin_lime])
    # off-hand: camera pitch (static) > animated hop/squash > display yaw (static) > blue / lime blocks
    g_off_blue = group('offhand_blue', list(OFF_C), [off_blue])
    g_off_lime = group('offhand_lime', list(OFF_C), [off_lime])
    g_off_yaw = nest('offhand_pose', OFF_C, [(0, 180, 0), (0, -BLOCK_DISPLAY['rotation'][1], 0)], [g_off_blue, g_off_lime])
    g_offhand = group('offhand', list(OFF_C), [g_off_yaw])
    g_off_cam = group('offhand_camera', list(OFF_C), [g_offhand], rotation=[-CAM_PITCH, 0, 0])
    # wrench: animated swing about the base of the handle > static display transform > cubes
    wr = WRENCH_DISPLAY['rotation']
    g_orient = nest('wrench_pose', w_centre, [(-CAM_PITCH, 0, 0), (0, 180, 0), (wr[0], 0, 0), (0, wr[1], 0), (0, 0, wr[2])], wrench_ids)
    g_wrench = group('wrench', list(pivot), [g_orient])
    g_sparks = []
    for label, i, uid_, c in sparks:
        g_sparks.append((label, i, group(f'spark_{label}_{i}', c, [uid_])))
    outliner = [g_copycat, g_off_cam, g_wrench,
                group('sparks_blue', hit[:], [g for l, _, g in g_sparks if l == 'blue']),
                group('sparks_lime', hit[:], [g for l, _, g in g_sparks if l == 'lime'])]

    # --- animation (times in seconds). One cycle = tap -> blue, swap to lime, tap -> lime, the lime
    # block spins away, tap -> back to the empty copycat, a new blue block drops into the hand.
    # Everything overlaps: the swap starts right after the first impact, the wrench is already
    # winding up for the second tap during it, and the new block drops in while the wrench settles.
    # The clip ends where it starts, so the loop has no seam.
    I1, I2, I3 = 0.55, 1.35, 2.65     # impacts: apply blue, apply lime, remove everything
    SWAP = 0.37                       # off-hand flip / vanish / drop-in duration
    BK = OFF_HALF / 5.0               # hop heights were tuned for a 10-unit block
    anim = {}                         # group uuid -> {channel: Track}
    def tr(g, channel, v0):
        t = Track(v0); anim.setdefault(g['uuid'], {'name': g['name'], 'ch': {}})['ch'][channel] = t; return t

    # wrench: pose = (rotation about the pivot, translation); strike and rebound only rotate
    w_rot_t, w_pos_t = tr(g_wrench, 'rotation', V0), tr(g_wrench, 'position', V0)
    pose = lambda k_rot, k_pos: (vscale(STRIKE_ROT, k_rot), vscale(thrust, k_pos))
    WIND, STRIKE, REBOUND, REST = ((-1.0, 0.0, 3.0), V0), pose(1.0, 0), pose(0.33, 0), (V0, V0)
    def W(t1, p, ease, t0=None):
        w_rot_t.to(t1, tuple(p[0]), ease, t0=t0); w_pos_t.to(t1, tuple(p[1]), ease, t0=t0)
    W(I1 - 0.1, WIND, ease_in_out_cubic, t0=I1 - 0.45)                 # wind-up
    W(I1, STRIKE, ease_in_quad)                                        # strike
    W(I1 + 0.15, REBOUND, ease_out_cubic)                              # rebound
    W(I2 - 0.1, WIND, ease_in_out_cubic)                               # wind-up straight out of the rebound
    W(I2, STRIKE, ease_in_quad)
    W(I2 + 0.15, REBOUND, ease_out_cubic)
    W(I2 + 0.65, REST, ease_out_back)                                  # settle while the lime block vanishes
    W(I3 - 0.1, WIND, ease_in_out_cubic, t0=I3 - 0.45)                 # third tap
    W(I3, STRIKE, ease_in_quad)
    W(I3 + 0.15, REBOUND, ease_out_cubic)
    W(I3 + 0.65, REST, ease_out_back)

    # copycat reaction: squash & stretch, wobble and a small shove away from the player at every impact
    c_scale, c_rot, c_pos = tr(g_copycat, 'scale', V1), tr(g_copycat, 'rotation', V0), tr(g_copycat, 'position', V0)
    for impact in (I1, I2, I3):
        c_scale.chain(impact, [(0.07, (1.07, 0.88, 1.07)), (0.12, (0.97, 1.06, 0.97)),
                               (0.13, (1.015, 0.99, 1.015)), (0.15, V1)])
        c_rot.chain(impact, [(0.09, (0, 0, -3.5)), (0.14, (0, 0, 1.8)), (0.14, (0, 0, -0.6)), (0.15, V0)])
        c_pos.chain(impact, [(0.07, (0, 0.4, 1.0)), (0.2, (0, -0.1, -0.2)), (0.2, V0)])

    # skins: grow out of the tapped point; the third tap shrinks lime, then blue, back into it
    sb = tr(g_skin_blue, 'scale', V0)
    sb.to(I1 + 0.35, V1, ease_out_back, t0=I1).to(I3 + 0.08, V1).to(I3 + 0.38, V0, ease_in_back)
    sl = tr(g_skin_lime, 'scale', V0)
    sl.to(I2 + 0.35, V1, ease_out_back, t0=I2).to(I3, V1).to(I3 + 0.32, V0, ease_in_back)

    # off-hand block: blue -> lime flip after the first impact, the lime block spins away before the
    # third tap, and a fresh blue block drops in right after it
    sw1 = I1 + 0.05
    van = I2 + 0.45                    # vanish: spin and shrink to nothing, rising a little
    sw3 = I3 + 0.5                     # blue drops back in only once the copycat is empty again
    sw1_mid, sw1_end, van_end, sw3_end = sw1 + SWAP / 2, sw1 + SWAP, van + SWAP, sw3 + SWAP
    ob_rot, ob_scale = tr(g_off_blue, 'rotation', V0), tr(g_off_blue, 'scale', V1)
    ol_rot, ol_scale = tr(g_off_lime, 'rotation', (0, -180, 0)), tr(g_off_lime, 'scale', V0)
    ob_rot.to(sw1_mid, (0, 180, 0), ease_in_cubic, t0=sw1).to(sw3, (0, 180, 0)).to(sw3_end, (0, 360, 0), ease_out_back)
    ob_scale.to(sw1_mid, V0, ease_in_cubic, t0=sw1).to(sw3, V0).to(sw3_end, V1, ease_out_back)
    ol_rot.to(sw1_mid, (0, -180, 0), linear, t0=sw1).to(sw1_end, V0, ease_out_back).to(van, V0).to(van_end, (0, 540, 0), ease_in_cubic)
    ol_scale.to(sw1_mid, V0, linear, t0=sw1).to(sw1_end, V1, ease_out_back).to(van, V1).to(van_end, V0, ease_in_quad)
    hop, squash = tr(g_offhand, 'position', V0), tr(g_offhand, 'scale', V1)
    hop.to(sw1_mid, (0, 4.0 * BK, 0), ease_out_cubic, t0=sw1).to(sw1_end, V0, ease_in_quad)
    squash.to(sw1 + 0.06, (0.94, 1.1, 0.94), ease_out_quad, t0=sw1).to(sw1_mid, (0.97, 1.04, 0.97)).to(sw1_end, (1.08, 0.88, 1.08), ease_in_quad)
    squash.chain(sw1_end, [(0.07, (0.98, 1.04, 0.98)), (0.08, V1)])
    hop.to(van_end, (0, 5.0 * BK, 0), ease_out_quad, t0=van)           # the vanishing block floats up...
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
    camera = {"position": [round(v, 3) for v in CAM_POS],
              "target": [round(v, 3) for v in vadd(CAM_POS, vscale(FWD, 30))],
              "pitch_deg": CAM_PITCH, "fov_vertical_deg": FOV_V, "aspect": round(ASPECT, 3)}
    with open(os.path.join(os.path.dirname(os.path.abspath(out)), 'pov_camera.json'), 'w') as f: json.dump(camera, f, indent=2)
    print('wrote', out, '| camera', camera['position'], '| strike rot', [round(v, 1) for v in STRIKE_ROT], 'push', [round(v, 1) for v in thrust],
          '| keyframes', sum(len(d['keyframes']) for d in anim_groups.values()))

if __name__ == '__main__':
    main(*sys.argv[1:4])
