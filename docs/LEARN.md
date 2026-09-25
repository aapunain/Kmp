# Learn

A reading list for developers working on this repo. One line each, with a pointer to
where the thing is actually used in our code so the docs connect to something real.

**This is a living file.** Add to it whenever you learn something that would have
saved you time. Keep entries to one line; if an entry needs a paragraph, it belongs
in [CONVENTIONS.md](CONVENTIONS.md) or as an [ADR](adr/).

Legend: **[used]** is in the codebase today · **[not yet]** is worth knowing but not wired in.

## Foundations

| Topic | One-liner | In our code |
| --- | --- | --- |
| [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) | Write logic once in `commonMain`, compile it for every target; `expect`/`actual` fills the platform gaps. | **[used]** every module |
| [KMP on Android](https://developer.android.com/kotlin/multiplatform) | Google's own KMP guidance, including which libraries are multiplatform-ready. | **[used]** background reading |
| [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) | Jetpack Compose extended to iOS, desktop and web, so one UI serves all five targets. | **[used]** `:presentation`, `:app:shared` |
| [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) | Structured concurrency: `suspend`, scopes, and why cancellation must never be swallowed. | **[used]** everywhere; see `safeApiCall` |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | Compile-time generated JSON serializers, no reflection, works on every target. | **[used]** `:contract` DTOs |

## Libraries

| Library | One-liner | In our code |
| --- | --- | --- |
| [Ktor client](https://ktor.io/docs/client-create-new-application.html) | Multiplatform HTTP client; engine is pluggable, which is why tests can swap in `MockEngine`. | **[used]** `:core:network` |
| [Koin](https://insert-koin.io/) · [KMP setup](https://insert-koin.io/docs/reference/koin-mp/kmp/) | Runtime DI with no code generation; each module publishes its own `Module`. | **[used]** every layer; see [ADR-0009](adr/0009-koin-and-wiring-ownership.md) |
| [Room for KMP](https://developer.android.com/kotlin/multiplatform/room) | SQLite abstraction that now works on iOS too; needs a per-platform driver factory. | **[not yet]** would land behind an interface in `:core:data` |

## AI-assisted development

| Resource | One-liner |
| --- | --- |
| [android/skills](https://github.com/android/skills) | Google's official repo of AI-optimised, modular skill files that teach agents current Android best practice. |
| [Claude Code docs](https://code.claude.com/docs) | Memory files, hooks, subagents, slash commands, and how context is assembled. |
| [docs/AGENTS.md](AGENTS.md) | Our own rules. Canonical for every tool; the root `AGENTS.md`/`CLAUDE.md`/`GEMINI.md` are symlinks to it. |

## Quality tooling we added

Each of these is wired into `./gradlew verify`. See [ADR-0014](adr/0014-executable-quality-gates.md)
for why they are build failures rather than guidelines.

| Tool | One-liner | Where it lives |
| --- | --- | --- |
| [Android Lint](https://developer.android.com/studio/write/lint) | Android-specific correctness and API-level checks. | via AGP |
| [ktlint](https://github.com/pinterest/ktlint) · [docs](https://ktlint.github.io/) | Kotlin formatter and style linter; our rules are in `.editorconfig`. | run through Spotless |
| [Spotless](https://github.com/diffplug/spotless) | Applies ktlint across the repo and can auto-fix: `./gradlew spotlessApply`. | root `build.gradle.kts` |
| [detekt](https://detekt.dev/) | Static analysis for smells and complexity, beyond formatting. | `config/detekt/detekt.yml` |
| [Kover](https://github.com/Kotlin/kotlinx-kover) | Coverage for Kotlin; ours is a regression ratchet, not a target. | root `build.gradle.kts` |
| [Git hooks](https://git-scm.com/docs/githooks) | Local pre-commit gate so manual edits get the same treatment as agent edits. | `.githooks/`, install via `scripts/install-hooks.sh` |
| [GitHub Actions](https://docs.github.com/en/actions) | Runs every gate on push and PR; Linux for most targets, macOS for Apple. | `.github/workflows/ci.yml` |
| [Gradle version catalogs](https://docs.gradle.org/current/userguide/platforms.html) | One file for every dependency version; inline versions fail `conventionsCheck`. | `gradle/libs.versions.toml` |

## Ours, and worth reading first

| Document | Why |
| --- | --- |
| [KMP_PITFALLS.md](KMP_PITFALLS.md) | Nine traps that already cost us time. Read before touching coroutines, Ktor or the build. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module boundaries, charters, and the test for adding a module. |
| [CONVENTIONS.md](CONVENTIONS.md) | MVI shape, the two-result-type error model, DI ownership, naming. |
| [TESTING.md](TESTING.md) | What to test in each layer, and why mocking libraries cannot be used here. |
| [adr/](adr/) | Fourteen decisions with the reasoning and the alternatives that lost. |

## Adding to this file

1. One line per entry. Say what it *is* and why it matters here, not what the docs say.
2. Link the canonical source, not a blog post, unless the blog post is genuinely better.
3. Mark it **[used]** or **[not yet]** so nobody assumes a library is wired in when it isn't.
4. Check the link resolves. A learning index full of 404s is worse than no index.
