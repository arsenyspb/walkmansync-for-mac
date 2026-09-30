import Foundation

public class CLIHandler {
    public static let version = "0.3.1"
    
    public static func shouldHandleCLI() -> Bool {
        let args = CommandLine.arguments.dropFirst().filter { !$0.starts(with: "-psn_") }
        return !args.isEmpty
    }
    
    public static func run() {
        let args = Array(CommandLine.arguments.dropFirst().filter { !$0.starts(with: "-psn_") })
        
        var isJSON = false
        var isDryRun = false
        var isVerbose = false
        var isForce = false
        var sourcePath: String?
        var dumpPath: String?
        var walkmanPath: String?
        var scanPath: String?
        var cacheDirPath: String?
        var selectedBitrate: WalkmanDBGenerator.MP3Bitrate = .kbps192
        var isVBR = false
        var action: Action = .none
        
        enum Action {
            case none
            case help
            case version
            case detect
            case doctor
            case scan
            case sync
            case clean
            case erase
            case dump
            case extractKey
            case extractKeyInternal
            case checkUpdate
        }
        
        var i = 0
        while i < args.count {
            let arg = args[i]
            switch arg {
            case "--help", "-h":
                action = .help
            case "--version", "-v":
                action = .version
            case "--doctor", "-D":
                action = .doctor
            case "--detect", "-d":
                action = .detect
            case "--scan":
                action = .scan
                if i + 1 < args.count && !args[i + 1].starts(with: "-") {
                    i += 1
                    scanPath = args[i]
                }
            case "--sync":
                action = .sync
            case "--clean":
                action = .clean
            case "--erase", "--wipe":
                action = .erase
            case "--dump":
                action = .dump
                if i + 1 < args.count && !args[i + 1].starts(with: "-") {
                    i += 1
                    dumpPath = args[i]
                }
            case "--force", "-f", "-y":
                isForce = true
            case "--check-update", "-u":
                action = .checkUpdate
            case "--extract-key", "-k":
                action = .extractKey
            case "--extract-key-internal":
                action = .extractKeyInternal
            case "--cache-dir":
                if i + 1 < args.count {
                    i += 1
                    cacheDirPath = args[i]
                }
            case "--source", "-s":
                if i + 1 < args.count {
                    i += 1
                    sourcePath = args[i]
                }
            case "--walkman", "-w", "--dest":
                if i + 1 < args.count {
                    i += 1
                    walkmanPath = args[i]
                }
            case "--bitrate", "-b":
                if i + 1 < args.count {
                    i += 1
                    selectedBitrate = WalkmanDBGenerator.MP3Bitrate.from(string: args[i])
                }
            case "--vbr":
                isVBR = true
            case "--codec", "-c":
                if i + 1 < args.count {
                    i += 1
                    let c = args[i].lowercased()
                    if c.contains("320") {
                        selectedBitrate = .kbps320
                    } else if c.contains("256") {
                        selectedBitrate = .kbps256
                    } else if c.contains("128") || c.contains("atrac3") || c.contains("lp2") {
                        selectedBitrate = .kbps128
                    } else if c.contains("96") || c.contains("lp4") || c.contains("66") {
                        selectedBitrate = .kbps96
                    } else {
                        selectedBitrate = .kbps192
                    }
                }
            case "--json":
                isJSON = true
                WalkmanLogger.silenceStdout = true
            case "--dry-run":
                isDryRun = true
            case "--verbose":
                isVerbose = true
            default:
                if arg.starts(with: "-") {
                    printError("Unknown option: \(arg). Run with --help for usage.")
                    exit(1)
                } else if sourcePath == nil {
                    sourcePath = arg
                }
            }
            i += 1
        }
        
        if isVerbose {
            WalkmanLogger.info("CLI invoked with args: \(args.joined(separator: " "))")
        }
        
        // If sourcePath provided without explicit action, default to sync
        if action == .none {
            if sourcePath != nil {
                action = .sync
            } else {
                action = .help
            }
        }
        
        switch action {
        case .help:
            printUsage()
            exit(0)
            
        case .version:
            if isJSON {
                print("{\"name\":\"WalkmanSync\",\"version\":\"\(version)\"}")
            } else {
                print("WalkmanSync version \(version) (Native macOS SonicStage alternative)")
            }
            exit(0)
            
        case .detect:
            handleDetect(explicitPath: walkmanPath, isJSON: isJSON)
            
        case .doctor:
            handleDoctor(explicitPath: walkmanPath, isJSON: isJSON)
            
        case .scan:
            guard let folder = scanPath ?? sourcePath else {
                printError("Please provide a folder to scan: --scan <path>")
                exit(1)
            }
            handleScan(folderPath: folder, isJSON: isJSON)
            
        case .clean:
            let targetURL = resolveWalkmanURL(explicitPath: walkmanPath)
            guard let url = targetURL else {
                printError("No Walkman drive found to clean. Connect your player or specify --walkman <path>.")
                exit(1)
            }
            SyncEngine.cleanAppleDouble(at: url)
            if isJSON {
                print("{\"status\":\"cleaned\",\"path\":\"\(url.path)\"}")
            } else {
                print("✓ Successfully cleaned macOS AppleDouble (._*) files from \(url.path)")
            }
            exit(0)
            
        case .erase:
            handleErase(explicitPath: walkmanPath, isForce: isForce, isDryRun: isDryRun, isJSON: isJSON)
            
        case .dump:
            let outPath = dumpPath ?? sourcePath
            guard let dest = outPath else {
                printError("Missing dump output folder. Usage: walkmansync --dump <output_path>")
                exit(1)
            }
            handleDump(explicitWalkman: walkmanPath, destinationPath: dest, isJSON: isJSON)
            
        case .extractKey:
            handleExtractKey(explicitPath: walkmanPath, isJSON: isJSON)
            
        case .extractKeyInternal:
            handleExtractKeyInternal(explicitPath: walkmanPath, explicitCacheDir: cacheDirPath)
            
        case .checkUpdate:
            handleCheckUpdate(isJSON: isJSON)
            
        case .sync:
            guard let src = sourcePath else {
                printError("Missing music source folder. Usage: WalkmanSync --sync --source <path>")
                exit(1)
            }
            let targetURL = resolveWalkmanURL(explicitPath: walkmanPath)
            guard let destURL = targetURL else {
                printError("No Walkman device detected. Connect via USB or specify --walkman <path>.")
                exit(1)
            }
            handleSync(sourcePath: src, destinationURL: destURL, bitrate: selectedBitrate, isVBR: isVBR, isDryRun: isDryRun, isJSON: isJSON)
            
        case .none:
            printUsage()
            exit(0)
        }
    }
    
