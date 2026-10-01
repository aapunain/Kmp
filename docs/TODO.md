# TODO

Agreed work that is deliberately deferred. Not a wish list: every item here was
discussed and parked, with the decisions still open listed against it.

This is the place for deferred work because `// TODO:` comments in code fail
`./gradlew detekt` (`ForbiddenComment`, on by default). When an item is picked up,
resolve its open questions with the developer first, then delete it from this file
in the same change that implements it.

## Deep links

### iOS delivery

Nothing on iOS handles a link today: no `CFBundleURLTypes` in `Info.plist`, no
`.onOpenURL` in `iOSApp.swift`, and `MainViewController()` calls `App()` with no link.

```
kmp://sample ─► Info.plist CFBundleURLTypes ─► iOSApp.swift .onOpenURL
             ─► Kotlin entry point ─► backStackFor(url)   (exists, tested)
```

Open questions:
- Custom scheme only, or Universal Links (`https://`) too? Universal Links need the
  Associated Domains entitlement (paid Apple Developer account) and an
  `apple-app-site-association` file on a real domain.
- Test with `xcrun simctl openurl booted "kmp://sample"`.

### Links that arrive while the app is running

Applies to every platform, Android included: `MainActivity` reads `intent.data` only in
`onCreate` and has no `onNewIntent`. On iOS, `makeUIViewController` runs once, so a
constructor parameter cannot carry a second link either. Needs a hand-off that holds a
link until navigation is ready, and an intent on `NavigationViewModel` to apply it.

Open questions:
- Replace the stack with `backStackFor(url)`, or push the target on top?
- While the app lock is up: hold the link until unlock, or drop it?

### Desktop and web delivery

- Desktop: command-line argument on Windows/Linux, `Desktop.setOpenURIHandler` on macOS,
  plus OS-level scheme registration in the installer.
- Web: read `window.location` at startup; Nav3 has no browser-history integration yet.

Open question: in scope at all, or Android + iOS only?

### Verified https links

`https://kmp.self.com/...` is a placeholder domain. Android App Links need
`/.well-known/assetlinks.json` and iOS needs `apple-app-site-association` on a domain we
control.

## Navigation

### Survive process death

The back stack lives in `NavigationViewModel`, which survives rotation but not process
death (ADR-0017, option B). Moving to `rememberNavBackStack(SavedStateConfiguration, …)`
fixes both; the change is contained to `NavigationViewModel` and `AppNavDisplay`.

### FAB menu state on rotation

The expanded/collapsed state of the todo FAB menu is a plain `remember` in
`TodoScreen.kt`, so it collapses on rotation. `rememberSaveable` would keep it.
