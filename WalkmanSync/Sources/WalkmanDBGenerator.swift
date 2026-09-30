import Foundation

// MARK: - Hardware-Accurate Sony OMGAUDIO Database Generator
// Generates the full suite of OMGAUDIO database files required by Sony Network Walkman firmware:
// - 00GTRLST.DAT (Group Tree List / Root index)
// - 01TREE01.DAT & 03GINF01.DAT (Track index & Album/Artist grouping)
// - 01TREE02.DAT & 03GINF02.DAT (Artist navigation index)
// - 01TREE03.DAT & 03GINF03.DAT (Album navigation index)
// - 01TREE04.DAT & 03GINF04.DAT (Genre navigation index)
// - 02TREINF.DAT (Tree Information header)
// - 04CNTINF.DAT (Content Information - track metadata)
// - 05CIDLST.DAT (Content ID List)

public class WalkmanDBGenerator {
    public var mp3Bitrate: MP3Bitrate
    public var isVBR: Bool
    private var isEncrypted3rdGen: Bool = true
    
    public init(mp3Bitrate: MP3Bitrate = .kbps192, isVBR: Bool = false, isEncrypted3rdGen: Bool = true) {
        self.mp3Bitrate = mp3Bitrate
        self.isVBR = isVBR
        self.isEncrypted3rdGen = isEncrypted3rdGen
    }
    
    public enum MP3Bitrate: String, CaseIterable {
        case kbps192 = "192" // High Quality (Recommended)
        case kbps320 = "320" // Maximum Fidelity
        case kbps256 = "256" // Very High Quality
        case kbps128 = "128" // Standard / Matches ATRAC3 LP2
        case kbps96  = "96"  // Compact / Maximum Capacity
        
        public var kbps: Int {
            return Int(self.rawValue) ?? 192
        }
        
        public var displayName: String {
            switch self {
            case .kbps192: return "192 kbps (High Quality - Recommended)"
            case .kbps320: return "320 kbps (Maximum Fidelity / Studio)"
            case .kbps256: return "256 kbps (Very High Quality)"
            case .kbps128: return "128 kbps (Standard - Matches ATRAC3 LP2)"
            case .kbps96:  return "96 kbps (Compact - Maximum Capacity)"
            }
        }
        
        public var averageMbPerSong: Double {
            switch self {
            case .kbps320: return 7.5
            case .kbps256: return 6.0
            case .kbps192: return 4.5
            case .kbps128: return 3.0
            case .kbps96:  return 2.2
            }
        }
        
        public var ffmpegBitrateFlag: String {
            return "\(self.rawValue)k"
        }
        
        public static func from(string: String) -> MP3Bitrate {
            let lower = string.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
            if lower.contains("320") {
                return .kbps320
            } else if lower.contains("256") {
                return .kbps256
            } else if lower.contains("128") {
                return .kbps128
            } else if lower.contains("96") {
                return .kbps96
            } else {
                return .kbps192
            }
        }
    }
    
    public struct WalkmanTitle {
        public var id: Int
        public var titleName: String
        public var artistName: String
        public var albumName: String
        public var genre: String
        public var length: Int // in seconds
        public var originalFile: URL?
        
