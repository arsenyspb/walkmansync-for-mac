<p align="center">
  <img src="img/walkman_logo.svg" alt="Sony Walkman Logo" width="220" />
</p>

# WalkmanSync for Mac

<p align="center">
  <img src="img/Sony%20Walkman%20NW-E405.jpeg" alt="Sony Network Walkman NW-E405" width="480" />
</p>

<p align="center">
  <strong>The easiest, modern way to put music onto your vintage Sony Network Walkman directly from your Mac. No Windows, no virtual machines, no clunky old software.</strong>
</p>

<p align="center">
  <a href="https://github.com/arsenyspb/walkmansync-for-mac/releases/latest"><img src="https://img.shields.io/badge/Download-WalkmanSync%20.DMG-007AFF?style=for-the-badge&logo=apple&logoColor=white" alt="Download WalkmanSync DMG" /></a>
  <a href="https://github.com/arsenyspb/walkmansync-for-mac/releases"><img src="https://img.shields.io/github/v/release/arsenyspb/walkmansync-for-mac?style=for-the-badge&logo=github&logoColor=white&color=black&label=Version" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/macOS-Apple%20Silicon%20%26%20Intel-black?style=for-the-badge&logo=apple&logoColor=white" alt="Platform macOS" />
  <img src="https://img.shields.io/badge/Setup-Zero%20Config%20(Drag%20%26%20Drop)-success?style=for-the-badge" alt="Zero Config" />
</p>

---

## What is WalkmanSync?

Did you buy a vintage **Sony Network Walkman** (like the NW-E405 or NW-E507) and want to load your favorite albums onto it from your Mac?

Back in the 2000s, Sony required an old Windows PC running obsolete software called **SonicStage**. Modern computers cannot run SonicStage, and simply copying MP3 files onto the player's USB drive makes the Walkman say **"CANNOT PLAY"** because Sony Walkmans require a secret factory key built into the player's chip to unlock music.

**WalkmanSync for Mac** fixes everything! It is a friendly, native Mac app that:
- Connects directly to your Walkman over USB.
- Reads your player's real built-in factory key so all your music actually plays with zero errors.
- Automatically builds the Walkman's internal music menus and artist lists.
- Works 100% natively on any modern Mac (MacBook Air, MacBook Pro, iMac, Mac mini, Mac Studio).

---

## 🚀 Easy 1-Minute Installation

You do not need to be a programmer or know terminal commands. Installing WalkmanSync is just like any normal Mac app:

### Step 1: Download the App
Click the blue download button below to grab the latest official installer:

<p align="center">
  <a href="https://github.com/arsenyspb/walkmansync-for-mac/releases/latest/download/WalkmanSync.dmg">
    <img src="https://img.shields.io/badge/⬇️_Download_WalkmanSync_for_Mac-(.DMG)-007AFF?style=for-the-badge&logo=apple&logoColor=white" alt="Download WalkmanSync" />
  </a>
</p>

