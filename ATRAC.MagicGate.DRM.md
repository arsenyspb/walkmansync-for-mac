# The ATRAC & MagicGate DRM Saga: Why 3rd-Gen Sony Walkmans Play MP3 via ASIC XOR but Lock Down ATRAC

## Executive Summary

Sony's Network Walkman series (NW-E400, NW-E500, NW-A, and NW-HD series) represents an intriguing inflection point in digital audio history. Built in 2005, these devices span two completely different technological and legal philosophies:

1. **Sony's Closed Past (1999–2004)**: The proprietary **ATRAC3 / ATRAC3plus** format, bound by Sony Music Entertainment's strict **MagicGate LSI DRM** (OpenMG).
2. **Sony's Open Future (2005 onwards)**: Native **MPEG-1 Layer 3 (MP3)** playback, enabled via a lightweight hardware XOR stream cipher (`0xFFFE`) bound to a factory device key (`DvID.DAT`) to compete with the Apple iPod.

During the development of **WalkmanSync for Mac**, we reverse-engineered the device's hardware DSP behavior when feeding it both modern open-source ATRAC streams (via `atracdenc`) and MP3 streams. 

This document chronicles our findings, explains the exact mechanisms behind the notorious **"MG Error"** and **"CANNOT PLAY"** failures, and explains why native MP3 playback (from 32 kbps to 320 kbps CBR and VBR) is the authentic, zero-friction path for preserving these legendary players today.

---

## 1. The Two Halves of Sony's 2005 DSP Silicon

Inside the Sony NW-E400/E500 ASIC, the hardware audio decoder is divided into two distinct processing pipelines:

```text
                               +---------------------------------------------+
                               |        Sony Network Walkman ASIC ROM        |
                               +---------------------------------------------+
                                                      |
                 +------------------------------------+------------------------------------+
                 |                                                                         |
                 v                                                                         v
      [ ATRAC Decoder Block ]                                                   [ MP3 Decoder Block ]
                 |                                                                         |
   Format ID: 0x00 / 0x01                                                    Format ID: 0x03
   DRM Requirement: MagicGate LSI DRM                                        DRM Requirement: 3rd-Gen XOR Stream Cipher
   Protection Flag: 0x0001                                                   Protection Flag: 0xFFFE
   Key Mechanism:                                                            Key Mechanism:
     - 30GRCT/ Key Directory                                                   - Hardware ASIC ROM Key (DvID.DAT)
     - 0001001D.DAT & 00010021.DAT                                             - 8-Byte Repeating XOR Block
     - SRCIDLST.DAT Certificate Blob                                           - Track Key = ((0x2465 + ID * 0x5296E435) & 0xFFFFFFFF) ^ K_hw
     - Table 5 (05CIDLST) 48-byte DES Keys                                     - Zero external certificate files
                 |                                                                         |
                 v                                                                         v
   Playback Status on Open-Source:                                           Playback Status on WalkmanSync:
   ❌ "MG Error" (if marked 0xFFFF)                                          ✅ 100% Crystal-Clear Audio Playback!
   ❌ "CANNOT PLAY" (if marked 0xFFFE)                                       (32 kbps – 320 kbps CBR & VBR)
```

---

## 2. Reverse-Engineering the Failures: "MG Error" vs "CANNOT PLAY"

When open-source utilities like `atracdenc` or `ffmpeg` generate an ATRAC3 or ATRAC3plus bitstream and write it to the Walkman's `OMGAUDIO/10Fxx/` directory, the physical player behaves in one of two ways depending on container headers:

### Scenario A: Marking ATRAC as Unprotected (`0xFFFF`) -> "MG Error"
When `atracdenc` creates an `.OMA` container, it sets bytes 6–7 of the 96-byte EA3 audio header to `0xFF 0xFF` (unprotected/no encryption). It also writes `0xFF 0xFF` into the track's entry in `04CNTINF.DAT`.

* **What the firmware does**: The Walkman's bootloader and filesystem scanner parse the track and recognize the codec as ATRAC3 (`Format 0x00`).
* **The check**: The firmware checks whether the track has valid MagicGate credentials. Seeing `0xFFFF` (plaintext, no protection), the firmware's digital rights management layer immediately aborts.
* **Device Screen**: Displays **`MG Error`** (MagicGate Error) the instant the track is selected.

