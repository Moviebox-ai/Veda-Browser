# Legacy Porting Candidates (Staged for Prompt 2)

The following components from the legacy codebase analysis have been preserved in `/temp_legacy_to_port/` for integration in Prompt 2:

1. **`SchemePolicy.kt`**:
   - Classifies URL schemes (`http`, `https`, `about`, `javascript`, `data`, `blob`, `velocity`).
   - Identifies external application schemes (`mailto`, `tel`, `sms`, `intent`, `geo`, `market`).
   - Blocks dangerous schemes (`file://`, `content://`) to prevent local file exfiltration.

2. **`IntentValidator.kt`**:
   - Sanitizes and safely parses `intent://` URIs.
   - Enforces `Intent.CATEGORY_BROWSABLE` and strips explicit internal component targets to prevent intent redirection and permission delegation attacks.

3. **`FilenameSanitizer.kt`**:
   - Strips directory traversal (`..`), path separators (`/`, `\`), and control characters.
   - Enforces 128-character maximum length while preserving valid file extensions.

4. **`ReaderModeExtractor.kt`**:
   - Injected JavaScript snippet extracting sanitized readable DOM content, headline, and article body.
   - Data class representation for Reader Mode UI presentation.

5. **`DownloadRepositoryImpl.kt`**:
   - Clean Architecture Repository managing download states (`DOWNLOADING`, `COMPLETED`, `CANCELLED`, `FAILED`) with filename sanitization.