    // MARK: - Handlers
    
    private static func handleDetect(explicitPath: String? = nil, isJSON: Bool) {
        let fm = FileManager.default
        var volumes = SyncEngine.findAllWalkmanVolumes()
        if let explicit = explicitPath {
            let expURL = URL(fileURLWithPath: explicit)
            if !volumes.contains(where: { $0.path == expURL.path }) {
                volumes.append(expURL)
            }
        }
        var detectedDevices: [[String: Any]] = []
        
        for vol in volumes {
            let name = vol.lastPathComponent
            let hasOmgAudio = fm.fileExists(atPath: vol.appendingPathComponent("OMGAUDIO").path)
            let hasMp3fm = fm.fileExists(atPath: vol.appendingPathComponent("MP3FM").path)
            
            let dvidPath = vol.appendingPathComponent("MP3FM/DvID.DAT")
            let key = WalkmanKeyManager.readDeviceKey(from: dvidPath)
            let isAuthentic = key != nil && WalkmanKeyManager.isKeyAuthentic(key: key!)
            let keyHex: String
            if let k = key {
                keyHex = isAuthentic ? String(format: "0x%08X (Authentic ✓)", k) : String(format: "0x%08X (Placeholder ⚠️ - run --extract-key)", k)
            } else {
                keyHex = "Not Initialized (Run --extract-key)"
            }
            
            let attrs = try? fm.attributesOfFileSystem(forPath: vol.path)
            let totalBytes = attrs?[.systemSize] as? Int64 ?? 0
            let freeBytes = attrs?[.systemFreeSize] as? Int64 ?? 0
            
            let freeMB = freeBytes / (1024 * 1024)
            let atrac3Songs = Int(Double(freeMB) / 1.8)
            let lp4Songs = Int(Double(freeMB) / 1.0)
            let mp3Songs = Int(Double(freeMB) / 7.5)
            
            let devInfo: [String: Any] = [
                "path": vol.path,
                "name": name,
                "hasOmgAudio": hasOmgAudio,
                "hasMp3fm": hasMp3fm,
                "deviceKey": keyHex,
                "isKeyAuthentic": isAuthentic,
                "totalCapacityBytes": totalBytes,
                "freeCapacityBytes": freeBytes,
                "estimatedSongsAtrac3LP2": atrac3Songs,
                "estimatedSongsAtrac3LP4": lp4Songs,
                "estimatedSongsMP3": mp3Songs
            ]
            detectedDevices.append(devInfo)
        }
        
        if isJSON {
            let output: [String: Any] = [
                "count": detectedDevices.count,
                "devices": detectedDevices
            ]
            if let data = try? JSONSerialization.data(withJSONObject: output, options: .prettyPrinted),
               let str = String(data: data, encoding: .utf8) {
                print(str)
            }
        } else {
            if detectedDevices.isEmpty {
                print("No Sony Walkman devices found mounted in /Volumes.")
                print("Ensure the player is connected via USB and displays 'USB CONNECT'.")
            } else {
                print("Found \(detectedDevices.count) Sony Walkman device(s):")
                for (idx, dev) in detectedDevices.enumerated() {
                    let path = dev["path"] as? String ?? ""
                    let key = dev["deviceKey"] as? String ?? ""
                    let freeMB = (dev["freeCapacityBytes"] as? Int64 ?? 0) / (1024 * 1024)
                    let totalMB = (dev["totalCapacityBytes"] as? Int64 ?? 0) / (1024 * 1024)
                    let atrac3Songs = dev["estimatedSongsAtrac3LP2"] as? Int ?? 0
                    let lp4Songs = dev["estimatedSongsAtrac3LP4"] as? Int ?? 0
                    let mp3Songs = dev["estimatedSongsMP3"] as? Int ?? 0
                    print("  [\(idx + 1)] Path:       \(path)")
                    print("      Device Key: \(key)")
                    print("      Storage:    \(freeMB) MB free / \(totalMB) MB total")
                    print("      Capacity:   ~\(atrac3Songs) songs in ATRAC3 LP2 (132k), ~\(lp4Songs) in LP4 (66k), ~\(mp3Songs) in MP3 (320k)")
                }
            }
        }
        
        exit(detectedDevices.isEmpty ? 1 : 0)
    }
    
