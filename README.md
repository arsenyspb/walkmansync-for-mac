<p align="center">
  <img src="img/walkman_logo.svg" alt="Sony Walkman Logo" width="220" />
</p>

# WalkmanSync for Mac

<p align="center">
  <img src="img/Sony%20Walkman%20NW-E405.jpeg" alt="Sony Network Walkman NW-E405" width="480" />
</p>

<p align="center">
  <strong>Native macOS SonicStage alternative for Sony Network Walkman (NW-E405 & NW-E400 series) with automated 3rd-generation OMGAUDIO database initialization and zero-friction MP3 music sync.</strong>
</p>

<p align="center">
  <a href="https://github.com/arsenyspb/walkmansync-for-mac/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/arsenyspb/walkmansync-for-mac/ci.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=CI%20Build" alt="CI Build Status" /></a>
  <a href="https://github.com/arsenyspb/walkmansync-for-mac/releases"><img src="https://img.shields.io/github/v/release/arsenyspb/walkmansync-for-mac?style=for-the-badge&logo=apple&logoColor=white&color=007AFF&label=Release" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Platform-macOS%20%7C%20Apple%20Silicon%20%26%20Intel-black?style=for-the-badge&logo=apple&logoColor=white" alt="Platform macOS" />
  <img src="https://img.shields.io/badge/Swift-5.9+-FA7343?style=for-the-badge&logo=swift&logoColor=white" alt="Language Swift" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="License GPLv3" /></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Hardware-Sony%20NW--E400%20%7C%20NW--E500%20%7C%20NW--HD-002F6C?style=for-the-badge&logo=sony&logoColor=white" alt="Hardware Target" />
  <img src="https://img.shields.io/badge/Protocol-3rd--Gen%20OMGAUDIO%20%7C%20OpenMG-orange?style=for-the-badge" alt="Protocol" />
  <img src="https://img.shields.io/badge/Dependencies-Zero%20(Pure%20Native)-success?style=for-the-badge" alt="Zero Dependencies" />
</p>

---

## Overview

**WalkmanSync for Mac** is a standalone, native macOS port and modern **SonicStage alternative** engineered specifically for retro-tech enthusiasts and indie developers using classic **Sony Network Walkman** devices. 

Historically, managing music on 3rd-generation Sony Walkmans required running deprecated Windows utilities, obsolete versions of Sony's **SonicStage**, or legacy Java tools like **JSymphonic** that depend on old JRE runtimes. WalkmanSync eliminates all friction by interacting directly with the proprietary **OMGAUDIO** file structure, handling 3rd-generation **OpenMG** device identification (`DvID.DAT`), generating big-endian database files, and performing hardware-accurate XOR audio scrambling natively on modern macOS (Apple Silicon & Intel).

---

## Key Features

- **Direct OMGAUDIO Database Initialization**: Interacts directly with the on-device `OMGAUDIO` filesystem rather than wrapping an external library. Generates `04CNTINF.DAT` content databases with big-endian UTF-16BE metadata encoding.
- **Zero-Friction 3rd-Generation OpenMG Support**:
  - Automatically manages and injects the 16-byte `MP3FM/DvID.DAT` device identity key without needing Windows `CopyTool.exe`.
  - Implements hardware-accurate in-place audio stream scrambling using Sony's key derivation formula:
    `key = ((0x2465 + trackId * 0x5296E435) & 0xFFFFFFFF) ^ deviceKey`
  - Encapsulates MP3 tracks into valid `.OMA` containers featuring the standard 3072-byte `ea3` ID3v2 tag and 96-byte `EA3` audio header (`0xFFFE` encryption marker).
- **Native macOS App (Swift & AppKit)**: Zero Java runtime dependency, zero virtual machines. Blazing-fast execution and native UI.
- **Automatic Walkman Mount Detection**: Immediately identifies connected Walkmans mounted at `/Volumes/WALKMAN`.
- **Universal Audio Ingest & Transcoding**: Syncs native MP3s directly, or automatically transcodes **FLAC, M4A (AAC/ALAC), WAV, AIFF, and OGG** to 320kbps MP3 on-the-fly via FFmpeg.
- **Native Metadata Extraction**: Uses macOS `AVFoundation` for instant extraction of track title, artist, album, genre, and duration across all audio formats.

