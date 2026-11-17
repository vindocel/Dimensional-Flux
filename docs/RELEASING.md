# Release automation

Publishing happens only when a **GitHub Release is published** (never on a push or on a bare tag).

## Branches

| Branch | What it holds |
|---|---|
| `production` | Day-to-day work. Every commit goes here; it may be unstable. Alphas and betas are released from it. |
| `main` | Stable versions only. It receives a merge from `production` when a version is final, and stable releases are published from it. |

CI (`.github/workflows/ci.yml`) builds and runs the GameTests on every push to `production` or `main` and on every
pull request.

## Versions

`gradle.properties` holds the **base version of the open update**: `mod_version=0.1.0`. It changes only when an
update is closed and the next one starts. Alphas, betas and the stable release of that update come from the tag, so
they need no version commit:

| Tag | Channel | GitHub "Pre-release" | Built version |
|---|---|---|---|
| `v0.1.0-alpha.1+26.1.2` | alpha | checked | `0.1.0-alpha.1` |
| `v0.1.0-beta.1+26.1.2` | beta | checked | `0.1.0-beta.1` |
| `v0.1.0+26.1.2` | release | unchecked | `0.1.0` |

A tag without a Minecraft suffix (`v0.1.0-beta.1`) builds and publishes **all configured Minecraft versions**.
The `+<minecraft>` suffix selects one version. Separate several versions with `-` to select a group, for example
`v0.1.0-beta.1+26.1.2-26.3` once both targets have been approved and configured. Unknown or repeated targets are rejected. A `mod_version` that already carries a channel
(`0.1.0-beta.2`) must match the tag exactly.

The JAR is named `dimensional-flux-<version>+<minecraft>.jar`, e.g. `dimensional-flux-0.1.0-beta.1+26.1.2.jar`.

## Publishing a release

1. Commit and push to `production` (to `main` after the merge, for a stable release); wait for CI to pass.
2. On GitHub: **Releases → Draft a new release**.
3. **Choose a tag**: type the new tag (e.g. `v0.1.0-beta.1+26.1.2`). **Target**: `production` for an alpha or a beta,
   `main` for a stable release.
4. Write the release notes (or press **Generate release notes** and edit them); the text becomes the
   CurseForge and Modrinth changelog.
5. Check **Set as a pre-release** for an alpha or a beta, leave it unchecked for a stable release, then **Publish**.

`release.yml` resolves the tag and builds a matrix from `settings.gradle.kts` and each target's properties:

1. validates the tag, base version, selected targets, Pre-release flag and ancestry on the target branch;
2. builds each selected target from scratch with the tag's version (`:<minecraft>:build`, `-Prelease_version`);
3. runs each target's GameTests with the same release version;
4. validates and saves one distribution JAR per target, checking its name, version and Minecraft range;
5. waits for **every selected build and GameTest** to pass before any store upload starts;
6. in the `management` environment, downloads and revalidates each artifact and checks which stores already have it;
7. publishes missing files to CurseForge and Modrinth as alpha, beta or release, with a distinct store version
   `v<version>+<minecraft>` and the Java version from that target's properties.

A failed build, GameTest or artifact check prevents publishing for the entire selection. Uploads to separate stores
are independent: if a store fails after another succeeds, rerun the workflow; existing files are skipped. CurseForge
lookups use the public CFWidget API and Modrinth uses its official API; lookup failures stop that target's uploads.

## Several Minecraft versions

Every Minecraft version lives on the same branch: **Stonecutter** builds each one as the Gradle project
`:<minecraft>`, listed in `settings.gradle.kts` (`versions(...)`) with its own `versions/<minecraft>/gradle.properties`
(NeoForge version and other version-specific values). Shared code and resources stay in `src/main`; code that differs
between versions uses Stonecutter conditions.

- After approval of a port, add its version to `versions(...)` in `settings.gradle.kts` and create
  `versions/<minecraft>/gradle.properties` with `neo_version`, `java_version` and the exact
  `minecraft_version_range=[<minecraft>]`. Adapt APIs and dependencies for that target. CI and suffix-free releases
  pick it up automatically; the client uses `run/<minecraft>` without another path change.
- To release one Minecraft version, publish a release whose tag carries its suffix, e.g. `v0.1.0-beta.1+26.3`.
- To release every configured target at once, use `v0.1.0-beta.1`.
- To release a group, use `v0.1.0-beta.1+26.1.2-26.3` after those versions are configured.
- Only Minecraft 26.1.2 is currently configured; other versions here illustrate future selection.
- No automation can decide which code is compatible with which Minecraft version; CI on every configured version is
  what proves it.

## Credentials

The `management` environment holds the secrets `CURSEFORGE_TOKEN`, `MODRINTH_TOKEN` and the variables
`ID_DO_CURSEFORGE`, `ID_DO_MODRINTH`. Keep it protected with required reviewers if manual approval is wanted. CI and
pull requests never get publishing credentials.

## Checking locally before a tag

```bash
python3 -B scripts/test-release.py
python3 -B scripts/test-published.py
./gradlew build                       # all configured targets
./gradlew :26.1.2:build               # one target
./gradlew :26.1.2:runGameTestServer
bash scripts/validate-release.sh versions
TAG=v0.1.0-beta.1+26.1.2 PRERELEASE=true bash scripts/validate-release.sh tag
```

On Windows use `gradlew.bat` and run the script from Git Bash.

## Local client instances

Run `gradlew.bat :26.1.2:runClient` on Windows. Each target uses `run/<minecraft>` with independent mods,
settings, shader packs and worlds. The existing 26.1.2 instance was moved intact from `run/` to `run/26.1.2`.
The development mod is loaded from the source build; do not put a second copy in `mods/`.
`-Ptest_run_root=<directory>` overrides all run directories to `<directory>/<minecraft>/<run-name>` for isolated checks.
