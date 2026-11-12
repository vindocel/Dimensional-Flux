<p align="center">
  <img src="src/main/resources/logo.png" alt="Dimensional Flux" width="720">
</p>

<h1 align="center">Dimensional Flux</h1>

<p align="center">
  Wireless energy. Connected dimensions. Control over every machine.
</p>

<p align="center">
  <strong>Minecraft 26.1.2</strong> · <strong>NeoForge</strong> · <strong>Java 25</strong><br>
  English and Brazilian Portuguese
</p>

## About

**Dimensional Flux** transfers Forge Energy (FE) between generators, batteries and machines through named wireless networks, including across dimensions. A pair of compact connectors can power one machine; a Tesla Broadcaster can supply a whole room. Priorities, transfer limits, access controls and live statistics keep the network manageable as it grows.

The mod has its own animated devices, materials, connected decorative blocks, Steelframe interfaces and an in-game guide. Survival energy comes from your generators: Dimensional Flux transports and stores that energy.

**Project status:** playable development snapshot, currently version `0.1.0`. This repository preserves the current source milestone while development continues. A public mod release has not been announced.

## Highlights

- **Cross-dimensional networks** with names, colors, members and public, private or password-protected access.
- **Face-mounted connectors** for taking energy from a source and delivering it directly to a machine.
- **Area delivery** with a configurable Tesla cube, movable coverage and an interactive 3D preview.
- **Individual machine controls** for priorities, transfer limits, names and inclusion in Tesla delivery.
- **Network storage** with an animated battery, a visible charge gauge and comparator output.
- **Wireless item charging** across dimensions through a Controller, or inside a Tesla's area.
- **Monitoring** with transfer charts, moving averages, machine rankings, optional deficit alerts and a handheld HUD.
- **Configurable operation** with redstone modes, chunk loading and backup-network failover.
- **Steelframe screens** with draggable windows, light and dark looks, network-colored displays and configurable particles.
- **A survival progression** built around Dimensional Flux Shards, craftable components and a recipe-aware guide book.

## Devices

| Device | Purpose |
| --- | --- |
| **Dimensional Emitter** | Takes FE from the generator or energy store it is mounted on and supplies the linked network. It supports pushed energy and demand-driven extraction. |
| **Dimensional Receiver** | Delivers network energy to the machine on its mounting face. |
| **Tesla Broadcaster** | Powers eligible machines inside its configured area and can charge the items of nearby eligible players. |
| **Dimensional Battery** | Stores surplus network energy and supplies it when generators cannot meet demand. |
| **Dimensional Controller** | Provides network-wide statistics and connections, global item charging and optional deficit alerts. |
| **Dimensional Configurator** | Copies device settings, pastes them onto other devices and displays a live network HUD while aimed at a device. |
| **Dimensional Battery Creative** | Provides an infinite FE source for creative testing. |

### Getting started

1. Find **Dimensional Flux Ore** in the Overworld. An iron pickaxe or better drops Dimensional Flux Shards; Fortune increases the yield and Silk Touch keeps the ore block.
2. Craft **Dimensional Cores**, **Dimensional Blocks**, an **Emitter** and a **Receiver**.
3. Place the Emitter on a generator or battery face that supplies FE.
4. Open it, create a network and choose its name, color and access mode.
5. Place the Receiver on a machine face that accepts FE, then link it to the same network.
6. Set priorities and transfer limits to suit the machines being supplied.
7. Add a Battery, Controller or Tesla Broadcaster as the installation grows.

```text
Generator → Dimensional Emitter → Wireless network → Dimensional Receiver → Machine
```

Both connectors exchange energy only through their mounting faces. A source or destination with configurable sides must allow FE on that face. The Emitter and Receiver may be in different dimensions; the participating devices must be loaded to operate.

The **Dimensional Flux Guide** is given when a player first collects a shard. It can also be crafted from a Book and a Dimensional Flux Shard. Its chapters explain the devices, network controls, recipes and troubleshooting in game.

## Managing a network

### Access and ownership

Each network has an owner and its own member permissions:

- **Public:** other players can link devices.
- **Private:** access is restricted to members.
- **Locked:** guests authenticate with a password.

Owners can assign User or Administrator access and block individual players. Explicit blocks take precedence over other access grants. Networks can also be shared with a vanilla scoreboard team and, when installed, an FTB Teams party.

### Priorities and limits

A higher priority is served first. Each output has a configurable FE/t limit and an **Ignore Limit** option. Network Options provides two distribution policies: **Priority** serves consumers in order, while **Share** distributes available energy in proportion to demand among consumers of the same priority.

Batteries collect surplus after machine delivery and supply stored energy after the generators. Emitters and outputs use temporary transfer buffers; long-term energy storage belongs in Dimensional Batteries.

The default battery holds **256,000,000 FE** and starts with a **720,000 FE/t** transfer limit. These values are server-configurable. A dropped battery retains its energy, network and settings. Crafting the battery by itself clears its network and settings while retaining the stored energy.

### Backup networks and redstone

Receivers and Tesla Broadcasters can use a backup network. Right-click a network in Network Selection to assign it as backup; right-click it again to clear the selection. After the primary network has had no available energy for five seconds, the device switches to an available backup. It returns after the primary network has had energy again for five seconds.

Emitters, Receivers and Tesla Broadcasters have redstone modes for continuous operation, operation with a signal or operation without a signal. Devices keep their network links while stopped.