        public init(id: Int, titleName: String, artistName: String, albumName: String, genre: String, length: Int, originalFile: URL? = nil) {
            self.id = id
            self.titleName = titleName.isEmpty ? "Track \(id)" : titleName
            self.artistName = artistName.isEmpty ? "Unknown Artist" : artistName
            self.albumName = albumName.isEmpty ? "Unknown Album" : albumName
            self.genre = genre.isEmpty ? "Unknown Genre" : genre
            self.length = length > 0 ? length : 180
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
        
        WalkmanLogger.info("Starting complete database generation for \(titles.count) tracks at: \(omgAudioDir.path)")
        
        // 1. Root Group Tree List
        try write00GTRLST(omgAudioDir: omgAudioDir)
        WalkmanLogger.info("Generated 00GTRLST.DAT (Root Group Tree List)")
        
        // 2. Track & Album trees (01TREE01 and 03GINF01)
        try write01TREE01and03GINF01(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 01TREE01.DAT & 03GINF01.DAT (Track & Album groups)")
        
        // 3. Artist trees (01TREE02 and 03GINF02)
        try write01TREE02and03GINF02(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 01TREE02.DAT & 03GINF02.DAT (Artist navigation)")
        
        // 4. Album trees (01TREE03 and 03GINF03)
        try write01TREE03and03GINF03(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 01TREE03.DAT & 03GINF03.DAT (Album navigation)")
        
        // 5. Genre trees (01TREE04 and 03GINF04)
        try write01TREE04and03GINF04(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 01TREE04.DAT & 03GINF04.DAT (Genre navigation)")
        
        // 5b. Tree 22 (01TREE22 and 03GINF22)
        try write01TREE22and03GINF22(omgAudioDir: omgAudioDir)
        WalkmanLogger.info("Generated 01TREE22.DAT & 03GINF22.DAT")
        
        // 5c. Tree 2D (01TREE2D and 03GINF2D - Artist/Album jog-dial navigation)
        try write01TREE2Dand03GINF2D(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 01TREE2D.DAT & 03GINF2D.DAT (Jog-dial navigation)")
        
        // 6. Tree Information (02TREINF)
        try write02TREINF(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 02TREINF.DAT (Tree Info)")
        
        // 7. Content Information (04CNTINF)
        try write04CNTINF(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 04CNTINF.DAT (Track Content metadata)")
        
        // 8. Content ID List (05CIDLST)
        try write05CIDLST(omgAudioDir: omgAudioDir, titles: titles)
        WalkmanLogger.info("Generated 05CIDLST.DAT (Content ID List)")
        
        WalkmanLogger.info("Database generation successfully finished! All 8 DAT files written.")
    }
    
    // MARK: - 1. 00GTRLST.DAT
    private func write00GTRLST(omgAudioDir: URL) throws {
        var data = Data()
        
        // Header
        data.append(writeTableHeader(tableName: "GTLT", numberOfClasses: 2))
        data.append(writeClassDescription(className: "SYSB", startAddress: 0x30, length: 0x70))
        data.append(writeClassDescription(className: "GTLB", startAddress: 0xA0, length: 0xE20))
        
        // Class 1 (SYSB)
        data.append(writeClassHeader(className: "SYSB", numberOfElements: 1, lengthOfOneElement: 0x50, complement1: 0xD0000000, complement2: 0x00000000))
        data.append(Data(count: 6 * 0x10))
        
        // Class 2 (GTLB)
        data.append(writeClassHeader(className: "GTLB", numberOfElements: 0x2D, lengthOfOneElement: 0x50, complement1: 0x00000006, complement2: 0x04000000))
        data.append(writeGTLBelement(fileRef: 1, unknown1: 1, numberOfTag: 1, tag1: "", tag2: "", unknown2: Data()))
        data.append(writeGTLBelement(fileRef: 2, unknown1: 3, numberOfTag: 1, tag1: "TPE1", tag2: "", unknown2: Data()))
        data.append(writeGTLBelement(fileRef: 3, unknown1: 3, numberOfTag: 1, tag1: "TALB", tag2: "", unknown2: Data()))
        data.append(writeGTLBelement(fileRef: 4, unknown1: 3, numberOfTag: 1, tag1: "TCON", tag2: "", unknown2: Data()))
        data.append(writeGTLBelement(fileRef: 0x22, unknown1: 2, numberOfTag: 0, tag1: "", tag2: "", unknown2: Data()))
        
        let unknown2Bytes = "TRNOTTCCTTCC".data(using: .utf8) ?? Data()
        data.append(writeGTLBelement(fileRef: 0x2D, unknown1: 3, numberOfTag: 2, tag1: "TPE1", tag2: "TALB", unknown2: unknown2Bytes))
        
        for i in 5...44 {
            if i == 34 { continue }
            data.append(writeGTLBelement(fileRef: i, unknown1: 0, numberOfTag: 0, tag1: "", tag2: "", unknown2: Data()))
        }
        
        let fileURL = omgAudioDir.appendingPathComponent("00GTRLST.DAT")
        try data.write(to: fileURL, options: .atomic)
    }
    
    // MARK: - 2. 01TREE01.DAT & 03GINF01.DAT (Titles grouped by Album/Artist)
    private func write01TREE01and03GINF01(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data11 = Data()
        var data31 = Data()
        
        // Sort titles by Artist then Album
        let sortedTitles = titles.sorted {
            if $0.artistName != $1.artistName { return $0.artistName < $1.artistName }
            if $0.albumName != $1.albumName { return $0.albumName < $1.albumName }
            return $0.id < $1.id
        }
        
        // Build Albums structure
        struct AlbumGroup {
            var name: String
            var artist: String
            var genre: String
            var totalDurationMs: Int
            var firstTrackIdx: Int
        }
        
        var albums: [AlbumGroup] = []
        var trackIdsInTPLB: [Int] = []
        
        for (idx, title) in sortedTitles.enumerated() {
            trackIdsInTPLB.append(title.id)
            let albumKey = "\(title.artistName)/\(title.albumName)"
            
            if let last = albums.last, last.name == albumKey {
                var updated = last
                updated.totalDurationMs += title.length * 1000
                albums[albums.count - 1] = updated
            } else {
                albums.append(AlbumGroup(
                    name: albumKey,
                    artist: title.artistName,
                    genre: title.genre,
                    totalDurationMs: title.length * 1000,
                    firstTrackIdx: idx + 1
                ))
            }
        }
        
        // 01TREE01 Header
        data11.append(writeTableHeader(tableName: "TREE", numberOfClasses: 2))
        data11.append(writeClassDescription(className: "GPLB", startAddress: 0x30, length: 0x4010))
        var class111Length = sortedTitles.count * 2 + 0x10
        if class111Length % 0x10 != 0 {
            class111Length += 0x10 - (class111Length % 0x10)
        }
        data11.append(writeClassDescription(className: "TPLB", startAddress: 0x4040, length: class111Length))
        
        // 03GINF01 Header
        data31.append(writeTableHeader(tableName: "GPIF", numberOfClasses: 1))
        data31.append(writeClassDescription(className: "GPFB", startAddress: 0x20, length: albums.count * 0x310 + 0x10))
        
        // 01TREE01 Class 1 (GPLB)
        data11.append(writeClassHeader(className: "GPLB", numberOfElements: albums.count, lengthOfOneElement: 0x8, complement1: UInt32(albums.count), complement2: 0))
        
        // 03GINF01 Class 1 (GPFB)
        data31.append(writeClassHeader(className: "GPFB", numberOfElements: albums.count, lengthOfOneElement: 0x310))
        
        for (i, album) in albums.enumerated() {
            data11.append(writeGPLBelement(itemId: i + 1, titleId: album.firstTrackIdx))
            data31.append(writeGPFBelement(albumKey: album.totalDurationMs, albumName: album.name, artistName: album.artist, genre: album.genre))
        }
        
        let zeros11Class1 = 0x4010 - 0x10 - (0x8 * albums.count)
        if zeros11Class1 > 0 {
            data11.append(Data(count: zeros11Class1))
        }
        
        // 01TREE01 Class 2 (TPLB)
        data11.append(writeClassHeader(className: "TPLB", numberOfElements: sortedTitles.count, lengthOfOneElement: 0x2, complement1: UInt32(sortedTitles.count), complement2: 0))
        for trackId in trackIdsInTPLB {
            data11.append(int2bytes(trackId, length: 2))
        }
        let padTPLB = 0x10 - ((sortedTitles.count * 2) % 0x10)
        if padTPLB < 0x10 {
            data11.append(Data(count: padTPLB))
        }
        
        try data11.write(to: omgAudioDir.appendingPathComponent("01TREE01.DAT"), options: .atomic)
        try data31.write(to: omgAudioDir.appendingPathComponent("03GINF01.DAT"), options: .atomic)
    }
    
    // MARK: - 3. 01TREE02.DAT & 03GINF02.DAT (Artists)
    private func write01TREE02and03GINF02(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data12 = Data()
        var data32 = Data()
        
        let sortedTitles = titles.sorted {
            if $0.artistName != $1.artistName { return $0.artistName < $1.artistName }
            return $0.id < $1.id
        }
        
        struct ArtistGroup {
            var artist: String
            var totalDurationMs: Int
            var firstTrackIdx: Int
        }
        
        var artists: [ArtistGroup] = []
        var trackIdsInTPLB: [Int] = []
        
        for (idx, title) in sortedTitles.enumerated() {
            trackIdsInTPLB.append(title.id)
            if let last = artists.last, last.artist == title.artistName {
                var updated = last
                updated.totalDurationMs += title.length * 1000
                artists[artists.count - 1] = updated
            } else {
                artists.append(ArtistGroup(
                    artist: title.artistName,
                    totalDurationMs: title.length * 1000,
                    firstTrackIdx: idx + 1
                ))
            }
        }
        
        data12.append(writeTableHeader(tableName: "TREE", numberOfClasses: 2))
        data12.append(writeClassDescription(className: "GPLB", startAddress: 0x30, length: 0x4010))
        var class121Length = sortedTitles.count * 2 + 0x10
        if class121Length % 0x10 != 0 {
            class121Length += 0x10 - (class121Length % 0x10)
        }
        data12.append(writeClassDescription(className: "TPLB", startAddress: 0x4040, length: class121Length))
        
        data32.append(writeTableHeader(tableName: "GPIF", numberOfClasses: 1))
        data32.append(writeClassDescription(className: "GPFB", startAddress: 0x20, length: artists.count * 0x90 + 0x10))
        
        data12.append(writeClassHeader(className: "GPLB", numberOfElements: artists.count, lengthOfOneElement: 0x8, complement1: UInt32(artists.count), complement2: 0))
        data32.append(writeClassHeader(className: "GPFB", numberOfElements: artists.count, lengthOfOneElement: 0x90))
        
        for (i, artist) in artists.enumerated() {
            data12.append(writeGPLBelement(itemId: i + 1, titleId: artist.firstTrackIdx))
            data32.append(writeGPFBelement(key: artist.totalDurationMs, name: artist.artist))
        }
        
        let zeros12Class1 = 0x4010 - 0x10 - (0x8 * artists.count)
        if zeros12Class1 > 0 {
            data12.append(Data(count: zeros12Class1))
        }
        
        data12.append(writeClassHeader(className: "TPLB", numberOfElements: sortedTitles.count, lengthOfOneElement: 0x2, complement1: UInt32(sortedTitles.count), complement2: 0))
        for trackId in trackIdsInTPLB {
            data12.append(int2bytes(trackId, length: 2))
        }
        let padTPLB = 0x10 - ((sortedTitles.count * 2) % 0x10)
        if padTPLB < 0x10 {
            data12.append(Data(count: padTPLB))
        }
        
        try data12.write(to: omgAudioDir.appendingPathComponent("01TREE02.DAT"), options: .atomic)
        try data32.write(to: omgAudioDir.appendingPathComponent("03GINF02.DAT"), options: .atomic)
    }
    
    // MARK: - 4. 01TREE03.DAT & 03GINF03.DAT (Albums)
    private func write01TREE03and03GINF03(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data13 = Data()
        var data33 = Data()
        
        let sortedTitles = titles.sorted {
            if $0.albumName != $1.albumName { return $0.albumName < $1.albumName }
            return $0.id < $1.id
        }
        
        struct AlbumSimpleGroup {
            var album: String
            var totalDurationMs: Int
            var firstTrackIdx: Int
        }
        
        var albums: [AlbumSimpleGroup] = []
        var trackIdsInTPLB: [Int] = []
        
        for (idx, title) in sortedTitles.enumerated() {
            trackIdsInTPLB.append(title.id)
            if let last = albums.last, last.album == title.albumName {
                var updated = last
                updated.totalDurationMs += title.length * 1000
                albums[albums.count - 1] = updated
            } else {
                albums.append(AlbumSimpleGroup(
                    album: title.albumName,
                    totalDurationMs: title.length * 1000,
                    firstTrackIdx: idx + 1
                ))
            }
        }
        
        data13.append(writeTableHeader(tableName: "TREE", numberOfClasses: 2))
        data13.append(writeClassDescription(className: "GPLB", startAddress: 0x30, length: 0x4010))
        var class131Length = sortedTitles.count * 2 + 0x10
        if class131Length % 0x10 != 0 {
            class131Length += 0x10 - (class131Length % 0x10)
        }
        data13.append(writeClassDescription(className: "TPLB", startAddress: 0x4040, length: class131Length))
        
        data33.append(writeTableHeader(tableName: "GPIF", numberOfClasses: 1))
        data33.append(writeClassDescription(className: "GPFB", startAddress: 0x20, length: albums.count * 0x90 + 0x10))
        
        data13.append(writeClassHeader(className: "GPLB", numberOfElements: albums.count, lengthOfOneElement: 0x8, complement1: UInt32(albums.count), complement2: 0))
        data33.append(writeClassHeader(className: "GPFB", numberOfElements: albums.count, lengthOfOneElement: 0x90))
        
        for (i, album) in albums.enumerated() {
            data13.append(writeGPLBelement(itemId: i + 1, titleId: album.firstTrackIdx))
            data33.append(writeGPFBelement(key: album.totalDurationMs, name: album.album))
        }
        
        let zeros13Class1 = 0x4010 - 0x10 - (0x8 * albums.count)
        if zeros13Class1 > 0 {
            data13.append(Data(count: zeros13Class1))
        }
        
        data13.append(writeClassHeader(className: "TPLB", numberOfElements: sortedTitles.count, lengthOfOneElement: 0x2, complement1: UInt32(sortedTitles.count), complement2: 0))
        for trackId in trackIdsInTPLB {
            data13.append(int2bytes(trackId, length: 2))
        }
        let padTPLB = 0x10 - ((sortedTitles.count * 2) % 0x10)
        if padTPLB < 0x10 {
            data13.append(Data(count: padTPLB))
        }
        
        try data13.write(to: omgAudioDir.appendingPathComponent("01TREE03.DAT"), options: .atomic)
        try data33.write(to: omgAudioDir.appendingPathComponent("03GINF03.DAT"), options: .atomic)
    }
    
    // MARK: - 5. 01TREE04.DAT & 03GINF04.DAT (Genres)
    private func write01TREE04and03GINF04(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data14 = Data()
        var data34 = Data()
        
        let sortedTitles = titles.sorted {
            if $0.genre != $1.genre { return $0.genre < $1.genre }
            return $0.id < $1.id
        }
        
        struct GenreGroup {
            var genre: String
            var totalDurationMs: Int
            var firstTrackIdx: Int
        }
        
        var genres: [GenreGroup] = []
        var trackIdsInTPLB: [Int] = []
        
        for (idx, title) in sortedTitles.enumerated() {
            trackIdsInTPLB.append(title.id)
            if let last = genres.last, last.genre == title.genre {
                var updated = last
                updated.totalDurationMs += title.length * 1000
                genres[genres.count - 1] = updated
            } else {
                genres.append(GenreGroup(
                    genre: title.genre,
                    totalDurationMs: title.length * 1000,
                    firstTrackIdx: idx + 1
                ))
            }
        }
        
        data14.append(writeTableHeader(tableName: "TREE", numberOfClasses: 2))
        data14.append(writeClassDescription(className: "GPLB", startAddress: 0x30, length: 0x4010))
        var class141Length = sortedTitles.count * 2 + 0x10
        if class141Length % 0x10 != 0 {
            class141Length += 0x10 - (class141Length % 0x10)
        }
        data14.append(writeClassDescription(className: "TPLB", startAddress: 0x4040, length: class141Length))
        
        data34.append(writeTableHeader(tableName: "GPIF", numberOfClasses: 1))
        data34.append(writeClassDescription(className: "GPFB", startAddress: 0x20, length: genres.count * 0x90 + 0x10))
        
        data14.append(writeClassHeader(className: "GPLB", numberOfElements: genres.count, lengthOfOneElement: 0x8, complement1: UInt32(genres.count), complement2: 0))
        data34.append(writeClassHeader(className: "GPFB", numberOfElements: genres.count, lengthOfOneElement: 0x90))
        
        for (i, g) in genres.enumerated() {
            data14.append(writeGPLBelement(itemId: i + 1, titleId: g.firstTrackIdx))
            data34.append(writeGPFBelement(key: g.totalDurationMs, name: g.genre))
        }
        
        let zeros14Class1 = 0x4010 - 0x10 - (0x8 * genres.count)
        if zeros14Class1 > 0 {
            data14.append(Data(count: zeros14Class1))
        }
        
        data14.append(writeClassHeader(className: "TPLB", numberOfElements: sortedTitles.count, lengthOfOneElement: 0x2, complement1: UInt32(sortedTitles.count), complement2: 0))
        for trackId in trackIdsInTPLB {
            data14.append(int2bytes(trackId, length: 2))
        }
        let padTPLB = 0x10 - ((sortedTitles.count * 2) % 0x10)
        if padTPLB < 0x10 {
            data14.append(Data(count: padTPLB))
        }
        
        try data14.write(to: omgAudioDir.appendingPathComponent("01TREE04.DAT"), options: .atomic)
        try data34.write(to: omgAudioDir.appendingPathComponent("03GINF04.DAT"), options: .atomic)
    }
    
    // MARK: - 5b. 01TREE22.DAT & 03GINF22.DAT
    private func write01TREE22and03GINF22(omgAudioDir: URL) throws {
        var data122 = Data()
        var data322 = Data()
        
        // Header 122
        data122.append(writeTableHeader(tableName: "TREE", numberOfClasses: 2))
        data122.append(writeClassDescription(className: "GPLB", startAddress: 0x30, length: 0x10))
        data122.append(writeClassDescription(className: "TPLB", startAddress: 0x40, length: 0x10))
        
        // Header 322
        data322.append(writeTableHeader(tableName: "GPIF", numberOfClasses: 1))
        data322.append(writeClassDescription(className: "GPFB", startAddress: 0x20, length: 0x10))
        
        // 122 Class 1 (GPLB)
        data122.append(writeClassHeader(className: "GPLB", numberOfElements: 0, lengthOfOneElement: 0x8, complement1: 0, complement2: 0))
        // 322 Class 1 (GPFB)
        data322.append(writeClassHeader(className: "GPFB", numberOfElements: 0, lengthOfOneElement: 0x310))
        // 122 Class 2 (TPLB)
        data122.append(writeClassHeader(className: "TPLB", numberOfElements: 0, lengthOfOneElement: 0x2, complement1: 0, complement2: 0))
        
        try data122.write(to: omgAudioDir.appendingPathComponent("01TREE22.DAT"), options: .atomic)
        try data322.write(to: omgAudioDir.appendingPathComponent("03GINF22.DAT"), options: .atomic)
    }
    
    // MARK: - 5c. 01TREE2D.DAT & 03GINF2D.DAT (Artist & Album Jog-Dial Navigation)
    private func write01TREE2Dand03GINF2D(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data12D = Data()
        var data32D = Data()
        
        let sortedTitles = titles.sorted {
            if $0.artistName != $1.artistName { return $0.artistName < $1.artistName }
            if $0.albumName != $1.albumName { return $0.albumName < $1.albumName }
            return $0.id < $1.id
        }
        
        var titlesIdInTPLB: [Int] = []
        var artistsSorted: [String] = []
        var albumsSorted: [String] = []
        var titleKeysSorted: [Int] = []
        var gplbElements: [Int] = []
        var albumsCounter: [Int] = []
        
        var lastArtistName = ""
        var lastAlbumName = ""
        var tempKey = 0
        var albumCounter = 0
        
        for (i, title) in sortedTitles.enumerated() {
            let artist = title.artistName.isEmpty ? "Unknown Artist" : title.artistName
            let album = title.albumName.isEmpty ? "Unknown Album" : title.albumName
            let titleLengthMs = title.length * 1000
            
            if album != lastAlbumName {
                if artist != lastArtistName {
                    artistsSorted.append(artist)
                    if !albumsSorted.isEmpty {
                        albumsCounter.append(albumCounter)
                    }
                    lastArtistName = artist
                    albumCounter = 0
                    tempKey = titleLengthMs
                }
                albumsSorted.append(album)
                gplbElements.append(i + 1)
                if !albumsSorted.isEmpty {
                    titleKeysSorted.append(tempKey)
                }
                lastAlbumName = album
                albumCounter += 1
                tempKey = titleLengthMs
            } else {
                tempKey += titleLengthMs
            }
            titlesIdInTPLB.append(title.id)
        }
        
        if !albumsSorted.isEmpty {
            titleKeysSorted.append(tempKey)
            albumsCounter.append(albumCounter)
        }
        
        let totalElements = artistsSorted.count + albumsSorted.count + 1
        
        // 01TREE2D Header
        data12D.append(writeTableHeader(tableName: "TREE", numberOfClasses: 2))
        data12D.append(writeClassDescription(className: "GPLB", startAddress: 0x30, length: 0x4010))
        var class12D1Length = titles.count * 2 + 0x10
        if class12D1Length % 0x10 != 0 {
            class12D1Length += 0x10 - (class12D1Length % 0x10)
        }
        data12D.append(writeClassDescription(className: "TPLB", startAddress: 0x4040, length: class12D1Length))
        
        // 03GINF2D Header
        data32D.append(writeTableHeader(tableName: "GPIF", numberOfClasses: 1))
        data32D.append(writeClassDescription(className: "GPFB", startAddress: 0x20, length: totalElements * 0x110 + 0x10))
        
        // 01TREE2D Class 1 (GPLB)
        data12D.append(writeClassHeader(className: "GPLB", numberOfElements: totalElements, lengthOfOneElement: 0x8, complement1: UInt32(totalElements), complement2: 0))
        
        // 03GINF2D Class 1 (GPFB)
        data32D.append(writeClassHeader(className: "GPFB", numberOfElements: totalElements, lengthOfOneElement: 0x110))
        
        // Element 0: Empty root element
        data12D.append(writeGPLBelement(itemId: 1, titleId: 0))
        data32D.append(writeGPFB2Delement(albumKey: 0, name1: "", name2: ""))
        
        var albumSortedCounter = 0
        for i in 0..<artistsSorted.count {
            let artist = artistsSorted[i]
            data12D.append(writeGPLBelement(itemId: i + albumSortedCounter + 2, titleId: 0))
            data32D.append(writeGPFB2Delement(albumKey: 0, name1: artist, name2: artist))
            
            let numAlbums = (i < albumsCounter.count) ? albumsCounter[i] : 0
            for _ in 0..<numAlbums {
                if albumSortedCounter < albumsSorted.count {
                    let album = albumsSorted[albumSortedCounter]
                    let key = (albumSortedCounter < titleKeysSorted.count) ? titleKeysSorted[albumSortedCounter] : 0
                    let firstTitleId = (albumSortedCounter < gplbElements.count) ? gplbElements[albumSortedCounter] : 1
                    
                    data12D.append(writeGPLBelement2(itemId: i + albumSortedCounter + 3, titleId: firstTitleId))
                    data32D.append(writeGPFB2Delement(albumKey: key, name1: album, name2: album))
                    albumSortedCounter += 1
                }
            }
        }
        
        // Zero pad Class 1 of 01TREE2D to 0x4010
        let padClass1 = 0x4010 - 0x10 - (0x8 * totalElements)
        if padClass1 > 0 {
            data12D.append(Data(count: padClass1))
        }
        
        // 01TREE2D Class 2 (TPLB)
        data12D.append(writeClassHeader(className: "TPLB", numberOfElements: titlesIdInTPLB.count, lengthOfOneElement: 0x2, complement1: UInt32(titlesIdInTPLB.count), complement2: 0))
        for trackId in titlesIdInTPLB {
            data12D.append(int2bytes(trackId, length: 2))
        }
        let padTPLB = 0x10 - ((titlesIdInTPLB.count * 2) % 0x10)
        if padTPLB < 0x10 {
            data12D.append(Data(count: padTPLB))
        }
        
        try data12D.write(to: omgAudioDir.appendingPathComponent("01TREE2D.DAT"), options: .atomic)
        try data32D.write(to: omgAudioDir.appendingPathComponent("03GINF2D.DAT"), options: .atomic)
    }
    private func write02TREINF(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data = Data()
        data.append(writeTableHeader(tableName: "GTIF", numberOfClasses: 1))
        data.append(writeClassDescription(className: "GTFB", startAddress: 0x20, length: 0x1F00))
        data.append(writeClassHeader(className: "GTFB", numberOfElements: 0x2D, lengthOfOneElement: 0x90))
        
        let totalMs = titles.reduce(0) { $0 + ($1.length * 1000) }
        
        // Elements 1 to 4
        for _ in 1...4 {
            data.append(writeGTFBelement(key: totalMs, text: ""))
        }
        // Elements 5 to 0x21 (33 elements = (0x21 - 5 + 1) = 29 elements of 0x90 zeros)
        data.append(Data(count: (0x21 - 5 + 1) * 0x90))
        
        // Element 0x22
        data.append(writeGTFBelement(key: 0, text: ""))
        
        // Elements 0x23 to 0x2C
        data.append(Data(count: (0x2C - 0x23 + 1) * 0x90))
        
        // Element 0x2D
        data.append(writeGTFBelement(key: totalMs, text: "STD_TPE1"))
        
        // Fill class with zeros
        let remaining = 0x1F00 - (0x2D * 0x90) - 0x10
        if remaining > 0 {
            data.append(Data(count: remaining))
        }
        
        try data.write(to: omgAudioDir.appendingPathComponent("02TREINF.DAT"), options: .atomic)
    }
    
    // MARK: - 7. 04CNTINF.DAT
    private func write04CNTINF(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data = Data()
        data.append(writeTableHeader(tableName: "CNIF", numberOfClasses: 1))
        
        let maxValue = titles.map { $0.id }.max() ?? 0
        let elementSize = 0x290
        let tableSize = (maxValue * elementSize) + 0x10
        
        data.append(writeClassDescription(className: "CNFB", startAddress: 0x20, length: tableSize))
        data.append(writeClassHeader(className: "CNFB", numberOfElements: maxValue, lengthOfOneElement: elementSize))
        
        if maxValue > 0 {
            for id in 1...maxValue {
                if let title = titles.first(where: { $0.id == id }) {
                    data.append(writeCNFBelement(title: title))
                } else {
                    data.append(writeCNFBelement(title: nil))
                }
            }
        }
        
        try data.write(to: omgAudioDir.appendingPathComponent("04CNTINF.DAT"), options: .atomic)
    }
    
    // MARK: - 8. 05CIDLST.DAT
    private func write05CIDLST(omgAudioDir: URL, titles: [WalkmanTitle]) throws {
        var data = Data()
        data.append(writeTableHeader(tableName: "CIDL", numberOfClasses: 1))
        
        let maxValue = titles.map { $0.id }.max() ?? 0
        let elementSize = 0x30
        let tableSize = (maxValue * elementSize) + 0x10
        
        data.append(writeClassDescription(className: "CILB", startAddress: 0x20, length: tableSize))
        data.append(writeClassHeader(className: "CILB", numberOfElements: maxValue, lengthOfOneElement: elementSize))
        
        // 48 bytes of zeros per track
        if maxValue > 0 {
            data.append(Data(count: maxValue * elementSize))
        }
        
        try data.write(to: omgAudioDir.appendingPathComponent("05CIDLST.DAT"), options: .atomic)
    }
    
    // MARK: - Binary Helper Methods
    
    private func writeTableHeader(tableName: String, numberOfClasses: Int) -> Data {
        var d = Data()
        d.append(contentsOf: tableName.utf8.prefix(4))
        d.append(contentsOf: [0x01, 0x01, 0x00, 0x00])
        d.append(UInt8(numberOfClasses & 0xFF))
        d.append(Data(count: 7))
        return d
    }
    
    private func writeClassDescription(className: String, startAddress: Int, length: Int) -> Data {
        var d = Data()
        d.append(contentsOf: className.utf8.prefix(4))
        d.append(int2bytes(startAddress, length: 4))
        d.append(int2bytes(length, length: 4))
        d.append(Data(count: 4))
        return d
    }
    
    private func writeClassHeader(className: String, numberOfElements: Int, lengthOfOneElement: Int) -> Data {
        var d = Data()
        d.append(contentsOf: className.utf8.prefix(4))
        d.append(int2bytes(numberOfElements, length: 2))
        d.append(int2bytes(lengthOfOneElement, length: 2))
        d.append(Data(count: 8))
        return d
    }
    
    private func writeClassHeader(className: String, numberOfElements: Int, lengthOfOneElement: Int, complement1: UInt32, complement2: UInt32) -> Data {
        var d = Data()
        d.append(contentsOf: className.utf8.prefix(4))
        d.append(int2bytes(numberOfElements, length: 2))
        d.append(int2bytes(lengthOfOneElement, length: 2))
        d.append(UInt8((complement1 >> 24) & 0xFF))
        d.append(UInt8((complement1 >> 16) & 0xFF))
        d.append(UInt8((complement1 >> 8) & 0xFF))
        d.append(UInt8(complement1 & 0xFF))
        d.append(UInt8((complement2 >> 24) & 0xFF))
        d.append(UInt8((complement2 >> 16) & 0xFF))
        d.append(UInt8((complement2 >> 8) & 0xFF))
        d.append(UInt8(complement2 & 0xFF))
        return d
    }
    
    private func writeGTLBelement(fileRef: Int, unknown1: Int, numberOfTag: Int, tag1: String, tag2: String, unknown2: Data) -> Data {
        var d = Data()
        d.append(int2bytes(fileRef, length: 2))
        d.append(int2bytes(unknown1, length: 2))
        d.append(Data(count: 12))
        d.append(int2bytes(numberOfTag, length: 2))
        d.append(Data(count: 2))
        
        // tag1 (4 bytes)
        var t1 = Data(tag1.utf8.prefix(4))
        if t1.count < 4 { t1.append(Data(count: 4 - t1.count)) }
        d.append(t1)
        
        // tag2 (4 bytes)
        var t2 = Data(tag2.utf8.prefix(4))
        if t2.count < 4 { t2.append(Data(count: 4 - t2.count)) }
        d.append(t2)
        
        d.append(Data(count: 20))
        d.append(unknown2)
        
        let remaining = 0x50 - d.count
        if remaining > 0 {
            d.append(Data(count: remaining))
        }
        return d
    }
    
    private func writeGPLBelement(itemId: Int, titleId: Int) -> Data {
        var d = Data()
        d.append(int2bytes(itemId, length: 2))
        d.append(contentsOf: [0x01, 0x00])
        d.append(int2bytes(titleId, length: 2))
        d.append(Data(count: 2))
        return d
    }
    
    private func writeGPLBelement2(itemId: Int, titleId: Int) -> Data {
        var d = Data()
        d.append(int2bytes(itemId, length: 2))
        d.append(contentsOf: [0x02, 0x00])
        d.append(int2bytes(titleId, length: 2))
        d.append(Data(count: 2))
        return d
    }
    
    private func writeGPFB2Delement(albumKey: Int, name1: String, name2: String) -> Data {
        var d = Data()
        d.append(Data(count: 8))
        d.append(int2bytes(albumKey, length: 4))
        d.append(contentsOf: [0x00, 0x02, 0x00, 0x80])
        
        let constant2: [UInt8] = [0x00, 0x02]
        appendSubElement(&d, tag: "TIT2", text: name1, constant: constant2)
        appendSubElement(&d, tag: "XSOT", text: name2, constant: constant2)
        return d
    }
    
    private func writeGPFBelement(albumKey: Int, albumName: String, artistName: String, genre: String) -> Data {
        var d = Data()
        d.append(Data(count: 8))
        d.append(int2bytes(albumKey, length: 4))
        d.append(contentsOf: [0x00, 0x06, 0x00, 0x80])
        
        let constant2: [UInt8] = [0x00, 0x02]
        appendSubElement(&d, tag: "TIT2", text: albumName, constant: constant2)
        appendSubElement(&d, tag: "TPE1", text: artistName, constant: constant2)
        appendSubElement(&d, tag: "TCON", text: genre, constant: constant2)
        appendEmptySubElement(&d, tag: "TSOP", constant: constant2)
        appendEmptySubElement(&d, tag: "PICP", constant: constant2)
        appendEmptySubElement(&d, tag: "PIC0", constant: constant2)
        
        return d
    }
    
    private func writeGPFBelement(key: Int, name: String) -> Data {
        var d = Data()
        d.append(Data(count: 8))
        d.append(int2bytes(key, length: 4))
        d.append(contentsOf: [0x00, 0x01, 0x00, 0x80])
        
        let constant2: [UInt8] = [0x00, 0x02]
        appendSubElement(&d, tag: "TIT2", text: name, constant: constant2)
        
        return d
    }
    
    private func writeGTFBelement(key: Int, text: String) -> Data {
        var d = Data()
        d.append(Data(count: 8))
        d.append(int2bytes(key, length: 4))
        d.append(contentsOf: [0x00, 0x01, 0x00, 0x80])
        
        let constant2: [UInt8] = [0x00, 0x02]
        appendSubElement(&d, tag: "TIT2", text: text, constant: constant2)
        return d
    }
    
    private func writeCNFBelement(title: WalkmanTitle?) -> Data {
        var d = Data()
        let constant1: [UInt8] = [0x00, 0x05, 0x00, 0x80]
        let constant2: [UInt8] = [0x00, 0x02]
        
        if let t = title {
            d.append(contentsOf: [0x00, 0x00])
            d.append(contentsOf: [0xFF, 0xFE]) // 3rd Gen OpenMG MP3 Hardware Scrambled
            
            // File properties (4 bytes):
            // Byte 0: 0x03 (MP3 format)
            // Byte 1: 0x90 for VBR, 0x80 for CBR
            // Byte 2: 0xD9 (MPEG-1 Layer 3)
            // Byte 3: 0x10 (Stereo)
            let vbrFlag: UInt8 = isVBR ? 0x90 : 0x80
            d.append(contentsOf: [0x03, vbrFlag, 0xD9, 0x10])
            d.append(int2bytes(t.length * 1000, length: 4))
            d.append(contentsOf: constant1)
            
            appendSubElement(&d, tag: "TIT2", text: t.titleName, constant: constant2)
            appendSubElement(&d, tag: "TPE1", text: t.artistName, constant: constant2)
            appendSubElement(&d, tag: "TALB", text: t.albumName, constant: constant2)
            appendSubElement(&d, tag: "TCON", text: t.genre, constant: constant2)
            appendEmptySubElement(&d, tag: "TSOP", constant: constant2)
        } else {
            d.append(Data(count: 12))
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
        
        let cleanText = String(text.prefix(59))
        let utf16Data = cleanText.data(using: .utf16BigEndian) ?? Data()
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
