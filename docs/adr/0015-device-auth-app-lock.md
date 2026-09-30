# ADR-0015: Gate app launch behind the device screen lock on Android and iOS

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

When the app launches, ask the user to authenticate with whatever screen lock they
already have — fingerprint, face, PIN, pattern or password — but only where the platform
and the device support it, and only once per process. Desktop and web are out of scope.

Both mobile platforms have an API for this: `BiometricPrompt` with
`BIOMETRIC_STRONG or DEVICE_CREDENTIAL` on Android, `LAContext` with
`LAPolicyDeviceOwnerAuthentication` on iOS. Neither has a desktop or browser
equivalent — the browser's nearest relative is WebAuthn, which is server-verified
authentication rather than a local gate.

The design constraint that shapes everything: **the two operations need different
things.**

| Operation | Needs | Consequence |
| --- | --- | --- |
| Is device auth supported? | a `Context` | ordinary injected dependency |
| Show the prompt | a `FragmentActivity` | UI-scoped, cannot be injected |

## Decision

A `feature/auth` module containing its own `domain`, `data`, `presentation` and `di`
packages, per the rule in [ADR-0002](0002-layer-modules.md). `:app:shared` depends on
it; `:presentation` does not, so there is no feature-to-feature coupling.

Four ports in the feature's domain package:

```kotlin
interface AuthRepository    { suspend fun availability(): DeviceAuthAvailability }  // injected
interface AuthSessionCache  { fun isAuthenticated(): Boolean; fun markAuthenticated() }
interface DeviceAuthPrompt  { suspend fun authenticate(): DeviceAuthResult }        // NOT injected
```

`DeviceAuthPrompt` is obtained from the composition via
`@Composable expect fun rememberDeviceAuthPrompt()`. On Android the actual reads
`LocalContext.current as? FragmentActivity` and passes it as a constructor parameter, so
its lifetime is the composition's. **No long-lived object ever holds an Activity.**

The policy is a domain use case:

```kotlin
ShouldRunAuthUseCase(authRepository, authSessionCache):
    if (session.isAuthenticated()) false          // already unlocked this process
    else repository.availability() == AVAILABLE    // fails open otherwise
```

The ViewModel is a state machine over domain types only. It never touches a platform API
and never holds a prompt. The UI performs the prompt when the state says
`AWAITING_PROMPT` and reports a `DeviceAuthResult` back as an intent.

```
UI                                ViewModel                     Domain
availability via injected repo ──▶ ShouldRunAuthUseCase ──▶ prompt needed?
                                   phase = AWAITING_PROMPT | UNLOCKED
prompt.authenticate()          ──▶ PromptCompleted(result)
                                   Success → markAuthenticated(), UNLOCKED
                                   else    → BLOCKED(message)
```

**Session scope is one process.** In-memory only, bound as a Koin `single`. A
configuration change keeps the unlocked state because the process survives; process
death re-locks. Deliberately **never persisted** — writing "user is authenticated" to
disk turns the gate into something an attacker can defeat by editing a file.

**Fails open.** A device with no screen lock has nothing to prove, so refusing entry
would make the app unusable rather than more secure. Correct for an app lock; wrong for
protecting secrets at rest.

Both platforms accept a **device credential as well as a biometric**, deliberately.
The biometrics-only variants would lock out users who have a passcode but no enrolled
fingerprint.

## Consequences

Easy: the policy is a pure function of session state and availability, so it is fully
tested on all five targets with no device. Desktop and web need no special casing —
their `AuthRepository` reports `NOT_SUPPORTED` and the gate unlocks without ever asking
for a prompt. `MainActivity` needs no registration calls.

Hard, and worth being explicit about:

**This is a UX lock, not cryptographic protection.** It is the boolean pattern: auth
succeeds, UI shows. On a rooted or jailbroken device, or with a patched build, it is
bypassable. Protecting data *at rest* requires binding a key to the authentication —
`setUserAuthenticationRequired(true)` on a `KeyGenParameterSpec` on Android,
`SecAccessControl` with `.userPresence` on iOS — so failing authentication leaves bytes
undecryptable rather than merely undisplayed. Nothing sensitive is stored on device
today. **If a session token or personal data is ever cached locally, this ADR is not
sufficient and must be replaced.**

**`MainActivity` must extend `FragmentActivity`**, not `ComponentActivity`. That is a
hard `BiometricPrompt` requirement and no design removes it.

**Koin has no Android `Context`,** because it starts from a Composable in common code
([ADR-0009](0009-koin-and-wiring-ownership.md)). An `androidx.startup` `Initializer`,
registered in the feature's own `AndroidManifest.xml` so consumers inherit it by
manifest merge, captures the Application Context for `AndroidAuthRepository`. This is
process-scoped and holds only the Application Context, so unlike an Activity reference
it cannot leak or go stale.

**The retry effect needs an attempt counter.** `LaunchedEffect` keyed only on `phase`
would not re-fire when a second failure moves `AWAITING_PROMPT → BLOCKED →
AWAITING_PROMPT`, silently breaking retry. `AuthGateUiState.attempt` is part of the key.

**Prompt strings are hardcoded English.** They are rendered by the OS rather than
Compose, so they cannot come from `composeResources`; localising means passing strings
down from the caller.

`androidx.biometric` is pinned at **1.1.0**, the last stable release; the 1.4.0 line is
alpha only.

## Enforcement

- `architectureCheck` pins `:app:shared → :feature:auth` and keeps `:presentation` out.
- `ShouldRunAuthUseCaseTest` covers fail-open, once-per-process, and the short-circuit
  that skips the capability check when already authenticated.
- `AuthGateViewModelTest` covers prompt request, immediate unlock, success, cancel, and
  that retry increments `attempt`.
- The platform adapters are not unit tested — they are thin wrappers over OS APIs that
  cannot run on a host JVM. They are covered by compilation per target and are excluded
  from Kover for the same reason. **They need manual verification on a device.**

## Alternatives considered

**Injecting the authenticator into the use case, with a global `AuthActivityRegistry`
holding a `WeakReference<FragmentActivity>`.** This was built first and then replaced.
It took a UI-scoped resource and pushed it into an application-scoped singleton, which
forced a global mutable holder, a `register`/`unregister` lifecycle contract in
`MainActivity` that could be silently broken, and a defensive
`?: return NOT_SUPPORTED` branch that existed only to paper over the registration race
it created. Splitting capability-check from prompt removes all of it. The lesson
generalises: **a dependency that needs a UI-scoped handle belongs in the UI layer, not
in DI.**

**Passing `deviceSupportsAuth` into the use case as a parameter** instead of injecting
`AuthRepository`. Avoids needing an Android `Context` entirely. Rejected in favour of a
proper repository port, on the grounds that the capability question is a real data
source and the `androidx.startup` cost is small and contained.

**Starting Koin in `Application.onCreate` on Android** to get a `Context`. Rejected: it
breaks the single-startup-path property that ADR-0009 exists to protect.

**A KMP biometric wrapper library** such as `moko-biometry` or `biometric-kmp`.
Rejected: most implement the boolean pattern only, which is the part we would replace
first if the requirement hardened into protecting secrets. A thin `expect`/`actual` we
own is about the same size and leaves that door open.

**Failing closed** when the device has no screen lock. Rejected — it would make the app
unusable on an unsecured device while adding no protection. The correct response to "no
screen lock" is to store nothing worth protecting.

**Re-prompting after N seconds in the background.** Deferred. Process-lifetime is enough
for now; adding a grace period means putting a timestamp in `AuthSessionCache` and
observing lifecycle, which is a change to the port rather than to the policy.
