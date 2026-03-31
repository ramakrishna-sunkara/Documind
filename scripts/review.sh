#!/bin/bash
# DocuMind Code Review Script
# Run this locally before pushing code

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       DocuMind Code Review                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# Function to run a check
run_check() {
    local name=$1
    local command=$2
    
    echo -e "${BLUE}▶ Running $name...${NC}"
    
    if eval "$command" 2>&1; then
        echo -e "${GREEN}✓ $name passed${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}✗ $name failed${NC}"
        echo ""
        return 1
    fi
}

# Check if we're in the project root
if [ ! -f "build.gradle.kts" ]; then
    echo -e "${RED}Error: Run this script from the project root directory${NC}"
    exit 1
fi

# 1. Run Detekt
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if ! run_check "Detekt (Static Analysis)" "./gradlew detekt --continue"; then
    ((ERRORS++))
fi

# 2. Run ktlint
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if ! run_check "ktlint (Code Formatting)" "./gradlew ktlintCheck --continue"; then
    ((ERRORS++))
fi

# 3. Run Android Lint
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if ! run_check "Android Lint" "./gradlew lint --continue"; then
    ((WARNINGS++))
fi

# 4. Run Project Rules Check (if Python available)
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if command -v python3 &> /dev/null; then
    # Get changed files
    CHANGED_FILES=$(git diff --name-only HEAD~1 2>/dev/null || git diff --name-only --cached 2>/dev/null || echo "")
    
    if [ -n "$CHANGED_FILES" ]; then
        echo -e "${BLUE}▶ Running Project Rules Check...${NC}"
        if python3 scripts/check-rules.py --rules code-review-rules/ --files "$CHANGED_FILES" 2>&1; then
            echo -e "${GREEN}✓ Project Rules passed${NC}"
        else
            echo -e "${YELLOW}⚠ Project Rules check found issues${NC}"
            ((WARNINGS++))
        fi
    else
        echo -e "${YELLOW}No changed files to check against project rules${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Python3 not found, skipping project rules check${NC}"
fi

# Summary
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║              Review Summary                ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Ready to push.${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ $WARNINGS warning(s) found. Consider fixing before push.${NC}"
    exit 0
else
    echo -e "${RED}✗ $ERRORS error(s) and $WARNINGS warning(s) found.${NC}"
    echo -e "${RED}Please fix errors before pushing.${NC}"
    exit 1
fi
