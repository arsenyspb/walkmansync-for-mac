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

- **Direct OMGAUDIO Database Initialization**: Interacts directly with the on-device `OMGAUDIO` filesystem rather than wrapping an external library. Generates the full 16-table database suite (`00GTRLST`, `01TREE01-04/22/2D`, `02TREINF`, `03GINF01-04/22/2D`, `04CNTINF`, `05CIDLST`) with big-endian UTF-16BE metadata encoding and jog-dial navigation trees.
- **Selectable Audio Codecs (ATRAC3 & MP3)**:
  - **ATRAC3 LP2 (132 kbps)**: Sony's native hardware audio format, providing ~212 songs on 512 MB.
  - **ATRAC3 LP4 (66 kbps)**: Ultra-compact storage mode providing ~383 songs on 512 MB.
  - **ATRAC3plus (256 kbps)**: High-bitrate studio quality.
  - **MP3 (320 kbps CBR)**: Universal MP3 audio with hardware-accurate XOR payload scrambling.
- **Dynamic Song Capacity Estimator**:
  - Live capacity forecasting displayed in both the AppKit GUI and CLI (`walkmansync --detect`).
  - Automatically updates estimated song counts as you toggle between codecs.
- **Hardware Encryption Key Extraction & Auto-Healing**:
  - Direct native extraction of authentic factory hardware keys from the Walkman's ASIC register over USB Bulk-Only Transport (`IOUSBHost.framework`), completely eliminating the need for Windows or Sony's legacy `CopyTool.exe`.
  - One-click elevation in GUI (with standard macOS administrator prompt) and CLI (`walkmansync --extract-key`).
  - Automatic key discovery and persistent multi-device backup to `~/Library/Application Support/WalkmanSync/DvID.DAT`.
  - Self-healing: if the Walkman is ever formatted, WalkmanSync immediately restores the authentic factory key with zero user intervention.
- **Native macOS App (Swift & AppKit) + CLI**: Zero Java runtime dependency, zero virtual machines. Native macOS interface with official 2000s Walkman branding and full-featured CLI for terminal/scripting workflows.
- **Universal Audio Ingest & Transcoding**: Ingests **MP3, FLAC, M4A (AAC/ALAC), WAV, AIFF, and OGG**, automatically converting and preparing containers on-the-fly.

---

## Dependencies & Audio Encoders

WalkmanSync is designed to be as self-contained as possible:

| Feature / Workflow | Required Tools | Bundled / Installation |
|---|---|---|
| **Direct MP3 Sync (Standard `.mp3` files)** | None | **100% Zero Dependencies** (Built-in pure Swift) |
| **ATRAC3 / ATRAC3plus Encoding** | `atracdenc` + `ffmpeg` | `atracdenc` is **Pre-Bundled** inside the App; `ffmpeg` via Homebrew |
| **Non-MP3 Ingest (FLAC, M4A, WAV, AIFF, OGG)** | `ffmpeg` | `brew install ffmpeg` |
| **Hardware Key Extraction & OMGAUDIO DB** | None | **Pure Native macOS (IOKit & Swift)** |

### Optional: Installing FFmpeg
If you plan to use Sony's native hardware **ATRAC3** encoder or transfer lossless **FLAC / M4A** files:
```bash
brew install ffmpeg
```
*(Tip: If your library is already in standard `.mp3` format and you select the **MP3 (320 kbps CBR)** option, no external tools are required!)*

### System Health & Dependency Doctor
Audit your machine's audio engine, encoders, and connected player anytime:

- **From the GUI**: Click the **"🩺 Doctor..."** button at the bottom of the window.
- **From the CLI**:
  ```bash
  walkmansync --doctor
  ```
  Or for machine-readable JSON:
  ```bash
  walkmansync --doctor --json
  ```

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
3. **One-Time Hardware Key Setup (If Needed)**:
   - If your Walkman was freshly formatted or you are on a new Mac, click **"🔑 Extract"** in the GUI (or run `walkmansync --extract-key` in Terminal).
   - Enter your macOS administrator password when prompted. WalkmanSync will seize the USB interface, extract your player's genuine hardware key, and permanently cache it.
4. Click **"Select"** under **Music Source** to pick your local folder of MP3 tracks.
5. Click **"Sync to Walkman"**.
6. WalkmanSync will:
   - Extract ID3 metadata from all audio tracks.
   - Read device identity from `MP3FM/DvID.DAT`.
   - Encapsulate and XOR-scramble raw audio frames into `OMGAUDIO/10Fxx/1000xxxx.OMA`.
   - Serialize and write the complete 16-table OMGAUDIO database suite (`00GTRLST.DAT`, `01TREE01-04/22/2D.DAT`, `02TREINF.DAT`, `03GINF01-04/22/2D.DAT`, `04CNTINF.DAT`, `05CIDLST.DAT`).
   - Clean macOS AppleDouble (`._*`) metadata files and flush disk caches.
6. Safely eject the Walkman volume in Finder and enjoy your music!

---

## Device Encryption Key & DvID.DAT Extraction

3rd-generation Network Walkman devices (NW-E400, NW-E500, and NW-HD series) require audio payloads to be scrambled with a 4-byte key unique to the player's internal ASIC/ROM. 

To understand how Sony's Windows installer (`MP3FMV2_ENG.EXE` / `CopyTool.exe`) retrieves this key via vendor SCSI commands, why audio playback displays "CANNOT PLAY" without the authentic device key, and how macOS user-space kernel storage policies affect native retrieval:

👉 **[Read the Full Reverse-Engineering Report: CopyTool.exe.DvID.dat.md](CopyTool.exe.DvID.dat.md)**

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
