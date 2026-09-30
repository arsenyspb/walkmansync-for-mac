import Cocoa

@main
struct WalkmanSyncMain {
    static func main() {
        if CLIHandler.shouldHandleCLI() {
            CLIHandler.run()
        } else {
            let app = NSApplication.shared
            let delegate = AppDelegate()
            app.delegate = delegate
            app.setActivationPolicy(.regular)
            app.run()
        }
    }
}

class AppDelegate: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    
    // UI Elements
    var sourcePathLabel: NSTextField!
    var walkmanPathLabel: NSTextField!
    var extractKeyBtn: NSButton!
    var storageLabel: NSTextField!
    var capacityLabel: NSTextField!
    var codecPopUp: NSPopUpButton!
    var syncButton: NSButton!
    var statusLabel: NSTextField!
    var dependencyLabel: NSTextField!
    
    var sourceUrl: URL?
    var walkmanUrl: URL?
    var deviceDetectionTimer: Timer?
    
    func applicationDidFinishLaunching(_ aNotification: Notification) {
        setupMainMenu()
        
        // Set Dock Icon dynamically
        if let iconUrl = Bundle.main.url(forResource: "AppIcon", withExtension: "icns"),
           let icon = NSImage(contentsOf: iconUrl) {
            NSApp.applicationIconImage = icon
        }
        
        window = NSWindow(contentRect: NSMakeRect(0, 0, 540, 430),
                          styleMask: [.titled, .closable, .miniaturizable],
                          backing: .buffered,
                          defer: false)
        window.center()
        window.title = "Walkman Sync"
        
        let contentView = NSView(frame: window.contentRect(forFrameRect: window.frame))
        
        // --- Walkman Official Logo ---
        var logoImg: NSImage?
        if let bundleUrl = Bundle.main.url(forResource: "walkman_logo", withExtension: "svg") {
            logoImg = NSImage(contentsOf: bundleUrl)
        } else if let localImg = NSImage(contentsOfFile: "WalkmanSync/Resources/walkman_logo.svg") ?? NSImage(contentsOfFile: "img/walkman_logo.svg") {
            logoImg = localImg
        }
        
        if let logo = logoImg {
            logo.isTemplate = true
            let logoView = NSImageView(frame: NSMakeRect(20, 356, 75, 40))
            logoView.image = logo
            logoView.imageScaling = .scaleProportionallyUpOrDown
            // Iconic Walkman Signature Orange #F26522
            logoView.contentTintColor = NSColor(red: 0.95, green: 0.40, blue: 0.13, alpha: 1.0)
            contentView.addSubview(logoView)
        }
        
        let titleLabel = NSTextField(labelWithString: "Walkman Sync")
        titleLabel.font = NSFont.boldSystemFont(ofSize: 22)
        titleLabel.frame = NSMakeRect(105, 368, 415, 28)
        contentView.addSubview(titleLabel)
        
        let subTitle = NSTextField(labelWithString: "Zero-friction native music sync for Sony Network Walkman")
        subTitle.font = NSFont.systemFont(ofSize: 11)
        subTitle.textColor = .secondaryLabelColor
        subTitle.frame = NSMakeRect(105, 350, 415, 18)
        contentView.addSubview(subTitle)
        
        // --- Source Folder ---
        let sourceTitle = NSTextField(labelWithString: "Music Source:")
        sourceTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        sourceTitle.frame = NSMakeRect(20, 305, 115, 20)
        contentView.addSubview(sourceTitle)
        
        sourcePathLabel = NSTextField(labelWithString: "Not Selected")
        sourcePathLabel.frame = NSMakeRect(140, 305, 280, 20)
        sourcePathLabel.textColor = .secondaryLabelColor
        contentView.addSubview(sourcePathLabel)
        
        let sourceBtn = NSButton(title: "Select", target: self, action: #selector(selectSource))
        sourceBtn.frame = NSMakeRect(430, 300, 90, 30)
        contentView.addSubview(sourceBtn)
        
        // --- Walkman Connection Status ---
        let walkmanTitle = NSTextField(labelWithString: "Walkman Device:")
        walkmanTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        walkmanTitle.frame = NSMakeRect(20, 260, 125, 20)
        contentView.addSubview(walkmanTitle)
        
        walkmanPathLabel = NSTextField(labelWithString: "Searching for Walkman via USB...")
        walkmanPathLabel.font = NSFont.systemFont(ofSize: 13)
        walkmanPathLabel.frame = NSMakeRect(140, 260, 280, 20)
        walkmanPathLabel.textColor = .systemOrange
        contentView.addSubview(walkmanPathLabel)
        
        extractKeyBtn = NSButton(title: "🔑 Extract", target: self, action: #selector(extractKeyClicked))
        extractKeyBtn.frame = NSMakeRect(430, 255, 90, 30)
        extractKeyBtn.isEnabled = false
        contentView.addSubview(extractKeyBtn)
        
        // --- Storage Capacity ---
        let storageTitle = NSTextField(labelWithString: "Storage Space:")
        storageTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        storageTitle.frame = NSMakeRect(20, 230, 115, 20)
        contentView.addSubview(storageTitle)
        
        storageLabel = NSTextField(labelWithString: "")
        storageLabel.font = NSFont.systemFont(ofSize: 11)
        storageLabel.textColor = .secondaryLabelColor
        storageLabel.frame = NSMakeRect(140, 230, 380, 18)
        contentView.addSubview(storageLabel)
        
        // --- Song Capacity Estimates ---
        let capacityTitle = NSTextField(labelWithString: "Song Capacity:")
        capacityTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        capacityTitle.frame = NSMakeRect(20, 192, 115, 20)
        contentView.addSubview(capacityTitle)
        
        capacityLabel = NSTextField(wrappingLabelWithString: "")
        capacityLabel.font = NSFont.systemFont(ofSize: 11)
        capacityLabel.frame = NSMakeRect(140, 175, 380, 38)
        contentView.addSubview(capacityLabel)
        
        // --- Audio Codec Selection ---
        let codecTitle = NSTextField(labelWithString: "Audio Codec:")
        codecTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        codecTitle.frame = NSMakeRect(20, 135, 115, 20)
        contentView.addSubview(codecTitle)
        
        codecPopUp = NSPopUpButton(frame: NSMakeRect(138, 130, 380, 28), pullsDown: false)
        for codec in WalkmanDBGenerator.AudioCodec.allCases {
            codecPopUp.addItem(withTitle: codec.displayName)
        }
        codecPopUp.target = self
        codecPopUp.action = #selector(codecChanged)
        contentView.addSubview(codecPopUp)
        
        // --- Dependency / Engine Status ---
        dependencyLabel = NSTextField(labelWithString: "")
        dependencyLabel.font = NSFont.systemFont(ofSize: 11)
        dependencyLabel.frame = NSMakeRect(140, 106, 380, 18)
        contentView.addSubview(dependencyLabel)
        
        // --- Status & Logs ---
        statusLabel = NSTextField(labelWithString: "Ready")
        statusLabel.alignment = .center
        statusLabel.textColor = .secondaryLabelColor
        statusLabel.frame = NSMakeRect(20, 75, 500, 20)
        contentView.addSubview(statusLabel)
        
        // --- Doctor Button ---
        let doctorBtn = NSButton(title: "🩺 Doctor...", target: self, action: #selector(showDoctorDialog))
        doctorBtn.bezelStyle = .inline
        doctorBtn.font = NSFont.systemFont(ofSize: 11)
        doctorBtn.frame = NSMakeRect(305, 23, 100, 26)
        contentView.addSubview(doctorBtn)
        
        // --- View Logs Button ---
        let logButton = NSButton(title: "View Logs", target: self, action: #selector(openLogs))
        logButton.bezelStyle = .inline
        logButton.font = NSFont.systemFont(ofSize: 11)
        logButton.frame = NSMakeRect(415, 23, 88, 26)
        contentView.addSubview(logButton)
        
        // --- Sync Button ---
        syncButton = NSButton(title: "Sync to Walkman", target: self, action: #selector(startSync))
        syncButton.frame = NSMakeRect(110, 18, 180, 38)
        syncButton.bezelStyle = .rounded
        syncButton.isEnabled = false
        contentView.addSubview(syncButton)
        
        window.contentView = contentView
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
        
        updateDependencyStatus()
        
        // Start continuous live device monitoring after UI is ready
        startDeviceMonitoring()
    }
    
    private func startDeviceMonitoring() {
        checkConnectedWalkman()
        
        NSWorkspace.shared.notificationCenter.addObserver(
            self,
            selector: #selector(volumeChanged),
            name: NSWorkspace.didMountNotification,
            object: nil
        )
        NSWorkspace.shared.notificationCenter.addObserver(
            self,
            selector: #selector(volumeChanged),
            name: NSWorkspace.didUnmountNotification,
            object: nil
        )
        
        deviceDetectionTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.checkConnectedWalkman()
        }
    }
    
    @objc func volumeChanged(_ notification: Notification) {
        DispatchQueue.main.async { [weak self] in
            self?.checkConnectedWalkman()
        }
    }
    
    private func checkConnectedWalkman() {
        let detected = SyncEngine.findWalkmanVolume()
        
        if let vol = detected {
            if walkmanUrl != vol {
                walkmanUrl = vol
                WalkmanLogger.info("Walkman device detected at: \(vol.path)")
                walkmanPathLabel?.stringValue = "● Connected (\(vol.lastPathComponent))"
                walkmanPathLabel?.textColor = .systemGreen
                updateStorageDisplay()
                updateSyncButton()
            }
        } else {
            if walkmanUrl != nil {
                walkmanUrl = nil
                WalkmanLogger.warn("Walkman device disconnected")
                walkmanPathLabel?.stringValue = "Waiting for device to connect via USB..."
                walkmanPathLabel?.textColor = .systemOrange
                extractKeyBtn?.isEnabled = false
                extractKeyBtn?.title = "🔑 Extract"
                storageLabel?.stringValue = ""
                capacityLabel?.stringValue = ""
                updateSyncButton()
            }
        }
    }
    
    @objc func codecChanged() {
        updateStorageDisplay()
        updateDependencyStatus()
    }
    
    private func updateStorageDisplay() {
        guard let vol = walkmanUrl else {
            storageLabel?.stringValue = ""
            capacityLabel?.stringValue = ""
            return
        }
        let attrs = try? FileManager.default.attributesOfFileSystem(forPath: vol.path)
        let freeBytes = attrs?[.systemFreeSize] as? Int64 ?? 0
        let totalBytes = attrs?[.systemSize] as? Int64 ?? 0
        let freeMB = freeBytes / (1024 * 1024)
        let totalMB = totalBytes / (1024 * 1024)
        let percentFree = totalMB > 0 ? Int((Double(freeMB) / Double(totalMB)) * 100) : 0
        
        let key = WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: vol)
        let isAuthentic = WalkmanKeyManager.isKeyAuthentic(key: key)
        let keyHex = String(format: "0x%08X", key)
        
        extractKeyBtn?.isEnabled = true
        if isAuthentic {
            storageLabel?.stringValue = "\(freeMB) MB free of \(totalMB) MB (\(percentFree)% available) • Key: \(keyHex) (Authentic ✓)"
            extractKeyBtn?.title = "🔑 Key ✓"
            extractKeyBtn?.toolTip = "Authentic hardware encryption key is verified. Click to re-extract if needed."
        } else {
            storageLabel?.stringValue = "\(freeMB) MB free of \(totalMB) MB (\(percentFree)% available) • Key: \(keyHex) (Placeholder ⚠️)"
            extractKeyBtn?.title = "🔑 Extract"
            extractKeyBtn?.toolTip = "Click to extract authentic factory encryption key from Walkman hardware."
        }
        
        let atrac3Songs = Int(Double(freeMB) / 1.8)
        let lp4Songs = Int(Double(freeMB) / 1.0)
        let a3plusSongs = Int(Double(freeMB) / 3.5)
        let mp3Songs = Int(Double(freeMB) / 7.5)
        
        let selectedIndex = codecPopUp?.indexOfSelectedItem ?? 0
        let selectedFormat: String
        let selectedCount: Int
        switch selectedIndex {
        case 0:
            selectedFormat = "ATRAC3 LP2 (132 kbps)"
            selectedCount = atrac3Songs
        case 1:
            selectedFormat = "ATRAC3 LP4 (66 kbps)"
            selectedCount = lp4Songs
        case 2:
            selectedFormat = "ATRAC3plus (256 kbps)"
            selectedCount = a3plusSongs
        case 3:
            selectedFormat = "MP3 (320 kbps CBR)"
            selectedCount = mp3Songs
        default:
            selectedFormat = "ATRAC3 LP2 (132 kbps)"
            selectedCount = atrac3Songs
        }
        
        let attr = NSMutableAttributedString()
        let primaryStr = "~\(selectedCount) songs with \(selectedFormat)\n"
        attr.append(NSAttributedString(string: primaryStr, attributes: [
            .font: NSFont.systemFont(ofSize: 11, weight: .semibold),
            .foregroundColor: NSColor.labelColor
        ]))
        
        let comparisonStr = "All: ~\(atrac3Songs) LP2 (132k) • ~\(lp4Songs) LP4 (66k) • ~\(a3plusSongs) A3+ (256k) • ~\(mp3Songs) MP3 (320k)"
        attr.append(NSAttributedString(string: comparisonStr, attributes: [
            .font: NSFont.systemFont(ofSize: 10, weight: .regular),
            .foregroundColor: NSColor.secondaryLabelColor
        ]))
        
        capacityLabel?.attributedStringValue = attr
    }
    
    @objc func selectSource() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.prompt = "Choose Music Folder"
        panel.message = "Select a folder containing your music (MP3, FLAC, M4A)"
        if panel.runModal() == .OK {
            sourceUrl = panel.url
            if let url = sourceUrl {
                let tracks = SyncEngine.scanForMusic(in: url)
                let count = tracks.count
                if count > 0 {
                    sourcePathLabel?.stringValue = "\(url.lastPathComponent) (\(count) track\(count == 1 ? "" : "s"))"
                    sourcePathLabel?.textColor = .labelColor
                    statusLabel?.stringValue = "Found \(count) audio track\(count == 1 ? "" : "s") ready to sync."
                } else {
                    sourcePathLabel?.stringValue = "\(url.lastPathComponent) (0 tracks found)"
                    sourcePathLabel?.textColor = .systemRed
                    statusLabel?.stringValue = "No supported audio files (MP3, FLAC, M4A) found."
                }
            }
            updateSyncButton()
        }
    }
    
    func updateSyncButton() {
        if let btn = syncButton {
            btn.isEnabled = (sourceUrl != nil && walkmanUrl != nil)
        }
    }
    
    private func updateDependencyStatus() {
        let hasFFmpeg = SyncEngine.findFFmpeg() != nil
        let hasAtracdenc = SyncEngine.findAtracdenc() != nil
        let selectedIndex = codecPopUp?.indexOfSelectedItem ?? 0
        let isMP3 = selectedIndex == 3
        
        if isMP3 {
            if hasFFmpeg {
                dependencyLabel?.stringValue = "✓ Pure MP3 Mode (FFmpeg available for non-MP3 files)"
                dependencyLabel?.textColor = .secondaryLabelColor
            } else {
                dependencyLabel?.stringValue = "✓ Pure MP3 Mode (Zero external tools required for .mp3)"
                dependencyLabel?.textColor = .systemGreen
            }
        } else {
            if hasFFmpeg && hasAtracdenc {
                dependencyLabel?.stringValue = "● Audio Engine: Ready (FFmpeg & ATRAC3 active)"
                dependencyLabel?.textColor = .systemGreen
            } else if !hasFFmpeg {
                dependencyLabel?.stringValue = "⚠️ FFmpeg missing for ATRAC3 (Click Doctor for setup)"
                dependencyLabel?.textColor = .systemOrange
            } else {
                dependencyLabel?.stringValue = "⚠️ atracdenc encoder missing (Click Doctor for setup)"
                dependencyLabel?.textColor = .systemRed
            }
        }
    }
    
    @objc func showDoctorDialog() {
        let ffmpeg = SyncEngine.findFFmpeg()
        let atracdenc = SyncEngine.findAtracdenc()
        let walkman = walkmanUrl ?? SyncEngine.findWalkmanVolume()
        let key = walkman != nil ? WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: walkman!) : nil
        
        let alert = NSAlert()
        alert.messageText = "WalkmanSync System Health & Dependencies"
        
        var message = ""
        
        // FFmpeg
        if let ff = ffmpeg {
            message += "✓ FFmpeg: Installed (\(ff))\n"
            message += "   Ready for: ATRAC3 encoding & FLAC/M4A/WAV decoding\n\n"
        } else {
            message += "✗ FFmpeg: Not Installed\n"
            message += "   Required for: ATRAC3 encoding & FLAC/M4A/WAV files\n"
            message += "   Install via Homebrew: brew install ffmpeg\n"
            message += "   (Note: Standard .mp3 files sync without FFmpeg!)\n\n"
        }
        
        // atracdenc
        if let at = atracdenc {
            let note = at.contains(".app/") ? " (Bundled with App)" : ""
            message += "✓ atracdenc: Available\(note)\n"
            message += "   Ready for: Sony ATRAC3 / ATRAC3plus encoding\n\n"
        } else {
            message += "✗ atracdenc: Missing\n"
            message += "   Required for: Sony ATRAC3 / ATRAC3plus encoding\n\n"
        }
        
        // Walkman
        if let vol = walkman {
            let attrs = try? FileManager.default.attributesOfFileSystem(forPath: vol.path)
            let freeMB = (attrs?[.systemFreeSize] as? Int64 ?? 0) / (1024 * 1024)
            let totalMB = (attrs?[.systemSize] as? Int64 ?? 0) / (1024 * 1024)
            message += "✓ Walkman USB: Connected (\(vol.lastPathComponent))\n"
            message += "   Storage: \(freeMB) MB free of \(totalMB) MB\n"
            if let k = key {
                if WalkmanKeyManager.isKeyAuthentic(key: k) {
                    message += "   Hardware Key: 0x\(String(format: "%08X", k)) (Authentic & Verified ✓)\n"
                } else {
                    message += "   Hardware Key: 0x\(String(format: "%08X", k)) (Placeholder ⚠️ - Click '🔑 Extract')\n"
                }
            }
        } else {
            message += "• Walkman USB: Not Connected\n"
            message += "   Connect your Walkman via USB to transfer music\n"
        }
        
        alert.informativeText = message
        alert.alertStyle = ffmpeg != nil ? .informational : .warning
        
        alert.addButton(withTitle: "OK")
        if ffmpeg == nil {
            alert.addButton(withTitle: "Copy 'brew install ffmpeg'")
        }
        alert.addButton(withTitle: "📖 View Online Guide")
        
        let response = alert.runModal()
        if ffmpeg == nil && response == .alertSecondButtonReturn {
            NSPasteboard.general.clearContents()
            NSPasteboard.general.setString("brew install ffmpeg", forType: .string)
        } else if (ffmpeg == nil && response == .alertThirdButtonReturn) || (ffmpeg != nil && response == .alertSecondButtonReturn) {
            if let url = URL(string: "https://github.com/arsenyspb/walkmansync-for-mac#dependencies--audio-encoders") {
                NSWorkspace.shared.open(url)
            }
        }
    }
    
    @objc func extractKeyClicked() {
        extractKeyBtn?.isEnabled = false
        statusLabel?.stringValue = "Requesting administrator access to capture USB interface..."
        statusLabel?.textColor = .systemOrange
        
        let targetVol = walkmanUrl ?? SyncEngine.findWalkmanVolume()
        
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            do {
                let key = try WalkmanKeyManager.extractHardwareKeyWithElevation(targetVolume: targetVol)
                let keyHex = String(format: "0x%08X", key)
                
                DispatchQueue.main.async {
                    self?.extractKeyBtn?.isEnabled = true
                    self?.updateStorageDisplay()
                    self?.statusLabel?.stringValue = "✓ Hardware Key 0x\(keyHex) Extracted & Saved"
                    self?.statusLabel?.textColor = .systemGreen
                    
                    let alert = NSAlert()
                    alert.messageText = "Hardware Key Extracted Successfully!"
                    alert.informativeText = "Authentic Factory Key: 0x\(keyHex)\n\nYour Walkman's unique cryptographic key was extracted directly from the ASIC register and saved to /Volumes/WALKMAN/MP3FM/DvID.DAT.\n\nIt is also permanently backed up in Application Support. Audio files will now play flawlessly on your player!"
                    alert.alertStyle = .informational
                    alert.addButton(withTitle: "OK")
                    alert.runModal()
                }
            } catch WalkmanKeyManager.ExtractionError.scriptCancelled {
                DispatchQueue.main.async {
                    self?.extractKeyBtn?.isEnabled = true
                    self?.statusLabel?.stringValue = "Key extraction cancelled"
                    self?.statusLabel?.textColor = .secondaryLabelColor
                }
            } catch {
                DispatchQueue.main.async {
                    self?.extractKeyBtn?.isEnabled = true
                    self?.statusLabel?.stringValue = "Key extraction failed"
                    self?.statusLabel?.textColor = .systemRed
                    
                    let alert = NSAlert()
                    alert.messageText = "Key Extraction Failed"
                    alert.informativeText = "\(error.localizedDescription)\n\nPlease make sure your Walkman is connected via USB and displays 'USB CONNECT'."
                    alert.alertStyle = .warning
                    alert.addButton(withTitle: "OK")
                    alert.runModal()
                }
            }
        }
    }
    
    @objc func openLogs() {
        WalkmanLogger.openLogInConsole()
    }
    
    @objc func startSync() {
        guard let source = sourceUrl, let destination = walkmanUrl else { return }
        
        // Preflight check for placeholder hardware key
        let currentKey = WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: destination)
        if !WalkmanKeyManager.isKeyAuthentic(key: currentKey) {
            let alert = NSAlert()
            alert.messageText = "⚠️ Hardware Encryption Key Not Initialized"
            alert.informativeText = "Your Walkman is currently using a placeholder encryption key (0x\(String(format: "%08X", currentKey))). Files transferred with this key will fail to play on your Walkman hardware ('CANNOT PLAY').\n\nWould you like to extract the authentic factory key from your player now?"
            alert.alertStyle = .warning
            alert.addButton(withTitle: "🔑 Extract Key Now")
            alert.addButton(withTitle: "Cancel")
            alert.addButton(withTitle: "Sync with Placeholder Anyway")
            let response = alert.runModal()
            if response == .alertFirstButtonReturn {
                extractKeyClicked()
                return
            } else if response == .alertSecondButtonReturn {
                return
            }
        }
        
        let selectedIndex = codecPopUp?.indexOfSelectedItem ?? 0
        let selectedCodec: WalkmanDBGenerator.AudioCodec
        switch selectedIndex {
        case 0: selectedCodec = .atrac3
        case 1: selectedCodec = .atrac3_lp4
        case 2: selectedCodec = .atrac3plus
        case 3: selectedCodec = .mp3
        default: selectedCodec = .atrac3
        }
        
        WalkmanLogger.info("Sync started from source: \(source.path) to Walkman: \(destination.path) using codec \(selectedCodec.rawValue)")
        syncButton?.isEnabled = false
        statusLabel?.stringValue = "Scanning files..."
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                // 1. Scan for Music and Read ID3 Tags
                let titles = SyncEngine.scanForMusic(in: source)
                guard !titles.isEmpty else {
                    WalkmanLogger.warn("No audio files found in: \(source.path)")
                    DispatchQueue.main.async {
                        self.statusLabel?.stringValue = "No supported audio files (MP3, FLAC, M4A) found."
                        self.syncButton?.isEnabled = true
                    }
                    return
                }
                
                WalkmanLogger.info("Scanned \(titles.count) tracks from source")
                
                // Preflight dependency validation
                let hasNonMP3 = titles.contains { ($0.originalFile?.pathExtension.lowercased() ?? "") != "mp3" }
                let requiresFFmpeg = selectedCodec != .mp3 || hasNonMP3
                
                if requiresFFmpeg && SyncEngine.findFFmpeg() == nil {
                    DispatchQueue.main.async {
                        self.syncButton?.isEnabled = true
                        self.statusLabel?.stringValue = "FFmpeg required. Run 'brew install ffmpeg' in Terminal."
                        
                        let alert = NSAlert()
                        alert.messageText = "FFmpeg Required for This Operation"
                        alert.informativeText = "ATRAC3 encoding and non-MP3 files (FLAC, M4A, WAV, etc.) require FFmpeg on your Mac.\n\nTo install FFmpeg, open Terminal and run:\n\n    brew install ffmpeg\n\nTip: You can sync standard .mp3 files directly using 'MP3 (320 kbps CBR)' with zero dependencies."
                        alert.alertStyle = .warning
                        alert.addButton(withTitle: "OK")
                        alert.addButton(withTitle: "Copy 'brew install ffmpeg'")
                        let response = alert.runModal()
                        if response == .alertSecondButtonReturn {
                            NSPasteboard.general.clearContents()
                            NSPasteboard.general.setString("brew install ffmpeg", forType: .string)
                        }
                    }
                    return
                }
                
                if selectedCodec != .mp3 && SyncEngine.findAtracdenc() == nil {
                    DispatchQueue.main.async {
                        self.syncButton?.isEnabled = true
                        self.statusLabel?.stringValue = "atracdenc binary missing."
                        
                        let alert = NSAlert()
                        alert.messageText = "atracdenc Encoder Missing"
                        alert.informativeText = "The ATRAC encoder binary (atracdenc) was not found in the application bundle or system PATH.\n\nPlease reinstall WalkmanSync or place atracdenc at /opt/homebrew/bin/atracdenc."
                        alert.alertStyle = .critical
                        alert.addButton(withTitle: "OK")
                        alert.runModal()
                    }
                    return
                }
                
                DispatchQueue.main.async {
                    self.statusLabel?.stringValue = "Starting sync for \(titles.count) tracks (\(selectedCodec.displayName))..."
                }
                
                // 2. Transfer files with selectable encoder
                try SyncEngine.transferFilesToWalkman(titles: titles, destination: destination, codec: selectedCodec) { msg in
                    DispatchQueue.main.async {
                        self.statusLabel?.stringValue = msg
                    }
                }
                
                DispatchQueue.main.async {
                    self.statusLabel?.stringValue = "Building hardware-accurate Walkman database..."
                }
                
                // 3. Generate all OMGAUDIO database files
                let generator = WalkmanDBGenerator(codec: selectedCodec, isEncrypted3rdGen: true)
                try generator.generateDatabase(titles: titles, destination: destination)
                
                // 4. Clean AppleDouble (._*) files created during DB writes and flush
                SyncEngine.cleanAppleDouble(at: destination)
                sync()
                
                WalkmanLogger.info("Sync completed successfully for \(titles.count) tracks!")
                DispatchQueue.main.async {
                    self.statusLabel?.stringValue = "Sync Complete (\(titles.count) tracks synced)!"
                    self.syncButton?.isEnabled = true
                }
                
            } catch {
                WalkmanLogger.error("Sync error: \(error.localizedDescription)")
                DispatchQueue.main.async {
                    self.statusLabel?.stringValue = "Error: \(error.localizedDescription)"
                    self.syncButton?.isEnabled = true
                }
            }
        }
    }
    
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return true
    }
    
    private func setupMainMenu() {
        let mainMenu = NSMenu()
        let appMenuItem = NSMenuItem()
        mainMenu.addItem(appMenuItem)
        let appMenu = NSMenu()
        appMenu.addItem(NSMenuItem(title: "About WalkmanSync", action: #selector(NSApplication.orderFrontStandardAboutPanel(_:)), keyEquivalent: ""))
        appMenu.addItem(NSMenuItem.separator())
        appMenu.addItem(NSMenuItem(title: "Quit WalkmanSync", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q"))
        appMenuItem.submenu = appMenu
        
        // Help / Logs Menu
        let helpMenuItem = NSMenuItem()
        mainMenu.addItem(helpMenuItem)
        let helpMenu = NSMenu(title: "Help")
        helpMenu.addItem(NSMenuItem(title: "Open Log File in Console", action: #selector(openLogs), keyEquivalent: "l"))
        helpMenu.addItem(NSMenuItem(title: "Reveal Log File in Finder", action: #selector(revealLogs), keyEquivalent: "L"))
        helpMenuItem.submenu = helpMenu
        
        NSApp.mainMenu = mainMenu
    }
    
    @objc func revealLogs() {
        WalkmanLogger.openLogInFinder()
    }
}
