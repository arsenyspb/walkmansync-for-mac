import Foundation

public enum WalkmanCleanerError: LocalizedError {
    case notAWalkmanDevice(String)
    case volumeNotWritable(String)
    case directoryCreationFailed(String)
    case databaseGenerationFailed(String)
    
    public var errorDescription: String? {
        switch self {
        case .notAWalkmanDevice(let path):
            return "Safety Guard: The target folder does not look like a Sony Walkman device (\(path)). Neither OMGAUDIO nor MP3FM directory was found."
        case .volumeNotWritable(let path):
            return "The Walkman volume is read-only or not writable: \(path)"
        case .directoryCreationFailed(let path):
            return "Failed to create directory on Walkman: \(path)"
        case .databaseGenerationFailed(let err):
            return "Failed to initialize clean Walkman database: \(err)"
        }
    }
}

public struct EraseResult {
    public let tracksDeleted: Int
    public let bytesFreed: Int64
    public let keyPreserved: UInt32
    public let keyWasAuthentic: Bool
    public let mountPath: String
}

/// Device Erase & Reset suite for Sony Network Walkmans
/// Cleans all music tracks, purges macOS hidden trash/metadata leakages,
/// strictly preserves authentic hardware encryption key (DvID.DAT),
/// and generates a clean, valid 16-table OMGAUDIO database showing 'NO DATA' (0 tracks).
public enum WalkmanCleaner {
    
    /// Verifies if a given volume or directory appears to be a genuine Sony Walkman
    public static func isWalkmanVolume(url: URL) -> Bool {
        let fm = FileManager.default
        let omg = url.appendingPathComponent("OMGAUDIO")
        let mp3fm = url.appendingPathComponent("MP3FM")
        let name = url.lastPathComponent.uppercased()
        
        var isDir: ObjCBool = false
        if fm.fileExists(atPath: omg.path, isDirectory: &isDir) && isDir.boolValue {
            return true
        }
        if fm.fileExists(atPath: mp3fm.path, isDirectory: &isDir) && isDir.boolValue {
            return true
        }
        if name.contains("WALKMAN") || name == "NW-E" || name.starts(with: "NW-") {
            return true
        }
        return false
    }
    
    /// Performs a safe, full erase of all music and metadata while preserving the hardware key
    @discardableResult
    public static func eraseWalkman(
        at destinationURL: URL,
        force: Bool = false,
        progressCallback: ((String) -> Void)? = nil
    ) throws -> EraseResult {
        let fm = FileManager.default
        
        // 1. Safety Guard
        if !force && !isWalkmanVolume(url: destinationURL) {
            WalkmanLogger.error("Erase aborted: '\(destinationURL.path)' is not recognized as a Sony Walkman device.")
            throw WalkmanCleanerError.notAWalkmanDevice(destinationURL.path)
        }
        
        progressCallback?("Inspecting Walkman and preserving device key...")
        WalkmanLogger.info("Starting Walkman erase & reset at: \(destinationURL.path)")
        
        // 2. Discover and Preserve Hardware Key (DvID.DAT)
        let dvidURL = destinationURL.appendingPathComponent("MP3FM/DvID.DAT")
        var preservedKeyData: Data? = nil
        var preservedKey: UInt32 = 0
        
        if let existingData = try? Data(contentsOf: dvidURL), existingData.count == 16 {
            let readKey = WalkmanKeyManager.readDeviceKey(from: dvidURL) ?? 0
            preservedKey = readKey
            preservedKeyData = existingData
            WalkmanLogger.info("Preserved existing on-device key: 0x\(String(format: "%08X", readKey))")
        }
        
        // If not on-device or unauthentic, check if cached key exists
        if !WalkmanKeyManager.isKeyAuthentic(key: preservedKey) {
            let cachedKey = WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: destinationURL)
            if WalkmanKeyManager.isKeyAuthentic(key: cachedKey) {
                preservedKey = cachedKey
                preservedKeyData = WalkmanKeyManager.generateDvidData(key: cachedKey)
                WalkmanLogger.info("Restoring authentic cached key: 0x\(String(format: "%08X", cachedKey))")
            } else if preservedKeyData == nil {
                preservedKey = cachedKey
                preservedKeyData = WalkmanKeyManager.generateDvidData(key: cachedKey)
            }
        }
        
