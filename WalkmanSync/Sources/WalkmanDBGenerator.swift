import Foundation

// MARK: - Core DB Generation Structures
// Reflects Sony OMGAUDIO DAT database layout and JSymphonic's DataBaseOmgaudioToolBox.java

public class WalkmanDBGenerator {
    private var isEncrypted3rdGen: Bool = true
    
    public init(isEncrypted3rdGen: Bool = true) {
        self.isEncrypted3rdGen = isEncrypted3rdGen
    }
    
    // Extracted metadata structures for database layout
    public struct WalkmanTitle {
        public var id: Int
        public var titleName: String
        public var artistName: String
        public var albumName: String
        public var genre: String
        public var length: Int
        public var originalFile: URL?
        
        public init(id: Int, titleName: String, artistName: String, albumName: String, genre: String, length: Int, originalFile: URL? = nil) {
            self.id = id
            self.titleName = titleName
            self.artistName = artistName
            self.albumName = albumName
            self.genre = genre
            self.length = length
            self.originalFile = originalFile
        }
    }
    
    public func generateDatabase(titles: [WalkmanTitle], destination: URL) throws {
        let omgAudioDir = destination.appendingPathComponent("OMGAUDIO", isDirectory: true)
        try FileManager.default.createDirectory(at: omgAudioDir, withIntermediateDirectories: true, attributes: nil)
        
        if isEncrypted3rdGen {
            let mp3fmDir = destination.appendingPathComponent("MP3FM", isDirectory: true)
            try FileManager.default.createDirectory(at: mp3fmDir, withIntermediateDirectories: true, attributes: nil)
        }
        
        print("Ready to generate database for \(titles.count) files.")
        
        try write04CNTINF(omgAudioDir: omgAudioDir, titles: titles)
    }
    
    // MARK: - Binary Serialization Implementation
    
    private func write04CNTINF(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        let table4 = omgAudioDir.appendingPathComponent("04CNTINF.DAT")
        var data = Data()
        
        // Header
        data.append(contentsOf: writeTableHeader(magic: "CNIF", tableNumber: 1))
        
        let maxValue = titles.map { $0.id }.max() ?? 0
        let elementSize = 0x290
        let tableSize = (maxValue * elementSize) + 0x10
        
        data.append(contentsOf: writeClassDescription(magic: "CNFB", something: 0x20, size: tableSize))
        data.append(contentsOf: writeClassHeader(magic: "CNFB", maxId: maxValue, size: elementSize))
        
        if maxValue > 0 {
            for id in 1...maxValue {
                if let title = titles.first(where: { $0.id == id }) {
                    data.append(contentsOf: writeCNFBelement(title: title, gotKey: isEncrypted3rdGen))
                } else {
                    data.append(contentsOf: writeCNFBelement(title: nil, gotKey: isEncrypted3rdGen))
                }
            }
        }
        
        try data.write(to: table4, options: .atomic)
    }
    
    private func writeTableHeader(magic: String, tableNumber: Int) -> Data {
        var d = Data()
        d.append(contentsOf: magic.utf8.prefix(4))
        d.append(contentsOf: [0x00, 0x01, 0x01, 0x00]) // Constants from Java implementation
        d.append(int2bytes(tableNumber, length: 4))
        d.append(contentsOf: [0x00, 0x00, 0x00, 0x00])
        return d
    }
    
    private func writeClassDescription(magic: String, something: Int, size: Int) -> Data {
        var d = Data()
        d.append(contentsOf: magic.utf8.prefix(4))
        d.append(int2bytes(something, length: 2))
        d.append(contentsOf: [0x00, 0x00])
        d.append(int2bytes(size, length: 4))
        d.append(contentsOf: [0x00, 0x00, 0x00, 0x00])
        return d
    }
    
    private func writeClassHeader(magic: String, maxId: Int, size: Int) -> Data {
        var d = Data()
        d.append(contentsOf: magic.utf8.prefix(4))
        d.append(contentsOf: [0x00, 0x02, 0x00, 0x00])
        d.append(int2bytes(maxId, length: 2))
        d.append(int2bytes(size, length: 2))
        d.append(contentsOf: [0x00, 0x00, 0x00, 0x00])
        return d
    }
    
    private func writeCNFBelement(title: WalkmanTitle?, gotKey: Bool) -> Data {
        var d = Data()
        
        let constant1: [UInt8] = [0x00, 0x05, 0x00, 0x80] // 5 tags of 128 (0x80) bytes
        let constant2: [UInt8] = [0x00, 0x02] // UTF-16BE encoding
        
        if let t = title {
            // Write element header
            d.append(contentsOf: [0x00, 0x00]) // 2 zeros
            
            // Protection Flag
            if gotKey {
                // 3rd Gen MP3 Encryption (0xFF 0xFE)
                d.append(contentsOf: [0xFF, 0xFE])
            } else {
                // No Protection
                d.append(contentsOf: [0xFF, 0xFF])
            }
            
            // File properties: 0x80 (CBR), 0xD9 (MPEG1 Layer III 128kbps), 0x10 (Stereo), 0x00
            d.append(contentsOf: [0x80, 0xD9, 0x10, 0x00])
            d.append(int2bytes(t.length * 1000, length: 4)) // title key in milliseconds
            
            d.append(contentsOf: constant1)
            
            // Sub-elements (TIT2, TPE1, TALB, TCON, TSOP)
            appendSubElement(&d, tag: "TIT2", text: t.titleName, constant: constant2)
            appendSubElement(&d, tag: "TPE1", text: t.artistName, constant: constant2)
            appendSubElement(&d, tag: "TALB", text: t.albumName, constant: constant2)
            appendSubElement(&d, tag: "TCON", text: t.genre, constant: constant2)
            appendEmptySubElement(&d, tag: "TSOP", constant: constant2)
            
        } else {
            // Write dummy padding for missing title ID
            d.append(Data(count: 12)) // 12 zeros
            d.append(contentsOf: constant1)
            appendSubElement(&d, tag: "TIT2", text: "", constant: constant2)
            appendSubElement(&d, tag: "TPE1", text: "", constant: constant2)
            appendSubElement(&d, tag: "TALB", text: "", constant: constant2)
            appendSubElement(&d, tag: "TCON", text: "", constant: constant2)
            appendEmptySubElement(&d, tag: "TSOP", constant: constant2)
        }
        
        return d
    }
    
    private func appendSubElement(_ d: inout Data, tag: String, text: String, constant: [UInt8]) {
        d.append(contentsOf: tag.utf8.prefix(4))
        d.append(contentsOf: constant)
        
        // Write string in UTF-16BE format as per original Sony spec
        let utf16Data = text.data(using: .utf16BigEndian) ?? Data()
        d.append(utf16Data)
        
        let remainingBytes = 0x80 - 4 - 2 - utf16Data.count
        if remainingBytes > 0 {
            d.append(Data(count: remainingBytes))
        }
    }
    
    private func appendEmptySubElement(_ d: inout Data, tag: String, constant: [UInt8]) {
        d.append(contentsOf: tag.utf8.prefix(4))
        d.append(contentsOf: constant)
        d.append(Data(count: 0x80 - 4 - 2))
    }
    
    // Utility to match JSymphonic's int2bytes big-endian layout
    private func int2bytes(_ value: Int, length: Int) -> Data {
        var data = Data(count: length)
        var val = value
        for i in (0..<length).reversed() {
            data[i] = UInt8(val & 0xFF)
            val >>= 8
        }
        return data
    }
}
