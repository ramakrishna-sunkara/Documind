#!/bin/bash
# Install Git hooks for DocuMind code review

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Installing DocuMind Git Hooks...${NC}"

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    echo -e "${YELLOW}Error: Not in a Git repository root${NC}"
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p .git/hooks

# Create pre-push hook
cat > .git/hooks/pre-push << 'EOF'
#!/bin/bash
# DocuMind pre-push hook
# Runs code review before allowing push

echo "Running pre-push code review..."

# Run the review script
if [ -f "scripts/review.sh" ]; then
    ./scripts/review.sh
    exit_code=$?
    
    if [ $exit_code -ne 0 ]; then
        echo ""
        echo "Push blocked due to code review failures."
        echo "Fix the issues and try again, or use --no-verify to skip (not recommended)."
        exit 1
    fi
else
    echo "Warning: scripts/review.sh not found, skipping review"
fi

exit 0
EOF

# Create pre-commit hook (lighter, runs ktlint only)
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
# DocuMind pre-commit hook
# Runs quick formatting check before commit

# Get staged Kotlin files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E "\.kt$|\.kts$" || true)

if [ -n "$STAGED_FILES" ]; then
    echo "Running ktlint on staged files..."
    
    # Run ktlint format (auto-fix)
    ./gradlew ktlintFormat --continue 2>/dev/null || true
    
    # Re-add formatted files
    echo "$STAGED_FILES" | xargs git add 2>/dev/null || true
fi

exit 0
EOF

# Make hooks executable
chmod +x .git/hooks/pre-push
chmod +x .git/hooks/pre-commit

echo -e "${GREEN}✓ Git hooks installed successfully!${NC}"
echo ""
echo "Installed hooks:"
echo "  - pre-commit: Auto-formats staged Kotlin files with ktlint"
echo "  - pre-push: Runs full code review before push"
echo ""
echo "To skip hooks (not recommended):"
echo "  git commit --no-verify"
echo "  git push --no-verify"
