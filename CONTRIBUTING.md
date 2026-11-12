# Contributing

Thanks for helping! Small, focused pull requests are easiest to review.

## Ground rules

- No `INTERNET` permission, no proprietary dependencies (Play Services, Firebase, analytics, crash reporters). CI enforces the permission check.
- Vault access only through the Storage Access Framework.
- New user-facing text goes into both `values/strings.xml` (English) and `values-de/strings.xml` (German). `StringsParityTest` fails otherwise.
- Every screen keeps light/dark `@ThemePreviews`.

## Setup

1. JDK 17+ and Android SDK 35 (Android Studio Ladybug or newer).
2. `./gradlew build` must pass.
3. Run `./gradlew ktlintFormat` before committing. ktlint and detekt are blocking in CI. Existing detekt findings live in `config/detekt/baseline.xml`; please do not add to it.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/):

```
feat(parser): support Dataview round brackets
fix(alarm): reschedule after time zone change
docs: explain write-back conflict handling
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`.

## Versioning

[Semantic Versioning](https://semver.org/). Version name and code live in `gradle.properties`. Every release gets a `CHANGELOG.md` entry and a fastlane changelog (`fastlane/metadata/android/*/changelogs/<versionCode>.txt`).

## Parser changes

`core-parser` is plain Kotlin. Add a test to `ReminderParserTest` and, for realistic cases, a line to the `sample-vault` plus the expected count in `SampleVaultTest`.
