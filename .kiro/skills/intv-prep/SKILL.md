---
name: intv-prep
description: Interview preparation log. Use whenever the user's message contains the tag "intv" (for example "intv: how does a ViewModel survive rotation?"). Answer the question, then record it under the right category in interview/INTV_PREP.md.
---

# intv-prep

Triggered by the tag `intv` anywhere in the user's message (`intv`, `intv:`, `#intv`, `[intv]`).
Without the tag, do nothing from this skill.

## Steps

1. **Answer the question first**, the normal way: short, diagrams and bullet points over prose.
2. **Record the question** in `interview/INTV_PREP.md`:
   - Write it as one clean interview question. Fix grammar and typos; keep the user's meaning.
   - Drop the `intv` tag and any project-specific context from the wording.
   - Record only the **main** question. Follow-ups that drill into an answer already in the file are not added.
   - Skip it if the same question (same meaning, not just same words) is already listed. Say so in one line.
   - If one message asks several distinct questions, record each one separately.
3. **Pick the category**:
   - Use an existing `##` heading if the question fits.
   - Otherwise add a new `##` heading with a short, general topic name (e.g. `Coroutines`, `Compose`, `Kotlin`, `Testing`, `System Design`).
   - Keep headings in a sensible order: general topics (Architecture) first, then platform topics.
4. **Number** questions as one running list across the whole file (1, 2, 3, …). When inserting into an earlier category, renumber everything after it.
5. **Confirm** at the end of the reply in one line: `Added to INTV_PREP.md → <Category> (#n)`.

## File rules

- Questions only. No answers, notes or explanations in the file.
- Keep the `# Interview questions` title as the first line.
- If the file does not exist, create it with that title and the first category.

## Example

User: `intv how do coroutines get cancelled when viewmodel is cleared`

File gains:

```markdown
## Coroutines

8. How are coroutines cancelled when a ViewModel is cleared?
```

Reply ends with: `Added to INTV_PREP.md → Coroutines (#8)`
