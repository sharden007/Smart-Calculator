# Setup and Build Instructions

## Quick Start

1. **Open the project in Android Studio**
   ```bash
   # Navigate to project directory
   cd SmartCalculator

   # Open with Android Studio or use command line
   studio .
   ```

2. **Sync Gradle**
   - Android Studio should automatically prompt to sync
   - Or click: File → Sync Project with Gradle Files
   - Wait for dependencies to download

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click the green "Run" button or press Shift+F10
   - Select your target device
   - Grant permissions when prompted

## Testing the App

### Test Calculator Functionality
1. Open the app - you'll see a calculator
2. Try basic math: `5 + 3 =` should show `8`
3. Test all operators: `+`, `-`, `×`, `÷`, `%`

### Test Secret PIN - Real Vault
1. On the calculator, type: `1`, `2`, `3`, `+`, `=`
2. Biometric prompt should appear (if device supports it)
3. Authenticate or use device PIN
4. You should see an empty gallery with "No files yet"

### Test Secret PIN - Decoy Vault
1. Go back to calculator (press back button)
2. Type: `4`, `5`, `6`, `+`, `=`
3. Authenticate again
4. You should see a separate empty gallery (decoy vault)

### Test Adding Files
1. Unlock either vault
2. Tap the orange `+` button
3. Grant photo/media permissions when prompted
4. Select a photo or video
5. File should appear in the gallery (encrypted in background)

### Test Changing PINs
1. In the gallery, tap the settings icon (top right)
2. Change the PIN patterns
3. Tap "Save Changes"
4. Go back to calculator and test new PINs

## Common Issues & Solutions

### Issue: App won't build - Kotlin/KSP version mismatch
**Error**: `Incompatible Kotlin version`

**Solution**: Update KSP version in `app/build.gradle.kts`:
```kotlin
id("com.google.devtools.ksp") version "2.1.0-1.0.29" // Adjust version to match Kotlin
```

Check your Kotlin version in project-level `build.gradle.kts` and match KSP accordingly:
- Kotlin 2.1.0 → KSP 2.1.0-1.0.29
- Kotlin 2.0.0 → KSP 2.0.0-1.0.21

### Issue: Biometric prompt doesn't appear
**Possible causes**:
1. Emulator doesn't have fingerprint configured
2. Device doesn't support biometrics
3. No screen lock set on device

**Solutions**:
- **Emulator**: Settings → Security → Fingerprint → Add fingerprint
- **Physical device**: Enable fingerprint/face unlock in device settings
- **Testing**: App will work without biometrics, just won't show the prompt

### Issue: Can't add files - Permission denied
**Error**: Permission denied when selecting files

**Solution**:
1. Go to device Settings → Apps → Smart Calculator → Permissions
2. Enable "Photos and videos" or "Storage"
3. Try again

**Android 13+ specific**:
- Must grant "Photos and videos" permission
- May need to manually enable in settings

### Issue: Files not appearing in gallery
**Check**:
1. Did you grant permissions?
2. Check logcat for errors: View → Tool Windows → Logcat
3. File might be encrypted but thumbnail not generated

**Debug**:
```kotlin
// Check encrypted files directory
// In EncryptionUtils.kt, the files are stored in:
// context.filesDir/.secure/
```

### Issue: Room database errors
**Error**: `Cannot find implementation for database`

**Solution**:
1. Clean and rebuild: Build → Clean Project, then Build → Rebuild Project
2. Invalidate caches: File → Invalidate Caches → Invalidate and Restart
3. Check that KSP plugin is applied in build.gradle.kts

### Issue: Coil image loading errors
**Error**: Images not displaying

**Solution**:
Make sure you're using Coil 3.x:
```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.0.4")
implementation("io.coil-kt.coil3:coil-video:3.0.4")
```

If still having issues, add:
```kotlin
implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
```

### Issue: Navigation not working
**Error**: Back button doesn't work or navigation crashes

