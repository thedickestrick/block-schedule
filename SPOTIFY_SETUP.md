# Spotify dance-party music — one-time setup

The dance party can play **her own Spotify playlist** (e.g. a Taylor Swift dance playlist)
instead of the built-in tune. This needs a 5-minute one-time setup because Spotify requires
every app that controls playback to be registered.

## Requirements
- **Spotify Premium** on her account (Spotify only allows on-demand playback for Premium).
- The **Spotify app installed** on her phone and logged in.

## 1. Create a Spotify developer app (free)
1. Go to <https://developer.spotify.com/dashboard> and log in (any Spotify account works).
2. Click **Create app**. Name it anything (e.g. "Block Schedule").
3. **Redirect URI** — add exactly:
   ```
   blockschedule://callback
   ```
4. Under **APIs used**, tick **Android**.
5. Save. Open the app's **Settings** and add the **Android package**:
   - **Package name:** `com.blockschedule`
   - **SHA1 fingerprint** (add BOTH so the installed release build *and* debug builds work):
     ```
     3C:96:F1:61:D1:94:4C:A4:67:97:32:59:34:5F:2C:C5:C2:72:04:10   (release)
     FC:EB:73:53:EA:C3:DE:B9:88:3D:1D:32:DB:57:3F:BE:00:97:92:56   (debug, optional)
     ```
6. Copy the **Client ID** shown on the app page.

## 2. Add a playlist link
In Spotify, open the playlist you want → **Share → Copy link to playlist**. It looks like
`https://open.spotify.com/playlist/XXXXXXXX?si=...`.

> Tip: make a playlist of her liked Taylor Swift dance songs and copy that link.

## 3. Enter it in the app
Block Schedule → **Settings → Dance party music**:
- Turn on **Play my Spotify playlist**
- Paste the **Client ID**
- Paste the **Playlist link**

Now mark a task as a **dance party** task (or finish the whole day) and the party will play
that playlist (shuffled) through Spotify. The first time, Spotify will ask her to authorize —
that's a one-time tap.

## Notes
- If Spotify isn't installed, she's not Premium, or it can't connect, the party automatically
  falls back to the **built-in tune** — it never breaks.
- The app can't include the actual songs (that's copyrighted); it plays them *through Spotify*.
- If the release signing key ever changes, update the SHA1 in the dashboard
  (`keytool -list -v -keystore blockschedule-release.jks -alias blockschedule`).
