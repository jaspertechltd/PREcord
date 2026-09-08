# 🎙️ Precord
> **Ever played a legendary riff, heard a hilarious roast, or caught a once-in-a-lifetime idea... right *after* you stopped recording?**  
> Meet **Precord** — the retroactive audio buffer for Android that captures the past before you even hit record! Think of it as **Ableton's Capture button** or a **DVR for your ears**. 👂✨

---

![Precord Banner](https://img.shields.io/badge/Precord-v0.5--Beta-6750A4?style=for-the-badge&logo=android&logoColor=white)
![Android 16 Ready](https://img.shields.io/badge/Android%2016-Pixel%2010a%20Optimized-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

---

## ⚡ What is Precord?

Most voice recorders ask you to predict the future. You press **Record**, wait for something interesting to happen, and hoard gigabytes of awkward silence.

**Precord flips the script.** 

It continuously buffers audio in the background using an ultra-lightweight, memory-efficient circular buffer. The moment something worth keeping happens, tap **Capture** (or hit a secret button combo) and Precord instantly saves the last 10 seconds up to **4 hours** of high-fidelity audio straight to your phone.

Whether you're a musician catching fleeting inspiration, a podcast host chasing spontaneous banter, or just someone who refuses to let great moments vanish into thin air — **Precord has your back.** 🎧

---

## ✨ Features That Slap

### 🎛️ The Core Engine
* **⏮️ Retrospective Time Travel**: Drag the logarithmic slider from **10 seconds** to **4 hours**. Decide *after* the fact how much audio to save.
* **📊 Live Waveform Visualizer**: Smooth 60 FPS real-time audio visualization built right into the home screen.
* **💾 Dynamic File Estimator**: Know down to the exact kilobyte how large your capture will be *before* you export.

### 🎧 Audio Enthusiast Heaven
* **Format Flexibility**: Export in uncompressed **WAV** or **AIFF**, pristine **FLAC**, crispy **MP3**, or versatile **OGG**.
* **Stereo / Mono Toggle**: Switch between spatial stereo width or compact mono recording on the fly.
* **External & Bluetooth Mic Support**: Seamlessly route from your wireless earbuds, lapel mic, or USB audio interface when connected.

### 🥷 Stealth & Background Controls
* **🎸 Volume Button Combo Capture**: Screen off in your pocket? Trigger a capture instantly with a configurable secret button sequence (e.g. `Vol Down` ➔ `Vol Up` ➔ `Vol Down`). Plays nicely with active media playback!
* **⚡ Quick Settings Tile**: Add the Precord Tile to your Android notification shade for 1-tap stealth capture from anywhere.
* **📳 Shake-to-Capture**: Give your phone a quick shake to snatch the last few minutes of audio.

### 🧠 Smart Tools & Post-Production
* **🔊 Audio Enhancer**: One-tap studio polish featuring peak normalization and active high-pass filtering to cut unwanted room rumble.
* **📝 On-Device Speech Transcription**: Turn voice memos into searchable text with built-in Speech-to-Text.
* **🔁 A-B Loop Player**: Replay tricky musical passages, interviews, or sound design snippets with interactive start/end markers.
* **🏷️ Tag & Search**: Organize captures with preset tags (`Melody`, `Voice`, `Sound FX`, `Meeting`, `Idea`, `Practice`), mark favorites ⭐, and search instantly.
* **☁️ Cloud & Directory Sync**: Auto-export captures directly to your favorite cloud-synced folder using Storage Access Framework (SAF).

---

## 💎 Free vs. Pro Mode ($4.99 CAD)

Precord believes in powerful free software, but offers extra superpowers for power users:

| Feature | Free Tier | 💎 Pro Mode |
| :--- | :---: | :---: |
| **Max Capture Time** | Up to 10 Minutes | **Up to 4 Hours** |
| **Audio Formats** | WAV | **WAV, AIFF, FLAC, MP3, OGG** |
| **Background Buffering** | ✅ | ✅ |
| **Tagging & A-B Player** | ✅ | ✅ |
| **Shake-to-Capture** | 🔒 | ✅ |
| **Smart Sound Detector** | 🔒 | ✅ |
| **Audio Enhancement (EQ/Norm)** | 🔒 | ✅ |
| **Speech Transcription** | 🔒 | ✅ |
| **Quick Settings Tile** | 🔒 | ✅ |
| **Cloud Auto-Upload** | 🔒 | ✅ |

*Includes a built-in Debug Purchase button for instant testing!*

---

## ⚖️ Responsible Recording

Recording in public or private comes with legal responsibilities depending on your country or state (One-Party vs. Two-Party consent). Precord includes a built-in **Legal Guidance & Country Selector** during onboarding with direct links to recording laws in Canada, the US, UK, Australia, and beyond. **Record responsibly!**

---

## 🛠️ Built With Modern Android Tech

* **Language**: Kotlin 2.0+
* **UI**: Jetpack Compose & Material 3 (Dark Theme First 🌙)
* **Architecture**: Clean Architecture + ViewModel + Coroutines / StateFlow
* **Navigation**: Modern `Navigation3` backstack
* **Audio Core**: AudioRecord + Circular RingBuffer + MediaCodec API + Custom PCM Encoders
* **Target SDK**: Android 16 (API 36) — Optimized for Pixel 10a & modern devices

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (or newer)
* Android SDK 36
* Physical device or Emulator running Android 10+ (Android 16 recommended)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/jaspertechltd/PREcord.git

# Navigate to project folder
cd PREcord

# Build Debug APK
./gradlew assembleDebug
```

The compiled APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🤝 Contributing

Got a feature idea, audio DSP improvement, or UI tweak? Contributions, issues, and feature requests are welcome! Feel free to check out the [issues page](https://github.com/jaspertechltd/PREcord/issues).

---

Made with ❤️ and lots of ☕ for audio lovers everywhere by **[Jasper Tech Ltd](https://github.com/jaspertechltd)**.
