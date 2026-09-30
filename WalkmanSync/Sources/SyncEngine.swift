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
    
    /// Locates atracdenc encoder binary (bundled in App, Homebrew, or system PATH)
    public static func findAtracdenc() -> String? {
        if let bundlePath = Bundle.main.path(forResource: "atracdenc", ofType: nil), FileManager.default.isExecutableFile(atPath: bundlePath) {
            return bundlePath
        }
        let execURL = URL(fileURLWithPath: CommandLine.arguments[0])
        let resURL = execURL.deletingLastPathComponent().deletingLastPathComponent().appendingPathComponent("Resources/atracdenc")
        if FileManager.default.isExecutableFile(atPath: resURL.path) {
            return resURL.path
        }
        if FileManager.default.isExecutableFile(atPath: "/opt/homebrew/bin/atracdenc") {
            return "/opt/homebrew/bin/atracdenc"
        }
        if FileManager.default.isExecutableFile(atPath: "/usr/local/bin/atracdenc") {
            return "/usr/local/bin/atracdenc"
        }
        let p = Process()
        p.executableURL = URL(fileURLWithPath: "/usr/bin/which")
        p.arguments = ["atracdenc"]
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
    
    /// Discovers all mounted Sony Walkman devices across /Volumes, system mounts, and custom paths
    public static func findAllWalkmanVolumes() -> [URL] {
        let fm = FileManager.default
        var discovered: [URL] = []
        var checkedPaths = Set<String>()
        
        func evaluate(url: URL) {
            let normalized = url.resolvingSymlinksInPath().standardized.path
            guard !checkedPaths.contains(normalized) else { return }
            checkedPaths.insert(normalized)
            
            let name = url.lastPathComponent.uppercased()
            let hasOmg = fm.fileExists(atPath: url.appendingPathComponent("OMGAUDIO").path)
            let hasMp3fm = fm.fileExists(atPath: url.appendingPathComponent("MP3FM").path)
            let hasNwwm = fm.fileExists(atPath: url.appendingPathComponent("NWWM").path)
            
            if name == "WALKMAN" || name == "SONY" || hasOmg || hasMp3fm || hasNwwm {
                discovered.append(url)
            }
        }
        
        // 1. Check /Volumes
        let volumesURL = URL(fileURLWithPath: "/Volumes")
        if let volumes = try? fm.contentsOfDirectory(at: volumesURL, includingPropertiesForKeys: nil) {
            for vol in volumes { evaluate(url: vol) }
        }
        
        // 2. Check mounted volume URLs from OS
        if let mounted = fm.mountedVolumeURLs(includingResourceValuesForKeys: nil, options: .skipHiddenVolumes) {
            for vol in mounted { evaluate(url: vol) }
        }
        
        // 3. Check common fallbacks (/tmp/walkman, /tmp/WALKMAN)
        for fallback in ["/tmp/walkman", "/tmp/WALKMAN"] {
            let u = URL(fileURLWithPath: fallback)
            if fm.fileExists(atPath: u.path) {
                evaluate(url: u)
            }
        }
        
        return discovered
    }
    
    public static func findWalkmanVolume() -> URL? {
        return findAllWalkmanVolumes().first
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
    
    /// Transfers files to Walkman with selectable audio encoder (ATRAC3, ATRAC3plus, or MP3):
    public static func transferFilesToWalkman(
        titles: [WalkmanDBGenerator.WalkmanTitle],
        destination: URL,
        codec: WalkmanDBGenerator.AudioCodec = .atrac3,
        progressCallback: ((String) -> Void)? = nil
    ) throws {
        // Prevent macOS idle system sleep during audio transcoding and USB transfer
        let sleepAssertion = ProcessInfo.processInfo.beginActivity(
            options: [.idleSystemSleepDisabled, .suddenTerminationDisabled, .userInitiated],
            reason: "Syncing and encoding audio tracks to Sony Walkman"
        )
        defer {
            ProcessInfo.processInfo.endActivity(sleepAssertion)
        }

        WalkmanLogger.info("Starting file transfer to Walkman at \(destination.path) using codec \(codec.rawValue) (\(ProcessInfo.processInfo.activeProcessorCount) CPU cores active)")
        let fm = FileManager.default
        let omgAudioDir = destination.appendingPathComponent("OMGAUDIO", isDirectory: true)
        try fm.createDirectory(at: omgAudioDir, withIntermediateDirectories: true, attributes: nil)
        
        let deviceKey = WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: destination)
        let ffmpegPath = findFFmpeg()
        let atracdencPath = findAtracdenc()
        
        let tempDir = fm.temporaryDirectory.appendingPathComponent("WalkmanSyncTemp", isDirectory: true)
        try fm.createDirectory(at: tempDir, withIntermediateDirectories: true, attributes: nil)
        
        for (index, title) in titles.enumerated() {
            guard let sourceURL = title.originalFile else { continue }
            
            let dirIndex = title.id / 256
            let dirName = String(format: "10F%02X", dirIndex)
            let trackDir = omgAudioDir.appendingPathComponent(dirName, isDirectory: true)
            try fm.createDirectory(at: trackDir, withIntermediateDirectories: true, attributes: nil)
            
            let fileName = String(format: "1000%04X.OMA", title.id)
            let destOMAURL = trackDir.appendingPathComponent(fileName)
            
            if codec == .atrac3 || codec == .atrac3_lp4 || codec == .atrac3plus {
                guard let atracdenc = atracdencPath else {
                    WalkmanLogger.error("atracdenc encoder not found")
                    throw NSError(
                        domain: "WalkmanSync",
                        code: 3,
                        userInfo: [NSLocalizedDescriptionKey: "atracdenc encoder is missing. Please ensure atracdenc is installed at /opt/homebrew/bin/atracdenc or bundled with WalkmanSync."]
                    )
                }
                guard let ffmpeg = ffmpegPath else {
                    WalkmanLogger.error("FFmpeg not found for ATRAC PCM preprocessing")
                    throw NSError(
                        domain: "WalkmanSync",
                        code: 1,
                        userInfo: [NSLocalizedDescriptionKey: "FFmpeg is required to prepare audio for ATRAC encoding. Please install via 'brew install ffmpeg'."]
                    )
                }
                
                let codecName = codec.displayName
                let trackDurationStr = String(format: "%d:%02d", title.length / 60, title.length % 60)
                let trackStartTime = Date()
                progressCallback?("[\(index + 1)/\(titles.count)] Preprocessing PCM: \(title.titleName)...")
                WalkmanLogger.info("[\(index + 1)/\(titles.count)] ATRAC PCM decode: \(sourceURL.lastPathComponent) [\(trackDurationStr)]")
                
                let tempWav = tempDir.appendingPathComponent("\(UUID().uuidString).wav")
                let tempOma = tempDir.appendingPathComponent("\(UUID().uuidString).oma")
                defer {
                    try? fm.removeItem(at: tempWav)
                    try? fm.removeItem(at: tempOma)
                }
                
                // 1. Convert source to 44.1kHz 16-bit Stereo PCM WAV via FFmpeg
                let ffProc = Process()
                ffProc.executableURL = URL(fileURLWithPath: ffmpeg)
                ffProc.arguments = ["-y", "-i", sourceURL.path, "-vn", "-ac", "2", "-ar", "44100", tempWav.path]
                ffProc.standardOutput = FileHandle.nullDevice
                ffProc.standardError = FileHandle.nullDevice
                try ffProc.run()
                ffProc.waitUntilExit()
                
                guard ffProc.terminationStatus == 0 && fm.fileExists(atPath: tempWav.path) else {
                    throw NSError(domain: "WalkmanSync", code: 4, userInfo: [NSLocalizedDescriptionKey: "Failed to decode '\(title.titleName)' to PCM WAV."])
                }
                
                // 2. Encode WAV to ATRAC OMA via atracdenc with live percentage feedback
                progressCallback?("[\(index + 1)/\(titles.count)] Encoding \(codecName): \(title.titleName) (0%)...")
                WalkmanLogger.info("[\(index + 1)/\(titles.count)] Encoding \(codecName) via atracdenc...")
                let atracProc = Process()
                atracProc.executableURL = URL(fileURLWithPath: atracdenc)
                let atracCodecFlag: String
                switch codec {
                case .atrac3_lp4: atracCodecFlag = "atrac3_lp"
                case .atrac3plus: atracCodecFlag = "atrac3plus"
                default: atracCodecFlag = "atrac3"
                }
                atracProc.arguments = ["-e", atracCodecFlag, "--container", "oma", "-i", tempWav.path, "-o", tempOma.path]
                
                // Drain stdout continuously to prevent 64KB kernel pipe deadlock & provide live %
                let atracOutPipe = Pipe()
                atracProc.standardOutput = atracOutPipe
                atracProc.standardError = FileHandle.nullDevice
                
                atracOutPipe.fileHandleForReading.readabilityHandler = { handle in
                    let data = handle.availableData
                    guard !data.isEmpty, let output = String(data: data, encoding: .utf8) else { return }
                    if let match = output.range(of: #"[0-9]+%"#, options: .regularExpression) {
                        let pct = String(output[match])
                        DispatchQueue.main.async {
                            progressCallback?("[\(index + 1)/\(titles.count)] Encoding \(codecName): \(title.titleName) (\(pct))...")
                        }
                    }
                }
                
                try atracProc.run()
                atracProc.waitUntilExit()
                atracOutPipe.fileHandleForReading.readabilityHandler = nil
                
                guard atracProc.terminationStatus == 0 && fm.fileExists(atPath: tempOma.path) else {
                    throw NSError(domain: "WalkmanSync", code: 5, userInfo: [NSLocalizedDescriptionKey: "atracdenc encoding failed for '\(title.titleName)'."])
                }
                
                // 3. Prepend Sony EA3 metadata tag to OMA
                progressCallback?("[\(index + 1)/\(titles.count)] Writing: \(title.titleName) (\(dirName)/\(fileName))...")
                let fullOMA = try OMAContainerBuilder.createAtracOMA(title: title, atracOmaURL: tempOma)
                try fullOMA.write(to: destOMAURL)
                
                let trackElapsed = Date().timeIntervalSince(trackStartTime)
                let speedMultiplier = title.length > 0 ? (Double(title.length) / max(0.1, trackElapsed)) : 1.0
                WalkmanLogger.info("Wrote ATRAC \(dirName)/\(fileName) (\(fullOMA.count) bytes) in \(String(format: "%.1f", trackElapsed))s (\(String(format: "%.1f", speedMultiplier))x real-time)")
                
            } else {
                // MP3 Pipeline
                let ext = sourceURL.pathExtension.lowercased()
                var mp3URL = sourceURL
                var isTempMP3 = false
                
                if ext != "mp3" {
                    guard let ffmpeg = ffmpegPath else {
                        throw NSError(
                            domain: "WalkmanSync",
                            code: 1,
                            userInfo: [NSLocalizedDescriptionKey: "FFmpeg is required to convert .\(ext) to MP3."]
                        )
                    }
                    
                    progressCallback?("[\(index + 1)/\(titles.count)] Converting \(title.titleName) (\(ext.uppercased()) → MP3)...")
                    let tempFile = tempDir.appendingPathComponent("\(UUID().uuidString).mp3")
                    
                    let process = Process()
                    process.executableURL = URL(fileURLWithPath: ffmpeg)
                    process.arguments = ["-y", "-i", sourceURL.path, "-vn", "-c:a", "libmp3lame", "-b:a", "320k", tempFile.path]
                    process.standardOutput = FileHandle.nullDevice
                    process.standardError = FileHandle.nullDevice
                    try process.run()
                    process.waitUntilExit()
                    
                    guard process.terminationStatus == 0 && fm.fileExists(atPath: tempFile.path) else {
                        throw NSError(domain: "WalkmanSync", code: 2, userInfo: [NSLocalizedDescriptionKey: "FFmpeg conversion failed for '\(title.titleName)'."])
                    }
                    mp3URL = tempFile
                    isTempMP3 = true
                }
                
                defer {
                    if isTempMP3 {
                        try? fm.removeItem(at: mp3URL)
                    }
                }
                
                progressCallback?("[\(index + 1)/\(titles.count)] Scrambling & writing \(title.titleName)...")
                let omaData = try OMAContainerBuilder.createEncryptedOMA(
                    title: title,
                    sourceMP3URL: mp3URL,
                    deviceKey: deviceKey
                )
                try omaData.write(to: destOMAURL)
                WalkmanLogger.info("Wrote MP3 \(dirName)/\(fileName) (\(omaData.count) bytes)")
            }
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
        p.standardError = FileHandle.nullDevice
        p.standardOutput = FileHandle.nullDevice
        try? p.run()
        p.waitUntilExit()
    }
}