    private static func handleDoctor(explicitPath: String?, isJSON: Bool) {
        let ffmpeg = SyncEngine.findFFmpeg()
        let dotCleanPath: String? = {
            if FileManager.default.isExecutableFile(atPath: "/usr/sbin/dot_clean") { return "/usr/sbin/dot_clean" }
            if FileManager.default.isExecutableFile(atPath: "/usr/bin/dot_clean") { return "/usr/bin/dot_clean" }
            return nil
        }()
        let destinationURL: URL?
        if let path = explicitPath {
            destinationURL = URL(fileURLWithPath: path)
        } else {
            destinationURL = SyncEngine.findWalkmanVolume()
        }
        
        let appSupportDir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!.appendingPathComponent("WalkmanSync/DvID.DAT")
        let hasCachedKey = FileManager.default.fileExists(atPath: appSupportDir.path)
        let resolvedKey: UInt32? = destinationURL != nil ? WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: destinationURL!) : (hasCachedKey ? WalkmanKeyManager.readDeviceKey(from: appSupportDir) : nil)
        
        var onDeviceTrackCount = 0
        if let vol = destinationURL {
            let omgURL = vol.appendingPathComponent("OMGAUDIO")
            if let enumerator = FileManager.default.enumerator(at: omgURL, includingPropertiesForKeys: nil) {
                for case let fileURL as URL in enumerator {
                    if fileURL.pathExtension.uppercased() == "OMA" {
                        onDeviceTrackCount += 1
                    }
                }
            }
        }
        
        if isJSON {
            let jsonDict: [String: Any] = [
                "ffmpeg": [
                    "available": ffmpeg != nil,
                    "path": ffmpeg ?? "",
                    "requiredFor": "FLAC/M4A/WAV to MP3 conversion"
                ],
                "dot_clean": [
                    "available": dotCleanPath != nil,
                    "path": dotCleanPath ?? ""
                ],
                "walkman": [
                    "connected": destinationURL != nil,
                    "mountPath": destinationURL?.path ?? "",
                    "hardwareKey": resolvedKey != nil ? String(format: "0x%08X", resolvedKey!) : "",
                    "trackCount": onDeviceTrackCount
                ],
                "cachedKey": [
                    "available": hasCachedKey,
                    "path": appSupportDir.path
                ],
                "mp3DirectSyncReady": true,
                "flacM4aSyncReady": ffmpeg != nil
            ]
            if let data = try? JSONSerialization.data(withJSONObject: jsonDict, options: [.prettyPrinted]),
               let str = String(data: data, encoding: .utf8) {
                print(str)
            }
            exit(0)
        }
        
        print("==================================================")
        print("WalkmanSync Health & Dependency Check")
        print("==================================================")
        print("")
        print("External Tools & Audio Encoders:")
        if let ff = ffmpeg {
            print("  [✓] FFmpeg:       \(ff)")
            print("                    (Used for: FLAC, Apple M4A, ALAC, WAV, and AIFF audio conversion)")
        } else {
            print("  [•] FFmpeg:       NOT FOUND (Optional)")
            print("                    -> Only needed to convert lossless FLAC or Apple M4A files")
            print("                    -> To install: brew install ffmpeg")
            print("                    -> Note: Standard .mp3 files sync with 100% zero external tools!")
        }
        
