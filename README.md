<p align="center">
  <img src="img/walkman_logo.svg" alt="Sony Walkman Logo" width="220" />
</p>

# WalkmanSync for Mac

<p align="center">
  <img src="img/Sony%20Walkman%20NW-E405.jpeg" alt="Sony Network Walkman NW-E405" width="480" />
</p>

<p align="center">
  <strong>Native macOS SonicStage alternative for Sony Network Walkman (NW-E400, NW-E500, NW-HD series) with silicon-level hardware cryptographic key extraction, automated 3rd-generation OMGAUDIO database generation, and drag-and-drop music synchronization.</strong>
</p>

<p align="center">
  <a href="https://github.com/arsenyspb/walkmansync-for-mac/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/arsenyspb/walkmansync-for-mac/ci.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=CI%20Build" alt="CI Build Status" /></a>
  <a href="https://github.com/arsenyspb/walkmansync-for-mac/releases"><img src="https://img.shields.io/github/v/release/arsenyspb/walkmansync-for-mac?style=for-the-badge&logo=apple&logoColor=white&color=007AFF&label=Release%20v0.3.1" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Platform-macOS%20%7C%20Apple%20Silicon%20%26%20Intel-black?style=for-the-badge&logo=apple&logoColor=white" alt="Platform macOS" />
  <img src="https://img.shields.io/badge/Packaging-.DMG%20Installer-green?style=for-the-badge&logo=apple&logoColor=white" alt="DMG Packaging" />
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

**WalkmanSync for Mac** is a standalone, native macOS application and CLI engineered specifically for retro-tech enthusiasts, audiophiles, and indie developers using classic **Sony Network Walkman** devices.

Historically, loading music onto 3rd-generation Sony Walkmans required running deprecated Windows utilities, obsolete versions of Sony's **SonicStage** inside virtual machines, or legacy Java tools like **JSymphonic** that depend on retired JRE versions. Furthermore, modern alternatives consistently caused the dreaded **"CANNOT PLAY"** or **"MG ERROR"** failure on authentic hardware because audio frames must be scrambled with an unforgeable factory cryptographic key burned into the Walkman's internal ASIC chip.

WalkmanSync solves this entirely on native macOS:
1. **Direct Hardware Key Extraction**: Interrogates the Walkman's internal silicon register over USB Bulk-Only Transport (`IOUSBHost.framework`), extracting the authentic 16-byte `DvID.DAT` payload and 4-byte hardware key (`0x08FF8139`).
2. **Automated OMGAUDIO Suite Generation**: Generates the complete 16-table database suite with big-endian UTF-16BE metadata encoding and jog-dial navigation trees.
3. **Hardware-Accurate Scrambling**: Encrypts MP3/ATRAC frames in-place using Sony's authentic XOR mask formula.
4. **Standard Drag-and-Drop `.dmg` Distribution**: Zero installation friction on Apple Silicon and Intel Macs.

---

## The "CANNOT PLAY" Mystery & Silicon Root of Trust

During our reverse-engineering of Sony's `CopyTool.exe` and `FrankPACAPI.dll`, we uncovered the architectural reason why previous open-source tools failed on physical hardware:

$$\text{track\_key} = ((0\text{x}2465 + \text{track\_id} \times 0\text{x}5296\text{E}435) \ \& \ 0\text{xFFFFFFFF}) \oplus \mathbf{K_{\text{hardware}}}$$

- **Zero-Trust Flash Architecture**: The Walkman's DSP chip **never reads `DvID.DAT` from flash storage during playback**. During playback, the DSP descrambles audio in hardware using its **own burned-in factory key etched into internal ASIC ROM/EEPROM**.
- **The Role of `DvID.DAT`**: `/Volumes/WALKMAN/MP3FM/DvID.DAT` is strictly an *interchange file* created by Sony's PC tools so the transfer program knows which key to scramble with.
- **Why Placeholders Fail**: If audio is scrambled using placeholder keys (`0x08DA6D03`), the DSP descrambles invalid MPEG frame headers (`0xFFFB`) and instantly halts playback with **"CANNOT PLAY"**.
- **The Solution**: WalkmanSync extracts the authentic key directly from the ASIC over USB, writes `/Volumes/WALKMAN/MP3FM/DvID.DAT`, and permanently caches it in `~/Library/Application Support/WalkmanSync/`. Audio playback on physical hardware succeeds with **100% audio fidelity**.

