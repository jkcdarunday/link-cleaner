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

## Signed release builds

Release builds use a self-signed developer certificate. Keep the release keystore
and its passwords backed up securely: future updates must use the same signing
key. Never commit them to Git.

Local signing reads `.signing/keystore.properties` (ignored by Git):

```properties
storeFile=.signing/release.p12
storePassword=YOUR_STORE_PASSWORD
keyAlias=link-cleaner
keyPassword=YOUR_KEY_PASSWORD
```

Create a private keystore using Android Studio's **Generate Signed Bundle / APK**
wizard, or reuse your existing release keystore, and set these properties.
`storeFile` is relative to the project root, or an absolute path. The
`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and
`RELEASE_KEY_PASSWORD` environment variables override the corresponding local
properties when supplied.

Build the signed release APK with:

```sh
./gradlew assembleRelease
```

The installable APK is written to
`app/build/outputs/apk/release/app-release.apk`. Release builds require signing
credentials and fail if they are missing; debug builds do not need them.

If you previously installed the debug APK, uninstall it before installing the
release APK because their signing keys differ. Uninstalling removes app data.

## GitHub Actions

Every push and pull request builds the `link-cleaner-debug` artifact. Pushes to
`main` and tags starting with `v` also build the signed `link-cleaner-release`
artifact. You can run the **Build Android APK** workflow manually on `main` or a
`v*` tag to build both. Pull requests never receive signing credentials.

In the repository's **Settings > Secrets and variables > Actions**, add:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded contents of your release keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias (`link-cleaner` for the locally generated key) |
| `RELEASE_KEY_PASSWORD` | Key password |

Use the same keystore and passwords as your local build. On Linux,
`base64 -w 0 .signing/release.p12` produces the keystore secret's value; do not
share this output or put it in Git. The workflow removes its temporary keystore
after building. Missing secrets fail the release job explicitly.

Download the APK artifact from a successful workflow run. To publish a public
release, attach the signed `app-release.apk` to a GitHub Release.
