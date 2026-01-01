# Development Roadmap

## Current Status: MVP Complete ✅

The MVP includes:
- ✅ Functional calculator UI
- ✅ Secret PIN pattern detection (dual vault)
- ✅ Biometric authentication
- ✅ AES file encryption
- ✅ File import and deletion
- ✅ Room database for file metadata
- ✅ Basic gallery UI
- ✅ Settings screen for PIN management

## High Priority Features

### 1. File Viewer
**Priority**: Critical
**Complexity**: Medium

Implement full-screen media viewer:
- [ ] Create `FileViewerScreen.kt`
- [ ] Add zoom/pan for images (using `Modifier.transformable()`)
- [ ] Video playback with controls (ExoPlayer)
- [ ] Swipe between files
- [ ] Share decrypted file temporarily
- [ ] Remember to clean up temp decrypted files

**Files to create**:
- `ui/viewer/FileViewerScreen.kt`
- `ui/viewer/ImageViewer.kt`
- `ui/viewer/VideoPlayer.kt`

### 2. Thumbnail Generation
**Priority**: High
**Complexity**: Medium

Improve gallery performance:
- [ ] Generate thumbnails on file import
- [ ] Store thumbnails separately
- [ ] Use Coil for thumbnail caching
- [ ] Add loading placeholder
- [ ] Lazy load in grid

**Modify**:
- `GalleryViewModel.kt` - Add thumbnail generation
- `EncryptionUtils.kt` - Add thumbnail methods
- `EncryptedFile.kt` - Already has thumbnailPath field

### 3. Better Error Handling
**Priority**: High
**Complexity**: Low

Add user-friendly error messages:
- [ ] Show SnackBar for errors
- [ ] Handle permission denied gracefully
- [ ] Handle encryption failures
- [ ] Handle storage full errors
- [ ] Add retry mechanisms

**Modify**: All ViewModels and screens

### 4. Import Multiple Files
**Priority**: Medium
**Complexity**: Low

Allow bulk import:
- [ ] Use `GetMultipleContents` contract
- [ ] Show progress indicator
- [ ] Process files in background
- [ ] Show success count

**Modify**: `GalleryScreen.kt`, `GalleryViewModel.kt`

### 5. File Organization
**Priority**: Medium
**Complexity**: Medium

Add albums/folders:
- [ ] Create Album entity
- [ ] Update database schema
- [ ] Album selector in gallery
- [ ] Create/rename/delete albums
- [ ] Move files between albums

**Files to create**:
- `data/Album.kt`
- `data/AlbumDao.kt`
- `ui/albums/AlbumScreen.kt`

## Medium Priority Features

### 6. Advanced Search & Filtering
- [ ] Search by filename
- [ ] Filter by file type (image/video)
- [ ] Sort by date, name, size
- [ ] Date range picker

### 7. Backup & Restore
- [ ] Export encrypted backup
- [ ] Import from backup
- [ ] Cloud backup option (Google Drive)
- [ ] Auto-backup setting

**Consider**:
- Backup file format (ZIP with encrypted files + database export)
- Versioning
- Encryption of backup itself

### 8. Break-in Detection
- [ ] Track failed authentication attempts
- [ ] Take photo on wrong PIN (front camera)
- [ ] Store break-in logs
- [ ] Show alert in real vault
- [ ] Option to send email/notification

**Files to create**:
- `data/BreakInAttempt.kt`
- `utils/BreakInDetector.kt`
- `ui/security/SecurityLogsScreen.kt`

### 9. File Metadata
- [ ] Show file size
- [ ] Show date added
- [ ] Show original location
- [ ] EXIF data for images
- [ ] File info dialog

### 10. Calculator Enhancements
- [ ] Scientific calculator mode
- [ ] Calculator history
- [ ] More realistic calculator UI
- [ ] Haptic feedback
- [ ] Sound effects (optional)

## Low Priority / Polish

### 11. UI/UX Improvements
- [ ] Splash screen
- [ ] Onboarding tutorial
- [ ] Animations (shared element transitions)
- [ ] Dark/Light theme toggle
- [ ] Customizable themes
- [ ] Material You dynamic colors

### 12. App Disguise Options
- [ ] Hide from recent apps
- [ ] Fake crash screen (panic button)
- [ ] Change app icon
- [ ] Stealth mode (hide from launcher)

### 13. Additional Security
- [ ] Auto-lock timer
- [ ] Require PIN on app resume
- [ ] Secure screen (prevent screenshots)
- [ ] Wipe data after X failed attempts
- [ ] Panic PIN (wipes vault)

### 14. Performance Optimizations
- [ ] Pagination for large galleries
- [ ] Better memory management
- [ ] Background encryption queue
- [ ] Optimize database queries
- [ ] Reduce APK size

### 15. Cloud Features (Advanced)
- [ ] Sync between devices
- [ ] Cloud storage integration
- [ ] Shared vaults
- [ ] Remote wipe

## Bug Fixes & Technical Debt

### Known Issues
- [ ] Files might not delete from system gallery on Android 13+
- [ ] Large file encryption blocks UI
- [ ] No progress indicator for encryption
- [ ] Temp files might accumulate
- [ ] No file size validation

