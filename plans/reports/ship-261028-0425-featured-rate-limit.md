# Featured rate-limit shipping status

The fix was committed as `334aa77d9be4b38f896a39669406b992f516d266`, pushed to
`fix/featured-rate-limit`, and published as [PR #17](https://github.com/arcrek/FlickrImageBrowser/pull/17),
which closes [issue #16](https://github.com/arcrek/FlickrImageBrowser/issues/16) on merge.

Independent review found and resolved a stale Retry snackbar across query changes.
Final review has no remaining findings. Fresh isolated verification passed all
69 unit tests, `lintDebug`, and `assembleDebug`. See the
[QA report](./test-261028-0425-featured-rate-limit.md) and
[review report](./code-review-261028-0425-featured-rate-limit.md).

PR #17 merged as `f6f1dd4d47f7cd3dbf1bde555f585edf94bcea39` on 2026-10-28
at 04:39:41 UTC. Initial readiness reported REVIEW_REQUIRED. The user clarified
that arcrek is exempt; live ruleset 25072853 verified User 64296818 has
bypass_mode=always, matching the authenticated account. The normal CLI path
refused policy-blocked metadata, so the authorized merge used `--admin` with
`--match-head-commit 334aa77d9be4b38f896a39669406b992f516d266` to apply the existing
account allowance. No ruleset changes, force-push, or auto-merge were used.
The code review was posted as COMMENTED because self-approval is disallowed.

GitHub Actions has no configured workflows and the head has no check runs or
commit statuses. CI is therefore not configured, not green. Device/live API tests
were not run. Journal writing was skipped by preference.

The unrelated codebase-remediation plan and pre-existing untracked reports were
preserved. Its registry entry was not completed or closed for this distinct fix.

Operational notes: the installed PR-body validator failed once reading a pipe
with `EAGAIN`; validation succeeded using a saved body file. `gh pr edit` hit a
deprecated Projects API field; the final body was updated via GitHub REST instead.

Remaining action: none for the requested ship and merge.
