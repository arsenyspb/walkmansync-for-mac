import Foundation
import AVFoundation

public class SyncEngine {
    
    public static let supportedAudioExtensions: Set<String> = [
        "mp3", "m4a", "flac", "wav", "aiff", "aif", "aac", "alac", "ogg"
    ]
    
    /// Finds FFmpeg executable on the host system
    public static func findFFmpeg() -> String? {
        let standardPaths = [
            "/opt/homebrew/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "/usr/bin/ffmpeg",
            "/bin/ffmpeg"
        ]
        for path in standardPaths {
            if FileManager.default.isExecutableFile(atPath: path) {
                return path
            }
        }
        
        let p = Process()
        p.executableURL = URL(fileURLWithPath: "/usr/bin/which")
        p.arguments = ["ffmpeg"]
        let pipe = Pipe()
        p.standardOutput = pipe
        try? p.run()
        p.waitUntilExit()
        if p.terminationStatus == 0 {
            let out = String(data: pipe.fileHandleForReading.readDataToEndOfFile(), encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !out.isEmpty && FileManager.default.isExecutableFile(atPath: out) {
                return out
            }
        }
        return nil
    }
    
    /// Scans directory for supported audio files (MP3, FLAC, M4A, WAV, etc.) and extracts metadata
    public static func scanForMusic(in folder: URL) -> [WalkmanDBGenerator.WalkmanTitle] {
        var titles: [WalkmanDBGenerator.WalkmanTitle] = []
        let fm = FileManager.default
        var idCounter = 1
        
        guard let enumerator = fm.enumerator(at: folder, includingPropertiesForKeys: [.isRegularFileKey]) else {
            return titles
        }
        
        for case let fileURL as URL in enumerator {
            let ext = fileURL.pathExtension.lowercased()
            if supportedAudioExtensions.contains(ext) {
                let asset = AVAsset(url: fileURL)
                
                var titleName = fileURL.deletingPathExtension().lastPathComponent
                var artistName = "Unknown Artist"
                var albumName = "Unknown Album"
                var genre = "Unknown Genre"
                
                let metadata = asset.metadata
                for item in metadata {
                    let key = item.commonKey?.rawValue ?? (item.key as? String) ?? ""
                    let value = item.stringValue ?? ""
                    if value.isEmpty { continue }
                    
                    let lowerKey = key.lowercased()
                    if lowerKey.contains("title") || key == AVMetadataKey.commonKeyTitle.rawValue {
                        titleName = value
                    } else if lowerKey.contains("artist") || key == AVMetadataKey.commonKeyArtist.rawValue {
                        artistName = value
                    } else if lowerKey.contains("album") || key == AVMetadataKey.commonKeyAlbumName.rawValue {
                        albumName = value
                    } else if lowerKey.contains("genre") || key == AVMetadataKey.commonKeyType.rawValue {
                        genre = value
                    }
                }
                
                let duration = CMTimeGetSeconds(asset.duration)
                let lengthInSeconds = (duration > 0 && !duration.isNaN) ? Int(duration) : 180
                
                let wTitle = WalkmanDBGenerator.WalkmanTitle(
                    id: idCounter,
                    titleName: titleName,
                    artistName: artistName,
                    albumName: albumName,
                    genre: genre,
                    length: lengthInSeconds,
                    originalFile: fileURL
                )
                
                titles.append(wTitle)
                idCounter += 1
            }
        }
        
        return titles
    }
    
    /// Transfers files to Walkman:
    /// Auto-transcodes non-MP3s (FLAC, M4A, etc.) to 320kbps MP3 on-the-fly via FFmpeg,
    /// resolves/generates DvID.DAT, wraps each track into encrypted .OMA, and places in OMGAUDIO/10Fxx/
    public static func transferFilesToWalkman(
        titles: [WalkmanDBGenerator.WalkmanTitle],
        destination: URL,
        progressCallback: ((String) -> Void)? = nil
    ) throws {
        WalkmanLogger.info("Starting file transfer to Walkman at \(destination.path)")
        let fm = FileManager.default
        let omgAudioDir = destination.appendingPathComponent("OMGAUDIO", isDirectory: true)
        try fm.createDirectory(at: omgAudioDir, withIntermediateDirectories: true, attributes: nil)
        
        // 1. Resolve or generate device encryption key in MP3FM/DvID.DAT
        let deviceKey = WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: destination)
        WalkmanLogger.info("Resolved Device Key: 0x\(String(format: "%08X", deviceKey))")
        let ffmpegPath = findFFmpeg()
        
        // 2. Process each track into 10Fxx/1000xxxx.OMA
        for (index, title) in titles.enumerated() {
            guard let sourceURL = title.originalFile else { continue }
            
            let ext = sourceURL.pathExtension.lowercased()
            var mp3URL = sourceURL
            var isTempMP3 = false
            
            if ext != "mp3" {
                guard let ffmpeg = ffmpegPath else {
                    WalkmanLogger.error("FFmpeg missing for transcoding \(title.titleName).\(ext)")
                    throw NSError(
                        domain: "WalkmanSync",
                        code: 1,
                        userInfo: [NSLocalizedDescriptionKey: "Track '\(title.titleName)' is .\(ext). Sony Walkman NW-E40x only plays MP3. Please install FFmpeg (brew install ffmpeg) to enable auto-conversion."]
                    )
                }
                
                progressCallback?("[\(index + 1)/\(titles.count)] Converting \(title.titleName) (\(ext.uppercased()) → MP3)...")
                WalkmanLogger.info("[\(index + 1)/\(titles.count)] Transcoding \(sourceURL.lastPathComponent) via FFmpeg")
                let tempDir = fm.temporaryDirectory.appendingPathComponent("WalkmanSyncTemp", isDirectory: true)
                try fm.createDirectory(at: tempDir, withIntermediateDirectories: true, attributes: nil)
                let tempFile = tempDir.appendingPathComponent("\(UUID().uuidString).mp3")
                
                let process = Process()
                process.executableURL = URL(fileURLWithPath: ffmpeg)
                process.arguments = ["-y", "-i", sourceURL.path, "-vn", "-c:a", "libmp3lame", "-b:a", "320k", tempFile.path]
                process.standardOutput = Pipe()
                process.standardError = Pipe()
                try process.run()
                process.waitUntilExit()
                
                guard process.terminationStatus == 0 && fm.fileExists(atPath: tempFile.path) else {
                    WalkmanLogger.error("FFmpeg failed to transcode \(sourceURL.path)")
                    throw NSError(
                        domain: "WalkmanSync",
                        code: 2,
                        userInfo: [NSLocalizedDescriptionKey: "FFmpeg conversion failed for '\(title.titleName)'."]
                    )
                }
                
                mp3URL = tempFile
                isTempMP3 = true
            }
            
            defer {
                if isTempMP3 {
                    try? fm.removeItem(at: mp3URL)
                }
            }
            
            let dirIndex = title.id / 256
            let dirName = String(format: "10F%02X", dirIndex)
            let trackDir = omgAudioDir.appendingPathComponent(dirName, isDirectory: true)
            try fm.createDirectory(at: trackDir, withIntermediateDirectories: true, attributes: nil)
            
            let fileName = String(format: "1000%04X.OMA", title.id)
            let destOMAURL = trackDir.appendingPathComponent(fileName)
            
            progressCallback?("[\(index + 1)/\(titles.count)] Encrypting & writing \(title.titleName)...")
            WalkmanLogger.info("Encrypting OMA Track #\(title.id): \(title.titleName) -> \(dirName)/\(fileName)")
            let omaData = try OMAContainerBuilder.createEncryptedOMA(
                title: title,
                sourceMP3URL: mp3URL,
                deviceKey: deviceKey
            )
            
            try omaData.write(to: destOMAURL)
            WalkmanLogger.info("Wrote \(dirName)/\(fileName) (\(omaData.count) bytes)")
        }
        
        // Clean AppleDouble (._*) files and sync disk buffers
        cleanAppleDouble(at: destination)
        sync()
    }
    
    /// Cleans macOS AppleDouble dot-underscore files (._*) which confuse legacy embedded hardware
    public static func cleanAppleDouble(at url: URL) {
        let p = Process()
        p.executableURL = URL(fileURLWithPath: "/usr/sbin/dot_clean")
        p.arguments = ["-m", url.path]
        try? p.run()
        p.waitUntilExit()
    }
}
