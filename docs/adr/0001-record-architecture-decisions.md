# ADR-0001: Record decisions as append-only ADRs and derive current state

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

This codebase is maintained by multiple developers and by AI agents using different
tools (Kiro, Claude Code, Codex, Gemini CLI). Agents read whatever instruction files
are in the repository and act on them. That makes stale documentation actively
dangerous rather than merely untidy: a wrong rule gets followed confidently.

The problem was already real. `README.md` described `/core` as "code that will be
shared between all targets" — accurate when the IDE wizard generated it, wrong two
commits later once `core/` became a grouping directory and the shared code moved to
`:contract`. Nobody broke it; hand-written prose describing current state simply
rots.

## Decision

Split documentation into three kinds, treated differently:

1. **Current state is derived, never described.** The module graph is generated from
   `moduleGraph` into `docs/ARCHITECTURE.md`, and a stale copy fails the build.
2. **Decisions are append-only ADRs** in `docs/adr/`, dated. An accepted ADR is
   never edited except to mark it Superseded with a link to its replacement.
3. **Rules are executable wherever possible.** Prose is the last resort, reserved
   for things no build tool can express.

Any structural or convention decision requires an ADR. `AGENTS.md` makes this part
of the definition of done.

## Consequences

Easy: understanding *why* a rule exists years later, including for people and agents
who were not present. Conflicting guidance becomes detectable, because there is one
canonical location per fact.

Hard: the discipline of writing an ADR when you would rather just change the code.
Accepted deliberately — the cost is a few minutes, the alternative is a codebase
whose rules nobody can explain.

Cost: more files. Mitigated by keeping each one single-purpose and by capping
`AGENTS.md` at roughly 200 lines so it always fits in an agent's context.

## Enforcement

- `./gradlew architectureDocsCheck` fails when the generated graph is stale.
- ADR creation itself is review-only. There is no way to mechanically detect that a
  decision was made without being recorded.

## Alternatives considered

**A single hand-maintained ARCHITECTURE.md.** Rejected: this is exactly what rotted
in the README, and the failure mode is silent.

**A wiki or external doc tool.** Rejected: documentation that does not live beside
the code is not in an agent's context and is not reviewed with the change.

**Comments in code only.** Rejected: comments explain local intent well but cannot
express cross-module decisions, and they disappear when the code is refactored.
