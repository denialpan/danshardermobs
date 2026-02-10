![Replace this with a description](https://cdn.modrinth.com/data/cached_images/2ca098c9b229203ddb83cfaf68baa151ef8fb308.png)

---

This mod introduces a new leveling system that dynamically scales mobs depending on the performance of the player. Through the leveling system, **mobs can spawn with more health, better armor, and effects**. Predominantly, the leveling system is based off of the time-to-kill for mobs, and adjusts as the player survives. This helps to aid in the feeling of possible end-game weapons or items killing mobs too quickly.

What sets this mod apart is the adaptation to player performance, most notably if performance **decreases**. Most mods only involve upward progression and does not take into account of player dying, losing equipment, etc. This mod addresses this issue in a manner where if performance deviates in both negative manners, the system will scale mobs appropriately so that mobs are not completely overpowered as the player regains items.

### State of the mod

This mod is **intentionally vanilla style** and **WILL NOT** introduce new mob AI, new entities, new items, nothing apart from the base vanilla game; _a vanilla client should be able to join a server without this mod installed client-side_. However, this mod is initially designed around [[UNOFFICIAL] TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1) due to its extremely strong weapons. 

| Version | Phase |
| --- | --- |
| 1.21.1 neoforge | Released 1/20/2026 |
| 1.20.1 forge | Released 1/21/2026 |

Current Features:
- Vanilla player scaling based on player mob time-to-kill.
- Continuous scaling, there are no discrete "stages of mob scaling".
- Player damage is the only limit in scaling. Optionally use [AttributeFix](https://modrinth.com/mod/attributefix) to allow mob attribute levels beyond 1024.
- Extremely configurable to adjust to any playstyle or modpack through datapack support and [documentation](https://github.com/denialpan/danshardermobs/wiki) to support entities, effects, armor, weapons, and enchantments from other mods, with blacklisting and level requirements and a whole bunch of stuff for fine tuning.
- Mob max health scaling
- Mob armor and weapons
- Mob effects
- Mob bosses
- Mob equipment can roll illegal enchanting attributes (disabled by default)
- **a mod to genuinely solve this [problem](https://www.reddit.com/r/feedthebeast/comments/1pdu3a4/looking_for_mods_that_add_hostile_mobs_that_are/)**
- **a [mod request](https://www.reddit.com/r/feedthebeast/comments/1qc6l0e/is_there_a_mod_where_mobs_spawn_with_equipment/) a few days into development**

With the current state of the mod, it is nearly feature-complete, except for:

- General out-of-the-box balancing. Feedback is welcome, as I am not well acquainted with modern vanilla combat flow.
- ~~Player health scaling...?~~ may be redundant 
- Better tuning and starting initial config
- Better showcase video

Mod conflicts:
- None listed here yet, but have not tested against scaling mods those commonly found in RPG-style modpacks, as I don't play them.

---

### Permissions
This mod can be ported to other versions and platforms, as well as used in modpacks. Provide credit in forks of this project by simply linking to the original mod page here.

---

### Dependencies
With the intent of being lightweight, **no mod dependencies** are required for this mod.

---

### Issues/Suggestions/Feedback
As my first major mod, all of the above are welcome. Ideally, this mod should not conflict with mods, unless they harshly overwrite or interact with mob attributes. However, if there are issues or suggestions for the mod, feel free to issue them [here](https://github.com/denialpan/danshardermobs).
