import Foundation

public class CLIHandler {
    public static let version = "0.2.0"
    
    public static func shouldHandleCLI() -> Bool {
        let args = CommandLine.arguments.dropFirst().filter { !$0.starts(with: "-psn_") }
        return !args.isEmpty
    }
    
    public static func run() {
        let args = Array(CommandLine.arguments.dropFirst().filter { !$0.starts(with: "-psn_") })
        
        var isJSON = false
        var isDryRun = false
        var isVerbose = false
        var sourcePath: String?
        var walkmanPath: String?
        var scanPath: String?
        var action: Action = .none
        
        enum Action {
            case none
            case help
            case version
            case detect
            case scan
            case sync
            case clean
        }
        
        var i = 0
        while i < args.count {
            let arg = args[i]
            switch arg {
            case "--help", "-h":
                action = .help
            case "--version", "-v":
                action = .version
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
            case "--json":
                isJSON = true
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
            handleDetect(isJSON: isJSON)
            
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
            handleSync(sourcePath: src, destinationURL: destURL, isDryRun: isDryRun, isJSON: isJSON)
            
        case .none:
            printUsage()
            exit(0)
        }
    }
    
    // MARK: - Handlers
    
    private static func handleDetect(isJSON: Bool) {
        let fm = FileManager.default
        let volumesURL = URL(fileURLWithPath: "/Volumes")
        var detectedDevices: [[String: Any]] = []
        
        if let volumes = try? fm.contentsOfDirectory(at: volumesURL, includingPropertiesForKeys: [.volumeNameKey, .volumeTotalCapacityKey, .volumeAvailableCapacityKey]) {
            for vol in volumes {
                let name = vol.lastPathComponent
                let hasOmgAudio = fm.fileExists(atPath: vol.appendingPathComponent("OMGAUDIO").path)
                let hasMp3fm = fm.fileExists(atPath: vol.appendingPathComponent("MP3FM").path)
                let hasNwwm = fm.fileExists(atPath: vol.appendingPathComponent("NWWM").path)
                
                if name.uppercased() == "WALKMAN" || name.uppercased() == "SONY" || hasOmgAudio || hasMp3fm || hasNwwm {
                    let dvidPath = vol.appendingPathComponent("MP3FM/DvID.DAT")
                    let key = WalkmanKeyManager.readDeviceKey(from: dvidPath)
                    let keyHex = key != nil ? String(format: "0x%08X", key!) : "Not Initialized"
                    
                    let attrs = try? fm.attributesOfFileSystem(forPath: vol.path)
                    let totalBytes = attrs?[.systemSize] as? Int64 ?? 0
                    let freeBytes = attrs?[.systemFreeSize] as? Int64 ?? 0
                    
                    let devInfo: [String: Any] = [
                        "path": vol.path,
                        "name": name,
                        "hasOmgAudio": hasOmgAudio,
                        "hasMp3fm": hasMp3fm,
                        "deviceKey": keyHex,
                        "totalCapacityBytes": totalBytes,
                        "freeCapacityBytes": freeBytes
                    ]
                    detectedDevices.append(devInfo)
                }
            }
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
                    print("  [\(idx + 1)] Path:       \(path)")
                    print("      Device Key: \(key)")
                    print("      Storage:    \(freeMB) MB free / \(totalMB) MB total")
                }
            }
        }
        
        exit(detectedDevices.isEmpty ? 1 : 0)
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
    
    private static func handleSync(sourcePath: String, destinationURL: URL, isDryRun: Bool, isJSON: Bool) {
        let sourceURL = URL(fileURLWithPath: sourcePath)
        guard FileManager.default.fileExists(atPath: sourceURL.path) else {
            printError("Source folder not found: \(sourcePath)")
            exit(1)
        }
        
        if !isJSON {
            print("==================================================")
            print("WalkmanSync CLI — Native SonicStage Alternative")
            print("==================================================")
            print("Source:      \(sourceURL.path)")
            print("Destination: \(destinationURL.path)")
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
                print("{\"status\":\"dry-run-complete\",\"trackCount\":\(titles.count)}")
            } else {
                print("[✓] Dry run complete. \(titles.count) tracks are ready to sync.")
            }
            exit(0)
        }
        
        do {
            if !isJSON {
                print("[+] Transferring, transcoding (if required), and scrambling audio...")
            }
            
            try SyncEngine.transferFilesToWalkman(titles: titles, destination: destinationURL) { progress in
                if !isJSON {
                    print("    -> \(progress)")
                }
            }
            
            if !isJSON {
                print("[+] Generating hardware-accurate OMGAUDIO database suite (all 8 DAT tables)...")
            }
            
            let generator = WalkmanDBGenerator(isEncrypted3rdGen: true)
            try generator.generateDatabase(titles: titles, destination: destinationURL)
            
            if isJSON {
                let result: [String: Any] = [
                    "status": "success",
                    "syncedTracks": titles.count,
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
    
    private static func resolveWalkmanURL(explicitPath: String?) -> URL? {
        if let path = explicitPath {
            let url = URL(fileURLWithPath: path)
            if FileManager.default.fileExists(atPath: url.path) {
                return url
            }
        }
        
        let fm = FileManager.default
        let volumesURL = URL(fileURLWithPath: "/Volumes")
        if let volumes = try? fm.contentsOfDirectory(at: volumesURL, includingPropertiesForKeys: nil) {
            for vol in volumes {
                let name = vol.lastPathComponent.uppercased()
                let hasOmgAudio = fm.fileExists(atPath: vol.appendingPathComponent("OMGAUDIO").path)
                let hasMp3fm = fm.fileExists(atPath: vol.appendingPathComponent("MP3FM").path)
                let hasNwwm = fm.fileExists(atPath: vol.appendingPathComponent("NWWM").path)
                if name == "WALKMAN" || name == "SONY" || hasOmgAudio || hasMp3fm || hasNwwm {
                    return vol
                }
            }
        }
        return nil
    }
    
    private static func printUsage() {
        print("""
        WalkmanSync CLI — Native macOS SonicStage Alternative for Sony Walkman
        
        USAGE:
            WalkmanSync [options]
            /Applications/WalkmanSync.app/Contents/MacOS/WalkmanSync [options]
        
        ACTIONS:
            --detect, -d                  Detect and inspect connected Walkman devices
            --scan <folder>               Scan music folder and list tracks, formats, metadata
            --sync --source <folder>      Sync local music folder to Walkman
            --clean                       Clean macOS AppleDouble (._*) files on Walkman
            --version, -v                 Print version information
            --help, -h                    Show this help screen
        
        OPTIONS:
            --source, -s <path>           Source music folder (MP3, FLAC, M4A, WAV, AIFF, OGG)
            --walkman, -w <path>          Walkman mount root (default: auto-detected in /Volumes)
            --json                        Format output as machine-readable JSON (great for agents!)
            --dry-run                     Simulate sync operations without writing to flash
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
