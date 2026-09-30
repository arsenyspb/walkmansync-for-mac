import Foundation
import IOUSBHost
import IOKit

/// Handles device key retrieval, generation, and XOR scrambling for Sony Network Walkman 3rd Generation (NW-E40x, NW-HDx, etc.)
public enum WalkmanKeyManager {
    
    /// Default factory device identity key used when no existing key file is present.
    /// NOTE: This is a well-known placeholder key that will cause "CANNOT PLAY" on authentic hardware.
    public static let defaultDeviceKey: UInt32 = 0x08DA6D03
    
    /// Possible locations for DvID.dat / DvID.DAT on the device volume
    public static let potentialKeyPaths = [
        "MP3FM/DvID.DAT",
        "MP3FM/DvID.dat",
        "mp3fm/DvID.DAT",
        "mp3fm/DvID.dat",
        "OMGAUDIO/DvID.DAT",
        "OMGAUDIO/DvID.dat",
        "omgaudio/DvID.DAT",
        "omgaudio/DvID.dat",
        "DvID.DAT",
        "DvID.dat"
    ]
    
    /// App Support directory for caching device keys
    public static var appSupportDirectory: URL {
        let appSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let dir = appSupport.appendingPathComponent("WalkmanSync", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true, attributes: nil)
        return dir
    }
    
    /// App Support backup URL for the default/most recent device key
    public static var appSupportBackupURL: URL {
        return appSupportDirectory.appendingPathComponent("DvID.DAT")
    }
    
    /// Checks whether a key is an authentic hardware key (not zero and not the dummy placeholder)
    public static func isKeyAuthentic(key: UInt32) -> Bool {
        return key != 0 && key != defaultDeviceKey
    }

    /// Discovers existing device key or creates and saves a standard DvID.DAT file to MP3FM/DvID.DAT
    public static func resolveOrCreateDeviceKey(deviceURL: URL) -> UInt32 {
        let fm = FileManager.default
        
        for relativePath in potentialKeyPaths {
            let fileURL = deviceURL.appendingPathComponent(relativePath)
            if fm.fileExists(atPath: fileURL.path) {
                if let key = readDeviceKey(from: fileURL) {
                    WalkmanLogger.info("Found existing DvID key at \(relativePath): 0x\(String(format: "%08X", key))")
                    // Backup key to Application Support for future recovery
                    if let rawData = try? Data(contentsOf: fileURL) {
                        saveKeyToCache(payload: rawData, key: key)
                    }
                    return key
                }
            }
        }
        
        // Check if we have a backed-up hardware key from previous extractions
        if fm.fileExists(atPath: appSupportBackupURL.path),
           let rawData = try? Data(contentsOf: appSupportBackupURL),
           let key = readDeviceKey(from: appSupportBackupURL) {
            let mp3fmDir = deviceURL.appendingPathComponent("MP3FM", isDirectory: true)
            try? fm.createDirectory(at: mp3fmDir, withIntermediateDirectories: true, attributes: nil)
            let dvidURL = mp3fmDir.appendingPathComponent("DvID.DAT")
            try? rawData.write(to: dvidURL)
            WalkmanLogger.info("Restored authentic hardware DvID.DAT from Application Support backup: 0x\(String(format: "%08X", key))")
            return key
        }
        
        // No key exists; create standard MP3FM/DvID.DAT with placeholder
        let mp3fmDir = deviceURL.appendingPathComponent("MP3FM", isDirectory: true)
        try? fm.createDirectory(at: mp3fmDir, withIntermediateDirectories: true, attributes: nil)
        let dvidURL = mp3fmDir.appendingPathComponent("DvID.DAT")
        
        let dvidData = generateDvidData(key: defaultDeviceKey)
        try? dvidData.write(to: dvidURL)
        WalkmanLogger.warn("Generated placeholder 16-byte DvID.DAT at MP3FM/DvID.DAT with key 0x\(String(format: "%08X", defaultDeviceKey)). Run extraction for authentic key.")
        return defaultDeviceKey
    }
    
