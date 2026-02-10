# Bolt's Journal

## 2024-05-22 - Initial Setup
**Learning:** Started tracking performance learnings.
**Action:** Always check this file before starting.

## 2024-05-22 - GitHub API Rate Limit Optimization
**Learning:** GitHub's `repos.get` endpoint returns `open_issues_count` which includes both Issues and PRs. By combining this cheap call with a single Search API call for PRs, we can derive the Issue count without a second expensive Search call.
**Action:** Use `getRepositoryStats` pattern instead of separate `getOpenIssueCount` and `getOpenPullRequestCount` calls when both are needed.

## 2024-05-22 - Pre-existing Type Errors
**Learning:** `App.tsx` contains pre-existing `TS2345` errors regarding `Set<unknown>` not being assignable to `Set<string>`. These errors do not block `vite build` or runtime, but show up in `tsc`.
**Action:** Ignore `TS2345` errors in `App.tsx` related to `Set` unless touching that specific code block.
