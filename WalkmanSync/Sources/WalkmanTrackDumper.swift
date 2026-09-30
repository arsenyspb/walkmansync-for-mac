import Foundation

public struct DumpResult {
    public let totalTracksFound: Int
    public let tracksDumped: Int
    public let tracksFailed: Int
    public let outputDirectory: URL
    public let totalBytesWritten: Int64
}

public struct DumpedTrackMetadata {
    public var id: Int
    public var title: String
    public var artist: String
    public var album: String
    public var genre: String
    public var trackNumber: Int
}

public enum WalkmanTrackDumperError: LocalizedError {
    case omaDirectoryNotFound(String)
    case deviceKeyUnavailable
    case outputDirectoryUnwritable(String)
    
    public var errorDescription: String? {
        switch self {
        case .omaDirectoryNotFound(let path):
            return "No OMGAUDIO music folder found at: \(path)"
        case .deviceKeyUnavailable:
            return "No valid hardware key found to descramble tracks. Run 'walkmansync --extract-key' first."
        case .outputDirectoryUnwritable(let path):
            return "Output destination is not writable: \(path)"
        }
    }
}

/// Dumper and Descrambler for Sony Network Walkmans
/// Reverses Sony's proprietary XOR scrambling and OpenMG containers to recover
/// pristine, fully-tagged MP3 files from the Walkman back onto macOS.
public enum WalkmanTrackDumper {
    
    /// Dumps and reverse-descrambles all .OMA audio tracks from a Walkman to a local folder
    public static func dumpTracks(
        from walkmanURL: URL,
        to destinationURL: URL,
        progressCallback: ((Int, Int, String) -> Void)? = nil
    ) throws -> DumpResult {
        let fm = FileManager.default
        let omgURL = walkmanURL.appendingPathComponent("OMGAUDIO")
        
        guard fm.fileExists(atPath: omgURL.path) else {
            throw WalkmanTrackDumperError.omaDirectoryNotFound(omgURL.path)
        }
        
        try fm.createDirectory(at: destinationURL, withIntermediateDirectories: true)
        
        // 1. Resolve Device Key
        let dvidURL = walkmanURL.appendingPathComponent("MP3FM/DvID.DAT")
        var deviceKey = WalkmanKeyManager.readDeviceKey(from: dvidURL) ?? WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: walkmanURL)
        if deviceKey == 0 {
            deviceKey = WalkmanKeyManager.defaultDeviceKey
        }
        
        WalkmanLogger.info("Dumping tracks using device key: 0x\(String(format: "%08X", deviceKey))")
        
        // 2. Load 04CNTINF.DAT database metadata if present
        let cntinfURL = omgURL.appendingPathComponent("04CNTINF.DAT")
        let dbMetadata = parse04CNTINF(at: cntinfURL)
        WalkmanLogger.info("Loaded \(dbMetadata.count) metadata entries from 04CNTINF.DAT")
        
        // 3. Discover all .OMA tracks
        var omaFiles: [URL] = []
        if let enumerator = fm.enumerator(at: omgURL, includingPropertiesForKeys: [.isRegularFileKey]) {
            for case let fileURL as URL in enumerator {
                if fileURL.pathExtension.uppercased() == "OMA" {
                    omaFiles.append(fileURL)
                }
            }
        }
        
        // Sort files by track number
        omaFiles.sort { $0.lastPathComponent < $1.lastPathComponent }
        
        let totalCount = omaFiles.count
        guard totalCount > 0 else {
            return DumpResult(totalTracksFound: 0, tracksDumped: 0, tracksFailed: 0, outputDirectory: destinationURL, totalBytesWritten: 0)
        }
        
        var dumped = 0
        var failed = 0
        var totalBytes: Int64 = 0
        