### Scenario B: Marking ATRAC with the MP3 XOR Flag (`0xFFFE`) -> "CANNOT PLAY"
In our experiments, we modified the EA3 audio header and `04CNTINF.DAT` to mark the ATRAC track with `0xFFFE` (the 3rd-generation protection flag) and scrambled the raw ATRAC frames using the player's authentic factory key (`0x08FF8139`).

* **What the firmware does**:
  1. The container-level MagicGate check inspects bytes 6–7: seeing `0xFFFE`, it accepts the file as authenticated. **The "MG Error" is eliminated!**
  2. The track title and artist appear on the OLED display and the transport starts.
  3. The firmware routes the audio stream into the hardware decoder.
  4. The audio router examines the format byte: `Format 0x00` (ATRAC3).
  5. The DSP rejects the stream because in Sony's silicon, the 8-byte XOR hardware descrambler is hardwired **exclusively** to the MPEG Layer 3 decoder (`Format 0x03`).
* **Device Screen**: The player advances 1 second in silence and displays **`CANNOT PLAY`**.

---

## 3. The Uncracked Fortress: MagicGate LSI DRM

Why couldn't open-source tools simply synthesize the MagicGate certificates for ATRAC?

When Sony's **SonicStage** (the official Windows software from 2002–2008) transferred ATRAC files to a Network Walkman, it performed a complex cryptographic mutual authentication handshake over SCSI/USB Mass Storage:

1. **Vendor SCSI Inquiries**: SonicStage probed the device controller using proprietary vendor commands (`0xA4 0x00 ... 0xBC 0x00 0x12 0x3F 0x00`).
2. **Session Key Exchange**: The PC software exchanged challenge-response tokens using Sony's private master certificates embedded deep within closed-source Windows DLLs (`FrankPACAPI.dll`, `PACAPIps.dll`, `MTbl.dll`).
3. **Certificate Generation**:
   - SonicStage created a hidden `OMGAUDIO/30GRCT/` directory.
   - It wrote cryptographic certificate files: `0001001D.DAT`, `00010021.DAT`, `SRCIDLST.DAT`, and `SRCIDLST.BAK`.
   - In Table 5 (`05CIDLST.DAT`), it generated a unique **48-byte DES cryptographic block** for every individual track.

Even **JSymphonic** (the open-source Java tool developed between 2007 and 2011) explicitly admitted defeat in its source code (`NWOmgaudio.java:490`):

```java
if (generation <= 3) {
    if (oma.getDrm() != Oma.DRM_LSI) {
        logger.warning("ATRAC files without LSI DRM are not supported.");
        titlesNotImported++;
        continue;
    }
    // Check that the LSI DRM key files are present (0001001D.DAT, 00010021.DAT, SRCIDLST.DAT, 30GRCT folder)
    if (!drmKeyFilesPresent) {
        logger.warning("No LSI DRM key files found. OMA files cannot be played.");
        titlesNotImported++;
        continue;
    }
}
```

JSymphonic could **only** transfer ATRAC tracks that had *already* been transferred to a Walkman by SonicStage on Windows, by preserving their existing `OMG_LSI` tags and pre-existing device certificate tables. It was completely unable to encode or inject new ATRAC files from scratch.

---

## 4. The 2005 Liberation: MP3 via `DvID.DAT` and `MP3FileManager`

By 2005, consumers were frustrated with SonicStage and digital rights management. Facing stiff competition from Apple's iPod Shuffle and drag-and-drop flash players, Sony made an uncharacteristic compromise:

They added **native MP3 support** to the NW-E400 and NW-E500 series through a standalone Windows tool called **`MP3FileManager.exe`**.

To prevent users from having to authenticate with Sony's central OpenMG servers, Sony engineers designed an entirely new, self-contained hardware security scheme:

1. **No External Certificates**: The device requires no `30GRCT/` directory, no `0001001D.DAT`, and Table 5 (`05CIDLST.DAT`) is filled with empty zeros.
2. **Silicon-Level Device Key**: Every Walkman has a 4-byte factory cryptographic key burned into its internal ASIC ROM/EEPROM (e.g. `0x08FF8139`).
3. **8-Byte XOR Stream Cipher**:
   $$\text{track\_key} = \Big(\big(0\text{x}2465 + \text{track\_id} \times 0\text{x}5296\text{E}435\big) \ \& \ 0\text{xFFFFFFFF}\Big) \oplus K_{\text{hardware}}$$
   This key is repeated to form an 8-byte XOR mask:
   $$\text{mask} = [\text{key}_0, \text{key}_1, \text{key}_2, \text{key}_3, \text{key}_0, \text{key}_1, \text{key}_2, \text{key}_3]$$
   The mask is applied in-place to the raw MPEG audio frames.
4. **Protection Marker `0xFFFE`**: Marked in the EA3 audio header and `04CNTINF.DAT`.

Because WalkmanSync natively extracts the authentic silicon key (`0x08FF8139`) directly from the player using our root USB BOT bypass (`IOUSBHost.framework` with `IOUSBHostObjectInitOptionsDeviceCapture`), **MP3 playback is 100% unlocked on native macOS without Windows, without virtual machines, and without SonicStage**.

---

## 5. Bitrate Freedom: Why MP3 Matches ATRAC3

A common misconception among Walkman enthusiasts is that ATRAC is required to fit a large music collection on a 512 MB or 1 GB device. 

While ATRAC3 LP2 (132 kbps) was efficient for its time, the NW-E400/E500's hardware MP3 decoder is **fully capable of decoding all standard bitrates from 32 kbps to 320 kbps, including Variable Bit Rate (VBR)**:

| Audio Format | Bitrate | Sound Quality | Est. Capacity (512 MB NW-E405) | Est. Capacity (1 GB NW-E407) |
|---|---|---|---|---|
| **MP3 320 kbps CBR** | 320 kbps | Studio / Max Fidelity | **~65 tracks** | ~135 tracks |
| **MP3 256 kbps CBR** | 256 kbps | Transparent / High End | **~85 tracks** | ~180 tracks |
| **MP3 192 kbps CBR** | 192 kbps | Near-CD (Recommended) | **~115 tracks** | ~240 tracks |
| **MP3 VBR (LAME -q:a 2)** | ~130–190 kbps | Adaptive Dynamics | **~120–150 tracks** | ~250–310 tracks |
| **MP3 128 kbps CBR** | 128 kbps | Equivalent to ATRAC3 LP2 | **~175 tracks** | ~360 tracks |
| **MP3 96 kbps CBR** | 96 kbps | Compact / Max Tracks | **~240 tracks** | ~500 tracks |

By allowing the user to select their desired MP3 bitrate (or use VBR), WalkmanSync achieves the exact storage density that users originally appreciated in ATRAC, while running on an authentic, hardware-verified playback pipeline that never throws **"MG Error"** or **"CANNOT PLAY"**.

---

## 6. Conclusion & Modern Architecture Decision

Our investigation into Sony's firmware confirms:
1. **ATRAC3 / ATRAC3plus on 3rd-Gen Network Walkmans requires MagicGate LSI DRM certificates** that cannot be legally or practically synthesized without proprietary Sony keys.
2. **Native MP3 playback via `0xFFFE` + ASIC XOR key extraction is 100% complete, fully documented, and verified on real hardware**.

WalkmanSync focuses on pure, zero-friction MP3 audio management:
* **Direct Pass-Through**: Existing MP3 files on your Mac are never degraded or re-encoded.
* **Smart Transcoding**: Lossless files (FLAC, Apple M4A, ALAC, WAV, AIFF) are converted on the fly using your choice of bitrate (96k to 320k or adaptive VBR).
* **Hardware-Accurate OMGAUDIO DB**: Complete 16-table database suite with UTF-16BE metadata and authentic XOR hardware scrambling.

*Sony, Walkman, ATRAC, ATRAC3, ATRAC3plus, SonicStage, OpenMG, and MagicGate are trademarks of Sony Corporation. This reverse-engineering analysis was conducted independently for hardware preservation and retro-computing interoperability.*