---

## Key Features

- **Native Silicon Key Extraction (1-Click Elevation)**:
  - Bypasses macOS kernel storage locks via `IOUSBHost.framework` (`IOUSBHostObjectInitOptionsDeviceCapture`).
  - Single-click **"🔑 Extract"** button in GUI prompts standard macOS Touch ID / Password dialog.
  - Headless CLI extraction via `walkmansync --extract-key`.
- **Automatic Self-Healing & Multi-Device Profiles**:
  - Permanently caches keys in `~/Library/Application Support/WalkmanSync/DvID.DAT` and `DvID_<KEY>.DAT`.
  - If the Walkman is ever formatted via macOS Disk Utility or the player's internal settings menu, WalkmanSync **automatically detects the erased drive and restores the authentic key** with zero prompts.
  - Supports multiple Walkmans independently without key collisions.
- **Complete OMGAUDIO Database Suite (16 DAT Tables)**:
  - Generates `00GTRLST`, `01TREE01`–`04`, `01TREE22`, `01TREE2D` (jog-dial navigation), `02TREINF`, `03GINF01`–`04`, `03GINF22`, `03GINF2D`, `04CNTINF`, and `05CIDLST`.
  - Formats all artist, album, and track strings in big-endian UTF-16BE.
- **Selectable Audio Codecs (ATRAC3 & MP3)**:
  - **ATRAC3 LP2 (132 kbps)**: Sony's native hardware audio format (~212 songs on 512 MB).
  - **ATRAC3 LP4 (66 kbps)**: High-efficiency mode (~383 songs on 512 MB).
  - **ATRAC3plus (256 kbps)**: Studio quality playback.
  - **MP3 (320 kbps CBR)**: Universal MP3 audio with hardware-accurate XOR payload scrambling.
- **Dynamic Song Capacity Estimator**:
  - Live capacity forecasting displayed in GUI and CLI (`walkmansync --detect`).
  - Recalculates remaining song estimates across all formats in real time.
- **Universal Audio Ingest**:
  - Ingests **MP3, FLAC, M4A (AAC/ALAC), WAV, AIFF, and OGG**, automatically converting and preparing containers on-the-fly.
- **Proactive System Health Doctor**:
  - Audits FFmpeg, bundled `atracdenc`, Walkman USB connection, and key authenticity (`walkmansync --doctor` or GUI **"🩺 Doctor..."** modal).
- **Auto-Update Self-Check**:
  - Asynchronously queries GitHub Releases for newer builds and presents a notification badge with direct download links.

---

## Dependencies & Audio Encoders

| Feature / Workflow | Required Tools | Status |
|---|---|---|
| **Direct MP3 Sync (Standard `.mp3` files)** | None | **100% Zero Dependencies** (Built-in pure Swift) |
| **ATRAC3 / ATRAC3plus Encoding** | `atracdenc` + `ffmpeg` | `atracdenc` is **Pre-Bundled**; `ffmpeg` via Homebrew |
| **Non-MP3 Ingest (FLAC, M4A, WAV, AIFF, OGG)** | `ffmpeg` | `brew install ffmpeg` |
| **Hardware Key Extraction & OMGAUDIO DB** | None | **Pure Native macOS (IOKit & IOUSBHost)** |

### Optional: Installing FFmpeg
If you plan to encode into Sony's native **ATRAC3** format or transfer lossless **FLAC / M4A** files:
```bash
brew install ffmpeg
```
*(Tip: If your library is already in standard `.mp3` format, selecting **MP3 (320 kbps CBR)** requires zero external tools!)*

---

## Installation & Distribution (.DMG)

