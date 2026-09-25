# ADR-0003: No module may be named core, common, util or shared

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

The IDE wizard generated a `:core` module holding one function, `sayHello`, consumed
by both `:app:shared` and `:server`. When the layered architecture was designed, the
first proposal put `AppResult`, `AppError` and `DispatcherProvider` into it — a result
type, a failure vocabulary and a threading abstraction whose only shared property was
"more than one layer needs this".

That is a dumping ground forming in a design document before any code existed.

The underlying reason: `core`, `common`, `util`, `shared` and `base` describe a
module's **position in the dependency graph** — "the thing at the bottom that
everyone points at" — not its **responsibility**. There is a mechanical test. Can you
complete the sentence "*X does not belong in this module*" in a way someone could
actually violate?

| Module | The sentence | Violable? |
| --- | --- | --- |
| `:core:domain` | "HTTP does not belong here" | yes |
| `:core:concurrency` | "Anything that is not a dispatcher does not belong here" | yes |
| `:core` | "… ?" | no |

A rule you cannot state is a rule you cannot enforce, so the module accretes by
default and nothing is ever out of scope.

Worse, `:core` was consumed with `api(project(":core"))`, so anything added to it
became visible to every consumer of `:app:shared` transitively, with no compile error
to signal it.

## Decision

No module may be named `core`, `common`, `util`, `utils`, `shared`, `base` or `lib`.
Name modules for their responsibility. `core/` may exist as a grouping **directory**
only.

Before creating any module, answer:

1. State its charter in one sentence as "X does not belong here." If you cannot name
   something a person could plausibly get wrong, the module has no boundary.
2. Does it have two or more consumers? One consumer means it is an implementation
   detail of that consumer — use an `internal` package there instead.
3. Name it for its responsibility, never its position.

The wizard's `:core` was retired: `sayHello` moved into `:server`, and the module was
repurposed as `:contract` with a precise charter ([ADR-0010](0010-shared-http-contract.md)).

## Consequences

Easy: every module can be held to a charter, so "is this in the right place?" has an
answer. `:core:concurrency` cannot accumulate unrelated helpers, because putting a
JSON parser there is self-evidently absurd in a way that putting it in `:core` is not.

Hard: genuinely cross-cutting technical concerns each need their own small module.
`:core:concurrency` holds one interface and five one-line actuals. A thin module feels
like overhead — that thinness is the price of a defensible boundary.

## Enforcement

`./gradlew architectureCheck` requires every module in `settings.gradle.kts` to have
an entry in `moduleCharters`. The naming rule itself is review-only, backed by this
ADR and by `AGENTS.md`.

## Alternatives considered

**Keep `:core` but document its boundary carefully.** Rejected: the boundary cannot be
stated, so the documentation would be aspirational. Renaming it to `:common` was
proposed and rejected for the same reason — `common` is the same category of name.

**One `:core:common` for small shared utilities.** Rejected: it is the same module
with a longer name.