---

## Supported Devices & Hardware Series

| Series | Models | Generation | Database Format |
|---|---|---|---|
| **NW-E400 Series** | NW-E403 (256 MB), **NW-E405** (512 MB), NW-E407 (1 GB) | 3rd Gen Flash | OMGAUDIO / OpenMG XOR |
| **NW-E500 Series** | NW-E505, NW-E507 | 3rd Gen Flash | OMGAUDIO / OpenMG XOR |
| **NW-HD Series** | NW-HD1, NW-HD3, NW-HD5 | 3rd/4th Gen HDD | OMGAUDIO |

*(Note: Designed for standard MP3 audio files. ATRAC support is part of the original protocol spec, but MP3 transcoding/transfer is prioritized for zero-dependency modern playback.)*

---

## Installation & Download

### Option 1: Pre-built Release (Recommended)
Download the latest pre-compiled build from [GitHub Releases](https://github.com/arsenyspb/walkmansync-for-mac/releases):
1. Download `WalkmanSync.app.zip`.
2. Extract the archive to get `WalkmanSync.app`.
3. Move `WalkmanSync.app` into `/Applications`.
4. Open the application.

### Option 2: Build From Source
Building requires macOS with Xcode Command Line Tools:

```bash
# Clone the repository
git clone https://github.com/arsenyspb/walkmansync-for-mac.git
cd walkmansync-for-mac

# Run the test suite
make test

# Build WalkmanSync.app
make build

# Launch the app
make run
```

---

## How to Use

1. **Connect your Sony Walkman** to your Mac via USB. The player will mount as a mass-storage drive (typically `/Volumes/WALKMAN`).
2. **Launch WalkmanSync for Mac**. The app will detect the mounted Walkman automatically.
3. Click **"Select"** under **Music Source** to pick your local folder of MP3 tracks.
4. Click **"Sync to Walkman"**.
5. WalkmanSync will:
   - Extract ID3 metadata from all audio tracks.
   - Initialize device identity and write `MP3FM/DvID.DAT` if not already present.
   - Encapsulate and XOR-scramble raw audio frames into `OMGAUDIO/10Fxx/1000xxxx.OMA`.
   - Serialize and write the `04CNTINF.DAT` database structure.
6. Safely eject the Walkman volume in Finder and enjoy your music!

---

## Automated Test Suite

A built-in test suite verifies all cryptographic and container primitives:

```bash
make test
```

Verification covers:
- 3rd-generation XOR key derivation against known Sony hardware vectors.
- Symmetric XOR audio scrambling round-trip.
- EA3 syncsafe tag and audio header byte structures.
- Binary `DvID.DAT` key serialization and extraction.

---

## Acknowledgments & Credits

This project stands on the shoulders of dedicated reverse-engineering work by the open-source community:

- **[JSymphonic](https://github.com/georgewoodall82/jsymphonic)** — The foundational open-source Java Sony Walkman manager created by Patrick Balleux, Nicolas Cardoso De Castro, and Daniel Žalar. JSymphonic pioneered community reverse-engineering of Sony's OMGAUDIO formats.
- **[MP3FM](https://github.com/xaskasdf/MP3FM)** — Clean-room specification, `FORMAT.md`, and reverse-engineering of Sony's `OMGAUDIO` protocol by `xaskasdf`.
- **FFmpeg (`libavformat/oma.c`)** — Reference implementation for OMA / EA3 container demuxing.

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)** in compliance and continuity with the original JSymphonic codebase. See the [LICENSE](LICENSE) file for the full license text.

*Sony, Network Walkman, OpenMG, ATRAC, and SonicStage are registered trademarks of Sony Corporation. WalkmanSync for Mac is an independent open-source project and is not affiliated with, endorsed by, or sponsored by Sony Corporation.*
