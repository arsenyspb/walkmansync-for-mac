# WalkmanSync for macOS

<p align="center">
  <img src="img/Sony%20Walkman%20NW-E405.jpeg" alt="Sony Walkman NW-E405" width="480" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" />
</p>

A lightweight, native macOS utility to sync MP3 audio tracks directly to classic **Sony Network Walkman** devices (such as the NW-E405, NW-E407, NW-E505, and NW-HD series) without requiring Windows, legacy SonicStage software, or Java runtimes.

---

## Features

- **Native macOS App (Swift & AppKit)**: Blazing fast, zero runtime dependencies. No JRE or Windows VMs required.
- **Auto-Detection**: Automatically detects connected Walkmans mounted at `/Volumes/WALKMAN`.
- **Zero-Friction 3rd Generation Support**:
  - Automatically manages `DvID.DAT` device keys.
  - Native in-place XOR scrambling using Sony's proprietary key formula:
    $$\text{key} = ((0\text{x}2465 + \text{trackId} \times 0\text{x}5296\text{E}435) \ \& \ 0\text{xFFFFFFFF}) \oplus \text{deviceKey}$$
  - Converts MP3 tracks into valid `.OMA` containers with standard 3072-byte `ea3\x03` ID3v2 tags and 96-byte `EA3\x02` audio headers (`0xFFFE` encryption marker).
- **Native Metadata Extraction**: Reads track titles, artists, albums, genres, and durations directly via `AVFoundation`.
- **Database Generation**: Generates Sony `OMGAUDIO/04CNTINF.DAT` content databases with big-endian UTF-16BE encoding.

---

## Supported Devices

- **Sony Network Walkman NW-E403 / NW-E405 / NW-E407** (Generation 3 Flash)
- **Sony Network Walkman NW-E505 / NW-E507**
- **Sony NW-HD1 / NW-HD3 / NW-HD5** (OMGAUDIO format)
- Other 3rd–4th generation Sony Walkman players using the `OMGAUDIO` database structure.

---

## Installation & Download

### Option 1: Download Pre-built Release (Recommended)
Download the latest `WalkmanSync.app.zip` from the [GitHub Releases](https://github.com/arsenyspb/jsymphonic-mac-port/releases) page:
1. Download the release archive.
2. Unzip `WalkmanSync.app`.
3. Move `WalkmanSync.app` to your `/Applications` folder.
4. Launch the application!

### Option 2: Build From Source
Building requires macOS with Xcode Command Line Tools installed:

```bash
# Clone the repository
git clone https://github.com/arsenyspb/jsymphonic-mac-port.git
cd jsymphonic-mac-port

# Run tests
make test

# Build WalkmanSync.app
make build

# Launch the app
make run
```

---

## How to Use

1. **Connect your Sony Walkman** to your Mac via USB. It will appear as a removable disk (e.g. `/Volumes/WALKMAN`).
2. **Launch WalkmanSync**. The application will automatically detect your mounted Walkman.
3. Click **"Select"** under **Music Source** and choose a local folder containing your `.mp3` files.
4. Click **"Sync to Walkman"**.
5. WalkmanSync will:
   - Read ID3 metadata from each track.
   - Automatically configure device identification (`DvID.DAT`).
   - Package tracks into encrypted `.OMA` containers inside `OMGAUDIO/10Fxx/`.
   - Rebuild the `04CNTINF.DAT` master database.
6. Safely eject the Walkman volume in Finder and enjoy your music on the go!

---

## Automated Test Suite

A built-in test suite verifies the cryptographic and container primitives:
```bash
make test
```
Verifies:
- Sony 3rd Gen XOR key derivation against known hardware vectors.
- Symmetric audio XOR scrambling round-trip.
- EA3 syncsafe tag and audio header byte structures.
- Binary `DvID.DAT` key serialization and deserialization.

---

## Acknowledgments & Credits

This project builds upon reverse-engineering research and work done by the open-source community:

- **[JSymphonic](https://github.com/georgewoodall82/jsymphonic)** — Original open-source Java Sony Walkman manager by Patrick Balleux, Nicolas Cardoso De Castro, and Daniel Žalar.
- **[MP3FM](https://github.com/xaskasdf/MP3FM)** — Clean-room specification, `FORMAT.md`, and reverse-engineering of Sony's `OMGAUDIO` protocol by `xaskasdf`.
- **FFmpeg (`libavformat/oma.c`)** — Reference implementation for OMA / EA3 container parsing.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
Sony, Walkman, OpenMG, and SonicStage are registered trademarks of Sony Corporation. This software is an independent open-source project and is not affiliated with or endorsed by Sony Corporation.
