# DocuMind Code Review Rules

Machine-parseable rules for automated code review in CI pipelines.

## Rule Format

Each rule follows this structure:

```markdown
## RULE-ID: Rule Name
- **Severity**: error | warning | info
- **Pattern**: regex pattern to detect violations
- **Message**: Description of the violation
- **Fix**: How to fix the issue

### Bad
<code example that violates the rule>

### Good
<code example that follows the rule>
```

## Rule Categories

| File | Category | Rules |
|------|----------|-------|
| `architecture.md` | Clean + MVVM Architecture | ARCH-001 to ARCH-005 |
| `compose.md` | Jetpack Compose | COMPOSE-001 to COMPOSE-005 |
| `coroutines.md` | Coroutines & Flow | COROUTINE-001 to COROUTINE-005 |
| `error-handling.md` | Error Handling | ERROR-001 to ERROR-003 |
| `di.md` | Hilt Dependency Injection | DI-001 to DI-003 |
| `code-quality.md` | General Code Quality | QUALITY-001 to QUALITY-005 |

## Severity Levels

- **error**: Must be fixed before merge. Blocks PR.
- **warning**: Should be fixed. Does not block PR but requires justification.
- **info**: Suggestion for improvement. Does not block PR.

## Usage

### Local
```bash
python3 scripts/check-rules.py --rules code-review-rules/ --files "path/to/files"
```

### CI
Rules are automatically checked in the GitHub Action workflow on every PR.

## Relationship to Cursor Rules

These rules are derived from `.cursor/rules/android-principal-engineer.mdc` but formatted for machine parsing. Keep both in sync:

| Cursor Rules | CI Rules |
|--------------|----------|
| Human-readable guidance | Machine-parseable patterns |
| IDE hints for Cursor AI | GitHub Action enforcement |
| `.mdc` format | `.md` with structured sections |
