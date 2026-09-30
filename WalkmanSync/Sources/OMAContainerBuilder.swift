import Foundation

/// Utilities to construct Sony OMA files (Sony EA3 tag + EA3 audio header + scrambled audio)
/// Reverse-engineered from Sony SonicStage and JSymphonic specs for Sony Network Walkmans.
public enum OMAContainerBuilder {
    
    public static let ea3TagSize = 3072
    public static let ea3AudioHeaderSize = 96
    
    /// Builds standard 3072-byte Sony EA3 Tag (UTF-16BE with Sony 3-byte encoding code)
    public static func buildEA3Tag(title: WalkmanDBGenerator.WalkmanTitle) -> Data {
        var tagData = Data()
        
        // 1. Tag header (10 bytes)
        // Magic "ea3\x03"
        tagData.append(contentsOf: [0x65, 0x61, 0x33, 0x03])
        // Tag size code: 0x17 * 0x80 + 0x80 = 0xC00 (3072 bytes)
        tagData.append(contentsOf: [0x00, 0x00, 0x00, 0x00, 0x17, 0x76])
        
        let encodageCode: [UInt8] = [0x00, 0x00, 0x02]
        
        func appendFrame(label: String, text: String) {
            let clean = String(text.prefix(59))
            guard !clean.isEmpty, let utf16Bytes = clean.data(using: .utf16BigEndian) else { return }
            
            // Label (4 bytes)
            tagData.append(contentsOf: label.utf8.prefix(4))
            // Length: utf16Bytes.count + 1 (4 bytes BigEndian)
            let frameLen = UInt32(utf16Bytes.count + 1)
            tagData.append(UInt8((frameLen >> 24) & 0xFF))
            tagData.append(UInt8((frameLen >> 16) & 0xFF))
            tagData.append(UInt8((frameLen >> 8) & 0xFF))
            tagData.append(UInt8(frameLen & 0xFF))
            // Encoding code (3 bytes)
            tagData.append(contentsOf: encodageCode)
            // Text in UTF-16BE
            tagData.append(utf16Bytes)
        }
        
        // TIT2 - Title
        appendFrame(label: "TIT2", text: title.titleName)
        // TPE1 - Artist
        appendFrame(label: "TPE1", text: title.artistName)
        // TALB - Album
        appendFrame(label: "TALB", text: title.albumName)
        // TCON - Genre
        appendFrame(label: "TCON", text: title.genre)
        
        // TXXX - OMG_TRACK
        let trackNum = title.id % 100
        let trackNumStr = "\(trackNum)"
        if let omgTrackBytes = "OMG_TRACK".data(using: .utf16BigEndian),
           let numBytes = trackNumStr.data(using: .utf16BigEndian) {
            tagData.append(contentsOf: "TXXX".utf8)
            let txxxContentLen = (trackNum < 10) ? 23 : 25
            tagData.append(UInt8((txxxContentLen >> 24) & 0xFF))
            tagData.append(UInt8((txxxContentLen >> 16) & 0xFF))
            tagData.append(UInt8((txxxContentLen >> 8) & 0xFF))
            tagData.append(UInt8(txxxContentLen & 0xFF))
            tagData.append(contentsOf: encodageCode)
            tagData.append(omgTrackBytes)
            tagData.append(contentsOf: [0x00, 0x00]) // 2 zeros separator
            tagData.append(numBytes)
        }
        
        // TYER - Year
        if let yearBytes = "2005".data(using: .utf16BigEndian) {
            tagData.append(contentsOf: "TYER".utf8)
            let yLen = UInt32(yearBytes.count + 1)
            tagData.append(UInt8((yLen >> 24) & 0xFF))
            tagData.append(UInt8((yLen >> 16) & 0xFF))
            tagData.append(UInt8((yLen >> 8) & 0xFF))
            tagData.append(UInt8(yLen & 0xFF))
            tagData.append(contentsOf: encodageCode)
            tagData.append(yearBytes)
        }
        
        // TLEN - Track duration in ms
        let msStr = "\(title.length * 1000)"
        if let msBytes = msStr.data(using: .utf16BigEndian) {
            tagData.append(contentsOf: "TLEN".utf8)
            let mLen = UInt32(msBytes.count + 1)
            tagData.append(UInt8((mLen >> 24) & 0xFF))
            tagData.append(UInt8((mLen >> 16) & 0xFF))
            tagData.append(UInt8((mLen >> 8) & 0xFF))
            tagData.append(UInt8(mLen & 0xFF))
            tagData.append(contentsOf: encodageCode)
            tagData.append(msBytes)
        }
        
        // Pad with zeros to exactly 3072 bytes (0xC00)
        if tagData.count < ea3TagSize {
            tagData.append(Data(count: ea3TagSize - tagData.count))
        } else {
            tagData = tagData.prefix(ea3TagSize)
        }
        
        return tagData
    }
    
