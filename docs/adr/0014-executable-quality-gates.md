# ADR-0014: Quality rules are executable, and one command means "safe"

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

This codebase is edited by multiple developers and by AI agents across different tools.
Two failure modes were observed directly while building it:

1. **Verifying all five targets took about ten separate Gradle invocations** — five test
   tasks plus Android, desktop, two web distributions and an iOS framework link. An
   agent that runs `./gradlew build`, sees green and reports success has verified
   almost nothing.
2. **Prose rules rot silently.** `README.md` described `/core` as shared code two
   commits after that became false.

And two bugs were found only by running tests on non-JVM targets and by feeding the
parser a malformed payload — see [KMP_PITFALLS.md](../KMP_PITFALLS.md).

## Decision

Rules are enforced in three tiers, and as much as possible is pushed into tier 1.

| Tier | Mechanism | Drift |
| --- | --- | --- |
| 1 Executable | compiler settings, Gradle checks, tests, generated docs | Impossible |
| 2 Checked | CI, coverage floor, formatters | Detected |
| 3 Prose | `AGENTS.md`, ADRs, pitfalls | Possible, so minimised and append-only |

Concretely:

- **`./gradlew verify`** is the single command that means safe: formatting, detekt,
  architecture rules, conventions, coverage, tests on all five targets, and every app
  entry point. ~6–7 minutes cold. `AGENTS.md` requires pasting its output.
- **`./gradlew qualityCheck`** is the seconds-long subset for pre-commit.
- **`explicitApi()`** makes visibility a decision rather than a default, so `internal`
  implementations cannot silently become public API.
- **`architectureCheck`** validates every module's dependencies against `moduleGraph`
  in both directions, and cross-checks `settings.gradle.kts` so a new module cannot
  appear without a charter.
- **`architectureDocs` / `architectureDocsCheck`** generate the module graph into
  `docs/ARCHITECTURE.md` and fail when the committed copy is stale.
- **`conventionsCheck`** greps for what the compiler cannot express: `Dispatchers.IO`
  outside `:core:concurrency`, and inline dependency versions.
- **Spotless + ktlint**, configured via `.editorconfig` so the IDE and build agree.
- **detekt** with `ignoreFailures = false`.
- **Kover** with a floor of 75%, a ratchet rather than a target.
- **CI** on GitHub Actions: Linux for everything host-agnostic, macOS for Apple targets.
  Free on this public repository.

When a gate fails, fix the cause. Disabling a rule to get green is prohibited by
`AGENTS.md`; if a rule is genuinely wrong, change it deliberately and say why.

## Consequences

Easy: "done" becomes checkable instead of a matter of opinion. Architectural mistakes
mostly fail to compile rather than needing to be spotted in review. Formatting is
uniform across tools, so diffs show semantic change only.

Hard: `verify` takes 6–7 minutes, which is the price of the guarantee. `explicitApi()`
cost a one-time pass adding `public` to about twenty declarations. detekt is on an
**alpha** release, because `dev.detekt:2.0.0-alpha.6` is the only line supporting
Kotlin 2.4 — if it becomes flaky, remove it from `qualityCheck` and supersede this ADR
rather than tolerating an unreliable gate.

`conventionsCheck` parses build files textually rather than inspecting Gradle
configurations, to stay configuration-cache safe. It would miss a project dependency
added indirectly by a plugin; none are today.

## Enforcement

Self-enforcing: these tasks are wired into `qualityCheck` and `verify`, run by CI on
every push and pull request, and by the optional pre-commit hook in `.githooks/`.

## Alternatives considered

**Documentation and code review only.** Rejected: this is what allowed the README to rot.

**Warn instead of fail.** Rejected: a warning is a rule that does not exist.

**Konsist or ArchUnit for architecture tests.** Reasonable, but both are JVM-only test
frameworks. A Gradle task fails earlier, needs no test infrastructure, and can also
generate the documentation from the same data.

**detekt 1.23.8 (stable).** Rejected: built against Kotlin 1.9, so it cannot reliably
parse Kotlin 2.4 sources.
