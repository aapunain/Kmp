# What and why

<!-- One paragraph. What changed, and what problem it solves. -->

## Verification

<!--
Paste the actual output, not a claim. "All tests pass" is not evidence.

  ./gradlew verify

Example:
  verify green in 6m29s
  JVM 19 / iOS 19 / Android host 19 / Wasm 19 / JS 19 / server 2, 0 failures
-->

```
```

## Not verified

<!--
What this change does NOT prove. Be specific: iOS device behaviour, real backend
calls, anything needing hardware or a simulator you did not run. "Nothing" is a
valid answer if it is true.
-->

## Checklist

- [ ] `./gradlew verify` passes and its output is pasted above
- [ ] Diff contains only what this PR is about — no drive-by refactors
- [ ] No gate was disabled, suppressed, or had its exclusions widened to go green
- [ ] Architecture changed? `moduleGraph` updated, `./gradlew architectureDocs` run
- [ ] Structural or convention decision made? ADR added in `docs/adr/`
- [ ] New behaviour has a test in `commonTest` so it runs on all five targets
- [ ] Scratch files and temporary scripts removed

## AI assistance

<!--
Which tool, and what you checked yourself. Not a confession — it tells the reviewer
where to look hardest. E.g. "Kiro wrote the mapper; I verified the error translation
table against the ADR by hand."
-->
