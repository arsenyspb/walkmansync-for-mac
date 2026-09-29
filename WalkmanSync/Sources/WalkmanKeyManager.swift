import Foundation

/// Handles device key retrieval, generation, and XOR scrambling for Sony Network Walkman 3rd Generation (NW-E40x, NW-HDx, etc.)
public enum WalkmanKeyManager {
    
    /// Default factory device identity key used when no existing key file is present.
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
    
    /// Discovers existing device key or creates and saves a standard DvID.DAT file to MP3FM/DvID.DAT
    public static func resolveOrCreateDeviceKey(deviceURL: URL) -> UInt32 {
        let fm = FileManager.default
        
        for relativePath in potentialKeyPaths {
            let fileURL = deviceURL.appendingPathComponent(relativePath)
            if fm.fileExists(atPath: fileURL.path) {
                if let key = readDeviceKey(from: fileURL) {
                    print("Found existing DvID key at \(relativePath): 0x\(String(format: "%08X", key))")
                    return key
                }
            }
        }
        
        // No key exists; create standard MP3FM/DvID.DAT
        let mp3fmDir = deviceURL.appendingPathComponent("MP3FM", isDirectory: true)
        try? fm.createDirectory(at: mp3fmDir, withIntermediateDirectories: true, attributes: nil)
        let dvidURL = mp3fmDir.appendingPathComponent("DvID.DAT")
        
        let dvidData = generateDvidData(key: defaultDeviceKey)
        try? dvidData.write(to: dvidURL)
        print("Generated standard 16-byte DvID.DAT at MP3FM/DvID.DAT with key 0x\(String(format: "%08X", defaultDeviceKey))")
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
    
    /// Scrambles audio data in-place using 8-byte repeating blocks
    public static func xorScramble(data: inout Data, keyBytes: [UInt8]) {
        let key8 = keyBytes + keyBytes
        let count = data.count
        let blockCount = count / 8
        
        data.withUnsafeMutableBytes { (ptr: UnsafeMutableRawBufferPointer) in
            guard let baseAddress = ptr.baseAddress?.assumingMemoryBound(to: UInt8.self) else { return }
            for b in 0..<blockCount {
                let offset = b * 8
                for j in 0..<8 {
                    baseAddress[offset + j] ^= key8[j]
                }
            }
        }
    }
}
