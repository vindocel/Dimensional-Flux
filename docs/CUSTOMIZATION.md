# 🧩 Customization Guide

How to customize Dimensional Flux with configuration files, the in-game editor, datapacks and KubeJS: available options, defaults, limits, examples, when changes apply and what requires Java changes.

This guide is for modpack creators, server administrators and modders. The server owns gameplay rules; each player's client owns display preferences.

| Target | Value |
| --- | --- |
| Minecraft | 26.1.2 and 26.3 |
| NeoForge | 26.1.2.126 (Minecraft 26.1.2), 26.3.0.115 (Minecraft 26.3) |
| Java | 25 |
| Mod ID | `dimensionalflux` |
| Data pack version | 101.1 (Minecraft 26.1.2), 121.0 (Minecraft 26.3) |
| Optional scripting | Minecraft 26.1.2: KubeJS `26.1.2-8.0.8`, Rhino `2101.2.8-build.91`, and their required runtime dependencies. Minecraft 26.3: not available yet (no KubeJS build for 26.3) |

For gameplay instructions, see the [README](../README.md) and the craftable in-game guide.

## Contents

1. [Choosing a customization method](#1-choosing-a-customization-method)
2. [Configuration files and screen](#2-configuration-files-and-screen)
3. [Every server option](#3-every-server-option)
4. [Client preferences](#4-client-preferences)
5. [KubeJS server options](#5-kubejs-server-options)
6. [Block and item filters](#6-block-and-item-filters)
7. [Recipes and alternative shard acquisition](#7-recipes-and-alternative-shard-acquisition)
8. [Datapacks, tags and world generation](#8-datapacks-tags-and-world-generation)
9. [What can and cannot be changed](#9-what-can-and-cannot-be-changed)
10. [Applying, checking and reverting changes](#10-applying-checking-and-reverting-changes)
11. [Troubleshooting](#11-troubleshooting)

## 1. Choosing a customization method

| Method | What it changes | How it applies |
| --- | --- | --- |
| Configuration screen | Client preferences and the active local server configuration | Done; see the option-specific behavior below |
| Server TOML | Network limits, administration, chunk loading, energy defaults, battery capacity, ore generation and blacklists | At world/server load or a NeoForge file reload |
| KubeJS server scripts | Every server option plus block/item filter operations; standard recipe and tag events | Server script load or `/reload` |
| Datapack | Recipes, loot, tags, ore placement and optional mining-resource definitions | Data reload; world-generation changes should be followed by a world/server restart and checked in new terrain |
| Resource pack | Client assets such as language text, item/block models and textures; some protected interface resources retain their built-in appearance | Resource reload; no server gameplay rules |
| Java code | Mechanics and constants without a configuration or data entry | Rebuild the mod |

Keep overrides in their own files rather than editing the mod JAR. Use a pack-specific namespace for new recipes and tags, and the original namespace/path when replacing an existing resource.

## 2. Configuration files and screen

### File locations

On this NeoForge target, the main server file applies to worlds without a world-specific override:

| Environment | Main server file | Optional override for one world |
| --- | --- | --- |
| Singleplayer | `<instance>/config/dimensionalflux-server.toml` | `<instance>/saves/<world>/serverconfig/dimensionalflux-server.toml` |
| Dedicated server | `<server>/config/dimensionalflux-server.toml` | `<server>/<level-name>/serverconfig/dimensionalflux-server.toml` |
| Development client | `run/<minecraft>/config/dimensionalflux-server.toml` | `run/<minecraft>/saves/<world>/serverconfig/dimensionalflux-server.toml` |

On Minecraft 26.3 the file keeps the same name, but the per-world override folder is `syncedconfig` instead of `serverconfig` (for example `<instance>/saves/<world>/syncedconfig/dimensionalflux-server.toml`).

The client TOML is `<instance>/config/dimensionalflux-client.toml`. Appearance preferences are stored separately in `<instance>/config/dimensionalflux-settings.json`.

The server TOML is loaded when a world/server starts. For a reproducible pack, ship the intended main config with the instance/server, or supply the override in the intended world's `serverconfig` directory. A joining client's local server file cannot override a multiplayer server.

### In-game editor

Open **Mods → Dimensional Flux → Config**. The four tabs cover Client, Networks, Energy, and World & filters. Hover or focus an option to read its explanation and default.

- Client preferences can be changed locally. Server rules are editable here only while a local world and its server configuration are loaded.
- Main-menu server values and remote multiplayer server values are read-only. Remote server owners use their server files or scripts.
- Script-owned values show the effective value and are read-only. Edit the owning script to change them.
- Per-row reset restores one default. Defaults restores the editable options of the current tab. Undo restores that tab's opening values.
- These actions edit a draft. Done validates and saves it; Escape discards it. In a list editor, Done updates the parent draft, so the parent Done is still required to persist it.
- The editor saves the active config file. If that is the main file, the change also applies to other worlds without an override.

Invalid numeric input disables saving. The script validator rejects invalid values; NeoForge validates TOML types and ranges when loading files. Check the logs after editing by hand. The limits in [section 6](#6-block-and-item-filters) describe the additional script/editor blacklist checks.

## 3. Every server option

Keys are case-sensitive. In particular, `blacklist.itemBlackListStrings` uses a capital `L` in `List`.

| Key | Type | Default | Allowed range or values |
| --- | --- | --- | --- |
| `networks.maximumPerPlayer` | Integer | `10` | -1 to 2147483647 |
| `networks.enableSuperAdmin` | Boolean | `true` | `true` / `false` |
| `networks.superAdminRequiredPermission` | Integer | `1` | 0 to 3 |
| `networks.backupSwitchSeconds` | Integer, seconds | `5` | 1 to 600 |
| `networks.alertShortfallSeconds` | Integer, seconds | `30` | 5 to 3600 |
| `networks.alertReserveSeconds` | Integer, seconds | `120` | 0 to 86400 |
| `networks.alertCooldownSeconds` | Integer, seconds | `300` | 0 to 86400 |
| `general.enableChunkLoading` | Boolean | `true` | `true` / `false` |
| `general.chunkLoadingOnlyOnline` | Boolean | `false` | `true` / `false` |
| `energy.defaultLimit` | Integer, FE/t | `800000` | 0 to 9223372036854775807 |
| `energy.batteryCapacity` | Integer, FE | `256000000` | 0 to 9223372036854775807 |
| `energy.batteryTransfer` | Integer, FE/t | `720000` | 0 to 9223372036854775807 |
| `energy.teslaMaxAreaSize` | Integer, blocks | `32` | 9 to 64 |
| `worldgen.enableOreGeneration` | Boolean | `true` | `true` / `false` |
| `blacklist.blockBlacklistStrings` | List of strings | `["actuallyadditions:block_phantom_energyface"]` | Block IDs or `#block_tags` |
| `blacklist.itemBlackListStrings` | List of strings | `[""]` | Item IDs or `#item_tags`; the blank default has no effect |

### Networks and administration

`maximumPerPlayer` limits how many networks a player may own. `-1` removes that limit. `0` prevents creation except by active superadmins. Active superadmins are exempt from the ownership limit.

The default is ten networks. An existing file keeps its explicitly saved value; change `networks.maximumPerPlayer` in that file or the local-world editor to use a different limit.

Superadmin is an active command-based network administration mode:

```text
/dimensionalflux superadmin <targets> <true|false>
```

It grants access to every network and permits deletion independently of ordinary membership. The command requires at least moderator permissions. A level 3 or 4 administrator can manage other online players; self-activation additionally checks `superAdminRequiredPermission`. Setting that value to `0` removes the additional self-activation check, but does not remove the command's moderator requirement. Setting `enableSuperAdmin` to `false` suspends existing flags rather than deleting them.

`enableChunkLoading` controls whether devices may keep their chunk loaded through the device's chunk-loading option. When disabled, the chunk-loading switch and its label disappear from device Home screens and connection editors. Panels shrink only where their side tabs permit: Tesla keeps its original full height in both states, so navigation remains inside the frame without adding space when the control is shown. Open windows follow synchronized policy changes without discarding other edits. Re-enabling the rule restores eligible controls. It does not change ownership or grant network access.

`chunkLoadingOnlyOnline` makes chunk loading depend on players: a device with Load Chunk keeps its chunk loaded only while its owner or a member of its network (a listed member with access, or a member of the team the network is shared with) is online. Anyone a public network or a password lets in does not count. The device's switch keeps its value, so the chunks load again as soon as someone of the network joins; the rule is checked when players join and leave and twice a minute for membership changes. In a singleplayer or LAN world the host chooses it in the in-game editor; on a dedicated server the server owner sets it and players see it read-only.

`backupSwitchSeconds` is how long a network must stay without energy before a Receiver or Tesla Broadcaster with a backup network moves to it, and how long the own network must have energy again before the device returns.

`alertShortfallSeconds`, `alertReserveSeconds` and `alertCooldownSeconds` time the deficit alerts of networks that turned them on: the shortfall must last that long, the stored energy must be projected to last at most the reserve time, and a network waits the cooldown between two alerts.

### Energy

`defaultLimit` is the initial transfer limit of new Emitters, Receivers and Controllers; the Tesla Broadcaster also initializes its transfer handler from this value. `batteryTransfer` is the initial transfer limit of new Dimensional Batteries. Existing devices keep their saved limits. Their own Ignore Limit setting remains a separate device control.

`teslaMaxAreaSize` is the largest edge of the cube a Tesla Broadcaster can cover. Broadcasters set to a larger area are reduced to it; the area screen and its preview follow it.

`batteryCapacity` is the live maximum of each battery, not a shared network limit. Reducing it does not delete existing charge: a battery can remain above its displayed maximum until that charge is consumed, accepting no additional energy while above capacity. A capacity of `0` provides no room for new stored energy. FE is an amount; FE/t is a transfer rate per game tick.

### Ore generation

`enableOreGeneration = false` stops the custom shard ore feature in newly generated terrain. It does not erase existing ores, unregister items, change drops or recipes, or disable a separate custom mining-resource entry. Alternative acquisition is covered in [section 7](#7-recipes-and-alternative-shard-acquisition).

### Complete server file example

This example uses the standard numbers and an empty item blacklist, which has the same effect as the blank default entry:

```toml
[networks]
maximumPerPlayer = 10
enableSuperAdmin = true
superAdminRequiredPermission = 1

backupSwitchSeconds = 5
alertShortfallSeconds = 30
alertReserveSeconds = 120
alertCooldownSeconds = 300

[general]
enableChunkLoading = true
chunkLoadingOnlyOnline = false

[energy]
defaultLimit = 800000
batteryCapacity = 256000000
batteryTransfer = 720000
teslaMaxAreaSize = 32

[worldgen]
enableOreGeneration = true

[blacklist]
blockBlacklistStrings = ["actuallyadditions:block_phantom_energyface"]
itemBlackListStrings = []
```

## 4. Client preferences

| Preference | Default | Storage and scope |
| --- | --- | --- |
| `gui.enableButtonSound` | `true` | Client TOML; device interface click sounds |
| Dark appearance | `false` | Appearance JSON; shared preference for device screens |
| Particles | `true` | Appearance JSON; shared particle preference |
| Appearance for all machines | `true` | Appearance JSON; shared versus individual device preferences |

The appearance controls use the same preferences as the devices' own settings windows. With shared appearance disabled, configure individual devices in their settings windows. Existing observed devices preserve their effective preference when leaving shared mode; unconfigured devices use their individual defaults. A particle preference does not disable animated textures or the Receiver antenna animation.

These are local player preferences, not KubeJS server options. A server script cannot impose a player's appearance.

## 5. KubeJS server options

KubeJS is optional. Without it, files and the editor still work. With the supported KubeJS build and its runtime dependencies installed, create a `.js` file such as:

```text
<instance-or-server>/kubejs/server_scripts/dimensional_flux.js
```

```js
DimensionalFluxEvents.config(event => {
  event.set('energy.batteryCapacity', 512000000)
  event.set('energy.batteryTransfer', 1000000)
  event.set('networks.maximumPerPlayer', 12)
  event.set('worldgen.enableOreGeneration', false)
  console.info(event.getKeys())
})
```

| Method | Contract |
| --- | --- |
| `event.getKeys()` | Returns every supported server key from section 3 |
| `event.set(key, value)` | Validates and overrides that key in memory |

Values take precedence over TOML and synchronize to clients. They are not written to TOML. Removing a setter or its script and running `/reload` restores the file value. Overrides are cleared when the server stops. They apply again when scripts load for the next server session.

The config event runs before the blacklist event. Setting a key more than once uses the last successful setter in execution order. Keep one owner for each key across scripts to avoid conflicts.

Use actual booleans, not strings such as `'false'`. Numbers must be whole and within their allowed range. For integers above JavaScript's exact Number range, pass a decimal string:

```js
DimensionalFluxEvents.config(event => {
  event.set('energy.batteryCapacity', '9007199254740993')
})
```

Unknown keys, wrong types, fractional numbers and out-of-range values are rejected. A failed setter does not update that key, but other successful setters in the script can still apply. Inspect `logs/kubejs/server.log` and `logs/latest.log`; a script with an error is not a whole-file transaction.

## 6. Block and item filters

The block blacklist skips external blocks when devices look for energy capabilities. The item blacklist skips items during wireless charging. Neither removes blocks/items from registries or recipes. A block entry and an item entry are separate rules, even if their IDs have the same spelling.

The blacklist event starts from the current server file, with config-event replacements applied first:

```js
DimensionalFluxEvents.blacklists(event => {
  event.addBlock('minecraft:furnace')
  event.addBlock('#minecraft:logs')
  event.removeBlock('actuallyadditions:block_phantom_energyface')
  event.addItem('examplemod:energy_tool')
  event.addItem('#examplemod:no_wireless_charging')
  event.removeItem('examplemod:other_tool')
})
```

| Method | Input |
| --- | --- |
| `addBlock(idOrTag)` / `removeBlock(idOrTag)` | Block registry ID or `#block_tag` |
| `addItem(idOrTag)` / `removeItem(idOrTag)` | Item registry ID or `#item_tag` |

Replace placeholder `examplemod` IDs with your pack's real entries. Removal matches an entry in the list; removing an item ID does not exempt it from a broader matching tag. Remove that tag rule if necessary.

To replace a whole list, use the config event instead:

```js
DimensionalFluxEvents.config(event => {
  event.set('blacklist.blockBlacklistStrings', [])
  event.set('blacklist.itemBlackListStrings', ['#examplemod:no_wireless_charging'])
})
```

The script/editor validator accepts at most 1024 entries per list, each up to 256 characters. It trims surrounding whitespace and checks ID/tag syntax. Blank entries have no effect. Syntax validation does not prove an optional mod or tag exists; an absent registry ID or empty tag matches nothing. Tag membership follows the loaded data. Updated filters also affect existing cached connector links.

## 7. Recipes and alternative shard acquisition

Dimensional Flux uses ordinary crafting recipes. Standard KubeJS recipe events can add, remove and replace them. This complete example disables natural ores and provides a new shard source plus an alternative Core recipe:

```js
DimensionalFluxEvents.config(event => {
  event.set('worldgen.enableOreGeneration', false)
})

ServerEvents.recipes(event => {
  event.shapeless('dimensionalflux:dimensional_flux_shard',
    ['minecraft:amethyst_shard', 'minecraft:ender_pearl'])
    .id('kubejs:dimensional_flux_shard')

  event.remove({ id: 'dimensionalflux:dimensional_core' })
  event.shaped('dimensionalflux:dimensional_core', ['CAC', 'ESE', 'COC'], {
    C: 'minecraft:copper_block', A: 'minecraft:amethyst_block',
    E: 'minecraft:ender_eye', S: 'dimensionalflux:dimensional_flux_shard',
    O: 'minecraft:obsidian'
  }).id('kubejs:dimensional_core')
})
```

Copy one intended config handler into your script rather than combining conflicting examples. A recipe filter with `id` removes only that recipe; a filter by output can remove several acquisition routes. The guide's crafting grids read the server's live crafting recipes.

Useful existing recipe IDs include `dimensionalflux:dimensional_core`, `dimensionalflux:dimensional_block`, `dimensionalflux:dimensional_emitter`, `dimensionalflux:dimensional_receiver`, `dimensionalflux:dimensional_battery`, `dimensionalflux:dimensional_controller`, `dimensionalflux:tesla_broadcaster` and `dimensionalflux:guide_book`. Check the [shipped recipes](../src/main/resources/data/dimensionalflux/recipe) before replacing one. The Creative Battery has no survival recipe.

## 8. Datapacks, tags and world generation

### A complete small datapack

Place this folder in `<world>/datapacks/`. In a ZIP, `pack.mcmeta` and `data/` must be at its root:

```text
FluxPackTweaks/
├── pack.mcmeta
└── data/
    └── mypack/
        └── recipe/
            └── dimensional_flux_shard.json
```

`pack.mcmeta` for Minecraft 26.1.2 (on Minecraft 26.3 use `[121, 0]` for both formats):

```json
{
  "pack": {
    "min_format": [101, 1],
    "max_format": [101, 1],
    "description": "Dimensional Flux pack tweaks"
  }
}
```

`data/mypack/recipe/dimensional_flux_shard.json`:

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": ["minecraft:amethyst_shard", "minecraft:ender_pearl"],
  "result": {"id": "dimensionalflux:dimensional_flux_shard", "count": 1}
}
```

This adds a recipe; it does not disable ores by itself. Disable `worldgen.enableOreGeneration` separately if the pack intends this to be the only natural acquisition route. Use `/datapack list enabled` and `/reload` to check loading, then try the recipe.

### Existing resource paths

Paths below are beneath `data/dimensionalflux/`. Use the same namespace/path to replace a resource, or your own namespace for an additional resource. Pack priority decides which replacement of the same resource wins.

| Resource | Path |
| --- | --- |
| Crafting recipes | `recipe/<recipe_id>.json` |
| Shallow ore placement | `worldgen/placed_feature/dimensional_flux_shard_shallow.json` |
| Deep ore placements | `worldgen/placed_feature/dimensional_flux_shard_deep.json`, `dimensional_flux_shard_deep_peak.json` |
| Custom vein configuration | `worldgen/configured_feature/dimensional_flux_shard_vein.json` (Minecraft 26.3: `worldgen/feature/dimensional_flux_shard_vein.json`) |
| Overworld insertion | `neoforge/biome_modifier/dimensional_flux_shard_ore.json` |
| Ore drops | `loot_table/blocks/dimensional_flux_shard_ore.json`, `deepslate_dimensional_flux_shard_ore.json` |
| Optional mining-resource definition | `dimensional_miner_resources/dimensional_flux_shard.json` |

The shipped placements use one attempt at Y -16..16, three attempts at Y -64..0, and two additional attempts at Y -58..-50. Counts and height providers are data-driven. The custom feature still restricts stone hosts to Y -16..16, deepslate hosts to Y -64..0, and connected veins to 2–8 blocks. Moving a placement outside those hardcoded host ranges cannot make it place ore there. Its configured feature has an empty `config` object (on Minecraft 26.3 the feature file holds only its `type`); adding a `size` property does not expose a vein-size option. Loot tables follow the format of each Minecraft version: from 26.3 a table uses `condition` and `modifier` (with `type`) where 26.1.2 uses `conditions` and `functions` (with `function`).

The biome modifier inserts all three features in Overworld biomes. The server ore toggle gates the custom feature even when a datapack changes placement. Existing chunks are not regenerated. Restart after changing worldgen data and test in newly generated terrain.

The optional mining resource is loaded by Dimensional Flux Miner when installed. It is separate from natural ore generation. A pack removing all shard acquisition through mining must customize that entry too, using that integration's own resource format.

### Tags

The shard belongs to `c:gems` and `c:gems/dimensional_flux_shard`. Both ores belong to `c:ores` and `c:ores/dimensional_flux_shard`, plus their ground-type tags. Block and item tags are distinct; their data paths use `tags/block/` and `tags/item/` on this target.

For example, add an item tag at `data/mypack/tags/item/no_wireless_charging.json`:

```json
{"replace": false, "values": ["examplemod:energy_tool"]}
```

Then blacklist `#mypack:no_wireless_charging`. Substitute an installed item ID. KubeJS can define the same tag through its standard event:

```js
ServerEvents.tags('item', event => {
  event.add('mypack:no_wireless_charging', 'examplemod:energy_tool')
})
```

## 9. What can and cannot be changed

| Feature | Supported customization |
| --- | --- |
| All ten server options in section 3 | TOML, local-world editor or config event |
| Filter entries and tag rules | TOML, editor, config event or blacklist event |
| Survival recipes, ore drops and tags | Datapacks and the corresponding standard KubeJS APIs |
| Ore placement frequency, height providers and biome insertion | Datapack overrides, within the custom feature's host restrictions |
| Device name, priority, limit, Ignore Limit, redstone, backup network and Tesla area | Existing in-game device/network controls; no generic server script setter for these per-device values |
| Buffer reserve formula and ceiling, failover timing, Tesla size/offset bounds, charging lookup cadence and alert timings | Fixed in Java; no TOML keys or custom KubeJS hooks |
| Custom vein size and host-selection rules | Fixed in Java |
| Registered block/item IDs, device types, screen layout and guide chapter structure | Code changes; editing config cannot unregister content or restructure screens |
| Local appearance | Client settings; outside the server scripting event |

There are no Dimensional Flux materials, fuel, arbitrary per-network editing or machine-tier script events. Unsupported keys are rejected. Standard KubeJS APIs for recipes, tags and other data remain separate from `DimensionalFluxEvents`.

## 10. Applying, checking and reverting changes

| Change | Apply with | Check |
| --- | --- | --- |
| Client preferences | Done in the editor/device settings; file edits are reliably read after restarting the client | Reopen a device screen; check local effects |
| Server TOML | NeoForge file reload, or restart the world/server after an offline edit | Reopen Config, confirm the active world override and inspect `logs/latest.log` |
| KubeJS config or blacklist events | `/reload`, or next world/server load | Script log, effective values in Config, battery capacity or an actual connection/charging test |
| Initial transfer defaults | Config update, then place a new device | Check its initial limit; existing saved limits are independent |
| Recipes, loot and tags | `/reload` | Recipe viewer, live guide recipe or a controlled crafting/drop/filter test |
| Ore toggle | Config/script update; then generate new terrain | Compare new chunks; old ores remain |
| Worldgen JSON | Restart world/server after installing/reloading the pack | New terrain within the feature's allowed host heights |

To revert script values, remove their setters and reload. To revert a filter addition/removal, remove that operation and reload; the list is rebuilt from the file rather than accumulating each reload. To restore defaults, use the editor reset controls or restore the corresponding TOML values with scripts removed. Back up files before deleting them to regenerate defaults.

Test one change at a time in a disposable world. Confirm loading without errors before measuring gameplay behavior. A displayed default is not proof that a script loaded; verify the intended effective value and its ownership marker.

## 11. Troubleshooting

| Symptom | Likely cause and check |
| --- | --- |
| Server options are gray | No local world is open, or this is a remote server. Edit the server's file/scripts |
| One option is locked | A script owns it. Remove/change the setter and reload |
| Editing TOML has no effect | A world override or script takes precedence, or the file has not reloaded |
| A new default does not change a placed device | Its saved transfer limit remains. Change that device's limit or place a new one |
| Battery shows charge above its new maximum | Lowering capacity preserves existing charge; consume it before charging again |
| Script reports an unknown key | Check section 3 and case, especially `itemBlackListStrings`; client preferences are not server keys |
| A large number changes unexpectedly | Use an exact decimal string above JavaScript's safe integer range |
| A filter does not match | Check block versus item tags, `#` prefix, the installed registry ID and actual loaded tag membership |
| Removing one ID does not unblock it | A tag rule may still match the entry |
| Ore still exists after disabling generation | Existing chunks/ores remain; also check independent mining-resource definitions |
| Ore moved outside its original heights does not generate | The custom host restrictions still apply |
| A datapack is missing or refuses to load | Check root structure, `pack.mcmeta`, target format, JSON errors and `/datapack list enabled` |
| A recipe override loses to another | Check the exact recipe ID, pack priority and other scripts removing/replacing it |
| KubeJS event is unknown | Check that compatible KubeJS and dependencies are installed and that the script is in `server_scripts` |

This guide describes the implementation on the target above. It does not promise that a configuration option or event from another mod exists here.