        // 4. Extract and descramble each track
        for (index, fileURL) in omaFiles.enumerated() {
            let trackId = extractTrackId(from: fileURL)
            let baseName = fileURL.deletingPathExtension().lastPathComponent
            
            do {
                let data = try Data(contentsOf: fileURL, options: .mappedIfSafe)
                guard data.count > (OMAContainerBuilder.ea3TagSize + OMAContainerBuilder.ea3AudioHeaderSize) else {
                    WalkmanLogger.warn("Skipping \(baseName): file too small (\(data.count) bytes)")
                    failed += 1
                    continue
                }
                
                // Parse metadata (prefer 04CNTINF, fallback to EA3 tag)
                var meta = dbMetadata[trackId] ?? parseEA3Tag(data: data.prefix(OMAContainerBuilder.ea3TagSize), trackId: trackId)
                if meta.trackNumber <= 0 {
                    meta.trackNumber = trackId
                }
                if meta.title.isEmpty {
                    meta.title = "Track \(trackId)"
                }
                if meta.artist.isEmpty {
                    meta.artist = "Unknown Artist"
                }
                if meta.album.isEmpty {
                    meta.album = "Unknown Album"
                }
                
                progressCallback?(index + 1, totalCount, "\(meta.artist) - \(meta.title)")
                
                // Read audio header
                let headerOffset = OMAContainerBuilder.ea3TagSize
                let audioOffset = headerOffset + OMAContainerBuilder.ea3AudioHeaderSize
                
                let isScrambled = (data[headerOffset + 6] == 0xFF && data[headerOffset + 7] == 0xFE)
                var audioPayload = data.subdata(in: audioOffset..<data.count)
                
                if isScrambled {
                    let xorKey = WalkmanKeyManager.computeXorKey(trackId: trackId, deviceKey: deviceKey)
                    WalkmanKeyManager.xorScramble(data: &audioPayload, keyBytes: xorKey)
                }
                
                // Build ID3v2.3 Tag
                let id3Tag = buildStandardID3v2(
                    title: meta.title,
                    artist: meta.artist,
                    album: meta.album,
                    trackNumber: meta.trackNumber,
                    genre: meta.genre
                )
                
                var finalMP3 = Data()
                finalMP3.reserveCapacity(id3Tag.count + audioPayload.count)
                finalMP3.append(id3Tag)
                finalMP3.append(audioPayload)
                
                // Format folder: <destination>/<Artist>/<Album>/<TrackNumber> - <Title>.mp3
                let cleanArtist = sanitizeFilename(meta.artist)
                let cleanAlbum = sanitizeFilename(meta.album)
                let cleanTitle = sanitizeFilename(meta.title)
                
                let trackPrefix = String(format: "%02d", meta.trackNumber)
                let outFileName = "\(trackPrefix) - \(cleanTitle).mp3"
                
                let artistDir = destinationURL.appendingPathComponent(cleanArtist)
                let albumDir = artistDir.appendingPathComponent(cleanAlbum)
                try fm.createDirectory(at: albumDir, withIntermediateDirectories: true)
                
                let outURL = albumDir.appendingPathComponent(outFileName)
                try finalMP3.write(to: outURL, options: .atomic)
                
                dumped += 1
                totalBytes += Int64(finalMP3.count)
                WalkmanLogger.info("Dumped track \(trackId): \(outURL.path) (\(finalMP3.count) bytes)")
            } catch {
                WalkmanLogger.error("Failed to dump \(baseName): \(error.localizedDescription)")
                failed += 1
            }
        }
        
