# Project rules

Kiro's entry point. It holds no rules of its own: it inlines the canonical file so
that Kiro, Claude Code, Gemini CLI and every other tool are reading the same bytes.

The canonical copy lives at `docs/AGENTS.md`. The `AGENTS.md`, `CLAUDE.md` and
`GEMINI.md` files at the repository root are symlinks to it, because that is where
each tool looks for them.

If you want to change a rule, edit `docs/AGENTS.md`. Never restate one here.

#[[file:docs/AGENTS.md]]

<!--
Fallback if the inline reference above does not resolve: read docs/AGENTS.md.
Linked rather than copied on purpose.
-->

See [docs/AGENTS.md](../../docs/AGENTS.md) for the rules, and the documents it links
to for detail.
