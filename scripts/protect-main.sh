#!/usr/bin/env bash
# Protection for main that lets AI-agent pull requests merge quickly but never unchecked:
# - every CI job and the AI review must pass (path-filtered jobs that skip count as passing),
# - every review thread must be resolved (the AI reviewer's suggestions are threads),
# - pull requests touching .github/CODEOWNERS paths need the code owner's approval,
# - force pushes and branch deletion are blocked.
# Admins can still push directly. Also turns on auto-merge, so an agent can run
# `gh pr merge --auto --squash` and the pull request merges once everything above is green.
# Finally it creates the "release" environment that Mobile CD and Mobile Promote run in. Only
# protected branches may use it, so release secrets stored there (gh secret set NAME --env
# release) are never readable by workflow code in a pull request.
# Usage: scripts/protect-main.sh [branch]
set -euo pipefail
command -v gh >/dev/null || { printf 'error: install the GitHub CLI (https://cli.github.com)\n' >&2; exit 1; }
gh auth status >/dev/null 2>&1 || { printf 'error: run gh auth login first\n' >&2; exit 1; }
branch="${1:-main}"

gh api "repos/{owner}/{repo}" --method PATCH --silent \
  -F allow_auto_merge=true -F delete_branch_on_merge=true

# Contexts are job names in .github/workflows/ci.yml and ai-review.yml.
gh api "repos/{owner}/{repo}/branches/$branch/protection" --method PUT --silent --input - <<'JSON'
{
  "required_status_checks": {
    "strict": false,
    "contexts": ["Template contracts", "Security scan", "Backend", "Mobile", "iOS", "Fastlane", "AI review"]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "require_code_owner_reviews": true,
    "dismiss_stale_reviews": true
  },
  "required_conversation_resolution": true,
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
gh api "repos/{owner}/{repo}/environments/release" --method PUT --silent --input - <<'JSON'
{ "deployment_branch_policy": { "protected_branches": true, "custom_branch_policies": false } }
JSON
printf 'Protected %s, enabled auto-merge, and created the "release" environment.\n' "$branch"
printf 'Store release secrets there: gh secret set NAME --env release (see docs/RELEASING.md).\n'