A **comparator beside a Dimensional Battery** reads its actual stored charge from **0 to 15**. The output updates independently of the eight visual gauge bars, allowing charge-based generator automation.

Devices with a **Load Chunk** control can keep their chunk loaded when the server allows it.

## Tesla Broadcaster

The Tesla supplies machines inside a cube with a side length from **9 to 32 blocks**. Each axis has an independent offset, allowing the Broadcaster to sit near the edge or in a corner of the covered area.

Its area window includes a live 3D preview:

- Left-drag to orbit the scene.
- Right-drag to pan.
- Scroll to zoom.
- Double-click to reset the view.
- Enable **Show Area** to see the coverage in the world.

The **Machines in area** window lists eligible machines. Each can be renamed, highlighted in the world, excluded from delivery, assigned a priority or given an individual transfer limit.

Machines are served by priority. **Share equally** is disabled by default; enabling it splits available energy equally within each priority and leaves unused shares available to machines that can still accept energy. An unlimited machine setting removes that machine's individual cap; combined delivery still respects the Tesla's total limit and the available network energy.

An assembled Dimensional Flux Miner is recognized as one machine, even when several of its shared energy ports are inside the area.

## Wireless charging and monitoring

### Charging players' items

The Controller charges eligible players' items across dimensions while linked and loaded. Supported slot groups include the hotbar, armor and hands, with accessory slots available through Curios.

Each Tesla has separate local charging settings for players inside its area. Controllers share their charging settings per network. Active global charging takes priority over Tesla charging on the same network. A dedicated inventory FE/t limit controls the combined charging budget.

### Statistics and live readings

Device statistics show their own transfers; the Controller and Configurator expose the network view. Monitoring includes:

- Current input and output, with one-minute and one-hour network averages.
- Separate input and output chart lanes and storage gauges.
- Generation and consumption rankings for Now, 10, 20 and 60 minutes.
- Individual Tesla-machine readings and recent histories.
- Optional deficit alerts sent through a loaded Controller.
- A Configurator HUD with live transfer and battery readings.

Sneak-use the Configurator on a device to copy its settings, use it on another device to paste them, or sneak-use it in the air to clear it.

## Materials and presentation

Dimensional Flux Shards, Dimensional Cores and Dimensional Blocks form the crafting progression. Stone and deepslate ores light up with purple and blue particles when interacted with. Dimensional Blocks can also be used as decoration: adjacent copper corners merge into larger joints while each crystal retains its own animated window.

Devices use network-colored details, crystals, transfer animations and particles. Their Steelframe screens support draggable windows and independent light or dark appearances.

**Particles** and **Apply to all devices** start enabled. Shared mode applies appearance changes to every device. Turning it off permits individual customization; unconfigured devices start light with particles enabled. Turning it back on copies the current device's choices to all devices. Appearance preferences are stored on the player's computer.

## Optional integrations

The core mod does not require another content mod. Optional integrations activate when the corresponding mods are installed.

| Integration | Support |
| --- | --- |
| **Jade** | The mod's animated energy bar for devices and batteries. |
| **JEI / REI** | Crafting-recipe viewing and screen exclusion areas for tabs and floating windows. |
| **Curios** | Wireless charging of eligible accessory items. |
| **FTB Teams** | Sharing network access with a party. |
| **FTB Chunks** | Device map colors that follow the network color. |
| **Dimensional Flux Miner** | Grouped multiblock delivery, an assembled Tesla preview and a resource entry for Dimensional Flux Ore. |

Other FE-compatible generators, energy stores and machines can connect through their supported energy faces.

## Configuration

Server configuration controls network ownership limits, administrative access, chunk loading, energy limits, battery capacity and blacklists. Client configuration controls button sounds; the in-game settings window manages appearance and particles.

| Setting | Default |
| --- | --- |
| Networks per player | 5 |
| Device transfer limit | 800,000 FE/t |
| Battery capacity | 256,000,000 FE |
| Battery transfer limit | 720,000 FE/t |
| Device chunk loading | Allowed |
| Button sounds | Enabled |
| Particles | Enabled |
| Apply appearance to all devices | Enabled |

Server settings may change these values. Individual devices retain their own transfer controls.

## Building from source

### Current target

| Component | Version |
| --- | --- |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.126 or newer within the supported Minecraft target |
| Java toolchain | JDK 25 |
| Gradle wrapper | 9.2.1 |
| ModDevGradle | 2.0.148 |
| Stonecutter | 0.9.8 |

The repository uses Stonecutter with one configured Minecraft target, `26.1.2`. Shared Java code and resources are under `src/main`; version-specific properties are under `versions/26.1.2`.

From the repository root on Windows:

```powershell
.\gradlew.bat :26.1.2:build
```

On Linux or macOS:

```sh
bash ./gradlew :26.1.2:build
```

The current build produces:

```text
versions/26.1.2/build/libs/dimensional-flux-0.1.0+26.1.2.jar
```

Development runs on Windows:

```powershell
.\gradlew.bat :26.1.2:runClient
.\gradlew.bat :26.1.2:runServer
```

The client uses the local `run/` directory. Runtime settings, worlds, local test mods, Gradle caches and generated build outputs are excluded from version control.

## License

The original code and assets are licensed under **Creative Commons Attribution 4.0 International (CC BY 4.0)**. Attribute reuse to Dimensional Flux and its contributors.

An additional permission allows modpack creators to include and distribute the **unmodified mod** in a modpack without requesting permission or providing attribution. See [LICENSE](LICENSE) for the terms.
