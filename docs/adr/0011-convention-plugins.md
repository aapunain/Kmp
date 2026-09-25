# ADR-0011: Convention plugins in `build-logic`; versions only in the catalog

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

Each KMP library module needs roughly thirty lines declaring six targets and the
Android block. With seven such modules that is the same block copied seven times, and
bumping `compileSdk` becomes a seven-file change. Worse for correctness: if one
module's target list drifts from another's, Gradle fails with "no matching variant"
errors that are hard to diagnose.

## Decision

An included build at `build-logic/` with precompiled script plugins:

| Plugin | Responsibility |
| --- | --- |
| `kmp.library` | The six targets, the Android block, JVM 11, `explicitApi()`, Kover, `kotlin-test` |
| `kmp.library.compose` | The above plus Compose, and `binaries.executable()` for js/wasmJs |
| `kmp.architecture` | The executable architecture rules; applied to the root only |

A module build file collapses to:

```kotlin
plugins { id("kmp.library") }
```

Two supporting rules:

- **Android namespace derives from the full Gradle path**, not the leaf name:
  `"com.self.kmp" + project.path.replace(":", ".")`. Leaf names repeat once features
  arrive, and duplicate Android namespaces are a hard build failure.
- **Versions live only in `gradle/libs.versions.toml`.** Never inline a coordinate.

## Consequences

Easy: target lists cannot drift. One place to bump a version or a target.

Hard: two indirections to understand — precompiled script plugins have their own
quirks, and the generated `libs` accessor is unavailable inside them, so the catalog
is read via `VersionCatalogsExtension`. A convention plugin can only apply a plugin
whose jar is on `build-logic`'s compile classpath, hence the `gradlePlugin-*` catalog
entries. Both quirks are recorded in
[KMP_PITFALLS.md](../KMP_PITFALLS.md#4-the-libs-accessor-does-not-exist-in-precompiled-script-plugins).

## Enforcement

- `./gradlew conventionsCheck` fails on any hardcoded `group:artifact:version` string
  in a `.gradle.kts` file.
- Target-list consistency is enforced structurally: modules do not declare targets at all.

## Alternatives considered

**Duplicated build files.** Rejected: seven copies of thirty lines, and drift is a
silent correctness problem rather than a visible one.

**`subprojects { }` configuration from the root.** Rejected: fights the configuration
cache and Gradle's project isolation direction.

**`buildSrc` instead of an included build.** Rejected: a change to `buildSrc`
invalidates the whole build's configuration cache; an included build is more granular.
