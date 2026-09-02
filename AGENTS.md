## Git workflow

- The canonical development branch is `main`.
- Before normal development, switch to `main` and synchronize with `origin/main` using a fast-forward-only pull.
- Commit and push normal Codex development directly to `main`.
- Do not create or use `codex/*`, feature, or temporary development branches unless the user explicitly requests one.
- Never force-push, reset, or rewrite shared history.
- Preserve unrelated user changes.

## Release workflow

- Do not publish a GitHub Release automatically.
- Generate general-user-facing Japanese release notes first and wait for explicit user approval.
- Only after approval may a GitHub Release be published with the distributable JAR attached.
