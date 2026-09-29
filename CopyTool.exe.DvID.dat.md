# Reverse-Engineering Sony's `CopyTool.exe`, `FrankPACAPI.dll` & Hardware Key Extraction Saga

This document details the reverse-engineering analysis of Sony's official software suite (`MP3FMV2_ENG.EXE`, `CopyTool.exe`, `MP3FileManager.exe`, and `FrankPACAPI.dll`), uncovering the proprietary SCSI/USB command sequences used to retrieve the authentic hardware encryption key from Sony Network Walkman devices (NW-E400, NW-E500, and NW-HD series).

---

## 1. Executive Summary & Hardware Playback Breakthrough

Sony 3rd-generation Network Walkman devices (NW-E400/E500 series) natively decode MP3 and ATRAC3 audio. However, a major hurdle for modern alternative software has been the dreaded **"CANNOT PLAY"** (or **"MG ERROR"**) message, even when all OMGAUDIO database tables are parsed successfully by the player's firmware and track titles render on the OLED display.

### The Root Cause: Hardware-Bound XOR Scrambling
Audio frames on the device must be XOR-scrambled with a 4-byte key derived from the track ID and a player-unique device key:

$$\text{track\_key} = ((0\text{x}2465 + \text{track\_id} \times 0\text{x}5296\text{E}435) \ \& \ 0\text{xFFFFFFFF}) \oplus \mathbf{K_{\text{hardware}}}$$

**Crucial Finding:** The physical player’s DSP chip **never reads `DvID.dat` from flash storage during playback**. The DSP chip unscrambles audio in hardware using its **own burned-in factory key inside ROM/EEPROM**.

`/Volumes/WALKMAN/MP3FM/DvID.DAT` is strictly an *interchange file* created by Sony's PC utilities so that transfer programs know the correct hardware key to scramble with. If files are scrambled with a placeholder key (e.g. `0x08DA6D03` from open-source forum dumps), the hardware DSP fails to unscramble valid MPEG frame sync headers (`0xFF 0xFB`) and aborts playback with **"CANNOT PLAY"**.

