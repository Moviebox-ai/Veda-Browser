# Download Pause & Resume Engine

Add robust Pause and Resume capabilities for all downloading files in Veda Browser, enabling users to save mobile data, pause downloads anytime, and seamlessly resume them later from the exact byte position across app restarts, foreground service notifications, and UI screens.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during Phase 1 interactive clarification and govern the implementation:

- **Confirmed Decision 1 (Controls Availability)**: Pause and Resume controls will be prominently available in three places:
  1. The in-app animated floating banner (`DownloadBanner`)
  2. The full downloads history screen (`DownloadsScreen`)
  3. The Android system notification (action buttons directly on the ongoing notification card)
- **Confirmed Decision 2 (Non-Resumable Servers)**: If an external server does not support HTTP Range requests (`Accept-Ranges: bytes` or returns `200 OK` instead of `206 Partial Content` on resume), the browser will display an alert dialog informing the user and requesting confirmation before restarting from 0 bytes, avoiding silent data loss.
- **Confirmed Decision 3 (Network Behavior)**: Downloads will only pause and resume upon explicit manual user command, giving the user complete and predictable control without unexpected automatic pauses.

---

## 1. Overview & Core Concept

- **What It Does**: Allows users to pause active downloads at any time and resume them later from the same byte offset using HTTP Range headers (`Range: bytes={downloadedBytes}-`), appending to the partial file in `Internal Storage/Veda Browser`. If the download is paused and the app is closed, the partial progress is preserved in persistent storage so it can be resumed whenever the user opens the browser again.
- **Target Audience / Persona**: Users on metered mobile data, daily quota limits, or unstable connections who need to manage their data consumption without losing partially downloaded large files (videos, APKs, documents).
- **Key Value**: Zero lost bandwidth on interrupted or intentionally paused downloads, with immediate one-tap controls in the status bar notification, in-app banner, and downloads manager.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Pausing from In-App Banner**:
   - User starts a download from any webpage.
   - The bottom in-app banner slides in with real-time speed/percentage.
   - A dedicated **Pause button** (Material pause icon) appears next to dismiss.
   - Tapping **Pause** stops data transfer immediately, changes badge to an amber "Paused" state with current progress retained, and morphs the button into a **Resume button** (play icon).
2. **Resuming from System Notification**:
   - When browsing other apps or on the home screen, the ongoing download notification shows action buttons: `[Pause]` and `[Cancel]`.
   - Tapping `[Pause]` pauses the download immediately in the background foreground service and flips the action button to `[Resume]`.
   - Tapping `[Resume]` re-establishes the HTTP Range request and continues downloading seamlessly.
3. **Downloads Screen Management**:
   - In `DownloadsScreen`, active downloads display both progress bar and inline `Pause` / `Resume` icons.
   - A distinct "Paused" pill tag in warning amber (`#F59E0B`) indicates paused items.
4. **Non-Resumable Server Warning**:
   - If a server responds with full stream (`200 OK`) instead of partial (`206`), download pauses and prompts: *"This server does not support resuming. Do you want to restart the download from the beginning?"* with `[Restart]` and `[Cancel]` buttons.

### Visual Identity & Theme Tokens

- **Active Downloading**: Primary Cyan/Blue `#38BDF8` with animated indeterminate or percentage linear progress bar.
- **Paused State**: Warm Amber `#F59E0B` container tint, amber progress bar and status text (`"Paused • 45 MB / 120 MB"`).
- **Completed State**: Emerald `#10B981` with instant "Open" button.
- **Touch Target Density**: Minimum 48dp touch targets on all notification actions and banner buttons for accessibility.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: HTTP Range (`Range: bytes=X-`) vs Custom Chunking**
  - *Chosen Approach*: Standard RFC 7233 HTTP Range requests with `FileOutputStream(destinationFile, true)` append mode.
  - *Why*: Compatible with all major web servers, CDNs, media hosts, and cloud storage providers without requiring custom server-side software.
  - *Alternatives Considered*: Android system `DownloadManager.pauseDownload()` (only available on hidden APIs or specific OEM forks; not standard across Android 9-15).
