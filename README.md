# HILAX PLAY — Android starter

This is a buildable Android starter project with a dark HILAX PLAY screen and Media3 playback for **direct, authorized media URLs** (MP4/HLS/DASH).

## Build APK using GitHub
1. Create a new GitHub repository.
2. Upload the contents of this folder to the repository root (not the ZIP file itself).
3. Open **Actions** → **Build HILAX PLAY APK** → **Run workflow** (or push to `main`).
4. Open the completed workflow run → **Artifacts** → download `HILAX-PLAY-debug-APK`.
5. Extract the ZIP artifact and install `app-debug.apk` on Android.

## Current limitation
The UpToDub movie-page URL resolver and automatic stream renewal are **not implemented**. A movie page URL is not itself a playable media URL. The source website must provide an authorized playback/API flow or other permitted way to obtain a fresh stream URL. This project does not bypass expiry or access controls. Once the source's supported method is confirmed, the resolver and renewal logic can be added.

## App package
`com.hilax.play`
