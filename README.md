# LockedLibrary Android App

Native Android shell for LockedLibrary (protected PDF bookstore).

**Anti-leak features**
- `FLAG_SECURE` — screenshots and screen recording show black on Android.
- Long-press context menu blocked — no copy/paste of book text.
- Downloads disabled — book files can't be saved to the phone.
- External links / intents blocked — stays inside the app.

**Build**
`gradle assembleRelease` (needs Android SDK; CI does this for free via GitHub Actions).

**Change the app URL**
Edit `app/src/main/res/values/strings.xml` → `app_url`.

The current URL is the live trycloudflare tunnel. When the tunnel URL changes,
update this string and push again — GitHub Actions rebuilds the APK automatically.