    /// Builds 96-byte EA3 audio header (Magic "EA3", protection, MP3 codec ID 3, duration & frame count)
    public static func buildEA3AudioHeader(
        title: WalkmanDBGenerator.WalkmanTitle,
        gotKey: Bool = true,
        channels: Int = 2,
        isVBR: Bool = false
    ) -> Data {
        var header = Data(count: ea3AudioHeaderSize)
        
        // Bytes 0-2: "EA3"
        header[0] = 0x45
        header[1] = 0x41
        header[2] = 0x33
        // Byte 3: Version 2
        header[3] = 0x02
        // Byte 4: 0x00
        header[4] = 0x00
        // Byte 5: Size 0x60 (96 bytes)
        header[5] = 0x60
        // Bytes 6-7: Protection Flag (0xFFFE for scrambled MP3, 0xFFFF for unencrypted)
        if gotKey {
            header[6] = 0xFF
            header[7] = 0xFE
        } else {
            header[6] = 0xFF
            header[7] = 0xFF
        }
        
        // Bytes 8-31: 24 zeros
        
        // Bytes 32-35: File Properties (4 bytes)
        header[32] = 0x03 // 0x03 = MP3 format
        header[33] = isVBR ? 0x90 : 0x80 // 0x90 = VBR, 0x80 = CBR
        header[34] = 0xD9 // MPEG-1 Layer 3 standard index
        header[35] = (channels >= 2) ? 0x10 : 0x30 // Stereo (0x10) or Mono (0x30)
        
        // Bytes 36-39: Track length in milliseconds (4 bytes BigEndian)
        let lengthMs = UInt32(title.length * 1000)
        header[36] = UInt8((lengthMs >> 24) & 0xFF)
        header[37] = UInt8((lengthMs >> 16) & 0xFF)
        header[38] = UInt8((lengthMs >> 8) & 0xFF)
        header[39] = UInt8(lengthMs & 0xFF)
        
        // Bytes 40-43: Total MP3 audio frames (4 bytes BigEndian)
        // Standard MPEG-1 Layer 3: 1152 samples/frame at 44.1 kHz
        let frames = UInt32((Double(lengthMs) * 44.1) / 1152.0)
        header[40] = UInt8((frames >> 24) & 0xFF)
        header[41] = UInt8((frames >> 16) & 0xFF)
        header[42] = UInt8((frames >> 8) & 0xFF)
        header[43] = UInt8(frames & 0xFF)
        
        // Bytes 44-95: Remaining padding zeros
        return header
    }
    
    /// Strips ID3v2 header, padding zeros, and Xing header from MP3 to extract raw audio frames starting cleanly with 0xFF 0xEx/0xFx
    public static func extractRawMP3Audio(from fileURL: URL) throws -> Data {
        let fileData = try Data(contentsOf: fileURL, options: .mappedIfSafe)
        guard fileData.count > 10 else { return fileData }
        
        var startOffset = 0
        if fileData[0] == 0x49 && fileData[1] == 0x44 && fileData[2] == 0x33 { // "ID3"
            let flags = fileData[5]
            let size = (Int(fileData[6]) << 21) |
                       (Int(fileData[7]) << 14) |
                       (Int(fileData[8]) << 7)  |
                       Int(fileData[9])
            startOffset = 10 + size
            if (flags & 0x10) != 0 {
                startOffset += 10 // ID3v2.4 footer
            }
        }
        
        // Scan forward past any ID3 padding/zeros to find the first valid MPEG sync word (0xFF 0xEx/0xFx)
        var mpegStart = startOffset
        while mpegStart < fileData.count - 4 {
            if fileData[mpegStart] == 0xFF {
                let second = fileData[mpegStart + 1]
                if (second & 0xE0) == 0xE0 && (second & 0x18) != 0x08 && (second & 0x06) != 0x00 {
                    break
                }
            }
            mpegStart += 1
        }
        
        if mpegStart < fileData.count {
            var endOffset = fileData.count
            // Check for trailing ID3v1 (128 bytes starting with "TAG")
            if endOffset >= mpegStart + 128 {
                let tagOffset = endOffset - 128
                if fileData[tagOffset] == 0x54 && fileData[tagOffset + 1] == 0x41 && fileData[tagOffset + 2] == 0x47 { // "TAG"
                    endOffset -= 128
                }
            }
            return fileData.subdata(in: mpegStart..<endOffset)
        }
        return fileData
    }
    
    /// Creates a complete encrypted .OMA file
    public static func createEncryptedOMA(
        title: WalkmanDBGenerator.WalkmanTitle,
        sourceMP3URL: URL,
        deviceKey: UInt32,
        isVBR: Bool = false
    ) throws -> Data {
        let ea3Tag = buildEA3Tag(title: title)
        let ea3AudioHeader = buildEA3AudioHeader(title: title, gotKey: true, isVBR: isVBR)
        
        var rawAudio = try extractRawMP3Audio(from: sourceMP3URL)
        let xorKey = WalkmanKeyManager.computeXorKey(trackId: title.id, deviceKey: deviceKey)
        WalkmanKeyManager.xorScramble(data: &rawAudio, keyBytes: xorKey)
        
        var oma = Data()
        oma.reserveCapacity(ea3Tag.count + ea3AudioHeader.count + rawAudio.count)
        oma.append(ea3Tag)
        oma.append(ea3AudioHeader)
        oma.append(rawAudio)
        return oma
    }
}