        if let dc = dotCleanPath {
            print("  [✓] dot_clean:    \(dc) (Built-in macOS system utility)")
        } else {
            print("  [!] dot_clean:    NOT FOUND")
        }
        
        print("")
        print("Connected Hardware & Encryption Keys:")
        if let vol = destinationURL {
            let attrs = try? FileManager.default.attributesOfFileSystem(forPath: vol.path)
            let freeMB = (attrs?[.systemFreeSize] as? Int64 ?? 0) / (1024 * 1024)
            let totalMB = (attrs?[.systemSize] as? Int64 ?? 0) / (1024 * 1024)
            print("  [✓] Walkman USB:  Connected at \(vol.path) (\(freeMB) MB free of \(totalMB) MB)")
            if let key = resolvedKey {
                if WalkmanKeyManager.isKeyAuthentic(key: key) {
                    print("  [✓] Hardware Key: 0x\(String(format: "%08X", key)) (Authentic & Verified ✓)")
                } else {
                    print("  [!] Hardware Key: 0x\(String(format: "%08X", key)) (Placeholder ⚠️)")
                    print("                    -> Audio will fail with 'CANNOT PLAY'. Run 'walkmansync --extract-key' to retrieve authentic key!")
                }
            } else {
                print("  [!] Hardware Key: Not yet resolved on device (Run 'walkmansync --extract-key')")
            }
            if onDeviceTrackCount > 0 {
                print("  [✓] Music Tracks: \(onDeviceTrackCount) tracks found on Walkman")
                print("                    -> Export to Mac: 'walkmansync --dump ~/Music/WalkmanDump'")
                print("                    -> Erase device:  'walkmansync --erase'")
            } else {
                print("  [•] Music Tracks: 0 tracks (Device is empty / NO DATA)")
            }
        } else {
            print("  [-] Walkman USB:  Not connected (Connect Walkman via USB to sync)")
            if hasCachedKey, let key = resolvedKey {
                if WalkmanKeyManager.isKeyAuthentic(key: key) {
                    print("  [✓] Hardware Key: 0x\(String(format: "%08X", key)) (Cached in Application Support ✓)")
                } else {
                    print("  [!] Hardware Key: 0x\(String(format: "%08X", key)) (Cached placeholder ⚠️)")
                }
            }
        }
        
