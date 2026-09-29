import Foundation

/// Utilities to construct Sony OMA files (EA3 ID3v2 tag + EA3 audio header + scrambled audio)
public enum OMAContainerBuilder {
    
    public static let ea3TagSize = 3072
    public static let ea3AudioHeaderSize = 96
    
    /// Builds standard 3072-byte EA3 Tag (ID3v2-compatible)
    public static func buildEA3Tag(title: WalkmanDBGenerator.WalkmanTitle) -> Data {
        var frames = Data()
        
        func addFrame(id: String, text: String) {
            guard !text.isEmpty else { return }
            var frameData = Data()
            frameData.append(0x03) // UTF-8 encoding marker
            frameData.append(contentsOf: text.utf8)
            
            var frame = Data()
            frame.append(contentsOf: id.utf8)
            let size = UInt32(frameData.count)
            frame.append(UInt8((size >> 24) & 0xFF))
            frame.append(UInt8((size >> 16) & 0xFF))
            frame.append(UInt8((size >> 8) & 0xFF))
            frame.append(UInt8(size & 0xFF))
            frame.append(0x00) // Flags
            frame.append(0x00)
            frame.append(frameData)
            frames.append(frame)
        }
        
        func addTXXX(description: String, value: String) {
            var payload = Data()
            payload.append(0x03) // UTF-8
            payload.append(contentsOf: description.utf8)
            payload.append(0x00) // Null separator
            payload.append(contentsOf: value.utf8)
            
            var frame = Data()
            frame.append(contentsOf: "TXXX".utf8)
            let size = UInt32(payload.count)
            frame.append(UInt8((size >> 24) & 0xFF))
            frame.append(UInt8((size >> 16) & 0xFF))
            frame.append(UInt8((size >> 8) & 0xFF))
            frame.append(UInt8(size & 0xFF))
            frame.append(0x00)
            frame.append(0x00)
            frame.append(payload)
            frames.append(frame)
        }
        
        addFrame(id: "TIT2", text: title.titleName)
        addFrame(id: "TPE1", text: title.artistName)
        addFrame(id: "TALB", text: title.albumName)
        addFrame(id: "TCON", text: title.genre)
        addTXXX(description: "OMG_TRACK", value: "\(title.id)")
        addTXXX(description: "OMG_TRLDA", value: "2005/01/01 00:00:00")
        
        var header = Data()
        header.append(contentsOf: "ea3".utf8)
        header.append(0x03) // Version 3
        header.append(0x00) // Revision
        header.append(0x00) // Flags
        
        let contentSize = ea3TagSize - 10
        let ss = ((contentSize & 0x0FE00000) << 3) |
                 ((contentSize & 0x001FC000) << 2) |
                 ((contentSize & 0x00003F80) << 1) |
                 (contentSize & 0x0000007F)
        
        header.append(UInt8((ss >> 24) & 0xFF))
        header.append(UInt8((ss >> 16) & 0xFF))
        header.append(UInt8((ss >> 8) & 0xFF))
        header.append(UInt8(ss & 0xFF))
        
        var tag = header + frames
        if tag.count < ea3TagSize {
            tag.append(Data(count: ea3TagSize - tag.count))
        } else {
            tag = tag.prefix(ea3TagSize)
        }
        return tag
    }
    
    /// Builds 96-byte EA3 audio header (Magic "EA3", 0xFFFE protection, MP3 codec ID 3)
    public static func buildEA3AudioHeader(bitrateKbps: Int = 128, channels: Int = 2) -> Data {
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
        // Bytes 6-7: 0xFFFE (Encrypted MP3 marker for 3rd Gen)
        header[6] = 0xFF
        header[7] = 0xFE
        
        // Byte 32: Codec ID (3 = MP3)
        header[32] = 0x03
        
        // Bytes 33-35: Codec parameters (CBR, MPEG1 Layer III, stereo)
        header[33] = 0x80 // CBR
        // (MPEG1: 3 << 6) | (Layer III: 1 << 4) | (128kbps: 0x09) = 0xD9
        header[34] = 0xD9
        header[35] = (channels >= 2) ? 0x10 : 0x30
        
        return header
    }
    
    /// Strips ID3v2 header from MP3 to extract raw audio frames
    public static func extractRawMP3Audio(from fileURL: URL) throws -> Data {
        let fileData = try Data(contentsOf: fileURL, options: .mappedIfSafe)
        guard fileData.count > 10 else { return fileData }
        
        if fileData[0] == 0x49 && fileData[1] == 0x44 && fileData[2] == 0x33 { // "ID3"
            let flags = fileData[5]
            let size = (Int(fileData[6]) << 21) |
                       (Int(fileData[7]) << 14) |
                       (Int(fileData[8]) << 7)  |
                       Int(fileData[9])
            var offset = 10 + size
            if (flags & 0x10) != 0 {
                offset += 10 // ID3v2.4 footer
            }
            if offset < fileData.count {
                return fileData.subdata(in: offset..<fileData.count)
            }
        }
        return fileData
    }
    
    /// Creates a complete encrypted .OMA file
    public static func createEncryptedOMA(
        title: WalkmanDBGenerator.WalkmanTitle,
        sourceMP3URL: URL,
        deviceKey: UInt32
    ) throws -> Data {
        let ea3Tag = buildEA3Tag(title: title)
        let ea3AudioHeader = buildEA3AudioHeader()
        
        var rawAudio = try extractRawMP3Audio(from: sourceMP3URL)
        let xorKey = WalkmanKeyManager.computeXorKey(trackId: title.id, deviceKey: deviceKey)
        WalkmanKeyManager.xorScramble(data: &rawAudio, keyBytes: xorKey)
        
        var omaData = Data()
        omaData.reserveCapacity(ea3Tag.count + ea3AudioHeader.count + rawAudio.count)
        omaData.append(ea3Tag)
        omaData.append(ea3AudioHeader)
        omaData.append(rawAudio)
        return omaData
    }
}