- **Decision 2: Foreground Service Lifecycle Management for Paused State**
  - *Chosen Approach*: When a download is paused, the Coroutine Job is cancelled, the partial file remains intact, and if all downloads are paused or finished, the Foreground Service relinquishes foreground status (`stopForeground`) to avoid keeping an unnecessary persistent notification pinned when idle.
  - *Why*: Respects Android battery optimization and Play Store guidelines by not holding an active `dataSync` foreground service when no network transfer is occurring.
- **Decision 3: Atomic Data Persistence**
  - *Chosen Approach*: Persist exact `downloadedBytes` and `status = DownloadStatus.PAUSED` to DataStore/Room repository immediately upon pausing.
  - *Why*: Ensures that even if the Android OS kills the process due to low memory, reopening the app shows the item as "Paused" with the exact resumed byte count ready to restart.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER                                  │
│  ┌───────────────────────┐  ┌───────────────────┐  ┌────────────────┐ │
│  │     DownloadBanner    │  │  DownloadsScreen  │  │ System Notif   │ │
│  │ (Pause/Resume Button) │  │(Inline Row Action)│  │(Action Buttons)│ │
│  └───────────┬───────────┘  └─────────┬─────────┘  └───────┬────────┘ │
└──────────────┼────────────────────────┼────────────────────┼───────────┘
               │                        │                    │
               ▼                        ▼                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         BrowserViewModel                               │
│      pauseDownload(id)  /  resumeDownload(id, forceRestart)            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        RealDownloadManager                             │
│   - Coordinates Service Intents (START, PAUSE, RESUME, CANCEL)         │
│   - Validates destination files in Internal Storage/Veda Browser       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
               ┌────────────────────┴────────────────────┐
               ▼                                         ▼
┌──────────────────────────────┐          ┌──────────────────────────────┐
│   DownloadForegroundService  │          │      BrowserRepository       │
│  - HttpURLConnection (Range) │          │  - DownloadItem DataStore    │
│  - 206 Partial Content check │◄────────►│  - DownloadStatus: PAUSED    │
│  - FileOutputStream(...,true)│          │  - downloadedBytes / total   │
│  - Coroutine Job lifecycle   │          └──────────────────────────────┘
└──────────────────────────────┘
```

### Data Model & State Updates

- **`DownloadStatus` Enum**:
  ```kotlin
  enum class DownloadStatus {
      DOWNLOADING,
      PAUSED,
      COMPLETED,
      FAILED,
      CANCELLED
  }
  ```
- **`DownloadItem` Additions**:
  - `supportsResume: Boolean = true` (tracks server Range capabilities)
  - Updated `formattedProgressMeta`:
    ```kotlin
    DownloadStatus.PAUSED -> "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)} • Paused"
    ```

### Foreground Service Intents & Actions

- `ACTION_PAUSE_DOWNLOAD`: Cancels active coroutine download job, saves current byte count to disk, updates status to `PAUSED`, updates or detaches foreground notification.
- `ACTION_RESUME_DOWNLOAD`: Inspects existing file length on disk (`destFile.length()`), opens `HttpURLConnection` with request property `"Range": "bytes=$offset-"`.
  - If response code is `206 Partial Content`: appends bytes to file.
  - If response code is `200 OK`: server ignored range; notifies UI that restart is required.
- `ACTION_CANCEL_DOWNLOAD`: Cancels job and cleans up partial file if requested.

### Notification Action Broadcasts

- `DownloadNotificationHelper` registers explicit `PendingIntent.getService` actions:
  - `Pause`: Triggers `ACTION_PAUSE_DOWNLOAD`
  - `Resume`: Triggers `ACTION_RESUME_DOWNLOAD`
  - `Cancel`: Triggers `ACTION_CANCEL_DOWNLOAD`
