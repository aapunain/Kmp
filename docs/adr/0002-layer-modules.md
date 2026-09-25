# ADR-0002: Use flat layer modules, with `core/` as a directory rather than a module

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

Clean architecture can be split two ways: one module per layer (`:domain`, `:data`,
`:network`, `:presentation`), or one module per feature with layers as packages
inside (`:feature:items:domain`). The project has a single feature today and will
grow.

## Decision

Flat layer modules, grouped under a `core/` **directory**:

```
core/domain  core/data  core/network  core/concurrency   presentation/  contract/
```

`core/` has no `build.gradle.kts` and there is no `:core` project. It is purely
organisational, and it establishes the axis that a future `feature/` directory will
contrast with.

Inside each layer module, code is packaged **by feature**:
`com.self.kmp.domain.items.*`, `com.self.kmp.data.items.*`. That makes a later split
into `feature/<name>/` a folder move rather than an untangling job.

## Consequences

Easy: fewer modules to configure now, and a layout that matches how the layers are
usually discussed.

Hard: layer modules grow monolithic. In two years `:core:data` could hold forty
repositories, and every feature change recompiles all of them. Cross-feature imports
inside a layer module are legal, so nothing stops `items` code reaching into
`orders` code — that is the specific risk feature modules would have removed.

Cost accepted knowingly, with feature packaging as the mitigation and the migration
path kept open.

Note: `core/` currently prefixes every non-app module, so it carries little
information yet. It earns its keep when `feature/` appears.

## Enforcement

- `./gradlew architectureCheck` verifies every module in `settings.gradle.kts` has a
  charter and that dependencies match `moduleGraph`.
- Feature packaging inside modules is review-only.

## Alternatives considered

**Feature modules from the start** (`:feature:items:{domain,data,presentation}`). This
was the recommendation: it makes "feature A cannot touch feature B's internals" a
compiler guarantee, and convention plugins reduce the per-module cost to a two-line
build file. Rejected by the project owner in favour of matching the layer vocabulary
directly while there is one feature.

**A `:core` module containing the layers.** Rejected, see
[ADR-0003](0003-no-catch-all-modules.md).
