#!/usr/bin/env bash
# Release checks shared by ci.yml and release.yml.
#
#   validate-release.sh versions    lists the Minecraft versions Stonecutter builds (one per line)
#   validate-release.sh tag         resolves the release from TAG (+ PRERELEASE, TARGET)
#   validate-release.sh artifact    checks the built JAR against VERSION and MINECRAFT
#
# Every Minecraft version lives on the same branch; Stonecutter builds each one as the Gradle project
# :<minecraft> (versions/<minecraft>). The tag chooses the version to publish:
#   v<X.Y.Z>[-alpha.N|-beta.N]+<minecraft>, e.g. v0.1.2-beta.1+26.1.2
# The +<minecraft> suffix may be left out only while a single version is configured. gradle.properties holds only
# the base version (mod_version=0.1.2); the channel comes from the tag, so alphas and betas need no version commit.
# A mod_version that already carries a channel (0.1.2-beta.2) must match the tag exactly.
set -euo pipefail

die() { echo "::error::$*" >&2; exit 1; }
property() { sed -n "s/^$1=//p" gradle.properties | head -n 1 | tr -d '\r'; }
output() { if [[ -n "${GITHUB_OUTPUT:-}" ]]; then echo "$1=$2" >> "$GITHUB_OUTPUT"; fi; }

NUM='(0|[1-9][0-9]*)'
BASE_RE="^$NUM\.$NUM\.$NUM$"
CHANNEL_RE="^$NUM\.$NUM\.$NUM-(alpha|beta)\.[1-9][0-9]*$"
MC_RE='[0-9]+\.[0-9]+(\.[0-9]+)?'

[[ -f gradle.properties && -f settings.gradle.kts ]] || die "Run from the repository root"
mod_version="$(property mod_version)"
[[ "$mod_version" =~ $BASE_RE || "$mod_version" =~ $CHANNEL_RE ]] \
  || die "Invalid mod_version $mod_version (expected X.Y.Z, or X.Y.Z-alpha.N / X.Y.Z-beta.N)"

# The versions(...) list of settings.gradle.kts, each with its versions/<minecraft> folder.
configured_versions() {
  local list
  list="$(tr -d '\r' < settings.gradle.kts | sed -n 's/.*versions(\(.*\)).*/\1/p' | head -n 1)"
  [[ -n "$list" ]] || die "No versions(...) list in settings.gradle.kts"
  grep -oE '"[^"]+"' <<< "$list" | tr -d '"'
}

mapfile -t versions < <(configured_versions)
[[ "${#versions[@]}" -gt 0 ]] || die "No Minecraft version configured"
for v in "${versions[@]}"; do
  [[ "$v" =~ ^$MC_RE$ ]] || die "Invalid Minecraft version in settings.gradle.kts: $v"
  [[ -d "versions/$v" ]] || die "versions/$v is missing for Minecraft $v"
done

mode="${1:-}"
case "$mode" in
  versions)
    printf '%s\n' "${versions[@]}"
    # JSON list for a GitHub Actions matrix
    json="[$(printf '"%s",' "${versions[@]}" | sed 's/,$//')]"
    output versions "$json"
    ;;

  tag)
    [[ -n "${TAG:-}" ]] || die "TAG is empty"
    [[ "$TAG" =~ ^v($NUM\.$NUM\.$NUM)(-(alpha|beta)\.([1-9][0-9]*))?(\+($MC_RE))?$ ]] \
      || die "Invalid tag $TAG (expected v0.1.2, v0.1.2-beta.1 or v0.1.2-beta.1+${versions[0]})"
    tag_base="${BASH_REMATCH[1]}"
    tag_channel="${BASH_REMATCH[6]}"
    tag_number="${BASH_REMATCH[7]}"
    minecraft="${BASH_REMATCH[9]}"

    if [[ -z "$minecraft" ]]; then
      [[ "${#versions[@]}" -eq 1 ]] \
        || die "Tag $TAG has no +<minecraft> suffix but several versions are configured: ${versions[*]}"
      minecraft="${versions[0]}"
    fi
    found=false
    for v in "${versions[@]}"; do
      [[ "$v" == "$minecraft" ]] && found=true
    done
    [[ "$found" == true ]] || die "Tag $TAG is for Minecraft $minecraft, which Stonecutter does not build (${versions[*]})"

    version="$tag_base"
    if [[ -n "$tag_channel" ]]; then
      version="$tag_base-$tag_channel.$tag_number"
    fi
    if [[ "$mod_version" =~ $BASE_RE ]]; then
      [[ "$tag_base" == "$mod_version" ]] || die "Tag $TAG is not a release of mod_version $mod_version"
    else
      [[ "$version" == "$mod_version" ]] || die "Tag $TAG does not match mod_version $mod_version"
    fi

    release_type=release
    if [[ -n "$tag_channel" ]]; then
      release_type="$tag_channel"
    fi
    if [[ "$release_type" == release && "${PRERELEASE:-false}" != false ]]; then
      die "Stable tag $TAG must not be marked Pre-release on GitHub"
    fi
    if [[ "$release_type" != release && "${PRERELEASE:-false}" != true ]]; then
      die "Alpha/Beta tag $TAG must be marked Pre-release on GitHub"
    fi

    # The release must come from the branch it targets: the tagged commit has to be on it.
    target="${TARGET:-}"
    if [[ -n "$target" && ! "$target" =~ ^[0-9a-f]{40}$ ]]; then
      git show-ref --verify --quiet "refs/remotes/origin/$target" || die "Release target branch $target not found"
      git merge-base --is-ancestor HEAD "refs/remotes/origin/$target" \
        || die "Tag $TAG points to a commit that is not on the release target branch $target"
    fi

    output version "$version"
    output release_type "$release_type"
    output minecraft_version "$minecraft"
    echo "Release $TAG: version $version, $release_type, Minecraft $minecraft"
    ;;

  artifact)
    [[ -n "${VERSION:-}" ]] || die "VERSION is empty"
    [[ -n "${MINECRAFT:-}" ]] || die "MINECRAFT is empty"
    libs="versions/$MINECRAFT/build/libs"
    [[ -d "$libs" ]] || die "$libs not found"
    mapfile -t jars < <(find "$libs" -maxdepth 1 -type f -name '*.jar' \
      ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-dev.jar' | sort)
    [[ "${#jars[@]}" -eq 1 ]] || die "Expected exactly one distribution JAR in $libs; got ${#jars[@]}"
    jar="${jars[0]}"
    [[ "$(basename "$jar")" == "dimensional-flux-$VERSION+$MINECRAFT.jar" ]] \
      || die "JAR $(basename "$jar") is not named for version $VERSION and Minecraft $MINECRAFT"
    metadata="$(unzip -p "$jar" META-INF/neoforge.mods.toml)" || die "NeoForge metadata missing from JAR"
    grep -Fq "version=\"$VERSION\"" <<< "$metadata" || die "Mod version metadata is not $VERSION"
    grep -Fq "versionRange=\"[$MINECRAFT]\"" <<< "$metadata" || die "Minecraft metadata range is not [$MINECRAFT]"
    output jar "$jar"
    echo "Artifact $jar: version $VERSION, Minecraft $MINECRAFT"
    ;;

  *)
    die "Usage: validate-release.sh versions | tag | artifact"
    ;;
esac
