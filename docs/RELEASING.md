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

The `+<minecraft>` suffix chooses the Minecraft version to build and publish. A tag without it (`v0.1.0-beta.1`) is
accepted only while a single Minecraft version is configured. A `mod_version` that already carries a channel
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

`release.yml` then checks out the tag and, in the `management` environment:

1. validates the tag (format, base version, Minecraft suffix, Pre-release flag), that the Minecraft version is one
   Stonecutter builds, and that the tagged commit is on the selected target branch;
2. builds that Minecraft version from scratch with the version of the tag (`:<minecraft>:build`);
3. runs the GameTests (`:<minecraft>:runGameTestServer`);
4. checks the JAR (one distribution JAR, its file name, its version and its Minecraft range in `neoforge.mods.toml`);
5. publishes it to CurseForge and Modrinth as alpha, beta or release for that Minecraft version.

Any failed check stops the job before anything is uploaded.

## Several Minecraft versions

Every Minecraft version lives on the same branch: **Stonecutter** builds each one as the Gradle project
`:<minecraft>`, listed in `settings.gradle.kts` (`versions(...)`) with its own `versions/<minecraft>/gradle.properties`
(NeoForge version and other version-specific values). Shared code and resources stay in `src/main`; code that differs
between versions uses Stonecutter conditions.

- To add a Minecraft version, add it to `versions(...)` in `settings.gradle.kts` and create `versions/<minecraft>`.
  CI builds and tests it from the next push.
- To release one Minecraft version, publish a release whose tag carries its suffix, e.g. `v0.1.0-beta.1+26.3`.
  Release each version with its own tag; the same mod version may ship for several Minecraft versions.
- No automation can decide which code is compatible with which Minecraft version; CI on every configured version is
  what proves it.

## Credentials

The `management` environment holds the secrets `CURSEFORGE_TOKEN`, `MODRINTH_TOKEN` and the variables
`ID_DO_CURSEFORGE`, `ID_DO_MODRINTH`. Keep it protected with required reviewers if manual approval is wanted. CI and
pull requests never get publishing credentials.

## Checking locally before a tag

```bash
./gradlew :26.1.2:build
./gradlew :26.1.2:runGameTestServer
bash scripts/validate-release.sh versions
TAG=v0.1.0-beta.1+26.1.2 PRERELEASE=true bash scripts/validate-release.sh tag
```

On Windows use `gradlew.bat` and run the script from Git Bash.
