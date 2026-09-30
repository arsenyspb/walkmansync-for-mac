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
    var updateBadgeBtn: NSButton!
    var storageLabel: NSTextField!
    var capacityLabel: NSTextField!
    var bitratePopUp: NSPopUpButton!
    var vbrCheckbox: NSButton!
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
        titleLabel.frame = NSMakeRect(105, 368, 250, 28)
        contentView.addSubview(titleLabel)
        
        // Update badge button (top-right, initially hidden)
        updateBadgeBtn = NSButton(title: "✨ Update Available", target: self, action: #selector(openUpdateURL))
        updateBadgeBtn.bezelStyle = .inline
        updateBadgeBtn.font = NSFont.systemFont(ofSize: 11, weight: .medium)
        updateBadgeBtn.contentTintColor = .systemBlue
        updateBadgeBtn.frame = NSMakeRect(380, 368, 140, 24)
        updateBadgeBtn.isHidden = true
        contentView.addSubview(updateBadgeBtn)
        
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
        
        // --- Audio Quality Selection ---
        let codecTitle = NSTextField(labelWithString: "MP3 Quality:")
        codecTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        codecTitle.frame = NSMakeRect(20, 135, 115, 20)
        contentView.addSubview(codecTitle)
        
        bitratePopUp = NSPopUpButton(frame: NSMakeRect(138, 130, 245, 28), pullsDown: false)
        for rate in WalkmanDBGenerator.MP3Bitrate.allCases {
            bitratePopUp.addItem(withTitle: rate.displayName)
        }
        bitratePopUp.target = self
        bitratePopUp.action = #selector(qualityChanged)
        contentView.addSubview(bitratePopUp)
        
        vbrCheckbox = NSButton(checkboxWithTitle: "Use VBR", target: self, action: #selector(qualityChanged))
        vbrCheckbox.frame = NSMakeRect(395, 133, 125, 22)
        vbrCheckbox.toolTip = "Variable Bit Rate: Dynamically adapts bitrate while capping at the selected ceiling."
        contentView.addSubview(vbrCheckbox)
        
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
        
        // Check for updates asynchronously on startup
        checkForUpdates(silentIfLatest: true)
        
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
    
    @objc func qualityChanged() {
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
        
        let isVBR = vbrCheckbox?.state == .on
        let selectedIndex = bitratePopUp?.indexOfSelectedItem ?? 0
        let selectedBitrate: WalkmanDBGenerator.MP3Bitrate
        if selectedIndex >= 0 && selectedIndex < WalkmanDBGenerator.MP3Bitrate.allCases.count {
            selectedBitrate = WalkmanDBGenerator.MP3Bitrate.allCases[selectedIndex]
        } else {
            selectedBitrate = .kbps192
        }
        
        let mbPerSong = isVBR ? (selectedBitrate.averageMbPerSong * 0.8) : selectedBitrate.averageMbPerSong
        let songEstimate = max(0, Int(Double(freeMB) / mbPerSong))
        
        let modeLabel = isVBR ? "\(selectedBitrate.rawValue) kbps VBR (Adaptive with \(selectedBitrate.rawValue)k cap)" : "\(selectedBitrate.rawValue) kbps CBR"
        let attr = NSMutableAttributedString()
        let primaryStr = "~\(songEstimate) songs at \(modeLabel)\n"
        attr.append(NSAttributedString(string: primaryStr, attributes: [
            .font: NSFont.systemFont(ofSize: 11, weight: .semibold),
            .foregroundColor: NSColor.labelColor
        ]))
        
        let c320 = Int(Double(freeMB) / 7.5)
        let c256 = Int(Double(freeMB) / 6.0)
        let c192 = Int(Double(freeMB) / 4.5)
        let c128 = Int(Double(freeMB) / 3.0)
        let c96 = Int(Double(freeMB) / 2.2)
        let comparisonStr = "All: ~\(c320) (320k) • ~\(c256) (256k) • ~\(c192) (192k) • ~\(c128) (128k) • ~\(c96) (96k)"
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
        if hasFFmpeg {
            dependencyLabel?.stringValue = "✓ Audio Engine: Ready (FFmpeg active for FLAC/M4A/WAV)"
            dependencyLabel?.textColor = .secondaryLabelColor
        } else {
            dependencyLabel?.stringValue = "✓ Pure MP3 Mode (Zero external tools needed for standard .mp3)"
            dependencyLabel?.textColor = .systemGreen
        }
    }
    
    @objc func showDoctorDialog() {
        let ffmpeg = SyncEngine.findFFmpeg()
        let walkman = walkmanUrl ?? SyncEngine.findWalkmanVolume()
        let key = walkman != nil ? WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: walkman!) : nil
        
        let alert = NSAlert()
        alert.messageText = "WalkmanSync System Health & Diagnostics"
        
        var message = ""
        
        // FFmpeg
        if let ff = ffmpeg {
            message += "✓ FFmpeg: Installed (\(ff))\n"
            message += "   Ready for: FLAC, Apple M4A, ALAC, WAV, and AIFF conversion\n\n"
        } else {
            message += "• FFmpeg: Not Installed (Optional)\n"
            message += "   Only needed if you want to sync lossless FLAC or Apple M4A files.\n"
            message += "   Standard .mp3 files sync with 100% zero external dependencies!\n"
            message += "   Install via Terminal: brew install ffmpeg\n\n"
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
        if let vol = walkman {
            alert.addButton(withTitle: "📥 Dump Tracks to Mac...")
            alert.addButton(withTitle: "🗑️ Erase Walkman Music...")
            
            let response = alert.runModal()
            if response == .alertSecondButtonReturn {
                startDumpTracks(walkmanURL: vol)
            } else if response == .alertThirdButtonReturn {
                startEraseWalkman(walkmanURL: vol)
            }
        } else {
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
    }
    
    func startDumpTracks(walkmanURL: URL) {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.canCreateDirectories = true
        panel.prompt = "Export Tracks Here"
        panel.message = "Choose a destination folder to export your Walkman music library:"
        
        if panel.runModal() == .OK, let destURL = panel.url {
            syncButton?.isEnabled = false
            statusLabel?.stringValue = "Scanning and descrambling tracks from Walkman..."
            statusLabel?.textColor = .labelColor
            
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                do {
                    let result = try WalkmanTrackDumper.dumpTracks(from: walkmanURL, to: destURL) { cur, tot, title in
                        DispatchQueue.main.async {
                            self?.statusLabel?.stringValue = "[\(cur)/\(tot)] Descrambling \(title)..."
                        }
                    }
                    
                    DispatchQueue.main.async {
                        self?.syncButton?.isEnabled = true
                        self?.statusLabel?.stringValue = "✓ Dumped \(result.tracksDumped) tracks to \(destURL.lastPathComponent)"
                        self?.statusLabel?.textColor = .systemGreen
                        
                        let alert = NSAlert()
                        alert.messageText = "Track Export Completed"
                        alert.informativeText = "Successfully recovered and descrambled \(result.tracksDumped) MP3 tracks to:\n\(result.outputDirectory.path)"
                        alert.addButton(withTitle: "Show in Finder")
                        alert.addButton(withTitle: "OK")
                        if alert.runModal() == .alertFirstButtonReturn {
                            NSWorkspace.shared.selectFile(nil, inFileViewerRootedAtPath: result.outputDirectory.path)
                        }
                    }
                } catch {
                    DispatchQueue.main.async {
                        self?.syncButton?.isEnabled = true
                        self?.statusLabel?.stringValue = "Export failed: \(error.localizedDescription)"
                        self?.statusLabel?.textColor = .systemRed
                        
                        let errAlert = NSAlert()
                        errAlert.messageText = "Track Export Failed"
                        errAlert.informativeText = error.localizedDescription
                        errAlert.alertStyle = .critical
                        errAlert.runModal()
                    }
                }
            }
        }
    }
    
    func startEraseWalkman(walkmanURL: URL) {
        let confirmAlert = NSAlert()
        confirmAlert.messageText = "Erase All Music from \(walkmanURL.lastPathComponent)?"
        confirmAlert.informativeText = "This will delete all music tracks and reset the OMGAUDIO database so your Walkman shows 'NO DATA' (100% free space).\n\nYour authentic hardware encryption key (DvID.DAT) will be safely preserved so future syncs will play immediately."
        confirmAlert.alertStyle = .critical
        confirmAlert.addButton(withTitle: "Cancel")
        let eraseBtn = confirmAlert.addButton(withTitle: "Erase Music")
        if #available(macOS 11.0, *) {
            eraseBtn.hasDestructiveAction = true
        }
        
        guard confirmAlert.runModal() == .alertSecondButtonReturn else { return }
        
        syncButton?.isEnabled = false
        statusLabel?.stringValue = "Erasing Walkman and cleaning storage..."
        statusLabel?.textColor = .labelColor
        
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            do {
                let result = try WalkmanCleaner.eraseWalkman(at: walkmanURL) { status in
                    DispatchQueue.main.async {
                        self?.statusLabel?.stringValue = status
                    }
                }
                
                DispatchQueue.main.async {
                    self?.syncButton?.isEnabled = true
                    self?.updateStorageDisplay()
                    
                    let keyHex = String(format: "0x%08X", result.keyPreserved)
                    self?.statusLabel?.stringValue = "✓ Erased \(result.tracksDeleted) tracks (Key \(keyHex) preserved)"
                    self?.statusLabel?.textColor = .systemGreen
                    
                    let doneAlert = NSAlert()
                    doneAlert.messageText = "Walkman Reset Complete"
                    doneAlert.informativeText = "Successfully erased \(result.tracksDeleted) tracks and cleaned all hidden storage leaks.\n\nHardware Key: \(keyHex) (\(result.keyWasAuthentic ? "Authentic ✓" : "Default"))\nYour player now shows 'NO DATA' with 100% capacity."
                    doneAlert.runModal()
                }
            } catch {
                DispatchQueue.main.async {
                    self?.syncButton?.isEnabled = true
                    self?.statusLabel?.stringValue = "Erase failed: \(error.localizedDescription)"
                    self?.statusLabel?.textColor = .systemRed
                    
                    let errAlert = NSAlert()
                    errAlert.messageText = "Erase Failed"
                    errAlert.informativeText = error.localizedDescription
                    errAlert.alertStyle = .critical
                    errAlert.runModal()
                }
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
        
        let selectedIndex = bitratePopUp?.indexOfSelectedItem ?? 0
        let selectedBitrate: WalkmanDBGenerator.MP3Bitrate
        if selectedIndex >= 0 && selectedIndex < WalkmanDBGenerator.MP3Bitrate.allCases.count {
            selectedBitrate = WalkmanDBGenerator.MP3Bitrate.allCases[selectedIndex]
        } else {
            selectedBitrate = .kbps192
        }
        let isVBR = vbrCheckbox?.state == .on
        let modeDesc = isVBR ? "\(selectedBitrate.rawValue)k VBR" : "\(selectedBitrate.rawValue)k CBR"
        
        WalkmanLogger.info("Sync started from source: \(source.path) to Walkman: \(destination.path) using MP3 \(modeDesc)")
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
                
                // Preflight dependency validation for non-MP3 files
                let hasNonMP3 = titles.contains { ($0.originalFile?.pathExtension.lowercased() ?? "") != "mp3" }
                if hasNonMP3 && SyncEngine.findFFmpeg() == nil {
                    DispatchQueue.main.async {
                        self.syncButton?.isEnabled = true
                        self.statusLabel?.stringValue = "FFmpeg required. Run 'brew install ffmpeg' in Terminal."
                        
                        let alert = NSAlert()
                        alert.messageText = "FFmpeg Required for Audio Conversion"
                        alert.informativeText = "Non-MP3 files (FLAC, M4A, WAV, etc.) require FFmpeg on your Mac to convert into Walkman MP3.\n\nTo install FFmpeg, open Terminal and run:\n\n    brew install ffmpeg\n\nTip: You can sync standard .mp3 files directly with zero external dependencies."
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
                
                DispatchQueue.main.async {
                    self.statusLabel?.stringValue = "Starting sync for \(titles.count) tracks (MP3 \(modeDesc))..."
                }
                
                // 2. Transfer files with selectable bitrate and VBR setting
                try SyncEngine.transferFilesToWalkman(titles: titles, destination: destination, bitrate: selectedBitrate, isVBR: isVBR) { msg in
                    DispatchQueue.main.async {
                        self.statusLabel?.stringValue = msg
                    }
                }
                
                DispatchQueue.main.async {
                    self.statusLabel?.stringValue = "Building hardware-accurate Walkman database..."
                }
                
                // 3. Generate all OMGAUDIO database files
                let generator = WalkmanDBGenerator(mp3Bitrate: selectedBitrate, isVBR: isVBR, isEncrypted3rdGen: true)
                try generator.generateDatabase(titles: titles, destination: destination)
                
                // 4. Clean AppleDouble (._*) files created during DB writes and flush
                SyncEngine.cleanAppleDouble(at: destination)
                Darwin.sync()
                
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
        appMenu.addItem(NSMenuItem(title: "Check for Updates...", action: #selector(checkForUpdatesManual), keyEquivalent: "U"))
        appMenu.addItem(NSMenuItem.separator())
        appMenu.addItem(NSMenuItem(title: "Quit WalkmanSync", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q"))
        appMenuItem.submenu = appMenu
        
        // Help / Logs Menu
        let helpMenuItem = NSMenuItem()
        mainMenu.addItem(helpMenuItem)
        let helpMenu = NSMenu(title: "Help")
        helpMenu.addItem(NSMenuItem(title: "Open Log File in Console", action: #selector(openLogs), keyEquivalent: "l"))
        helpMenu.addItem(NSMenuItem(title: "Reveal Log File in Finder", action: #selector(revealLogs), keyEquivalent: "L"))
        helpMenu.addItem(NSMenuItem.separator())
        helpMenu.addItem(NSMenuItem(title: "Visit GitHub Project Page", action: #selector(openProjectURL), keyEquivalent: ""))
        helpMenuItem.submenu = helpMenu
        
        NSApp.mainMenu = mainMenu
    }
    
    @objc func openProjectURL() {
        if let url = URL(string: "https://github.com/arsenyspb/walkmansync-for-mac") {
            NSWorkspace.shared.open(url)
        }
    }
    
    var latestReleaseURL: URL?
    
    @objc func checkForUpdatesManual() {
        checkForUpdates(silentIfLatest: false)
    }
    
    @objc func openUpdateURL() {
        if let url = latestReleaseURL ?? URL(string: "https://github.com/arsenyspb/walkmansync-for-mac/releases/latest") {
            NSWorkspace.shared.open(url)
        }
    }
    
    func checkForUpdates(silentIfLatest: Bool) {
        guard let url = URL(string: "https://api.github.com/repos/arsenyspb/walkmansync-for-mac/releases/latest") else { return }
        
        var request = URLRequest(url: url)
        request.timeoutInterval = 4.0
        request.setValue("WalkmanSync/\(CLIHandler.version)", forHTTPHeaderField: "User-Agent")
        request.setValue("application/vnd.github.v3+json", forHTTPHeaderField: "Accept")
        
        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
            guard let data = data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let tagName = json["tag_name"] as? String,
                  let htmlUrlStr = json["html_url"] as? String,
                  let releaseURL = URL(string: htmlUrlStr) else {
                if !silentIfLatest {
                    DispatchQueue.main.async {
                        let alert = NSAlert()
                        alert.messageText = "Update Check Failed"
                        alert.informativeText = "Unable to connect to GitHub to check for updates. Please check your internet connection."
                        alert.alertStyle = .warning
                        alert.addButton(withTitle: "OK")
                        alert.runModal()
                    }
                }
                return
            }
            
            let latestVersion = tagName.trimmingCharacters(in: CharacterSet(charactersIn: "vV"))
            let currentClean = CLIHandler.version.trimmingCharacters(in: CharacterSet(charactersIn: "vV"))
            let isNewer = latestVersion.compare(currentClean, options: .numeric) == .orderedDescending
            
            DispatchQueue.main.async {
                self?.latestReleaseURL = releaseURL
                if isNewer {
                    self?.updateBadgeBtn?.title = "✨ Update: v\(latestVersion)"
                    self?.updateBadgeBtn?.isHidden = false
                    
                    if !silentIfLatest {
                        let alert = NSAlert()
                        alert.messageText = "New Update Available!"
                        alert.informativeText = "WalkmanSync v\(latestVersion) is now available (you are running v\(CLIHandler.version)).\n\nWould you like to open GitHub Releases to download the latest DMG?"
                        alert.alertStyle = .informational
                        alert.addButton(withTitle: "Download Update")
                        alert.addButton(withTitle: "Later")
                        if alert.runModal() == .alertFirstButtonReturn {
                            NSWorkspace.shared.open(releaseURL)
                        }
                    }
                } else if !silentIfLatest {
                    let alert = NSAlert()
                    alert.messageText = "You're Up to Date!"
                    alert.informativeText = "WalkmanSync v\(CLIHandler.version) is currently the newest version available."
                    alert.alertStyle = .informational
                    alert.addButton(withTitle: "OK")
                    alert.runModal()
                }
            }
        }.resume()
    }
    
    @objc func revealLogs() {
        WalkmanLogger.openLogInFinder()
    }
}