**Solution**:
- Check that Navigation Compose version matches Compose BOM
- Current version: `androidx.navigation:navigation-compose:2.8.5`

### Issue: Encrypted files taking too much space
**Solution**: Implement cleanup in `GalleryViewModel`:
```kotlin
// Add to ViewModel
fun clearTempFiles() {
    viewModelScope.launch(Dispatchers.IO) {
        encryptionUtils.getTempDir().listFiles()?.forEach { it.delete() }
    }
}

// Call in onCleared()
override fun onCleared() {
    super.onCleared()
    clearTempFiles()
}
```

## Gradle Dependencies Checklist

Make sure these are in `app/build.gradle.kts`:

```kotlin
dependencies {
    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coil
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-video:3.0.4")
}
```

## Testing on Different Android Versions

### Android 13+ (API 33+)
- Uses granular media permissions
- Must request `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`
- Scoped storage enforced

### Android 10-12 (API 29-32)
- Scoped storage with legacy support
- Uses `READ_EXTERNAL_STORAGE`
- `requestLegacyExternalStorage="true"` helps compatibility

### Android 7-9 (API 25-28)
- Traditional storage model
- Easier file access
- Basic permissions

## Performance Testing

### Test Encryption Speed
- Small files (<10MB): Should be instant
- Large files (>100MB): May take several seconds
- Monitor in logcat for any errors

### Test Memory Usage
- Add 20+ photos
- Scroll through gallery
- Check for memory leaks in Android Profiler

### Test Database
- Add 100+ files
- Check query performance
- Verify no UI lag

## Debugging Tips

### Enable Logging
Add to `MainActivity.onCreate()`:
```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.enableDefaults()
}
```

### Check Encrypted Files
In Android Studio:
1. View → Tool Windows → Device Explorer
2. Navigate to: `/data/data/com.example.smartcalculator/files/.secure/`
3. You should see encrypted files (unreadable)

### Monitor Database
In Android Studio:
1. View → Tool Windows → App Inspection
2. Select Database Inspector
3. View `encrypted_files` table

### Check SharedPreferences
1. Device Explorer → `/data/data/com.example.smartcalculator/shared_prefs/`
2. Should see `smart_calculator_prefs.xml` (encrypted)

## Building Release APK

### Debug Build
```bash
./gradlew assembleDebug
# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
1. Create keystore:
```bash
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

2. Add to `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/my-release-key.keystore")
            storePassword = "password"
            keyAlias = "my-key-alias"
            keyPassword = "password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
}
```

3. Build:
```bash
./gradlew assembleRelease
# APK location: app/build/outputs/apk/release/app-release.apk
```

## Next Steps After MVP

1. **Test thoroughly** on different devices and Android versions
2. **Add comprehensive error handling** and user feedback
3. **Implement file viewer** for full-screen media display
4. **Add thumbnails** for better performance
5. **Optimize encryption** for large files
6. **Add backup/restore** functionality
7. **Implement break-in detection** (optional)
8. **Add unit tests** for critical functions
9. **Improve UI/UX** with animations and polish
10. **Consider obfuscation** for additional security

## Getting Help

If you encounter issues:
1. Check logcat for error messages
2. Clean and rebuild the project
3. Invalidate caches and restart
4. Check that all dependencies are properly synced
5. Verify device/emulator meets minimum requirements (API 25+)

## Useful Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Check for dependency updates
./gradlew dependencyUpdates
```

## Project Health Checklist

- [ ] App builds without errors
- [ ] Calculator functions work correctly
- [ ] Secret PIN detection works for both vaults
- [ ] Biometric authentication prompts appear (if supported)
- [ ] Files can be added to both vaults
- [ ] Files are encrypted (check file directory)
- [ ] Files can be deleted
- [ ] Settings screen allows PIN changes
- [ ] Navigation works (back button, etc.)
- [ ] Permissions are requested and granted
- [ ] No crashes in common workflows
- [ ] Database operations work correctly
