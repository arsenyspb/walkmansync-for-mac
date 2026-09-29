# WalkmanSync for macOS

<p align="center">
  <img src="img/Sony%20Walkman%20NW-E405.jpeg" alt="Sony Walkman NW-E405" width="480" />
</p>

A lightweight, native macOS utility to sync MP3 audio tracks directly to classic **Sony Network Walkman** devices (such as the NW-E405, NW-E407, NW-E505, and NW-HD series) without requiring Windows, legacy SonicStage software, or Java runtimes.

---

## Features

- **Native macOS App (Swift & AppKit)**: Fast and responsive with zero runtime dependencies. No JRE or Windows virtual machines needed.
- **Auto-Detection**: Automatically detects connected Walkmans mounted at `/Volumes/WALKMAN`.
- **Zero-Friction 3rd Generation Support**:
  - Automatically manages `DvID.DAT` device keys without requiring user intervention.
  - Native in-place XOR scrambling using Sony's key derivation formula:
    `key = ((0x2465 + trackId * 0x5296E435) & 0xFFFFFFFF) ^ deviceKey`
  - Encapsulates MP3 tracks into valid `.OMA` containers with standard 3072-byte `ea3` ID3v2 tags and 96-byte `EA3` audio headers (`0xFFFE` encryption marker).
- **Native Metadata Extraction**: Reads track titles, artists, albums, genres, and durations directly using macOS `AVFoundation`.
- **Database Generation**: Generates Sony `OMGAUDIO/04CNTINF.DAT` content databases with big-endian UTF-16BE string encoding.

---

## Supported Devices

- **Sony Network Walkman NW-E403 / NW-E405 / NW-E407** (3rd Generation Flash)
- **Sony Network Walkman NW-E505 / NW-E507**
- **Sony NW-HD1 / NW-HD3 / NW-HD5** (OMGAUDIO database format)
- Other 3rd and 4th generation Sony Network Walkman players using the `OMGAUDIO` database structure.

---

## Installation & Download

### Option 1: Download Pre-built Release (Recommended)
Download the latest release archive from [GitHub Releases](https://github.com/arsenyspb/jsymphonic-mac-port/releases):
1. Download `WalkmanSync.app.zip`.
2. Double-click the zip archive to extract `WalkmanSync.app`.
3. Move `WalkmanSync.app` to your `/Applications` folder.
4. Open the application.

### Option 2: Build From Source
Building requires macOS with Xcode Command Line Tools installed:

```bash
# Clone the repository
git clone https://github.com/arsenyspb/jsymphonic-mac-port.git
cd jsymphonic-mac-port

# Run automated tests
make test

# Build WalkmanSync.app
make build

# Launch the app
make run
```

---

## How to Use

1. **Connect your Sony Walkman** to your Mac via USB. It will mount as a removable drive (e.g. `/Volumes/WALKMAN`).
2. **Launch WalkmanSync**. It will automatically detect the mounted Walkman.
3. Click **"Select"** under **Music Source** and choose the folder containing your MP3 files.
4. Click **"Sync to Walkman"**.
5. WalkmanSync will:
   - Read ID3 metadata from each track.
   - Verify or create the device key file (`MP3FM/DvID.DAT`).
   - Wrap and scramble tracks into `.OMA` files inside `OMGAUDIO/10Fxx/`.
   - Generate the `04CNTINF.DAT` database.
6. Eject the Walkman volume in Finder and enjoy your music!

---

## Automated Test Suite

A built-in test suite verifies cryptographic and container primitives:

```bash
make test
```

Tests verify:
- 3rd Generation XOR key derivation against known hardware vectors.
- Symmetric XOR audio scrambling round-trip.
- EA3 syncsafe tag and audio header byte structures.
- Binary `DvID.DAT` key serialization and deserialization.

---

## Acknowledgments & Credits

This project builds upon the hard work and reverse-engineering research of the open-source community:

- **[JSymphonic](https://github.com/georgewoodall82/jsymphonic)** — Original open-source Java Sony Walkman manager by Patrick Balleux, Nicolas Cardoso De Castro, and Daniel Žalar (licensed under GNU GPLv3).
- **[MP3FM](https://github.com/xaskasdf/MP3FM)** — Clean-room specification, `FORMAT.md`, and reverse-engineering of Sony's `OMGAUDIO` protocol by `xaskasdf` (released into the public domain via Unlicense).
- **FFmpeg (`libavformat/oma.c`)** — Reference implementation for OMA and EA3 container demuxing.

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)** in compliance and continuity with the original JSymphonic codebase. See the [LICENSE](LICENSE) file for the full license text.

*Sony, Network Walkman, OpenMG, and SonicStage are trademarks of Sony Corporation. This software is an independent open-source tool and is not affiliated with or endorsed by Sony Corporation.*