        print("")
        print("Readiness Summary:")
        print("  • Pure MP3 Sync:          [READY] (Zero external dependencies required)")
        if ffmpeg != nil {
            print("  • FLAC / M4A / WAV Sync:  [READY] (FFmpeg installed)")
        } else {
            print("  • FLAC / M4A / WAV Sync:  [NEEDS FFMPEG] (Run: brew install ffmpeg)")
        }
        print("==================================================")
        exit(0)
    }
    
    private static func handleErase(explicitPath: String?, isForce: Bool, isDryRun: Bool, isJSON: Bool) {
        guard let targetURL = resolveWalkmanURL(explicitPath: explicitPath) else {
            printError("No Walkman device detected. Connect via USB or specify --walkman <path>.")
            exit(1)
        }
        
        if isDryRun {
            let omgURL = targetURL.appendingPathComponent("OMGAUDIO")
            var tracksFound = 0
            var bytes: Int64 = 0
            if let enumerator = FileManager.default.enumerator(at: omgURL, includingPropertiesForKeys: [.fileSizeKey]) {
                for case let fileURL as URL in enumerator {
                    if fileURL.pathExtension.uppercased() == "OMA" {
                        tracksFound += 1
                        if let res = try? fileURL.resourceValues(forKeys: [.fileSizeKey]), let sz = res.fileSize {
                            bytes += Int64(sz)
                        }
                    }
                }
            }
            if isJSON {
                print("{\"status\":\"dry-run\",\"action\":\"erase\",\"tracksFound\":\(tracksFound),\"bytes\":\(bytes),\"walkman\":\"\(targetURL.path)\"}")
            } else {
                print("[DRY-RUN] Erase would remove \(tracksFound) tracks (\(bytes / (1024 * 1024)) MB) from \(targetURL.path) and reset OMGAUDIO database.")
            }
            exit(0)
        }
        
        if !isForce {
            print("⚠️  WARNING: You are about to ERASE ALL MUSIC from your Sony Walkman:")
            print("    Path: \(targetURL.path)")
            print("    All .OMA tracks and database tables will be deleted.")
            print("    The hardware encryption key (DvID.DAT) will be safely preserved.")
            print("")
            print("Are you sure you want to proceed? (y/N): ", terminator: "")
            fflush(stdout)
            guard let line = readLine(), line.lowercased().starts(with: "y") else {
                print("Erase cancelled.")
                exit(0)
            }
        }
        
        do {
            if !isJSON {
                print("[+] Erasing Walkman music and purging macOS junk...")
                fflush(stdout)
            }
            let result = try WalkmanCleaner.eraseWalkman(at: targetURL, force: isForce) { msg in
                if !isJSON {
                    print("  -> \(msg)")
                    fflush(stdout)
                }
            }
            if isJSON {
                let dict: [String: Any] = [
                    "status": "success",
                    "action": "erase",
                    "tracksDeleted": result.tracksDeleted,
                    "bytesFreed": result.bytesFreed,
                    "keyPreserved": String(format: "0x%08X", result.keyPreserved),
                    "walkman": result.mountPath
                ]
                if let data = try? JSONSerialization.data(withJSONObject: dict, options: .prettyPrinted),
                   let str = String(data: data, encoding: .utf8) {
                    print(str)
                }
            } else {
                print("--------------------------------------------------")
                print("✓ Walkman successfully erased and reset!")
                print("  Erased: \(result.tracksDeleted) tracks (\(result.bytesFreed / (1024 * 1024)) MB freed)")
                print("  Key Preserved: 0x\(String(format: "%08X", result.keyPreserved)) (\(result.keyWasAuthentic ? "Authentic ✓" : "Default"))")
                print("  Device is ready with clean 'NO DATA' (100% capacity).")
                print("==================================================")
            }
            exit(0)
        } catch {
            printError("Failed to erase Walkman: \(error.localizedDescription)")
            exit(1)
        }
    }
    
    private static func handleDump(explicitWalkman: String?, destinationPath: String, isJSON: Bool) {
        guard let targetURL = resolveWalkmanURL(explicitPath: explicitWalkman) else {
            printError("No Walkman device detected. Connect via USB or specify --walkman <path>.")
            exit(1)
        }
        
        let destURL = URL(fileURLWithPath: destinationPath)
        
        if !isJSON {
            print("==================================================")
            print("WalkmanSync — Reverse-Descramble & Track Dumper")
            print("==================================================")
            print("Walkman:     \(targetURL.path)")
            print("Destination: \(destURL.path)")
            print("--------------------------------------------------")
            print("[+] Scanning and descrambling tracks from Walkman...")
            fflush(stdout)
        }
        
        do {
            let result = try WalkmanTrackDumper.dumpTracks(from: targetURL, to: destURL) { cur, tot, title in
                if !isJSON {
                    print("  [\(cur)/\(tot)] Descrambled: \(title)")
                    fflush(stdout)
                }
            }
            
            if isJSON {
                let dict: [String: Any] = [
                    "status": "success",
                    "action": "dump",
                    "tracksFound": result.totalTracksFound,
                    "tracksDumped": result.tracksDumped,
                    "tracksFailed": result.tracksFailed,
                    "totalBytesWritten": result.totalBytesWritten,
                    "outputDirectory": result.outputDirectory.path
                ]
                if let data = try? JSONSerialization.data(withJSONObject: dict, options: .prettyPrinted),
                   let str = String(data: data, encoding: .utf8) {
                    print(str)
                }
            } else {
                print("--------------------------------------------------")
                print("✓ Successfully dumped \(result.tracksDumped) tracks to:")
                print("  \(result.outputDirectory.path)")
                if result.tracksFailed > 0 {
                    print("  (Warning: \(result.tracksFailed) tracks could not be decoded)")
                }
                print("==================================================")
            }
            exit(0)
        } catch {
            printError("Failed to dump tracks: \(error.localizedDescription)")
            exit(1)
        }
    }
    
    private static func handleScan(folderPath: String, isJSON: Bool) {
        let folderURL = URL(fileURLWithPath: folderPath)
        guard FileManager.default.fileExists(atPath: folderURL.path) else {
            printError("Folder not found: \(folderPath)")
            exit(1)
        }
        
        let titles = SyncEngine.scanForMusic(in: folderURL)
        
        if isJSON {
            let trackList: [[String: Any]] = titles.map {
                [
                    "id": $0.id,
                    "title": $0.titleName,
                    "artist": $0.artistName,
                    "album": $0.albumName,
                    "genre": $0.genre,
                    "lengthSeconds": $0.length,
                    "format": $0.originalFile?.pathExtension.uppercased() ?? "UNKNOWN",
                    "file": $0.originalFile?.path ?? ""
                ]
            }
            let output: [String: Any] = [
                "folder": folderURL.path,
                "trackCount": titles.count,
                "tracks": trackList
            ]
            if let data = try? JSONSerialization.data(withJSONObject: output, options: .prettyPrinted),
               let str = String(data: data, encoding: .utf8) {
                print(str)
            }
        } else {
            print("Scanned: \(folderURL.path)")
            print("Found \(titles.count) supported audio track(s):")
            for t in titles {
                let ext = t.originalFile?.pathExtension.uppercased() ?? "MP3"
                let idStr = String(format: "%03d", t.id)
                let durStr = String(format: "%02d:%02d", t.length / 60, t.length % 60)
                let formatStr = ext.padding(toLength: 4, withPad: " ", startingAt: 0)
                let titleStr = t.titleName.padding(toLength: 25, withPad: " ", startingAt: 0)
                let artistStr = t.artistName.padding(toLength: 20, withPad: " ", startingAt: 0)
                print("  [\(idStr)] [\(formatStr)] \(titleStr) — \(artistStr) (\(durStr))")
            }
        }
        
        exit(0)
    }
    
    private static func handleSync(sourcePath: String, destinationURL: URL, bitrate: WalkmanDBGenerator.MP3Bitrate, isVBR: Bool, isDryRun: Bool, isJSON: Bool) {
        let sourceURL = URL(fileURLWithPath: sourcePath)
        guard FileManager.default.fileExists(atPath: sourceURL.path) else {
            printError("Source folder not found: \(sourcePath)")
            exit(1)
        }
        
        let modeDesc = isVBR ? "MP3 VBR (\(bitrate.rawValue)k target ceiling)" : "MP3 \(bitrate.rawValue) kbps CBR"
        if !isJSON {
            print("==================================================")
            print("WalkmanSync CLI — Native SonicStage Alternative")
            print("==================================================")
            print("Source:      \(sourceURL.path)")
            print("Destination: \(destinationURL.path)")
            print("Quality:     \(modeDesc)")
            print("Dry Run:     \(isDryRun ? "YES" : "NO")")
            print("--------------------------------------------------")
        }
        
        let titles = SyncEngine.scanForMusic(in: sourceURL)
        guard !titles.isEmpty else {
            printError("No supported audio files (MP3, FLAC, M4A, WAV, etc.) found in \(sourceURL.path)")
            exit(1)
        }
        
        if !isJSON {
            print("[+] Scanned \(titles.count) tracks from source directory.")
        }
        
        if isDryRun {
            if isJSON {
                print("{\"status\":\"dry-run-complete\",\"trackCount\":\(titles.count),\"quality\":\"\(modeDesc)\"}")
            } else {
                print("[✓] Dry run complete. \(titles.count) tracks are ready to sync using \(modeDesc).")
            }
            exit(0)
        }
        
        // Warn if using placeholder key
        let dvidPath = destinationURL.appendingPathComponent("MP3FM/DvID.DAT")
        let key = WalkmanKeyManager.readDeviceKey(from: dvidPath) ?? WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: destinationURL)
        if !WalkmanKeyManager.isKeyAuthentic(key: key) {
            print("  ⚠️  WARNING: Device encryption key is uninitialized or a placeholder (0x\(String(format: "%08X", key))).")
            print("      Songs transferred with this key will fail with 'CANNOT PLAY' on authentic hardware.")
            print("      Run 'walkmansync --extract-key' to extract the authentic factory key from your player.")
            print("--------------------------------------------------")
        }
        
        // Preflight dependency check for non-MP3 files
        let hasNonMP3 = titles.contains { ($0.originalFile?.pathExtension.lowercased() ?? "") != "mp3" }
        if hasNonMP3 && SyncEngine.findFFmpeg() == nil {
            printError("FFmpeg is required to convert non-MP3 files (FLAC, M4A, WAV).\n       Please install via 'brew install ffmpeg', or sync standard .mp3 files directly.")
            exit(1)
        }
        
        do {
            if !isJSON {
                print("[+] Encoding and transferring audio tracks (\(modeDesc))...")
                fflush(stdout)
            }
            
            try SyncEngine.transferFilesToWalkman(titles: titles, destination: destinationURL, bitrate: bitrate, isVBR: isVBR) { progress in
                if !isJSON {
                    print("  -> \(progress)")
                    fflush(stdout)
                }
            }
            
            if !isJSON {
                print("[+] Generating hardware-accurate OMGAUDIO database suite (16 DAT tables)...")
                fflush(stdout)
            }
            
            let generator = WalkmanDBGenerator(mp3Bitrate: bitrate, isVBR: isVBR, isEncrypted3rdGen: true)
            try generator.generateDatabase(titles: titles, destination: destinationURL)
            
            // Clean AppleDouble files generated during DB write and flush buffers
            SyncEngine.cleanAppleDouble(at: destinationURL)
            Darwin.sync()
            
            if isJSON {
                let result: [String: Any] = [
                    "status": "success",
                    "syncedTracks": titles.count,
                    "quality": modeDesc,
                    "destination": destinationURL.path
                ]
                if let data = try? JSONSerialization.data(withJSONObject: result, options: .prettyPrinted),
                   let str = String(data: data, encoding: .utf8) {
                    print(str)
                }
            } else {
                print("--------------------------------------------------")
                print("✓ Sync successfully completed! (\(titles.count) tracks synced)")
                print("  You may now safely unplug or eject your Walkman.")
                print("==================================================")
            }
            exit(0)
            
        } catch {
            printError("Sync failed: \(error.localizedDescription)")
            exit(2)
        }
    }
    
    private static func handleCheckUpdate(isJSON: Bool) {
        let sema = DispatchSemaphore(value: 0)
        var latestTag: String?
        var releaseURL: URL?
        
        guard let url = URL(string: "https://api.github.com/repos/arsenyspb/walkmansync-for-mac/releases/latest") else {
            printError("Invalid GitHub releases URL.")
            exit(1)
        }
        
        var request = URLRequest(url: url)
        request.timeoutInterval = 4.0
        request.setValue("WalkmanSync/\(version)", forHTTPHeaderField: "User-Agent")
        request.setValue("application/vnd.github.v3+json", forHTTPHeaderField: "Accept")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            defer { sema.signal() }
            guard let data = data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let tagName = json["tag_name"] as? String,
                  let htmlUrlStr = json["html_url"] as? String,
                  let u = URL(string: htmlUrlStr) else {
                return
            }
            latestTag = tagName
            releaseURL = u
        }.resume()
        
        _ = sema.wait(timeout: .now() + 5.0)
        
        guard let tag = latestTag, let relURL = releaseURL else {
            if isJSON {
                print("{\"status\":\"error\",\"message\":\"Failed to fetch latest release from GitHub\"}")
            } else {
                print("Unable to fetch latest release from GitHub (check network connection).")
            }
            exit(1)
        }
        
        let latestVersion = tag.trimmingCharacters(in: CharacterSet(charactersIn: "vV"))
        let currentClean = version.trimmingCharacters(in: CharacterSet(charactersIn: "vV"))
        let isNewer = latestVersion.compare(currentClean, options: .numeric) == .orderedDescending
        
        if isJSON {
            let output: [String: Any] = [
                "currentVersion": version,
                "latestVersion": latestVersion,
                "updateAvailable": isNewer,
                "releaseURL": relURL.absoluteString
            ]
            if let d = try? JSONSerialization.data(withJSONObject: output, options: .prettyPrinted),
               let s = String(data: d, encoding: .utf8) {
                print(s)
            }
        } else {
            if isNewer {
                print("★ Update Available: WalkmanSync v\(latestVersion) is available! (Current: v\(version))")
                print("  Download DMG at: \(relURL.absoluteString)")
            } else {
                print("✓ You are running the latest version of WalkmanSync (v\(version)).")
            }
        }
        exit(0)
    }
    
    private static func handleExtractKey(explicitPath: String? = nil, isJSON: Bool) {
        let destinationURL = resolveWalkmanURL(explicitPath: explicitPath)
        if !isJSON {
            print("==================================================")
            print("WalkmanSync — Hardware Key Extraction")
            print("==================================================")
            print("Target Walkman: \(destinationURL?.path ?? "Auto-detecting via USB...")")
            print("Initiating hardware cryptographic extraction over USB...")
            fflush(stdout)
        }
        
        do {
            let key = try WalkmanKeyManager.extractHardwareKeyWithElevation(targetVolume: destinationURL)
            let keyHex = String(format: "0x%08X", key)
            let cachePath = WalkmanKeyManager.appSupportBackupURL.path
            let deviceDvidPath = destinationURL?.appendingPathComponent("MP3FM/DvID.DAT").path
            
            if isJSON {
                let dict: [String: Any] = [
                    "status": "success",
                    "hardwareKey": keyHex,
                    "targetVolume": destinationURL?.path ?? "",
                    "deviceDvIDPath": deviceDvidPath ?? "",
                    "cachedDvIDPath": cachePath
                ]
                if let data = try? JSONSerialization.data(withJSONObject: dict, options: .prettyPrinted),
                   let str = String(data: data, encoding: .utf8) {
                    print(str)
                }
            } else {
                print("--------------------------------------------------")
                print("✓ SUCCESS: Authentic Hardware Key Extracted!")
                print("  Hardware Key:       \(keyHex)")
                if let devPath = deviceDvidPath {
                    print("  Saved to Walkman:   \(devPath)")
                }
                print("  Permanently Cached: \(cachePath)")
                print("==================================================")
                print("Your Walkman is now ready for flawless audio playback.")
            }
            exit(0)
        } catch {
            if isJSON {
                let dict: [String: Any] = [
                    "status": "error",
                    "error": error.localizedDescription
                ]
                if let data = try? JSONSerialization.data(withJSONObject: dict, options: .prettyPrinted),
                   let str = String(data: data, encoding: .utf8) {
                    print(str)
                }
            } else {
                printError("Key extraction failed: \(error.localizedDescription)")
            }
            exit(1)
        }
    }
    
    private static func handleExtractKeyInternal(explicitPath: String?, explicitCacheDir: String?) {
        guard geteuid() == 0 else {
            printError("Internal helper must be run with root privileges.")
            exit(1)
        }
        
        let destinationURL = resolveWalkmanURL(explicitPath: explicitPath)
        let cacheURL = explicitCacheDir != nil ? URL(fileURLWithPath: explicitCacheDir!) : nil
        
        do {
            let result = try WalkmanKeyManager.extractHardwareKeyDirectly()
            let keyHex = String(format: "0x%08X", result.key)
            
            // Save to cache dir
            if let cDir = cacheURL {
                WalkmanKeyManager.saveKeyToCache(payload: result.payload, key: result.key, cacheDirectory: cDir)
                // Fix ownership of the created cache files to match parent directory owner
                if let attrs = try? FileManager.default.attributesOfItem(atPath: cDir.path),
                   let uid = attrs[.ownerAccountID] as? uid_t,
                   let gid = attrs[.groupOwnerAccountID] as? gid_t {
                    chown(cDir.appendingPathComponent("DvID.DAT").path, uid, gid)
                    chown(cDir.appendingPathComponent("DvID_\(keyHex).DAT").path, uid, gid)
                }
            }
            
            // Save to Walkman volume if mounted
            if let vol = destinationURL {
                let mp3fmDir = vol.appendingPathComponent("MP3FM", isDirectory: true)
                try? FileManager.default.createDirectory(at: mp3fmDir, withIntermediateDirectories: true, attributes: nil)
                let dvidURL = mp3fmDir.appendingPathComponent("DvID.DAT")
                try? result.payload.write(to: dvidURL)
            }
            
            print("KEY:\(keyHex)")
            exit(0)
        } catch {
            printError("Extraction failed: \(error.localizedDescription)")
            exit(1)
        }
    }
    
    private static func resolveWalkmanURL(explicitPath: String?) -> URL? {
        if let path = explicitPath {
            let url = URL(fileURLWithPath: path)
            if FileManager.default.fileExists(atPath: url.path) {
                return url
            }
        }
        return SyncEngine.findWalkmanVolume()
    }
    
    private static func printUsage() {
        print("""
        WalkmanSync CLI — Native macOS SonicStage Alternative for Sony Walkman
        
        USAGE:
            WalkmanSync [options]
            /Applications/WalkmanSync.app/Contents/MacOS/WalkmanSync [options]
        
        ACTIONS:
            --detect, -d                  Detect and inspect connected Walkman devices
            --extract-key, -k             Extract authentic hardware encryption key from Walkman
            --doctor, -D                  Check system health, dependencies (FFmpeg), and keys
            --erase                       Safely wipe all tracks and reset database (preserves key)
            --dump <folder>               Dump and reverse-descramble all tracks from Walkman to Mac
            --check-update, -u            Check GitHub for newer WalkmanSync releases
            --scan <folder>               Scan music folder and list tracks, formats, metadata
            --sync --source <folder>      Sync local music folder to Walkman
            --clean                       Clean macOS AppleDouble (._*) files on Walkman
            --version, -v                 Print version information
            --help, -h                    Show this help screen
        
        OPTIONS:
            --bitrate, -b <rate>          Target MP3 bitrate (320, 256, 192, 128, 96). Default: 192
            --vbr                         Enable Variable Bit Rate (uses selected bitrate as ceiling)
            --source, -s <path>           Source music folder (MP3, FLAC, M4A, WAV, AIFF, OGG)
            --walkman, -w <path>          Walkman mount root (default: auto-detected in /Volumes)
            --force, -f, -y               Skip interactive prompts for destructive actions (--erase)
            --json                        Format output as machine-readable JSON (great for agents!)
            --dry-run                     Simulate sync or erase operations without writing to flash
            --verbose                     Enable detailed debug logging to stdout
        
        EXAMPLES:
            # 1. Detect connected player
            WalkmanSync --detect
        
            # 2. Inspect a music library in JSON
            WalkmanSync --scan ~/Music/MyAlbum --json
        
            # 3. One-line automated sync
            WalkmanSync --sync --source ~/Music/MyAlbum
        
            # 4. Agent automation with JSON output
            WalkmanSync --sync --source ~/Music/Favorites --json
        """)
    }
    
    private static func printError(_ message: String) {
        fputs("Error: \(message)\n", stderr)
    }
}