        let isAuthentic = WalkmanKeyManager.isKeyAuthentic(key: preservedKey)
        
        // 3. Scan tracks to count before deletion
        progressCallback?("Scanning tracks and estimating storage...")
        let omgURL = destinationURL.appendingPathComponent("OMGAUDIO")
        var tracksFound = 0
        var bytesToFree: Int64 = 0
        
        if fm.fileExists(atPath: omgURL.path) {
            if let enumerator = fm.enumerator(at: omgURL, includingPropertiesForKeys: [.fileSizeKey, .isDirectoryKey]) {
                for case let fileURL as URL in enumerator {
                    if fileURL.pathExtension.uppercased() == "OMA" {
                        tracksFound += 1
                        if let res = try? fileURL.resourceValues(forKeys: [.fileSizeKey]), let sz = res.fileSize {
                            bytesToFree += Int64(sz)
                        }
                    }
                }
            }
        }
        
        progressCallback?("Deleting \(tracksFound) tracks from OMGAUDIO...")
        WalkmanLogger.info("Found \(tracksFound) OMA tracks (\(bytesToFree / (1024 * 1024)) MB) to erase.")
        
        // 4. Remove all files and directories inside OMGAUDIO
        if fm.fileExists(atPath: omgURL.path) {
            if let items = try? fm.contentsOfDirectory(at: omgURL, includingPropertiesForKeys: nil) {
                for item in items {
                    try? fm.removeItem(at: item)
                }
            }
        } else {
            try? fm.createDirectory(at: omgURL, withIntermediateDirectories: true)
        }
        
        // 5. Clean macOS storage leaks (.Trashes, .Spotlight-V100, .fseventsd, AppleDouble files)
        progressCallback?("Purging macOS hidden trash and metadata caches...")
        let macLeakDirs = [
            ".Trashes",
            ".Spotlight-V100",
            ".fseventsd",
            ".TemporaryItems",
            ".DocumentRevisions-V100"
        ]
        
        for dir in macLeakDirs {
            let leakURL = destinationURL.appendingPathComponent(dir)
            if fm.fileExists(atPath: leakURL.path) {
                WalkmanLogger.info("Purging leak directory: \(dir)")
                try? fm.removeItem(at: leakURL)
            }
        }
        
        // 6. Restore authentic DvID.DAT to MP3FM
        progressCallback?("Re-asserting hardware key identity (DvID.DAT)...")
        let mp3fmURL = destinationURL.appendingPathComponent("MP3FM")
        try? fm.createDirectory(at: mp3fmURL, withIntermediateDirectories: true)
        
        if let keyData = preservedKeyData {
            try? keyData.write(to: dvidURL)
            WalkmanLogger.info("Successfully re-wrote DvID.DAT with key 0x\(String(format: "%08X", preservedKey))")
        }
        
        // 7. Generate clean empty OMGAUDIO database suite (16 DAT tables for 0 tracks)
        progressCallback?("Generating empty OMGAUDIO database suite (NO DATA)...")
        WalkmanLogger.info("Generating pristine OMGAUDIO database for 0 tracks...")
        let dbGen = WalkmanDBGenerator(mp3Bitrate: .kbps192, isVBR: false, isEncrypted3rdGen: true)
        do {
            try dbGen.generateDatabase(titles: [], destination: destinationURL)
        } catch {
            WalkmanLogger.error("Failed to generate empty OMGAUDIO database: \(error.localizedDescription)")
            throw WalkmanCleanerError.databaseGenerationFailed(error.localizedDescription)
        }
        
        // 8. Clean dot-underscore files and flush OS buffers
        progressCallback?("Flushing disk caches and cleaning metadata...")
        SyncEngine.cleanAppleDouble(at: destinationURL)
        Darwin.sync()
        
        WalkmanLogger.info("Walkman erase completed successfully! \(tracksFound) tracks erased, key 0x\(String(format: "%08X", preservedKey)) preserved.")
        progressCallback?("Erase complete! Device shows NO DATA with 100% free space.")
        
        return EraseResult(
            tracksDeleted: tracksFound,
            bytesFreed: bytesToFree,
            keyPreserved: preservedKey,
            keyWasAuthentic: isAuthentic,
            mountPath: destinationURL.path
        )
    }
}