### Code Quality
- [ ] Add unit tests
- [ ] Add UI tests
- [ ] Add KDoc comments
- [ ] Extract string resources
- [ ] Implement ProGuard rules for release
- [ ] Add Lint checks
- [ ] Code review and refactoring

### Security Hardening
- [ ] Implement certificate pinning (if using network)
- [ ] Add tamper detection
- [ ] Obfuscate PIN storage further
- [ ] Implement secure deletion (overwrite file data)
- [ ] Add root detection
- [ ] Screen capture prevention

## Feature Details & Implementation Notes

### File Viewer Implementation

```kotlin
// ui/viewer/FileViewerScreen.kt
@Composable
fun FileViewerScreen(
    file: EncryptedFile,
    onClose: () -> Unit,
    viewModel: GalleryViewModel
) {
    val decryptedFile = remember {
        viewModel.getDecryptedFile(file)
    }

    if (file.fileType == "image") {
        ImageViewer(decryptedFile, onClose)
    } else {
        VideoPlayer(decryptedFile, onClose)
    }

    DisposableEffect(Unit) {
        onDispose {
            // Clean up decrypted file
            decryptedFile?.delete()
        }
    }
}
```

### Thumbnail Generation

```kotlin
// Add to EncryptionUtils.kt
fun generateThumbnail(
    sourceFile: File,
    outputFile: File,
    maxSize: Int = 200
): Boolean {
    return try {
        val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
        val ratio = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        val thumbnail = Bitmap.createScaledBitmap(bitmap, width, height, true)
        FileOutputStream(outputFile).use {
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, it)
        }
        true
    } catch (e: Exception) {
        false
    }
}
```

### Break-in Detection

```kotlin
// data/BreakInAttempt.kt
@Entity(tableName = "break_in_attempts")
data class BreakInAttempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptedPin: String,
    val photoPath: String? = null
)

// Modify CalculatorScreen to track failed attempts
var failedAttempts by remember { mutableStateOf(0) }

fun checkSecretPin(input: String) {
    when {
        input.endsWith(realPin) -> {
            failedAttempts = 0
            onSecretPinDetected(true)
        }
        input.endsWith(decoyPin) -> {
            failedAttempts = 0
            onSecretPinDetected(false)
        }
        input.length >= 5 && !input.any { it in "0123456789" } -> {
            // Possible PIN attempt
            failedAttempts++
            if (failedAttempts >= 3) {
                // Take photo, log attempt
                breakInDetector.recordAttempt(input)
            }
        }
    }
}
```

### File Organization (Albums)

```kotlin
// data/Album.kt
@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val vaultType: String,
    val dateCreated: Long = System.currentTimeMillis()
)

// Update EncryptedFile.kt
@Entity(tableName = "encrypted_files")
data class EncryptedFile(
    // ... existing fields
    val albumId: Long? = null // Add this
)

// Add to EncryptedFileDao.kt
@Query("SELECT * FROM encrypted_files WHERE albumId = :albumId ORDER BY dateAdded DESC")
fun getFilesByAlbum(albumId: Long): Flow<List<EncryptedFile>>
```

## Testing Checklist

Before marking features complete, ensure:
- [ ] Feature works on Android 7, 10, 13, 14
- [ ] No memory leaks
- [ ] Proper error handling
- [ ] User feedback (loading states, errors)
- [ ] Accessibility support
- [ ] Edge cases handled
- [ ] Code documented
- [ ] Unit tests added (if applicable)

## Long-term Vision

### Premium Features (Optional)
- Multi-device sync
- Advanced video editing
- OCR for documents
- Secure notes
- Password manager integration
- Duress features (advanced panic modes)

### Business Model Ideas
- Free with ads (outside vault)
- Premium unlock ($2.99 one-time)
- Subscription for cloud storage
- Family sharing

## Resources & References

### Libraries to Consider
- **ExoPlayer**: Video playback - `androidx.media3:media3-exoplayer`
- **CameraX**: Break-in photos - `androidx.camera:camera-camera2`
- **WorkManager**: Background encryption - `androidx.work:work-runtime-ktx`
- **Paging 3**: Large galleries - `androidx.paging:paging-compose`
- **DataStore**: Settings - `androidx.datastore:datastore-preferences`

### Security References
- Android Keystore: https://developer.android.com/training/articles/keystore
- Biometric Auth: https://developer.android.com/training/sign-in/biometric-auth
- Scoped Storage: https://developer.android.com/training/data-storage

### Similar Apps (for inspiration)
- Calculator Vault
- KeepSafe
- Vaulty
- Gallery Vault

## Contributing

If you're building on this MVP:
1. Pick a feature from this list
2. Create a feature branch
3. Implement with tests
4. Update this TODO
5. Document any new dependencies

## Version History

- **v1.0 (MVP)**: Basic calculator, dual vaults, encryption, biometric auth
- **v1.1 (planned)**: File viewer, thumbnails, error handling
- **v1.2 (planned)**: Albums, search, bulk import
- **v2.0 (future)**: Break-in detection, backup/restore, advanced security
