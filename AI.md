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
* **OpenMG 3rd-Gen Cryptography & Hardware Key Extraction:**
  * **Device Key:** Stored at byte offset `0x0A..0x0D` of `DvID.DAT`. Default placeholder key: `0x08DA6D03` (causes "CANNOT PLAY" on authentic hardware).
  * **Hardware Key Extraction:** Direct ASIC query via USB Bulk-Only Transport (`IOUSBHost.framework` with `IOUSBHostObjectInitOptionsDeviceCapture` and root privilege elevation). Bypasses macOS kernel storage locks to send CDB `A4 00 00 00 00 00 00 BC 00 12 3F 00` and read the authentic 16-byte payload (e.g. `0x08FF8139`).
  * **Track XOR Key Formula:**
    `key = ((0x2465 + UInt64(trackId) * 0x5296E435) & 0xFFFFFFFF) ^ deviceKey`
  * **Scrambling:** In-place 8-byte repeating XOR block applied to raw audio frames (ID3v2 tags stripped).
* **OMA / EA3 Container Format:**
  * **EA3 ID3v2 Tag:** Exactly 3072 bytes (`ea3\x03\x00\x00`), syncsafe header, standard ID3 frames + custom `OMG_TRACK` and `OMG_TRLDA` frames.
  * **EA3 Audio Header:** Exactly 96 bytes (`EA3\x02\x00\x60\xFF\xFE`), protection marker `0xFFFE`, MP3 codec ID `0x03`.

---

## 2. Environment & Tooling
* **Language & Runtime:** Swift 5.9+ targeting macOS 13.0+ (`arm64` and `x86_64`).
* **Frameworks:** Native `Foundation`, `AppKit`, `AVFoundation` (metadata extraction), `IOKit`, `IOUSBHost` (macOS 12.0+).
* **Dependencies:** Zero external dependencies for MP3 sync. Bundled tools (`atracdenc`) must be 100% self-contained Universal 2 binaries with NO external dynamic library linkages (`libsndfile`, Homebrew, etc.).
* **Dev Environment:** Native macOS with Xcode Command Line Tools. (Linux DevContainers are not supported due to macOS AppKit/AVFoundation/IOUSBHost requirements).
* **Build Tools:**
  * `make test`: Compiles and executes `Tests/TestRunner.swift` and runs `Tests/verify_binaries.sh` (Mach-O architecture, deployment target, and dependency validator).
  * `make build`: Compiles `WalkmanSync.app` into `WalkmanSync/WalkmanSync.app` as a Universal 2 fat binary.
  * `make dmg`: Builds `WalkmanSync.app` and packages a standard drag-and-drop installer `.dmg`.
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
1. **Mandatory Universal 2 Packaging (`arm64` + `x86_64`):**
   * Every compiled executable (the main Swift app and any bundled helpers in `Resources/`) **MUST** be compiled as a Universal 2 fat binary supporting both Apple Silicon (`arm64`) and Intel (`x86_64`). Single-architecture binaries cause instant fatal crashes on Intel Macs (`POSIXErrorCode 86: Bad CPU type in executable`).
2. **Explicit Deployment Target Flag (`-target <arch>-apple-macos13.0`):**
   * Never invoke `swiftc` or `clang`/`clang++` without explicit deployment target flags (`-target arm64-apple-macos13.0` and `-target x86_64-apple-macos13.0`).
   * Omitting `-target` allows pre-release SDK versions or host kernel triples (e.g. `arm64-apple-macosx27.2.0`) to leak into the Mach-O `LC_BUILD_VERSION` load command, preventing execution on older macOS versions with *"The application requires macOS XX.0 or later"*.
3. **Strict Zero Non-System Dynamic Dependencies:**
   * Any helper tool bundled into `WalkmanSync.app/Contents/Resources/` (such as `atracdenc`) **MUST NEVER** link dynamically against Homebrew libraries (`/opt/homebrew/...` or `/usr/local/...`). All helpers must be statically linked or implement self-contained parsers linking only to standard macOS system libraries (`/usr/lib/libSystem.B.dylib`, `/usr/lib/libc++.1.dylib`).
4. **Automated Binary Verification in Tests:**
   * `make test` enforces `Tests/verify_binaries.sh`, validating that all bundled binaries are dual-architecture, have `minos <= 13.0`, and have zero non-system dynamic linkages.
5. **Hardware Compatibility & Round-Trip Verification:**
   * Any modification to cryptographic routines, OMA container headers, or binary DB serialization must be accompanied by tests in `Tests/TestRunner.swift`.
6. **Non-Destructive Device Operations:**
   * Do not format or erase user files on connected volumes outside the managed `OMGAUDIO/` and `MP3FM/` folders.
7. **Validation Routine:**
   * Always run `make test` and `make dmg` before submitting changes. Ensure all targets compile cleanly with zero errors.

---

## 5. Known Limitations & Technical Roadmap
* **Bitrate Assumptions:** Currently hardcoded to 128 kbps CBR in `OMAContainerBuilder.buildEA3AudioHeader` (`0xD9`) and `WalkmanDBGenerator.writeCNFBelement`. Support for dynamic bitrates (VBR/CBR header inspection) is an active area for improvement.
* **Volume Auto-Detection:** Currently looks for `/Volumes/WALKMAN` or mounted volumes containing `OMGAUDIO`.
* **ATRAC Transcoding:** Future enhancement if modern ATRAC encoders are integrated.
