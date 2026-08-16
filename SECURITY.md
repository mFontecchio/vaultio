# Security Policy

## Supported versions

Security fixes are applied to the latest published release on the default branch. Older tags may not
receive backports.

## Reporting a vulnerability

Please **do not** open a public issue for security reports.

1. Use [private vulnerability reporting](https://github.com/mFontecchio/vaultio/security/advisories/new)
   on this repository (preferred), or
2. Contact the maintainer privately via the email on their GitHub profile.

Include steps to reproduce, affected build types/versions, and impact. We will acknowledge reports
and coordinate disclosure when a fix is ready.

## Secrets and signing

- Never commit API keys, `local.properties`, keystores (`*.jks` / `*.keystore`), or
  `keystore.properties`.
- JustTCG API keys belong only in the in-app Settings screen (DataStore), not in the repo or
  BuildConfig.
- Release / nightly signing credentials live in GitHub Actions secrets or a local gitignored
  `keystore.properties` for maintainers.
- Sideload in-app updates download APKs from this repo’s GitHub Releases and verify package
  signature + `versionCode` before prompting install. Play-installed builds do not use that path.