Once the genuine hardware key (in this unit's case, **`0x08FF8139`**) was extracted and used for scrambling, **physical audio playback on the Walkman succeeded with 100% audio fidelity**.

---

## 2. Decompilation Analysis of Sony Software Suite

Unpacking Sony's official self-extracting installer (`MP3FMV2_ENG.EXE`) via InstallShield cabinet extraction revealed:
- `TARGETDIRFiles/CopyTool.exe` (237 KB) — Setup initialization tool
- `TARGETDIRFiles/MP3FileManager.exe` (495 KB) — Main transfer utility
- `TARGETDIRFiles/MTbl.dll` (40 KB) — OMGAUDIO database table builder
- `DLL_Files/FrankPACAPI.dll` (913 KB) — Core Personal Audio Client (PAC) COM engine
- `DLL_Files/PACAPIps.dll` (36 KB) — PAC API proxy/stub

---

## 3. Disassembly of `CopyTool.exe` (The Device ID Query)

In `CopyTool.exe` at virtual address `0x4022ee`–`0x402330`:

```assembly
0x4022ee: xor     edi, edi
0x4022f0: push    0xa4            ; CDB[0] = Opcode 0xA4 (Sony Vendor SCSI)
0x4022f5: lea     ecx, [esp + 0x14]
0x4022fd: call    0x407400
0x402302: push    edi             ; CDB[2..5] = 0x00000000
0x402307: call    0x407410
0x40230c: push    0xbc            ; CDB[7] = 0xBC (Subcommand identifier)
0x402315: call    0x407430
0x40231a: push    0x12            ; CDB[8..9] = 0x0012 (Allocation Length: 18 bytes)
0x402320: call    0x407440        ; Byte-swaps to big-endian
0x402325: push    0x3f            ; CDB[10] = 0x3F (Sub-page: Device ID)
0x40232b: call    0x407460
```

### The 12-Byte SCSI Command Block (CDB):
```text
Byte 00:      0xA4  (Vendor-Specific SCSI Opcode)
Bytes 01-06:  0x00 00 00 00 00 00
Byte 07:      0xBC  (Subcommand ID)
Bytes 08-09:  0x00 0x12 (Allocation Length: 18 bytes)
Byte 10:      0x3F  (Sub-page: Device ID)
Byte 11:      0x00  (Control byte)
```

### Device Response Format (18 Bytes):
The device firmware responds with 18 bytes:
- **Bytes 0–1 (`0x00 0x10`)**: 2-byte big-endian wrapper length (`0x0010` = 16 bytes).
- **Bytes 2–17 (16 bytes)**: The actual Device ID written directly to `DvID.DAT`.

```
Offset  Size  Value                 Description
------  ----  -----                 -----------
0x00    2     03 01                 Magic Header (Version 3.1)
0x02    2     01 00                 Sub-type
0x04    2     00 00                 Flags
0x06    2     01 28                 Model & Manufacturing Segment
0x08    2     00 00                 Reserved
0x0A    4     08 FF 81 39           Hardware DSP Encryption Key (Big-Endian uint32)
0x0E    2     00 00                 Trailing Padding
```

---

## 4. Disassembly of `FrankPACAPI.dll` (The 3rd-Gen MagicGate Engine)

Further reverse-engineering of Sony's core library `FrankPACAPI.dll` at `0x6c131f0c` and `0x6c15d4ce` uncovered the full MagicGate / Leaf ID authentication protocol for NW-E400/E500/AAD2 hardware:

```assembly
0x6c15d4ce: push    0xa4            ; Opcode 0xA4
0x6c15d4e8: push    0xbc            ; Subcommand 0xBC
0x6c15d4f5: mov     edi, 0x404      ; Allocation Length: 0x0404 (1,028 bytes)
0x6c15d503: push    0x33            ; Sub-page: 0x33 (MagicGate Leaf ID / Certs)
```

When queried with sub-page **`0x33`** and length **`0x0404`**, the Walkman returns a 1,028-byte hardware payload:
- **Bytes 0–3 (`04 02 00 00`)**: 1,026-byte certificate wrapper.
- **Bytes 4–11 (`00 30 00 98 00 00 00 48`)**: Leaf descriptor block.
- **Bytes 12–27 (`80 00 00 00 00 01 00 21 79 FC 2F 79 5A 57 35 55`)**: Hardware Leaf ID signature.

---

## 5. Why macOS Blocks Direct SCSI Pass-Through

On Windows, `CopyTool.exe` simply calls:
```c
HANDLE hDrive = CreateFileA("\\\\.\\E:", GENERIC_READ | GENERIC_WRITE, ...);
DeviceIoControl(hDrive, IOCTL_SCSI_PASS_THROUGH, &sptdwb, ...);
```
On Linux, user-space code issues `ioctl(fd, SG_IO, &hdr)` on `/dev/sdb`.

On macOS, Apple implements a strict security boundary:
1. **In-Kernel Storage Claim**: When the Walkman is connected, `IOUSBMassStorageDriver` binds exclusively to the USB Mass Storage interface (`Class 8, SubClass 5/6, Protocol 0x50`).
2. **Blocked SCSI Pass-Through**: macOS provides `SCSITaskLib` (`kIOSCSITaskDeviceUserClientTypeID`), but Apple's kernel implementation (`IOSCSIPeripheralDeviceType00`) explicitly restricts user-space pass-through to **authoring devices** (optical CD-R/W and DVD-R/W drives). Attempting to open an `IOSCSILogicalUnitNub` for a USB disk returns **`0xE00002C7` (`kIOReturnUnsupported`)**.
3. **Interface Locking**: Attempting to claim the USB interface from user space returns **`0xE00002C5` (`kIOReturnExclusiveAccess`)**.

---

## 6. The Native macOS Solution: USB Bulk-Only Transport (BOT)

To send the SCSI vendor command without a custom kernel extension or DriverKit DEXT, we implement a low-level **USB Bulk-Only Transport (BOT)** routine:

1. **Unmount Filesystem**: `diskutil unmountDisk force /dev/diskN` ensures no active filesystem locks.
2. **Seize USB Device**: Using elevated privileges (`sudo` / admin authorization), invoke `IOUSBDeviceInterface::USBDeviceOpenSeize()` to seize the `IOUSBHostDevice`.
3. **Seize Mass Storage Interface**: Invoke `IOUSBInterfaceInterface::USBInterfaceOpenSeize()` on Interface 0.
4. **Locate Endpoints**: Identify Bulk-OUT (Pipe 2) and Bulk-IN (Pipe 1).
5. **Send 31-Byte Command Block Wrapper (CBW)**:
   ```text
   Bytes 0-3:   0x55 0x53 0x42 0x43  ; Magic "USBC"
   Bytes 4-7:   0x53 0x4F 0x4E 0x59  ; Tag "SONY"
   Bytes 8-11:  0x12 0x00 0x00 0x00  ; Transfer Length = 18
   Byte 12:     0x80                 ; Direction: Data-IN
   Byte 13:     0x00                 ; LUN 0
   Byte 14:     0x0C                 ; CDB Length = 12
   Bytes 15-26: A4 00 00 00 00 00 00 BC 00 12 3F 00 ; Sony Vendor SCSI CDB
   Bytes 27-30: 00 00 00 00          ; Padding
   ```
6. **Read 18-Byte Data-IN**: Captures the raw 18-byte payload containing the genuine 16-byte Device ID.
7. **Read 13-Byte Command Status Wrapper (CSW)**: Verifies `bCSWStatus == 0`.
8. **Release & Remount**: Closes pipes, releases interface, remounts `/Volumes/WALKMAN`.

---

## 7. Zero-Friction User Experience in `WalkmanSync`

To ensure users never have to deal with complex extraction steps repeatedly:

1. **Automatic Persistence & Self-Healing**:
   When an authentic `DvID.DAT` is first detected or extracted, `WalkmanKeyManager` automatically caches it in macOS user storage:
   ```text
   ~/Library/Application Support/WalkmanSync/DvID.DAT
   ```
   If the user ever formats their Walkman via disk utility or player menu, `WalkmanSync` detects that `DvID.DAT` is missing and **automatically restores the authentic key instantly** with zero prompts.

2. **UI & CLI Integration**:
   - **CLI**: `walkmansync --detect` displays the resolved hardware key alongside storage statistics and capacity estimates.
   - **GUI**: Live storage bar displays the hardware key (`Key: 0x08FF8139`) and dynamically recalculates estimated song capacity across all codecs.
