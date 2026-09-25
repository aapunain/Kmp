# Architecture Decision Records

## Why these exist

Documentation that describes **current state** in prose always rots. This
repository's own README described `/core` as "code shared between all targets" two
commits after that stopped being true.

So the split is:

- **Current state** is *derived*, never *described*. The module graph in
  [../ARCHITECTURE.md](../ARCHITECTURE.md) is generated from `moduleGraph` in
  `build-logic/src/main/kotlin/kmp.architecture.gradle.kts`, and
  `./gradlew architectureDocsCheck` fails if the committed copy is stale.
- **Decisions** are recorded here as dated, **append-only** records. A historical
  record cannot go stale because it never claimed to describe the present.
- **Rules** are enforced by the build wherever possible, so they cannot drift.

## How to use them

- **Never edit an accepted ADR** except to change its status to Superseded and link
  the ADR that replaced it. Write a new ADR instead.
- Add an ADR for any structural or convention decision: a new module, a changed
  dependency direction, a new library, a shift in a pattern.
- Read the relevant ADR before proposing to change a rule. The ADR explains what
  the rule protects against, which is usually not obvious from the rule.
- Start from [0000-template.md](0000-template.md).

## Index

| ADR | Decision |
| --- | --- |
| [0001](0001-record-architecture-decisions.md) | Record decisions as append-only ADRs; derive current state |
| [0002](0002-layer-modules.md) | Flat layer modules; `core/` is a directory, not a module |
| [0003](0003-no-catch-all-modules.md) | No module named core, common, util or shared |
| [0004](0004-domain-has-no-dependencies.md) | `:core:domain` depends on nothing |
| [0005](0005-two-result-types.md) | Separate transport and domain result types |
| [0006](0006-datasources-return-not-throw.md) | Remote data sources return `ApiResult` rather than throwing |
| [0007](0007-injected-dispatchers.md) | Dispatchers are injected; `Dispatchers.IO` is banned from common code |
| [0008](0008-mvi-lite-presentation.md) | MVI-lite presentation with display-ready state |
| [0009](0009-koin-and-wiring-ownership.md) | Koin, with wiring ownership following visibility |
| [0010](0010-shared-http-contract.md) | A `:contract` module shared with the server |
| [0011](0011-convention-plugins.md) | Convention plugins in `build-logic`; versions only in the catalog |
| [0012](0012-fakes-not-mocks.md) | Hand-written fakes, and every test runs on every target |
| [0013](0013-mock-engine-default.md) | MockEngine is the default HTTP engine; the engine is a parameter |
| [0014](0014-executable-quality-gates.md) | Quality rules are executable; one command means "safe" |
