# ADR-0017: Navigation 3, with deep link parsing in common code

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

The app grew a second screen: the todo list is home, and the original network-backed
items list is kept as a sample reachable from the FAB. Both screens need deep links.

Until now there was no navigation at all — `App()` rendered one screen directly.

## Decision

**Navigation 3**, with a notable artifact split:

| Artifact | Group |
| --- | --- |
| `navigation3-runtime` | `androidx.navigation3` — already multiplatform (common/js/jvm/native/wasm) |
| `navigation3-ui` | `org.jetbrains.androidx.navigation3` — the Compose UI layer |
| `lifecycle-viewmodel-navigation3` | `org.jetbrains.androidx.lifecycle` |

Mixing `androidx` and `org.jetbrains.androidx` is correct here, not a mistake. It is
surprising enough to be worth the comment that sits in the version catalog.

Routes are `@Serializable` `data object`s implementing `NavKey`, held in `:app:shared`.
The `NavDisplay` lives in the composition root, inside the `AuthGate`, so the lock covers
every destination.

**Deep link parsing is common code; only delivery is platform-specific.**

```
backStackFor(deepLink: String?): List<NavKey>       // commonMain, in :app:shared

kmp://todo     https://kmp.self.com/todo     -> [TodoRoute]
kmp://sample   https://kmp.self.com/sample   -> [TodoRoute, SampleRoute]
```

`App(deepLink: String?)` takes the URL as a parameter. Android reads `intent.data`; other
entry points can pass one when they need to. The parser uses plain string handling rather
than a URI type, because `android.net.Uri` does not exist in common code and the accepted
shapes are simple.

A deep link to the sample returns **a two-entry stack**, so Back still reaches the todo
list rather than exiting the app.

Both a custom scheme and an https host are accepted. The custom scheme needs no server
setup and is testable immediately:

```bash
adb shell am start -a android.intent.action.VIEW -d "kmp://sample"
```

`:presentation` takes navigation as a **callback** (`onOpenSample: () -> Unit`) rather
than a dependency, so it needs no navigation library and its screens stay previewable.

**The back stack lives in `NavigationViewModel`**, in `:app:shared`, not in `remember`.

```
MainActivity (recreated on rotation)
  └─ App(deepLink)
       └─ koinViewModel<NavigationViewModel> { parametersOf(deepLink) }   ← Activity's store
            │  StateFlow<List<NavKey>>      onIntent(Open | Back)
            ▼
          NavDisplay(entryDecorators = [SaveableStateHolder, ViewModelStore])
            ├─ TodoRoute   → own ViewModelStore → TodoViewModel
            └─ SampleRoute → own ViewModelStore → ItemsViewModel   (cleared on pop)
```

- On Android every configuration change — rotation, dark mode, locale, font scale, a
  foldable or split-screen resize — destroys and recreates the Activity. A `remember`ed
  stack was rebuilt from scratch, which lost the user's place **and replayed the launch
  deep link**. The ViewModel survives recreation, and reads the deep link only when it
  is first created.
- Same shape as every other ViewModel: an immutable `StateFlow<List<NavKey>>` and a
  sealed `NavigationIntent`. No Compose types in the class.
- `Back` never pops the root (`NavDisplay` requires a non-empty stack). `Open` ignores
  a key that is already on top, so a double tap cannot stack a duplicate.
- Resolved **outside** `NavDisplay`, so it belongs to the screen-level owner rather than
  to an entry.

**Each entry gets its own `ViewModelStore`** via `rememberViewModelStoreNavEntryDecorator()`
from `lifecycle-viewmodel-navigation3`. `koinViewModel()` inside a screen is scoped to
that entry and cleared when it is popped. Without it, every screen's ViewModel lived as
long as the Activity. Passing `entryDecorators` replaces Nav3's default list, so the
`rememberSaveableStateHolderNavEntryDecorator()` default is restated alongside it.

## Consequences

Easy: one parser, tested once, covers all five platforms — `DeepLinkTest` asserts scheme
and https forms, case-insensitivity, trailing slashes, query strings, and the fallback.
Adding a screen is a route object plus a `NavEntry` branch.

Hard:

**The https intent filter is not verified.** Opening `https://kmp.self.com/...` without a
chooser needs a Digital Asset Links file at `/.well-known/assetlinks.json` on a domain we
control. The domain is a placeholder, so this is left unverified deliberately.

**Only Android delivers deep links today, and only at launch.** iOS, desktop and web
delivery, and links that arrive while the app is already running, are open items in
[docs/TODO.md](../TODO.md). The shared parser is ready for all of them.

**The back stack survives configuration changes, not process death.** If Android kills
the app in the background, the user returns to the stack the launch intent describes
(usually the todo list). A ViewModel is not saved state; surviving process death would
need `rememberNavBackStack` (alternative A below).

`navigation3-ui` is at **1.2.0-beta01**. Beta, not stable.

## Enforcement

- `DeepLinkTest` and `NavigationViewModelTest` run on all five targets.
- `CompositionRootTest` checks the deep link reaches `NavigationViewModel` through Koin.
- `architectureCheck` keeps navigation confined to `:app:shared`; `:presentation` has no
  navigation dependency, enforced by it taking callbacks instead.

## Alternatives considered

**State hoisting in `:app:shared`** — a `var screen by remember` with two branches. This
was the recommendation for two screens: zero dependencies, trivially understood.
Rejected by the project owner in favour of a real back stack and deep link support, which
is the right call if a third screen is coming.

**Navigation Compose (nav2).** Rejected: nav3 is the direction of travel, and starting on
the older API would mean a migration later.

**Passing a `NavKey` into `App()` instead of a URL string.** Rejected: it would push URL
parsing into each platform entry point, which is exactly the duplication this avoids.

**Back stack ownership — three options were compared.**

| Option | Rotation | Process death | Why not |
| --- | --- | --- | --- |
| A. `rememberNavBackStack(SavedStateConfiguration, …)` | ✔ | ✔ | Recommended; not chosen by the project owner. Needs every route registered for polymorphic serialization on iOS/web |
| **B. Back stack in a ViewModel** | ✔ | ✗ | **Chosen** |
| C. `android:configChanges` in the manifest | partial | ✗ | Hides only the changes listed; any other change still loses the stack |

Moving from B to A later is contained to `NavigationViewModel` and `AppNavDisplay`.