    /// Reads 4-byte big-endian key at offset 0x0A (10) from DvID.DAT
    public static func readDeviceKey(from url: URL) -> UInt32? {
        guard let data = try? Data(contentsOf: url), data.count >= 14 else {
            return nil
        }
        let b0 = UInt32(data[10])
        let b1 = UInt32(data[11])
        let b2 = UInt32(data[12])
        let b3 = UInt32(data[13])
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
    }
    
    /// Saves authentic DvID payload and key to Application Support cache
    public static func saveKeyToCache(payload: Data, key: UInt32, cacheDirectory: URL? = nil) {
        let dir = cacheDirectory ?? appSupportDirectory
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true, attributes: nil)
        
        let primaryURL = dir.appendingPathComponent("DvID.DAT")
        try? payload.write(to: primaryURL)
        
        // Also save per-device keyed file so multiple walkmans don't overwrite each other
        let keyHex = String(format: "%08X", key)
        let perDeviceURL = dir.appendingPathComponent("DvID_\(keyHex).DAT")
        try? payload.write(to: perDeviceURL)
    }
    
    /// Generates standard 16-byte DvID.DAT binary payload
    public static func generateDvidData(key: UInt32) -> Data {
        var data = Data(count: 16)
        // Standard header observed across Walkman units: 03 01 01 00 00 00 01 28 00 00
        data[0] = 0x03
        data[1] = 0x01
        data[2] = 0x01
        data[3] = 0x00
        data[4] = 0x00
        data[5] = 0x00
        data[6] = 0x01
        data[7] = 0x28
        data[8] = 0x00
        data[9] = 0x00
        // Key at 0x0A..0x0D (big-endian)
        data[10] = UInt8((key >> 24) & 0xFF)
        data[11] = UInt8((key >> 16) & 0xFF)
        data[12] = UInt8((key >> 8) & 0xFF)
        data[13] = UInt8(key & 0xFF)
        // Trailing 2 bytes: 00 00
        data[14] = 0x00
        data[15] = 0x00
        return data
    }
    
    /// Computes the 4-byte track XOR key as documented in the community MP3FM specification (xaskasdf):
    /// key = ((0x2465 + trackId * 0x5296E435) & 0xFFFFFFFF) ^ deviceKey
    public static func computeXorKey(trackId: Int, deviceKey: UInt32) -> [UInt8] {
        let titleId = UInt64(trackId)
        let formula: UInt64 = (0x2465 + titleId * 0x5296E435) & 0xFFFFFFFF
        let omaKey = UInt32(formula) ^ deviceKey
        
        return [
            UInt8((omaKey >> 24) & 0xFF),
            UInt8((omaKey >> 16) & 0xFF),
            UInt8((omaKey >> 8) & 0xFF),
            UInt8(omaKey & 0xFF)
        ]
    }
    
    /// Scrambles audio data in-place using repeating 4-byte key (processed in 8-byte blocks + tail)
    public static func xorScramble(data: inout Data, keyBytes: [UInt8]) {
        let key8 = keyBytes + keyBytes
        let count = data.count
        let blockCount = count / 8
        let remainder = count % 8
        
        data.withUnsafeMutableBytes { (ptr: UnsafeMutableRawBufferPointer) in
            guard let baseAddress = ptr.baseAddress?.assumingMemoryBound(to: UInt8.self) else { return }
            for b in 0..<blockCount {
                let offset = b * 8
                for j in 0..<8 {
                    baseAddress[offset + j] ^= key8[j]
                }
            }
            if remainder > 0 {
                let offset = blockCount * 8
                for j in 0..<remainder {
                    baseAddress[offset + j] ^= key8[j]
                }
            }
        }
    }
    
    // MARK: - Native USB Hardware Key Extraction
    
    // 31-byte USB Bulk-Only Transport (BOT) Command Block Wrapper
    private struct USB_CBW {
        var dCBWSignature: UInt32 = 0x43425355 // "USBC"
        var dCBWTag: UInt32 = 0x12345678
        var dCBWDataTransferLength: UInt32 = 18
        var bmCBWFlags: UInt8 = 0x80 // IN (Device-to-Host)
        var bCBWLUN: UInt8 = 0
        var bCBWCBLength: UInt8 = 12
        var CBWCB: (UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8, UInt8) =
            (0xA4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xBC, 0x00, 0x12, 0x3F, 0x00, 0x00, 0x00, 0x00, 0x00)
    }

    private struct USB_CSW {
        var dCSWSignature: UInt32 = 0
        var dCSWTag: UInt32 = 0
        var dCSWDataResidue: UInt32 = 0
        var bCSWStatus: UInt8 = 0
    }
    
    public enum ExtractionError: LocalizedError {
        case deviceNotFound
        case rootRequired
        case interfaceNotFound
        case transferFailed(String)
        case scriptCancelled
        case scriptFailed(String)
        
        public var errorDescription: String? {
            switch self {
            case .deviceNotFound:
                return "Sony Network Walkman USB device was not detected. Please ensure the player is connected via USB and displays 'USB CONNECT'."
            case .rootRequired:
                return "Hardware key extraction requires administrator privileges to temporarily seize the USB interface."
            case .interfaceNotFound:
                return "Could not acquire the Walkman's USB Mass Storage interface."
            case .transferFailed(let reason):
                return "USB data transfer failed: \(reason)"
            case .scriptCancelled:
                return "Administrator authorization was cancelled."
            case .scriptFailed(let reason):
                return "Key extraction helper failed: \(reason)"
            }
        }
    }
    
    /// Directly extracts the hardware key from the Walkman hardware ASIC over USB Bulk-Only Transport.
    /// NOTE: Must be executed with root privileges (UID 0) to allow IOUSBHostObjectInitOptionsDeviceCapture.
    public static func extractHardwareKeyDirectly() throws -> (key: UInt32, payload: Data) {
        guard geteuid() == 0 else {
            throw ExtractionError.rootRequired
        }
        
        let dict = IOServiceMatching("IOUSBHostDevice")!
        var iter: io_iterator_t = 0
        guard IOServiceGetMatchingServices(kIOMainPortDefault, dict, &iter) == kIOReturnSuccess else {
            throw ExtractionError.deviceNotFound
        }
        defer { IOObjectRelease(iter) }
        
        while case let service = IOIteratorNext(iter), service != 0 {
            defer { IOObjectRelease(service) }
            
            guard let vidProp = IORegistryEntryCreateCFProperty(service, "idVendor" as CFString, kCFAllocatorDefault, 0)?.takeRetainedValue() as? NSNumber,
                  vidProp.intValue == 0x054C else {
                continue
            }
            
            // Capture device (terminates in-kernel mass storage driver cleanly)
            let device = try IOUSBHostDevice(__ioService: service, options: .deviceCapture, queue: nil, interestHandler: nil)
            defer { device.destroy() }
            
            var childIter: io_iterator_t = 0
            guard IORegistryEntryGetChildIterator(service, kIOServicePlane, &childIter) == kIOReturnSuccess else {
                continue
            }
            defer { IOObjectRelease(childIter) }
            
            while case let intfService = IOIteratorNext(childIter), intfService != 0 {
                defer { IOObjectRelease(intfService) }
                
                var className = [CChar](repeating: 0, count: 128)
                IOObjectGetClass(intfService, &className)
                guard String(cString: className) == "IOUSBHostInterface" else { continue }
                
                let iface = try IOUSBHostInterface(__ioService: intfService, options: [], queue: nil, interestHandler: nil)
                defer { iface.destroy() }
                
                guard let outPipe = try? iface.copyPipe(withAddress: 0x02),
                      let inPipe = try? iface.copyPipe(withAddress: 0x81) else {
                    continue
                }
                
                var cbw = USB_CBW()
                let cbwData = try iface.ioData(withCapacity: MemoryLayout<USB_CBW>.size)
                withUnsafeBytes(of: &cbw) { rawBuf in
                    cbwData.replaceBytes(in: NSRange(location: 0, length: MemoryLayout<USB_CBW>.size), withBytes: rawBuf.baseAddress!)
                }
                
                var xfer: Int = 0
                try outPipe.__sendIORequest(with: cbwData, bytesTransferred: &xfer, completionTimeout: 5.0)
                
                let respData = try iface.ioData(withCapacity: 18)
                try inPipe.__sendIORequest(with: respData, bytesTransferred: &xfer, completionTimeout: 5.0)
                
                let cswData = try iface.ioData(withCapacity: MemoryLayout<USB_CSW>.size)
                _ = try? inPipe.__sendIORequest(with: cswData, bytesTransferred: &xfer, completionTimeout: 5.0)
                
                let rawBytes = [UInt8](respData as Data)
                if rawBytes.count >= 18 {
                    // Extract payload: bytes 2..17
                    let payload = Data(rawBytes[2..<18])
                    let k0 = UInt32(rawBytes[12])
                    let k1 = UInt32(rawBytes[13])
                    let k2 = UInt32(rawBytes[14])
                    let k3 = UInt32(rawBytes[15])
                    let key = (k0 << 24) | (k1 << 16) | (k2 << 8) | k3
                    return (key, payload)
                }
            }
        }
        
        throw ExtractionError.deviceNotFound
    }
    
    /// Requests one-time administrator authorization to extract the authentic hardware key,
    /// writes it to the Walkman drive (`MP3FM/DvID.DAT`), and permanently caches it to Application Support.
    public static func extractHardwareKeyWithElevation(targetVolume: URL?, cacheDirectory: URL? = nil) throws -> UInt32 {
        let destCacheDir = cacheDirectory ?? appSupportDirectory
        
        // If already running as root, execute directly
        if geteuid() == 0 {
            let result = try extractHardwareKeyDirectly()
            saveKeyToCache(payload: result.payload, key: result.key, cacheDirectory: destCacheDir)
            if let vol = targetVolume {
                let mp3fmDir = vol.appendingPathComponent("MP3FM", isDirectory: true)
                try? FileManager.default.createDirectory(at: mp3fmDir, withIntermediateDirectories: true, attributes: nil)
                let dvidURL = mp3fmDir.appendingPathComponent("DvID.DAT")
                try? result.payload.write(to: dvidURL)
                WalkmanLogger.info("Saved authentic DvID.DAT to Walkman at \(dvidURL.path)")
            }
            return result.key
        }
        
        // Escalate via macOS standard administrator prompt (do shell script with administrator privileges)
        let binaryPath = Bundle.main.executablePath ?? CommandLine.arguments[0]
        var args = ["'\(binaryPath)'", "--extract-key-internal"]
        if let vol = targetVolume {
            args.append("--walkman '\(vol.path)'")
        }
        args.append("--cache-dir '\(destCacheDir.path)'")
        let fullCommand = args.joined(separator: " ")
        
        let scriptSource = "do shell script \"\(fullCommand)\" with administrator privileges"
        var errorDict: NSDictionary?
        guard let script = NSAppleScript(source: scriptSource) else {
            throw ExtractionError.scriptFailed("Failed to create AppleScript authorization helper.")
        }
        
        let outputDescriptor = script.executeAndReturnError(&errorDict)
        if let err = errorDict {
            let errorNumber = err[NSAppleScript.errorNumber] as? Int ?? 0
            if errorNumber == -128 { // User cancelled the authentication dialog
                throw ExtractionError.scriptCancelled
            }
            let errorMsg = err[NSAppleScript.errorMessage] as? String ?? "Unknown AppleScript error"
            throw ExtractionError.scriptFailed(errorMsg)
        }
        
        let outputString = outputDescriptor.stringValue ?? ""
        WalkmanLogger.info("Elevation helper completed: \(outputString)")
        
        // Parse the key returned from the helper
        // Output format: KEY:0x08FF8139
        for line in outputString.components(separatedBy: "\n") {
            let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.starts(with: "KEY:") {
                let hexStr = trimmed.replacingOccurrences(of: "KEY:", with: "").trimmingCharacters(in: .whitespaces)
                let cleanHex = hexStr.replacingOccurrences(of: "0x", with: "").replacingOccurrences(of: "0X", with: "")
                if let parsedKey = UInt32(cleanHex, radix: 16) {
                    return parsedKey
                }
            }
        }
        
        // Fallback: Check if DvID.DAT was written to the cache directory or volume
        if let key = readDeviceKey(from: destCacheDir.appendingPathComponent("DvID.DAT")) {
            return key
        }
        if let vol = targetVolume, let key = readDeviceKey(from: vol.appendingPathComponent("MP3FM/DvID.DAT")) {
            return key
        }
        
        throw ExtractionError.scriptFailed("Key was extracted but could not be parsed from helper output.")
    }
}
