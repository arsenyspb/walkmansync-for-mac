# AI Agent Development Instructions — WalkmanSync for Mac

## 1. Project Overview & Domain Context
WalkmanSync for Mac is a zero-dependency, native macOS tool (Swift 5.9+, AppKit) engineered to manage music on 3rd-generation Sony Network Walkman devices (NW-E400/E500/HD series). It replaces obsolete Windows utilities (SonicStage) and legacy Java tools (JSymphonic) by directly communicating with the Walkman's native mass-storage filesystem.

The application operates in **dual-mode**:
* **Native GUI (Default):** AppKit window with live USB volume auto-detection, music directory picker, and progress feedback.
* **Transparent CLI:** Headless CLI for scripting and automation with `--sync`, `--detect`, `--scan`, `--clean`, `--dry-run`, and `--json` support.

### Core Device Specifications & Protocols:
* **Filesystem Structure:**
  * `OMGAUDIO/`: Destination for audio containers and database files.
    * `10Fxx/1000xxxx.OMA`: Encrypted audio tracks (max 256 tracks per directory, indexed by `trackId`).
    * Full database suite: `00GTRLST.DAT`, `01TREE01.DAT`..`04.DAT`, `02TREINF.DAT`, `03GINF01.DAT`..`04.DAT`, `04CNTINF.DAT`, `05CIDLST.DAT`.
  * `MP3FM/`: Contains `DvID.DAT`, a 16-byte device identity file.
* **OpenMG 3rd-Gen Cryptography:**
  * **Device Key:** Stored at byte offset `0x0A..0x0D` of `DvID.DAT`. Default factory key: `0x08DA6D03`.
  * **Track XOR Key Formula:**
    `key = ((0x2465 + UInt64(trackId) * 0x5296E435) & 0xFFFFFFFF) ^ deviceKey`
  * **Scrambling:** In-place 8-byte repeating XOR block applied to raw audio frames (ID3v2 tags stripped).
* **OMA / EA3 Container Format:**
  * **EA3 ID3v2 Tag:** Exactly 3072 bytes (`ea3\x03\x00\x00`), syncsafe header, standard ID3 frames + custom `OMG_TRACK` and `OMG_TRLDA` frames.
  * **EA3 Audio Header:** Exactly 96 bytes (`EA3\x02\x00\x60\xFF\xFE`), protection marker `0xFFFE`, MP3 codec ID `0x03`.

---

## 2. Environment & Tooling
* **Language & Runtime:** Swift 5.9+ targeting macOS 12.0+ (`darwin`).
* **Frameworks:** Native `Foundation`, `AppKit`, `AVFoundation` (metadata extraction).
* **Dependencies:** Zero external dependencies (no third-party packages, no Java, no VMs).
* **Dev Environment:** Native macOS with Xcode Command Line Tools. (Linux DevContainers are not supported due to macOS AppKit/AVFoundation requirements).
* **Build Tools:**
  * `make test`: Compiles and executes `Tests/TestRunner.swift`.
  * `make build`: Compiles `WalkmanSync.app` into `WalkmanSync/WalkmanSync.app`.
  * `make run`: Compiles and launches `WalkmanSync.app`.
  * `make clean`: Removes binaries, build artifacts, and generated `.app` bundles.

---

## 3. Architecture & Module Map
* `WalkmanSync/Sources/WalkmanSyncApp.swift`: Main entry point (`WalkmanSyncMain`), routes to CLI or GUI mode, handles AppKit UI lifecycle, dynamic dock icon, and periodic USB volume detection.
* `WalkmanSync/Sources/CLIHandler.swift`: Command-line interface parser supporting `--detect`, `--scan`, `--sync`, `--clean`, `--dry-run`, `--json`, and `--verbose`.
* `WalkmanSync/Sources/SyncEngine.swift`: Enumerates local MP3 files, extracts metadata via `AVFoundation`, prepares directories, drives OMA conversion, and manages the sync pipeline.
* `WalkmanSync/Sources/OMAContainerBuilder.swift`: Strips MP3 ID3 tags, constructs the 3072-byte `ea3` tag, 96-byte `EA3` header, and assembles the encrypted `.OMA` payload.
* `WalkmanSync/Sources/WalkmanDBGenerator.swift`: Generates the complete 8-table (12 files) OMGAUDIO database suite (`00GTRLST`, `01TREE01..04`, `02TREINF`, `03GINF01..04`, `04CNTINF`, `05CIDLST`) with UTF-16BE metadata strings and group relationships.
* `WalkmanSync/Sources/WalkmanKeyManager.swift`: Handles `DvID.DAT` discovery/generation, XOR key derivation, and buffer scrambling.
* `WalkmanSync/Sources/WalkmanLogger.swift`: Thread-safe file and console logging stored at `~/Library/Logs/WalkmanSync/walkmansync.log`.
* `Tests/TestRunner.swift`: Self-contained verification suite checking key derivation, XOR round-trip, EA3 tag/header structural integrity, DvID I/O, and database generation.

---

## 4. Development & Testing Principles
1. **Zero External Dependencies:** Do not introduce third-party Swift packages or external binaries unless explicitly approved.
2. **Hardware Compatibility & Round-Trip Verification:** Any modification to cryptographic routines, OMA container headers, or binary DB serialization must be accompanied by tests in `Tests/TestRunner.swift`.
3. **Non-Destructive Device Operations:** Do not format or erase user files on connected volumes outside the managed `OMGAUDIO/` and `MP3FM/` folders.
4. **Validation Routine:** Always run `make test` before submitting changes. Ensure `make build` compiles with zero warnings.

---

## 5. Known Limitations & Technical Roadmap
* **Bitrate Assumptions:** Currently hardcoded to 128 kbps CBR in `OMAContainerBuilder.buildEA3AudioHeader` (`0xD9`) and `WalkmanDBGenerator.writeCNFBelement`. Support for dynamic bitrates (VBR/CBR header inspection) is an active area for improvement.
* **Volume Auto-Detection:** Currently looks for `/Volumes/WALKMAN` or mounted volumes containing `OMGAUDIO`.
* **ATRAC Transcoding:** Future enhancement if modern ATRAC encoders are integrated.
