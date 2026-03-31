#!/usr/bin/env python3
"""
DocuMind Project Rules Checker

Parses markdown rule files and checks Kotlin source files against patterns.
"""

import argparse
import json
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional


@dataclass
class Rule:
    """Represents a code review rule."""
    id: str
    name: str
    severity: str  # error, warning, info
    pattern: str
    message: str
    fix: str
    file: str


@dataclass
class Violation:
    """Represents a rule violation found in code."""
    rule_id: str
    rule_name: str
    severity: str
    message: str
    fix: str
    file: str
    line: int
    content: str


def parse_rule_file(file_path: Path) -> List[Rule]:
    """Parse a markdown rule file and extract rules."""
    rules = []
    
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception as e:
        print(f"Warning: Could not read {file_path}: {e}", file=sys.stderr)
        return rules
    
    # Split by rule headers (## RULE-ID: Name)
    rule_pattern = r'##\s+([A-Z]+-\d+):\s+(.+?)(?=\n)'
    severity_pattern = r'\*\*Severity\*\*:\s*(\w+)'
    pattern_pattern = r'\*\*Pattern\*\*:\s*`([^`]+)`'
    message_pattern = r'\*\*Message\*\*:\s*(.+?)(?=\n)'
    fix_pattern = r'\*\*Fix\*\*:\s*(.+?)(?=\n)'
    
    # Find all rule sections
    rule_matches = list(re.finditer(rule_pattern, content))
    
    for i, match in enumerate(rule_matches):
        rule_id = match.group(1)
        rule_name = match.group(2).strip()
        
        # Get the section content (until next rule or end)
        start = match.end()
        end = rule_matches[i + 1].start() if i + 1 < len(rule_matches) else len(content)
        section = content[start:end]
        
        # Extract rule properties
        severity_match = re.search(severity_pattern, section)
        pattern_match = re.search(pattern_pattern, section)
        message_match = re.search(message_pattern, section)
        fix_match = re.search(fix_pattern, section)
        
        if pattern_match:
            rules.append(Rule(
                id=rule_id,
                name=rule_name,
                severity=severity_match.group(1).lower() if severity_match else 'warning',
                pattern=pattern_match.group(1),
                message=message_match.group(1).strip() if message_match else rule_name,
                fix=fix_match.group(1).strip() if fix_match else '',
                file=file_path.name
            ))
    
    return rules


def check_file(file_path: Path, rules: List[Rule]) -> List[Violation]:
    """Check a single file against all rules."""
    violations = []
    
    try:
        content = file_path.read_text(encoding='utf-8')
        lines = content.split('\n')
    except Exception as e:
        print(f"Warning: Could not read {file_path}: {e}", file=sys.stderr)
        return violations
    
    # Check each rule
    for rule in rules:
        try:
            # Compile pattern with multiline support
            pattern = re.compile(rule.pattern, re.MULTILINE | re.DOTALL)
            
            # Find all matches
            for match in pattern.finditer(content):
                # Calculate line number
                line_num = content[:match.start()].count('\n') + 1
                
                # Get the line content
                if 0 < line_num <= len(lines):
                    line_content = lines[line_num - 1].strip()
                else:
                    line_content = match.group(0)[:50] + '...'
                
                violations.append(Violation(
                    rule_id=rule.id,
                    rule_name=rule.name,
                    severity=rule.severity,
                    message=rule.message,
                    fix=rule.fix,
                    file=str(file_path),
                    line=line_num,
                    content=line_content[:100]
                ))
        except re.error as e:
            # Skip invalid regex patterns
            print(f"Warning: Invalid pattern in {rule.id}: {e}", file=sys.stderr)
            continue
    
    return violations


def main():
    parser = argparse.ArgumentParser(description='DocuMind Project Rules Checker')
    parser.add_argument('--rules', required=True, help='Path to rules directory')
    parser.add_argument('--files', help='Files to check (space or newline separated)')
    parser.add_argument('--output', help='Output JSON file for results')
    parser.add_argument('--severity', default='warning', 
                        choices=['error', 'warning', 'info'],
                        help='Minimum severity to report')
    args = parser.parse_args()
    
    rules_dir = Path(args.rules)
    if not rules_dir.exists():
        print(f"Error: Rules directory not found: {rules_dir}", file=sys.stderr)
        sys.exit(1)
    
    # Parse all rule files
    rules = []
    for rule_file in rules_dir.glob('*.md'):
        if rule_file.name != 'README.md':
            rules.extend(parse_rule_file(rule_file))
    
    print(f"Loaded {len(rules)} rules from {rules_dir}")
    
    # Get files to check
    files_to_check = []
    if args.files:
        # Split by whitespace or newlines
        file_list = re.split(r'[\s\n]+', args.files.strip())
        for f in file_list:
            f = f.strip()
            if f and f.endswith(('.kt', '.kts')):
                path = Path(f)
                if path.exists():
                    files_to_check.append(path)
    else:
        # Check all Kotlin files in app and prescription_demo
        for module in ['app', 'prescription_demo']:
            module_path = Path(module)
            if module_path.exists():
                files_to_check.extend(module_path.rglob('*.kt'))
    
    print(f"Checking {len(files_to_check)} files...")
    
    # Check files
    all_violations = []
    severity_order = {'error': 0, 'warning': 1, 'info': 2}
    min_severity = severity_order.get(args.severity, 1)
    
    for file_path in files_to_check:
        violations = check_file(file_path, rules)
        # Filter by severity
        violations = [v for v in violations 
                     if severity_order.get(v.severity, 1) <= min_severity]
        all_violations.extend(violations)
    
    # Output results
    if args.output:
        # JSON output for CI
        output_data = {
            'total_violations': len(all_violations),
            'by_severity': {
                'error': len([v for v in all_violations if v.severity == 'error']),
                'warning': len([v for v in all_violations if v.severity == 'warning']),
                'info': len([v for v in all_violations if v.severity == 'info'])
            },
            'violations': [
                {
                    'rule_id': v.rule_id,
                    'rule_name': v.rule_name,
                    'severity': v.severity,
                    'message': v.message,
                    'fix': v.fix,
                    'file': v.file,
                    'line': v.line,
                    'content': v.content
                }
                for v in all_violations
            ]
        }
        
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(output_data, indent=2))
        print(f"Results written to {args.output}")
    
    # Console output
    if all_violations:
        print(f"\nFound {len(all_violations)} issue(s):\n")
        
        # Group by file
        by_file = {}
        for v in all_violations:
            if v.file not in by_file:
                by_file[v.file] = []
            by_file[v.file].append(v)
        
        for file_path, violations in sorted(by_file.items()):
            print(f"  {file_path}:")
            for v in sorted(violations, key=lambda x: x.line):
                severity_icon = {'error': '✗', 'warning': '⚠', 'info': 'ℹ'}
                icon = severity_icon.get(v.severity, '•')
                print(f"    {icon} Line {v.line}: [{v.rule_id}] {v.message}")
            print()
        
        # Summary
        errors = len([v for v in all_violations if v.severity == 'error'])
        warnings = len([v for v in all_violations if v.severity == 'warning'])
        
        if errors > 0:
            print(f"✗ {errors} error(s), {warnings} warning(s)")
            sys.exit(1)
        else:
            print(f"⚠ {warnings} warning(s)")
            sys.exit(0)
    else:
        print("✓ No issues found")
        sys.exit(0)


if __name__ == '__main__':
    main()
