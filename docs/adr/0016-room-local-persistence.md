# ADR-0016: Room 3 for local persistence, behind one narrow interface per table

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

The home screen showed a hardcoded list from `MockEngine`. It now shows todos the user
adds, which must survive restarts. The app targets Android, iOS, desktop, JS and Wasm,
and `:core:data` is deliberately free of platform source sets.

Two candidates: Room 3 (KSP, Google-official) and SQLDelight (own compiler, SQL-first).
A spike settled it, and produced three findings that were not obvious beforehand.

## Decision

**Room 3.0.3**, in a new `:core:database` module, exposed as one narrow interface per
table.

```
:core:database   TodoLocalStore (public)  ·  Room entity, DAO, @Database (internal)
:core:domain     Todo, TodoRepository, use cases
:core:data       TodoRepositoryImpl — maps TodoRecord <-> Todo
:presentation    TodoViewModel, TodoScreen
```

`TodoLocalStore` exposes `TodoRecord`, a plain data class, so no Room type escapes the
module. One interface **per table** rather than one app-wide `LocalDbStore`: a single
store would accumulate a method per feature until it became a god-interface.

Driver split by platform, because `sqlite-bundled` has no js/wasm variant:

| Target | Store |
| --- | --- |
| Android, iOS, desktop | Room + `sqlite-bundled` |
| JS, Wasm | `InMemoryTodoLocalStore` |

The seam is at the **store**, not at Room's builder, so a platform that cannot run
SQLite returns a different implementation of the same port rather than being forced to
produce a database.

## Consequences

Easy: todos persist on the three platforms where users expect it. Nothing above
`:core:database` knows which implementation it got, so domain, presentation and tests are
identical everywhere. `TodoRepositoryImpl` has no `AppResult` surface because the local
database is the source of truth and a query against it does not fail in ways the domain
must model.

Hard, and all three were discovered by building it:

**KSP has no Kotlin 2.4.x release.** The newest is 2.3.12, and KSP dropped its
`<kotlin>-<ksp>` version prefix at 2.3.0, so compatibility is no longer readable from the
version string. It was tested rather than assumed: **KSP 2.3.12 does run on Kotlin
2.4.20** and Room's codegen succeeds on all five targets. That is an unsupported
combination on paper, so a future Kotlin bump may break it before KSP catches up.

**KSP breaks the Gradle configuration cache.** `KspAATask` cannot be serialized, which is
a hard build failure. `org.gradle.configuration-cache` is now `false`, with a comment
saying why. This costs build speed on every task, for everyone. It is the single largest
price paid for choosing Room over SQLDelight.

**JS and Wasm get no real database.** `androidx.sqlite:sqlite-web` exists and provides
`WebWorkerSQLiteDriver`, but it requires a JS `Worker`, which means an npm dependency on
`@sqlite.org/sqlite-wasm`, a worker entry script, webpack wiring, and COOP/COEP response
headers for OPFS. That is its own piece of work. Web todos live until the page reloads.

Two smaller ones: `@Database` must list every entity, so `:core:database` inevitably
knows about every table — inherent to Room, not a layering mistake. And
`exportSchema = false`, because the `androidx.room3` Gradle plugin that supplies
`room.schemaLocation` is only published as `3.1.0-alpha01`; turn it on when migration
tests are wanted.

`fallbackToDestructiveMigration(dropAllTables = true)` is set. Acceptable pre-release;
it must be replaced with real migrations before any build ships to a user.

## Enforcement

- `architectureCheck` pins `:core:database` to zero project dependencies and
  `:core:data → :core:database`.
- `internal` plus `explicitApi()` means no Room type can leak; `TodoLocalStore` and
  `TodoRecord` are the only public declarations.
- `AddTodoUseCaseTest` covers trimming, blank rejection and duplicates.
- The Room-backed store is not unit tested: it needs a real SQLite driver. It is covered
  by compilation per target and needs manual verification on a device.

## Alternatives considered

**SQLDelight 2.4.0.** Uses its own compiler, so no KSP and the configuration cache would
have survived — the most attractive property it had. Rejected because the project owner
chose Room, and because SQLDelight's browser drivers are also experimental, so it would
not have solved web persistence either.

**Downgrading Kotlin to 2.3.x** to get a nominally supported KSP. Rejected once the
spike showed 2.4.20 works; downgrading the whole project for a paper guarantee is a poor
trade.

**One app-wide `LocalDbStore`.** Rejected: it grows a method per feature forever.

**Exposing Room's `AppDatabase` directly** and letting `:core:data` use DAOs. Rejected:
it puts Room types on the data layer's compile classpath.

**Writing the browser driver now.** Deferred rather than rejected. The port already
allows it: implement `createTodoLocalStore()` for js/wasmJs and nothing else changes.
