# Code Review Agent - Setup Guide

This document explains how to configure the Code Review Agent for DocuMind.

## Overview

The Code Review Agent provides:
- **Local Review**: Pre-push checks via Git hooks
- **CI Review**: Automated PR review via GitHub Actions
- **Merge Blocking**: Required checks that block merging until fixed

## Quick Start

### Local Setup

```bash
# One-time setup - install Git hooks
./scripts/install-hooks.sh

# Manual review (optional)
./scripts/review.sh
```

### GitHub Setup (Required for Merge Blocking)

To make the code review mandatory for merging PRs, configure **Branch Protection Rules**:

#### Step 1: Go to Repository Settings

1. Navigate to your GitHub repository
2. Click **Settings** > **Branches**

#### Step 2: Add Branch Protection Rule

1. Click **Add branch protection rule**
2. Enter branch name pattern: `main` (repeat for `develop` if needed)

#### Step 3: Configure Required Checks

Enable these settings:

| Setting | Value |
|---------|-------|
| **Require a pull request before merging** | ✅ Enabled |
| **Require status checks to pass before merging** | ✅ Enabled |
| **Require branches to be up to date before merging** | ✅ Enabled |
| **Status checks that are required** | `Static Analysis & Review` |

#### Step 4: Save Rules

Click **Create** or **Save changes**.

## How It Works

```
Developer pushes code
        │
        ▼
┌───────────────────┐
│   PR Created      │
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  Code Review      │◄── GitHub Action runs automatically
│  Workflow         │
└───────────────────┘
        │
        ▼
   ┌────┴────┐
   │         │
   ▼         ▼
┌─────┐  ┌──────┐
│PASS │  │ FAIL │
└─────┘  └──────┘
   │         │
   ▼         ▼
Merge     Merge
Allowed   BLOCKED
```

## Checks Performed

| Check | Tool | Blocks Merge |
|-------|------|--------------|
| Static Analysis | Detekt | Yes (on errors) |
| Code Formatting | ktlint | Yes (on errors) |
| Android Lint | Android Lint | No (warnings only) |
| Project Rules | Custom | Yes (on errors) |

## Severity Levels

- **Error**: Must be fixed. Blocks merge.
- **Warning**: Should be fixed. Does not block merge.
- **Info**: Suggestion. Does not block merge.

## Fixing Issues

### View Issues in PR

1. Check the **Checks** tab on your PR
2. Click on **Static Analysis & Review**
3. Review inline comments on changed files
4. Check the summary comment for overview

### Fix Locally

```bash
# Run full review
./scripts/review.sh

# Auto-fix formatting issues
./gradlew ktlintFormat

# Check specific files
python3 scripts/check-rules.py --rules code-review-rules/ --files "path/to/file.kt"
```

## Bypassing Checks (Emergency Only)

If you absolutely must merge without passing checks (not recommended):

1. Repository admin can temporarily disable branch protection
2. Use `git push --no-verify` to skip local hooks

**Warning**: Bypassing should only be done in emergencies and requires admin approval.

## Troubleshooting

### Workflow Not Running

- Ensure `.github/workflows/code-review.yml` exists
- Check that the PR targets a protected branch (main, develop, feature/*)

### Check Not Appearing as Required

- Ensure the workflow has run at least once on the repository
- The status check name must match exactly: `Static Analysis & Review`

### False Positives

If a rule is incorrectly flagging valid code:
1. Check `code-review-rules/` for the rule definition
2. Update the pattern or add exceptions
3. Create a PR with the fix

## Configuration Files

| File | Purpose |
|------|---------|
| `.github/workflows/code-review.yml` | GitHub Action workflow |
| `config/detekt/detekt.yml` | Detekt rules configuration |
| `code-review-rules/*.md` | Project-specific rules |
| `.editorconfig` | Editor formatting settings |
| `scripts/review.sh` | Local review script |
| `scripts/check-rules.py` | Project rules checker |

## Support

For issues with the code review agent:
1. Check this documentation
2. Review the workflow logs in GitHub Actions
3. Contact the DevOps team
