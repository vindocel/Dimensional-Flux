<div align="center">

<img src="src/main/resources/logo.png" alt="Dimensional Flux" width="720">

**Wireless energy. Connected dimensions. Control over every machine.**
Put an Emitter on a generator, a Receiver on a machine, link both to the same network, and the energy arrives: in the same base or in another dimension.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?style=for-the-badge)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-26.1.2.126-E07A2F?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-25-5382A1?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-CC%20BY%204.0-9366DE?style=for-the-badge)](LICENSE)
[![CI](https://img.shields.io/github/actions/workflow/status/vindocel/Dimensional-Flux/ci.yml?branch=production&style=for-the-badge&label=CI)](https://github.com/vindocel/Dimensional-Flux/actions/workflows/ci.yml)

[![Visit the showcase](https://img.shields.io/badge/Visit%20the%20showcase-vindocel.github.io%2FShowcase--DFM-8A5CF5?style=for-the-badge)](https://vindocel.github.io/Showcase-DFM/dimensional-flux.html)

**See every device in 3D on the [showcase site](https://vindocel.github.io/Showcase-DFM/dimensional-flux.html).**

</div>

---

## 📚 Contents

- [✨ Overview](#-overview)
- [🚀 Getting started](#-getting-started)
- [📖 Dimensional Flux Guide](#-dimensional-flux-guide)
- [🔌 Devices](#-devices)
- [🌐 Managing a network](#-managing-a-network)
- [🌩️ Tesla Broadcaster](#%EF%B8%8F-tesla-broadcaster)
- [🧲 Wireless charging and monitoring](#-wireless-charging-and-monitoring)
- [💎 Materials and presentation](#-materials-and-presentation)
- [🔌 Compatibility](#-compatibility)
- [🧩 Configuration](#-configuration)
- [🛠️ Building from source](#%EF%B8%8F-building-from-source)
- [🚢 Continuous integration and releases](#-continuous-integration-and-releases)
- [📜 License](#-license)

---

## ✨ Overview

| | |
|---|---|
| 🌐 **Cross-dimensional networks** | Named, colored networks with members and public, private or password-protected access. Energy that enters one can leave it anywhere, in the same tick. |
| 🔌 **Face-mounted connectors** | The **Emitter** takes energy from a source; the **Receiver** delivers it straight to a machine. |
| 🌩️ **Area delivery** | The **Tesla Broadcaster** powers a whole room, with a movable cube and an interactive 3D preview. |
| 🔋 **Network storage** | The **Dimensional Battery** keeps the reserve; device buffers never become storage. |
| 🧲 **Wireless item charging** | Across dimensions through a **Controller**, or inside a Tesla's area. |
| 📊 **Monitoring** | Transfer charts, moving averages, machine rankings, deficit alerts and a handheld HUD. |
| 🔁 **Configurable operation** | Priorities, limits, redstone modes, chunk loading and backup-network failover. |
| 🖥️ **Steelframe screens** | Draggable windows, light and dark looks, network-colored displays and particles you control. |
| 📖 **Dimensional Flux Guide** | An animated book that explains the whole mod, in game. |

| | |
|---|---|
| Minecraft | **26.1.2** |
| NeoForge | **26.1.2.126** |
| Java | **25** |
| Mod ID | `dimensionalflux` |
| Version | `0.1.0` (development) |
| License | CC BY 4.0, with a modpack permission (see [`LICENSE`](LICENSE)) |

> 🔋 The mod generates **no energy**: power your networks with generators from any other mod. Need some? A reactor from **[Bigger Reactors Rework](https://www.curseforge.com/minecraft/mc-mods/biggerreactors-rework)** with an Emitter on it feeds a whole network.

---

## 🚀 Getting started

1. ⛏️ Find **Dimensional Flux Ore** deep in the Overworld. An iron pickaxe or better drops one **Dimensional Flux Shard**; Fortune raises the yield and Silk Touch keeps the ore.
2. 🧱 Craft **Dimensional Cores**, **Dimensional Blocks**, an **Emitter** and a **Receiver**.
3. 🔌 Place the Emitter on a generator or battery face that supplies FE.
4. 🎨 Open it, create a network and pick its name, color and access mode.
5. 📡 Place the Receiver on a machine face that accepts FE and link it to the same network.
6. 🔢 Set priorities and transfer limits for the machines being supplied.
7. 🔋 Add a Battery, a Controller or a Tesla Broadcaster as the installation grows.

```text
Generator → Dimensional Emitter → Wireless network → Dimensional Receiver → Machine
```

Both connectors exchange energy only through their mounting faces; a machine with configurable sides must allow FE on that face. The Emitter and the Receiver may be in different dimensions, as long as both are loaded.

---

## 📖 Dimensional Flux Guide

Your first **Dimensional Flux Shard** earns you the guide; more copies are crafted from a **Book + Dimensional Flux Shard** (shapeless).

- 🎞️ **Animated book**: it opens in your hand, stays half open while you read, turns a leaf with every page and closes when you leave.
- 📄 **Real pages**: a presentation and clickable **contents** open the book; every chapter starts on a left page, with a page-turn animation and sound. The cyan ribbon goes back to the contents.
- 🧾 **Live data**: crafting grids come from the server, and the battery table follows its configuration.
- 🔎 The first time a chapter names an item, the word is underlined: hover it to see the item.

All text lives in the language files (`guide.dimensionalflux.*`), in English and Brazilian Portuguese.

---

## 🔌 Devices

| Device | Purpose |
|---|---|
| 🔌 **Dimensional Emitter** | Takes FE from the generator or energy store it is mounted on and supplies the network, both pushed energy and demand-driven extraction. |
| 📡 **Dimensional Receiver** | Delivers network energy to the machine on its mounting face; its antenna moves while energy flows. |
| 🌩️ **Tesla Broadcaster** | Powers the machines inside its area and can charge the items of players standing in it. |
| 🔋 **Dimensional Battery** | Stores surplus network energy and supplies it when generators fall short. |
| 🧠 **Dimensional Controller** | Network-wide statistics and connections, global item charging and optional deficit alerts. |
| 🪄 **Dimensional Configurator** | Copies device settings, pastes them on other devices and shows a live HUD for the device you look at. |
| ♾️ **Dimensional Battery Creative** | An infinite FE source for creative testing. |

---

## 🌐 Managing a network

### 🔐 Access and ownership

| Mode | Who can link devices |
|---|---|
| **Public** | anyone |
| **Private** | members only |
| **Locked** | guests with the password |

Owners assign **User** or **Administrator** access and can block individual players; a block wins over every other kind of access. A network can also be shared with a vanilla scoreboard team (`/team`) and, when installed, an **FTB Teams** party.

### 🔢 Priorities and limits

- A **higher priority** is served first.
- Every device has an FE/t limit and an **Ignore Limit** switch.
- **Distribution** in Network Options: **Priority** serves consumers in order; **Share** splits what is left among consumers of the same priority, in proportion to their demand.
- Batteries collect the surplus after the machines and give it back after the generators. Device buffers never hold more than **1,000,000 FE** between ticks: long-term storage belongs in batteries.

### 🔁 Backup networks, redstone and chunk loading

- 🔁 **Backup network** (Receivers and Tesla Broadcasters): right-click a network in Network Selection to make it the backup, again to clear it. After five seconds without energy on its own network the device moves to the backup, and it returns once its own network has had energy for five seconds.
- 🔴 **Redstone tab** (Emitters, Receivers and Tesla Broadcasters): always on, only with a signal or only without one. A stopped device stays linked.
- 📶 **Comparator**: next to a Dimensional Battery it reads the stored charge from 0 to 15.
- 🗺️ **Load Chunk** keeps a device's chunk loaded when the server allows it.

<details>
<summary>🔬 Battery details</summary>

- The default battery holds **256,000,000 FE** with a **720,000 FE/t** transfer limit; both are server settings.
- A broken battery keeps its energy, network and settings. Crafting it alone clears the network and settings and keeps the energy.
- The comparator output follows the real charge, apart from the eight bars of the block's gauge, so it can drive generator automation.
</details>

---

## 🌩️ Tesla Broadcaster

The Tesla powers the machines inside a cube from **9 to 32 blocks** on a side. Each axis has its own offset, so the broadcaster can sit at the edge or in a corner of the room it powers.

Its area window has a live 3D preview of the covered machines, drawn as idle models:

- 🖱️ Left-drag to orbit, right-drag to pan, scroll to zoom, double-click to reset.
- 📦 **Show Area** outlines the coverage in the world.

The **Machines in area** window lists every machine that accepts energy. Each can be renamed, lit up in the world, left out, or given its own priority and limit. Machines are served by priority; **Share equally** (off by default) splits the energy within each priority and passes unused shares on. The Tesla's total limit and the network supply still apply.

> ⛏️ An assembled **[Dimensional Flux Miner](https://www.curseforge.com/minecraft/mc-mods/dimensional-flux-miner)** counts as one machine, even with several of its energy ports in the area, and shows assembled in the preview. Need resources? The Miner makes the ores of its dimension appear, Dimensional Flux Ore included.

---

## 🧲 Wireless charging and monitoring

### 🧲 Charging players' items

- The **Controller** charges the items of every eligible player, across dimensions, while it is linked and loaded.
- Slot groups: hot bar, armor and hands, plus accessory slots with **Curios**.
- Each **Tesla** has its own charging settings for players inside its area; Controllers share theirs per network, and global charging takes priority on the same network.
- A total inventory FE/t limit caps the charging budget.

### 📊 Statistics and live readings

- Current input and output, with one-minute and one-hour network averages.
- Separate input and output chart lanes and storage gauges.
- Generation and consumption rankings for Now, 10, 20 and 60 minutes.
- Individual Tesla-machine readings and recent histories.
- Optional deficit alerts, sent through a loaded Controller.
- A **Configurator** HUD with live transfer and battery readings: sneak-use it on a device to copy, use it on another to paste, sneak-use it in the air to clear.

Device statistics show their own transfers; the Controller and the Configurator show the whole network.

---

## 💎 Materials and presentation

- 💠 **Dimensional Flux Shards**, **Dimensional Cores** and **Dimensional Blocks** form the crafting progression. The stone and deepslate ores light up with purple and blue sparks when touched.
- 🧱 **Dimensional Blocks** also decorate: neighboring copper corners merge into larger joints while each crystal keeps its own animated window.
- 🎨 Devices take the network color on their details, crystals, transfer animations and particles.
- 🖥️ **Steelframe screens**: draggable windows, light and dark looks and a particles switch, for one device or all of them. Their drawing and textures are protected from mods that recolor screens.

---

## 🔌 Compatibility

The mod needs no other content mod. These integrations switch on when the mods are installed:

| Integration | Support |
|---|---|
| 🔍 **Jade** | The mod's animated energy bar for every device and battery. |
| 📘 **JEI / REI** | Recipe viewing; the side tabs and floating windows push the item lists out of the way. |
| 💍 **Curios** | Wireless charging of accessory items. |
| 👥 **FTB Teams** | Sharing network access with a party. |
| 🗺️ **FTB Chunks** | Device map colors that follow the network color. |
| ⛏️ **Dimensional Flux Miner** | One-machine delivery to the formed Miner, its assembled Tesla preview and Dimensional Flux Ore in the Miner. |

Any FE generator, energy store or machine connects through its energy faces, such as the reactors and turbines of **[Bigger Reactors Rework](https://www.curseforge.com/minecraft/mc-mods/biggerreactors-rework)** through an Emitter.

---

## 🧩 Configuration

The server configuration sets network ownership limits, administrative access, chunk loading, energy limits, battery capacity and blacklists. The client configuration sets button sounds; the in-game settings window handles the look and the particles.

| Setting | Default |
|---|---|
| Networks per player | 5 |
| Device transfer limit | 800,000 FE/t |
| Battery capacity | 256,000,000 FE |
| Battery transfer limit | 720,000 FE/t |
| Device chunk loading | Allowed |
| Button sounds | Enabled |
| Particles | Enabled |
| Apply the look to all devices | Enabled |

---

## 🛠️ Building from source

The repository uses **Stonecutter** with **ModDevGradle**: every Minecraft version lives on the same branch. Shared code and resources are under `src/main`, the GameTests under `src/gametest`, and the version-specific properties under `versions/<minecraft>`.

```bash
./gradlew :26.1.2:build               # jar in versions/26.1.2/build/libs/dimensional-flux-<version>+26.1.2.jar
./gradlew :26.1.2:runGameTestServer   # in-world GameTests (a separate test mod, never in the released jar)
./gradlew :26.1.2:runClient           # development client (uses the local run/ folder)
./gradlew :26.1.2:runServer           # development server
```

On Windows use `gradlew.bat`. The Gradle wrapper provisions Gradle itself; a JDK 25 is required.

| Component | Version |
|---|---|
| Gradle wrapper | 9.2.1 |
| ModDevGradle | 2.0.148 |
| Stonecutter | 0.9.8 |

---

## 🚢 Continuous integration and releases

| Workflow | When | What |
|---|---|---|
| 🧪 **CI** (`ci.yml`) | every push to `production` or `main` and every pull request | builds every Stonecutter version and runs its GameTests; no publishing credentials |
| 📦 **Release** (`release.yml`) | a GitHub Release is published | validates the tag and its target branch, builds the tagged Minecraft version with the tag's version, runs the GameTests, checks the jar and uploads it to CurseForge and Modrinth with the release body as changelog |

- 🌿 **Branches**: day-to-day work goes to `production`; `main` only receives stable versions.
- 🏷️ **Tags** look like `v0.1.0-beta.1+26.1.2`: `gradle.properties` keeps the base version, the channel (alpha, beta, release) comes from the tag, and the `+<minecraft>` suffix picks the Stonecutter version to publish.
- ✅ Alpha and beta releases are marked **Pre-release** on GitHub; stable releases are not.
- 📘 Full release and multi-version workflow: [docs/RELEASING.md](docs/RELEASING.md).

---

## 📜 License

- 📄 The original code and assets are licensed under **Creative Commons Attribution 4.0 International (CC BY 4.0)**. Attribute reuse to Dimensional Flux and its contributors.
- 📦 An additional permission lets modpack creators include and distribute the **unmodified mod** in a modpack without asking or giving attribution. See [`LICENSE`](LICENSE) for the terms.

🐛 Found a bug or have an idea? Open an issue on [GitHub](https://github.com/vindocel/Dimensional-Flux/issues).
