# Smart Calculator - Hidden Gallery App

A fully functional calculator app that secretly contains a fingerprint-protected encrypted photo/video vault. The app looks and behaves like a normal calculator, but entering a secret PIN pattern unlocks hidden galleries.

## Features

### Core Features (MVP)
- **Functional Calculator**: Works as a real calculator for basic arithmetic operations
- **Secret PIN Detection**: Enter special patterns (e.g., `123+=`) to unlock hidden features
- **Biometric Authentication**: Fingerprint or device credentials protect access to vaults
- **Dual Vault System**:
  - **Real Vault** (default PIN: `123+=`): Your actual private files
  - **Decoy Vault** (default PIN: `456+=`): Fake vault with harmless content for plausible deniability
- **File Encryption**: AES encryption using Android Keystore
- **Encrypted Storage**: Files are encrypted and hidden from system gallery
- **File Management**: Add, view, and delete photos/videos from vaults

## How to Use

### 1. Normal Calculator Mode
- The app opens as a regular calculator
- Perform calculations normally - it's fully functional
- Others won't know about the hidden features

### 2. Accessing Hidden Vaults
- Type your secret PIN pattern on the calculator
  - Default Real Vault PIN: `123+=`
  - Default Decoy Vault PIN: `456+=`
- Authenticate with fingerprint/device credentials
- Access your encrypted gallery

### 3. Managing Files
- **Add Files**: Tap the `+` button in the gallery
- **Delete Files**: Tap the trash icon on any file
- **Change PINs**: Tap the settings icon in the gallery

### 4. Dual Vault Strategy
- Use the **Real Vault** for your actual private content
- Add innocent photos to the **Decoy Vault**
- If someone forces you to unlock, use the decoy PIN
- They'll see harmless content and won't know about the real vault

## Project Structure

```
app/src/main/java/com/example/smartcalculator/
├── data/
│   ├── AppDatabase.kt           # Room database
│   ├── EncryptedFile.kt         # File entity model
│   └── EncryptedFileDao.kt      # Database operations
├── ui/
│   ├── calculator/
│   │   └── CalculatorScreen.kt  # Calculator UI with PIN detection
│   ├── gallery/
│   │   ├── GalleryScreen.kt     # Gallery UI
│   │   └── GalleryViewModel.kt  # File management logic
│   ├── settings/
│   │   └── SettingsScreen.kt    # PIN configuration
│   └── theme/                   # App theming
├── utils/
│   ├── BiometricUtils.kt        # Fingerprint authentication
│   ├── EncryptionUtils.kt       # AES file encryption
│   └── SettingsManager.kt       # Encrypted preferences
└── MainActivity.kt              # App entry point & navigation
```

## Technical Details

### Security Features
- **AES Encryption**: Files encrypted with AES/CBC/PKCS7Padding
- **Android Keystore**: Encryption keys stored securely in hardware-backed keystore
- **Encrypted SharedPreferences**: PIN patterns stored encrypted
- **Biometric Protection**: Optional fingerprint/face unlock
- **Hidden Storage**: Files stored in app's private directory (`.secure` folder)
- **Media Removal**: Attempts to remove original files from gallery (API level dependent)

### Technologies Used
- **Kotlin** - Programming language
- **Jetpack Compose** - Modern Android UI
- **Room Database** - Local database for file metadata
- **Android Keystore** - Secure key storage
- **Biometric API** - Fingerprint/device authentication
- **Coroutines** - Asynchronous operations
- **Navigation Compose** - Screen navigation
- **Coil** - Image/video loading
- **Security Crypto** - Encrypted SharedPreferences

## Building the Project

### Requirements
- Android Studio Hedgehog or newer
- Minimum SDK: 25 (Android 7.1)
- Target SDK: 36

### Steps
1. Open project in Android Studio
2. Sync Gradle files
3. Build and run on device or emulator
4. Grant necessary permissions when prompted

## Permissions Required
- `USE_BIOMETRIC` - Fingerprint authentication
- `READ_MEDIA_IMAGES` - Access photos (Android 13+)
- `READ_MEDIA_VIDEO` - Access videos (Android 13+)
- `READ_EXTERNAL_STORAGE` - Access media (Android 12 and below)

## Future Enhancements

### Recommended Next Features
1. **File Viewer**: Full-screen image/video viewer with zoom and playback
2. **Bulk Import**: Add multiple files at once
3. **Thumbnails**: Generate and cache thumbnails for faster loading
4. **Search**: Search files by name or date
5. **Albums**: Organize files into folders/albums
6. **Backup/Restore**: Cloud or local backup of encrypted files
7. **Break-in Detection**: Take photo if wrong PIN entered multiple times
8. **Panic Mode**: Quick clear vault button or panic PIN that wipes data
9. **Stealth Mode**: Hide app icon or disguise as system app
10. **Advanced Calculator**: Add scientific calculator functions for authenticity

### Code Improvements
- Add comprehensive error handling and user feedback
- Implement progress indicators for file operations
- Add file size limits and storage management
- Optimize encryption for large files (chunked processing)
- Add unit and integration tests
- Implement proper file URI handling for Android 13+
- Add file restoration (decrypt back to gallery)
- Implement proper memory management for large images

## Security Considerations

### What This Protects Against
- Casual snooping through your gallery
- Someone quickly checking your phone
- Basic privacy from friends/family

### Limitations
- **Not forensics-proof**: Determined attackers with device access can potentially extract data
- **Root access**: Rooted devices compromise security
- **Screen recording**: PIN entry could be recorded
- **Backups**: Device backups might expose data
- **Memory dumps**: Files are decrypted to temp storage for viewing

### Best Practices
1. Use strong, unique PIN patterns
2. Regularly update your PINs
3. Don't share your PIN patterns
4. Keep some innocent photos in decoy vault
5. Be aware of screen recording/shoulder surfing
6. Consider full device encryption
7. Use a secure lock screen

## Disclaimer

This app is for personal privacy and should only be used for legitimate purposes. The dual vault system is designed for plausible deniability in situations where you might be pressured to unlock your device (e.g., border crossings, theft).

**Important**:
- In some jurisdictions, you may be legally required to provide all passwords/PINs
- This is an MVP and may have security vulnerabilities
- Do not rely solely on this for highly sensitive data
- Always maintain backups of important files elsewhere
- Use at your own risk

## License

This is a sample project for educational purposes. Feel free to modify and use as needed.

## Contributing

This is an MVP starter project. Areas for contribution:
- Security improvements
- UI/UX enhancements
- Performance optimizations
- Additional features from the roadmap
- Bug fixes and testing

---

**Note**: Remember your PIN patterns! There's no recovery mechanism if you forget them. The default PINs are:
- Real Vault: `123+=`
- Decoy Vault: `456+=`
