## What and why

<!-- One or two sentences. Link issues with "Fixes #123". Keep one concern per pull request. -->

## How it was verified

<!-- Commands run and what you observed. AI agents: paste the command and its final lines;
     "should work" is not verification. -->

- [ ] `make quick` while iterating, then the relevant `make check-backend` / `make check-mobile`
- [ ] `make lint-repo` (workflows, scripts, spelling, Swift, architecture contracts)
- [ ] New logic has ViewModel / use-case / backend tests that assert behaviour
- [ ] UI changes: screenshot tests updated and the flow exercised on Android and iOS
      (add the `preview` label for an installable Android build)
- [ ] Mobile database changes include a SQLDelight `.sqm` migration
- [ ] No secrets, keystores, or `google-services.json` committed

## Review

- Address every AI review suggestion: apply it, or reply why not, then resolve the thread.
- Changes to paths in `.github/CODEOWNERS` wait for the owner's approval.