### Option 1: Official Drag-and-Drop DMG (Recommended)
Download the latest `.dmg` installer from [GitHub Releases](https://github.com/arsenyspb/walkmansync-for-mac/releases):
1. Download **`WalkmanSync-0.3.1.dmg`**.
2. Open the disk image.
3. Drag **WalkmanSync.app** into the **Applications** folder.
4. Launch WalkmanSync from Launchpad or Spotlight.

```
+-----------------------------------------------------+
|                     WalkmanSync                     |
|                                                     |
|       [ WalkmanSync.app ]   --->   [ Applications ] |
|                                                     |
|       Drag WalkmanSync to Applications to install   |
+-----------------------------------------------------+
```

### Option 2: Build From Source
Building requires macOS 12.0+ with Xcode Command Line Tools:

```bash
# Clone the repository
git clone https://github.com/arsenyspb/walkmansync-for-mac.git
cd walkmansync-for-mac

# Run the 8-suite test suite
make test

# Build the standard DMG package
make dmg

# Launch the app
make run
```

---

## How to Use

### Graphic User Interface (GUI)
1. **Connect your Sony Walkman** to your Mac via USB. The player will mount as a mass-storage drive (`/Volumes/WALKMAN`).
2. **Launch WalkmanSync**. The app will detect the connected player automatically.
3. **One-Time Key Extraction (First Run or New Walkman)**:
   - If the player is uninitialized or uses a placeholder key, the status bar displays `• Key: Placeholder ⚠️` and the button shows **"🔑 Extract"**.
   - Click **"🔑 Extract"** and enter your macOS administrator password. WalkmanSync will seize the USB interface, extract the authentic factory key, and permanently cache it.
4. Click **"Select"** under **Music Source** to pick your local folder of music files.
5. Select your target **Audio Codec** (e.g. *ATRAC3 LP2* or *MP3*).
6. Click **"Sync to Walkman"**.

### Command-Line Interface (CLI)

WalkmanSync includes a transparent command-line interface:

```bash
# Inspect system health, audio encoders, and hardware key
walkmansync --doctor

# Detect mounted Walkman devices and song capacity estimates
walkmansync --detect

# Extract authentic hardware key from connected Walkman (JSON supported)
walkmansync --extract-key

# Check GitHub for newer WalkmanSync releases
walkmansync --check-update

# Scan a local folder to inspect track metadata and formats
walkmansync --scan ~/Music/Album

# Sync music library to Walkman using ATRAC3 LP2 (Default)
walkmansync --sync --source ~/Music/Album

# Sync music library with 320k CBR MP3 encoding
walkmansync --sync --source ~/Music/Album --codec mp3

# Perform a dry run without writing to flash
walkmansync --sync --source ~/Music/Album --dry-run

# Output machine-readable JSON for scripting and agent automation
walkmansync --detect --json
walkmansync --doctor --json
```

---

## Supported Hardware

| Series | Models | Generation | Database Format |
|---|---|---|---|
| **NW-E400 Series** | NW-E403 (256 MB), **NW-E405** (512 MB), NW-E407 (1 GB) | 3rd Gen Flash | OMGAUDIO / OpenMG XOR |
| **NW-E500 Series** | NW-E503, NW-E505, NW-E507 | 3rd Gen Flash | OMGAUDIO / OpenMG XOR |
| **NW-HD Series** | NW-HD1, NW-HD3, NW-HD5 | 3rd/4th Gen HDD | OMGAUDIO |
| **NW-A Series** | NW-A600, NW-A1000, NW-A3000 | 3rd/4th Gen | OMGAUDIO |

---

## Technical Documentation & Research

For deep-dive reverse-engineering reports, disassembly analyses, SCSI command specifications, and architectural Mermaid diagrams of the macOS kernel bypass:
- **[Reverse-Engineering Sony's CopyTool.exe & Hardware Key Extraction Saga](CopyTool.exe.DvID.dat.md)**
- **[AI Agent Development & Architecture Instructions](AI.md)**

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.

*Sony, Walkman, ATRAC, ATRAC3, ATRAC3plus, SonicStage, and OpenMG are trademarks or registered trademarks of Sony Corporation. This project is an independent, clean-room open-source initiative developed for hardware preservation and retro-computing compatibility.*