*(Or visit [GitHub Releases](https://github.com/arsenyspb/walkmansync-for-mac/releases/latest) to pick `WalkmanSync.dmg`).*

### Step 2: Drag to Applications
Open the downloaded `WalkmanSync.dmg` file, then drag the **WalkmanSync** icon into your **Applications** folder:

```text
+-----------------------------------------------------+
|                     WalkmanSync                     |
|                                                     |
|       [ WalkmanSync.app ]   --->   [ Applications ] |
|                                                     |
|          Drag WalkmanSync into Applications         |
+-----------------------------------------------------+
```

### Step 3: Open WalkmanSync
Open your **Applications** folder (or press `Cmd + Space` and search for *WalkmanSync*) and launch it!

---

## 🎵 How to Put Songs on Your Walkman (Quick Start)

### 1. Plug In Your Walkman
Connect your Sony Walkman to your Mac with its USB cable. The screen on your Walkman will light up and display **`USB CONNECT`**.

### 2. Launch WalkmanSync
WalkmanSync will immediately detect your player and show its name and available storage space.

### 3. First-Time Setup: Click "🔑 Extract"
If this is the first time you are using this Walkman on your Mac (or if you recently erased it):
- You will see a small button that says **`🔑 Extract`**.
- Click it! Your Mac will ask for your password or Touch ID.
- *Why does it ask?* Sony built a private cryptographic key into the Walkman's internal chip. Giving permission lets WalkmanSync fetch this key directly from the chip once and save it permanently on your Mac. You only ever need to do this once!
- The button will turn into a green **`🔑 Key ✓`**.

### 4. Pick Your Music Folder
Click the **"Select"** button next to **Music Source** and pick the folder on your Mac that contains your music files (MP3s, FLAC, M4A, etc.).

### 5. Click "Sync to Walkman"
Click the big **"Sync to Walkman"** button at the bottom! WalkmanSync will copy your songs, build the Walkman's database, and scramble the audio with your player's real key.

When it says **"Sync Complete"**, you're done! Unplug your Walkman, plug in your headphones, and enjoy your music!

---

## 🎧 Supported Sony Walkman Models

WalkmanSync is engineered specifically for Sony's 3rd-generation audio players:

| Model Series | Common Devices | How Music is Stored |
|---|---|---|
| **NW-E400 Series** | **NW-E403** (256 MB), **NW-E405** (512 MB), **NW-E407** (1 GB) | Built-in Flash Memory |
| **NW-E500 Series** | **NW-E503**, **NW-E505**, **NW-E507** (FM Radio models) | Built-in Flash Memory |
| **NW-HD Series** | **NW-HD1**, **NW-HD3**, **NW-HD5** | Internal Mini Hard Drive |
| **NW-A Series** | **NW-A608**, **NW-A1000**, **NW-A3000** | Flash / Hard Drive |

---

## ✨ Cool Things WalkmanSync Does Automatically

- **No More "CANNOT PLAY" Errors**: Previous tools failed on real Walkman hardware because they used fake placeholder keys. WalkmanSync reads your player's authentic factory key directly from the hardware chip so your songs play with 100% crystal-clear sound.
- **Works With Your Music**: Supports regular **MP3**, plus **FLAC**, **M4A (Apple Music/iTunes files)**, and **WAV**.
- **Live Song Capacity Estimator**: As you switch audio quality settings, WalkmanSync tells you in plain English approximately how many songs will fit on your player (e.g. *~212 songs in high quality*).
- **Self-Healing Backup**: If you ever format or erase your Walkman, WalkmanSync remembers your player's unique key and automatically restores it the next time you plug it in!
- **Automatic Update Alerts**: WalkmanSync checks GitHub quietly in the background and shows a little badge when an update is available so you can update in one click.

---

## ❓ Frequently Asked Questions (FAQ)

#### *Q: Why does my Mac ask for my administrator password when I click "Extract"?*
**A:** macOS is designed to protect your USB devices from unauthorized software. Because the Walkman stores its playback key deep inside its hardware silicon chip, macOS requires your permission once to let WalkmanSync temporarily communicate with the chip and retrieve your key. Once extracted, it is cached permanently on your Mac so you don't have to enter your password again.

#### *Q: Do I need to install any extra software to sync MP3 files?*
**A:** **No!** If you have normal `.mp3` files, WalkmanSync has **100% zero external dependencies**. It works completely out of the box.

#### *Q: What if I have FLAC or Apple M4A music files?*
**A:** To convert lossless FLAC or M4A files on the fly, WalkmanSync can use the standard free Mac audio tool `ffmpeg`. You can install it in 10 seconds using Homebrew by typing `brew install ffmpeg` in Terminal, or simply click the **"🩺 Doctor..."** button inside WalkmanSync to check if your Mac is ready!

#### *Q: How do I check if my Walkman is connected properly?*
**A:** Click the **"🩺 Doctor..."** button at the bottom of the WalkmanSync window anytime. It will show you a friendly checklist verifying your Walkman connection, storage space, and audio tools.

#### *Q: Can I manage multiple different Walkmans?*
**A:** **Yes!** If you own more than one Walkman (for example, an NW-E405 and an NW-HD5), WalkmanSync automatically remembers each device's key independently so you can switch between them seamlessly.

---

<details>
<summary><b>🛠️ For Developers & Terminal Users (Click to Expand)</b></summary>

### Command-Line Interface (CLI)

WalkmanSync includes a full-featured CLI for terminal users and automation scripts:

```bash
# Check system health, dependencies, and hardware key
walkmansync --doctor

# Detect connected Walkmans and view live capacity estimates
walkmansync --detect

# Extract authentic hardware key from connected Walkman
walkmansync --extract-key

# Check GitHub for newer releases
walkmansync --check-update

# Scan a local folder to inspect track metadata
walkmansync --scan ~/Music/MyAlbum

# Sync music to Walkman with ATRAC3 LP2 (Default)
walkmansync --sync --source ~/Music/MyAlbum

# Sync music with MP3 320k CBR encoding
walkmansync --sync --source ~/Music/MyAlbum --codec mp3

# Perform a dry-run test without writing to flash
walkmansync --sync --source ~/Music/MyAlbum --dry-run

# Output machine-readable JSON for scripts and agents
walkmansync --detect --json
walkmansync --doctor --json
```

### Building From Source

Requires macOS 12.0+ with Xcode Command Line Tools installed:

```bash
# Clone the repository
git clone https://github.com/arsenyspb/walkmansync-for-mac.git
cd walkmansync-for-mac

# Run the 8-suite test suite
make test

# Build WalkmanSync.app and create the DMG installer
make dmg

# Launch the app
make run
```

### Technical Documentation & Architecture
- **[Reverse-Engineering Sony's CopyTool.exe & Hardware Key Extraction Saga](CopyTool.exe.DvID.dat.md)**: Disassembly analysis, proprietary SCSI CDB sequences (`A4 00 ... BC ... 3F`), and the native `IOUSBHost.framework` `DeviceCapture` kernel bypass.
- **[AI Agent Development & Architecture Instructions](AI.md)**: Developer documentation and protocol specifications for coding agents.

</details>

---

## License

This project is open-source software licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.

*Sony, Walkman, ATRAC, ATRAC3, ATRAC3plus, SonicStage, and OpenMG are trademarks or registered trademarks of Sony Corporation. This project is an independent, clean-room open-source initiative created for hardware preservation and retro-computing compatibility.*
