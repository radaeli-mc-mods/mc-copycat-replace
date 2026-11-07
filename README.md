<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/logo.png" alt="Create: Copycat Replace!"></p>

<p align="center" style="text-align: center;">A quality-of-life addon for <b>Create</b></p>

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/section-titles/what_it_is.png" alt="WHAT IT IS?" width="267"></p>

Replacing a Copycat's material usually means removing it first. **Create: Copycat Replace!** lets you swap the material directly with a Create Wrench and a block in your hands.

It works with Create's Copycats and supports compatible shapes from **Create: Copycats+**, including multi-part blocks. It adds no new blocks or items.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/section-titles/what_can_it_do.png" alt="WHAT CAN IT DO?" width="375"></p>

### Replace one Copycat at a time

**Change a Copycat's material in one click.** Hold a Create Wrench and the material you want, then right-click the Copycat.

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/copycat-replace.gif" alt="Replacing a standard Copycat material" width="480"></p>

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/copycat-replace-multipart.gif" alt="Replacing one part of a multi-part Copycat" width="480"></p>

- Either hand can hold the Wrench or the material.
- Works on empty Copycats as well as decorated ones.
- Hold **Sneak** while right-clicking to use the Wrench's normal Create interaction.

### Apply, replace, or remove connected materials

**Update a group of face-connected Copycats together.** Hold the connected-action key while right-clicking a Copycat to preview and perform a bulk action.

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/copycat-connected-apply.gif" alt="Applying a material to connected Copycats" width="480"></p>

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/copycat-connected-replace.gif" alt="Replacing materials on connected Copycats" width="480"></p>

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/copycat-connected-remove.gif" alt="Removing materials from connected Copycats" width="480"></p>

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/copycat-connected-multipart-replace.gif" alt="Replacing connected multi-part Copycats" width="480"></p>

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/copycat-connected-multipart-remove.gif" alt="Removing materials from connected multi-part Copycats" width="480"></p>

- Apply a material to connected empty Copycats, replace the clicked material on matching Copycats, or remove that material from the group.
- Only face-connected blocks of the same Copycat type are included.
- Works with multi-part Copycats+ shapes too.
- The default key is **Left Alt**. Change **Bulk Action on Connected Copycats** in **Controls**.
- The preview shows the estimated number of blocks before the action.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/section-titles/how-do-i-use-it.png" alt="HOW DO I USE IT?" width="371"></p>

1. Hold a **Create Wrench** in one hand.
2. Hold the block material you want to use in the other hand.
3. Right-click a Copycat to replace its material, or hold **Left Alt** as you right-click to act on connected Copycats.
4. For a bulk removal, hold only the Wrench and **Left Alt**, then right-click the material you want to remove.

> **Heads up:** Hold **Sneak** to pass the click through to Create's normal Wrench interaction. The connected action only includes face-connected blocks of the same Copycat type.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/section-titles/can_i_config.png" alt="CAN I CONFIGURE IT?" width="479"></p>

| Option | Default | What it does |
| --- | --- | --- |
| `max_connected_blocks` | 64 | Maximum number of connected Copycat positions affected by one bulk action, from 1 to 256. Set it in `copycat_replace-server.toml` on the server. |

On Forge, the file is in `<world>/serverconfig/`. Fabric and NeoForge use `config/` by default. See the [configuration guide](https://mods.radaeli.dev/en/mods/copycat-replace/player/configuration) for per-world settings.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/section-titles/requirements.png" alt="REQUIREMENTS" width="355"></p>

- **Minecraft 1.20.1** with **Forge 47.4.0+** and **Create 6.0.8**
- **Minecraft 1.20.1** with **Fabric Loader 0.17.2+**, **Fabric API 0.92.6+1.20.1** or later for 1.20.1, and **Create Fabric 6.0.8.1 (build 1744)**
- **Minecraft 1.21.1** with **NeoForge 21.1.236+** and **Create 6.0.10**
- **Create: Copycats+** is optional. Use **3.0.10** on Fabric and **3.0.4** on Forge or NeoForge.
- Install the matching Copycat Replace version on **both the client and server**. Install Copycats+ on both sides when you want its additional shapes supported.
- Fabric servers can use **Open Parties and Claims 0.26.3** to protect Copycats in claimed chunks.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/section-titles/found_a_bug.png" alt="FOUND A BUG? HAVE AN IDEA?" width="639"></p>

[Open a GitHub issue](https://github.com/radaelilucca/mc-copycat-replace/issues) and include your Minecraft, loader, Create, and Copycat Replace versions, plus the steps to reproduce the problem. (The title image currently spells “FOUND” incorrectly.)

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-copycat-replace/master/docs/gallery/section-titles/can_i_use.png" alt="CAN I USE THIS IN A MODPACK?" width="679"></p>

**Yes.** Create: Copycat Replace! is available under the **MIT License**.

---

*For development notes, see the [technical documentation](https://github.com/radaelilucca/mc-copycat-replace/tree/master/docs). See the [changelog](https://github.com/radaelilucca/mc-copycat-replace/blob/master/CHANGELOG.md) for release history.*
