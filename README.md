# Link Cleaner

Link Cleaner is a small Android share target. Share a web link to it and it:

1. Follows HTTP redirects (including short links such as `vt.tiktok.com`).
2. Removes the query string and fragment from the resolved TikTok URL.
3. Copies the resulting URL to the clipboard.

Tap **Share clean link** to reshare the result. Link Cleaner closes when the
Android share sheet opens.

## Build

Open the directory in Android Studio and run the `app` configuration, or use:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds the debug APK on every push and pull request. You can also
run the **Build Android APK** workflow manually from the **Actions** tab. Download
the `link-cleaner-debug` artifact from a successful workflow run to get the APK.