        return DumpResult(
            totalTracksFound: totalCount,
            tracksDumped: dumped,
            tracksFailed: failed,
            outputDirectory: destinationURL,
            totalBytesWritten: totalBytes
        )
    }
    
    // MARK: - Metadata Extraction
    
    /// Parses track ID from Sony filename (e.g. 10000001.OMA -> 1, 1000000A.OMA -> 10)
    public static func extractTrackId(from fileURL: URL) -> Int {
        let name = fileURL.deletingPathExtension().lastPathComponent
        if name.count >= 6, let id = Int(name.suffix(6), radix: 16) {
            return id
        }
        return 1
    }
    
    /// Parses Sony's 04CNTINF.DAT table for authoritative track metadata
    public static func parse04CNTINF(at fileURL: URL) -> [Int: DumpedTrackMetadata] {
        guard let data = try? Data(contentsOf: fileURL), data.count >= 0x30 else { return [:] }
        
        var result: [Int: DumpedTrackMetadata] = [:]
        let elementSize = 0x290
        let count = (data.count - 0x30) / elementSize
        
        for i in 0..<count {
            let offset = 0x30 + (i * elementSize)
            guard offset + elementSize <= data.count else { break }
            let trackId = i + 1
            
            func readSubElementString(start: Int, maxLen: Int = 122) -> String {
                guard start < data.count else { return "" }
                let end = min(start + maxLen, data.count)
                let raw = data.subdata(in: start..<end)
                var textEnd = raw.count
                var j = 0
                while j + 1 < raw.count {
                    if raw[j] == 0x00 && raw[j + 1] == 0x00 {
                        textEnd = j
                        break
                    }
                    j += 2
                }
                let valid = raw.prefix(textEnd)
                return String(data: valid, encoding: .utf16BigEndian)?.trimmingCharacters(in: .controlCharacters) ?? ""
            }
            
            // Sub-elements start at offset + 16
            // TIT2: offset + 16 (tag: 4, constant: 2, string at +22)
            let title = readSubElementString(start: offset + 22)
            // TPE1: offset + 144 (string at +150)
            let artist = readSubElementString(start: offset + 150)
            // TALB: offset + 272 (string at +278)
            let album = readSubElementString(start: offset + 278)
            // TCON: offset + 400 (string at +406)
            let genre = readSubElementString(start: offset + 406)
            
            if !title.isEmpty || !artist.isEmpty {
                result[trackId] = DumpedTrackMetadata(
                    id: trackId,
                    title: title.isEmpty ? "Track \(trackId)" : title,
                    artist: artist.isEmpty ? "Unknown Artist" : artist,
                    album: album.isEmpty ? "Unknown Album" : album,
                    genre: genre,
                    trackNumber: trackId
                )
            }
        }
        
        return result
    }
    
    /// Parses EA3 ID3v2 tag (first 3072 bytes) from an .OMA file
    public static func parseEA3Tag(data: Data, trackId: Int) -> DumpedTrackMetadata {
        var title = ""
        var artist = ""
        var album = ""
        var genre = ""
        var trackNumber = trackId
        
        guard data.count >= 3072, data.prefix(3) == Data("ea3".utf8) else {
            return DumpedTrackMetadata(id: trackId, title: "Track \(trackId)", artist: "Unknown Artist", album: "Unknown Album", genre: "", trackNumber: trackId)
        }
        
        let knownFrames: Set<String> = ["TIT2", "TPE1", "TALB", "TCON", "TXXX", "TYER", "TLEN"]
        var offset = 10
        
        while offset < 3060 {
            guard offset + 8 <= data.count else { break }
            let idData = data.subdata(in: offset..<(offset + 4))
            guard let frameId = String(data: idData, encoding: .ascii) else {
                offset += 1
                continue
            }
            
            if !knownFrames.contains(frameId) {
                offset += 1
                continue
            }
            
            let frameLen = (Int(data[offset + 4]) << 24) |
                           (Int(data[offset + 5]) << 16) |
                           (Int(data[offset + 6]) << 8)  |
                            Int(data[offset + 7])
            
            guard frameLen > 0, offset + 8 + 3 <= data.count else {
                offset += 1
                continue
            }
            
            let encodingByte = data[offset + 10]
            let textLen = max(0, frameLen - 1)
            let textStart = offset + 11
            let textEnd = min(textStart + textLen, data.count)
            let textBytes = data.subdata(in: textStart..<textEnd)
            
            var decoded: String? = nil
            if encodingByte == 0x02 {
                decoded = String(data: textBytes, encoding: .utf16BigEndian)
            } else if encodingByte == 0x00 {
                decoded = String(data: textBytes, encoding: .isoLatin1)
            } else if encodingByte == 0x03 {
                decoded = String(data: textBytes, encoding: .utf8)
            } else {
                decoded = String(data: textBytes, encoding: .utf16BigEndian) ?? String(data: textBytes, encoding: .utf8)
            }
            
            let cleanText = decoded?.trimmingCharacters(in: .controlCharacters) ?? ""
            
            switch frameId {
            case "TIT2":
                if !cleanText.isEmpty { title = cleanText }
            case "TPE1":
                if !cleanText.isEmpty { artist = cleanText }
            case "TALB":
                if !cleanText.isEmpty { album = cleanText }
            case "TCON":
                if !cleanText.isEmpty { genre = cleanText }
            case "TXXX":
                if cleanText.contains("OMG_TRACK") {
                    if let num = cleanText.components(separatedBy: CharacterSet.decimalDigits.inverted).filter({ !$0.isEmpty }).last,
                       let parsedNum = Int(num) {
                        trackNumber = parsedNum
                    }
                }
            default:
                break
            }
            
            // Advance past frame: 4 (ID) + 4 (Len) + 3 (Encoding code) + textLen
            offset += 11 + textLen
        }
        
        return DumpedTrackMetadata(
            id: trackId,
            title: title.isEmpty ? "Track \(trackId)" : title,
            artist: artist.isEmpty ? "Unknown Artist" : artist,
            album: album.isEmpty ? "Unknown Album" : album,
            genre: genre,
            trackNumber: trackNumber
        )
    }
    
    // MARK: - ID3v2 Construction
    
    /// Constructs a clean, compliant ID3v2.3 tag in UTF-16 with BOM for universal player compatibility
    public static func buildStandardID3v2(
        title: String,
        artist: String,
        album: String,
        trackNumber: Int,
        genre: String
    ) -> Data {
        func makeTextFrame(id: String, text: String) -> Data {
            guard !text.isEmpty, let textData = text.data(using: .utf16) else { return Data() }
            
            var frame = Data()
            frame.append(contentsOf: id.utf8.prefix(4))
            
            // Frame size = 1 (encoding byte) + textData.count
            let size = UInt32(textData.count + 1)
            frame.append(UInt8((size >> 24) & 0xFF))
            frame.append(UInt8((size >> 16) & 0xFF))
            frame.append(UInt8((size >> 8) & 0xFF))
            frame.append(UInt8(size & 0xFF))
            
            frame.append(contentsOf: [0x00, 0x00]) // Flags
            frame.append(0x01) // Encoding 1: UTF-16 with BOM
            frame.append(textData)
            return frame
        }
        
        var frames = Data()
        frames.append(makeTextFrame(id: "TIT2", text: title))
        frames.append(makeTextFrame(id: "TPE1", text: artist))
        frames.append(makeTextFrame(id: "TALB", text: album))
        if trackNumber > 0 {
            frames.append(makeTextFrame(id: "TRCK", text: "\(trackNumber)"))
        }
        if !genre.isEmpty && genre != "Unknown Genre" {
            frames.append(makeTextFrame(id: "TCON", text: genre))
        }
        
        // 10-Byte Header
        var tag = Data()
        tag.append(contentsOf: "ID3".utf8)
        tag.append(contentsOf: [0x03, 0x00, 0x00]) // v2.3.0, no flags
        
        let tagSize = frames.count
        tag.append(UInt8((tagSize >> 21) & 0x7F))
        tag.append(UInt8((tagSize >> 14) & 0x7F))
        tag.append(UInt8((tagSize >> 7) & 0x7F))
        tag.append(UInt8(tagSize & 0x7F))
        
        tag.append(frames)
        return tag
    }
    
    /// Sanitizes path components for local filesystem safety
    public static func sanitizeFilename(_ string: String) -> String {
        let invalid = CharacterSet(charactersIn: "\\/:*?\"<>|")
        var clean = string.components(separatedBy: invalid).joined(separator: "_")
        clean = clean.trimmingCharacters(in: .whitespacesAndNewlines)
        if clean.isEmpty {
            clean = "Unknown"
        }
        return clean
    }
}
