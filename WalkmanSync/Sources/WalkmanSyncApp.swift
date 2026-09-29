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
    var storageLabel: NSTextField!
    var capacityLabel: NSTextField!
    var codecPopUp: NSPopUpButton!
    var syncButton: NSButton!
    var statusLabel: NSTextField!
    
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
        walkmanPathLabel.frame = NSMakeRect(140, 260, 380, 20)
        walkmanPathLabel.textColor = .systemOrange
        contentView.addSubview(walkmanPathLabel)
        
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
        
        // --- Status & Logs ---
        statusLabel = NSTextField(labelWithString: "Ready")
        statusLabel.alignment = .center
        statusLabel.textColor = .secondaryLabelColor
        statusLabel.frame = NSMakeRect(20, 80, 480, 20)
        contentView.addSubview(statusLabel)
        
        let logButton = NSButton(title: "View Logs", target: self, action: #selector(openLogs))
        logButton.bezelStyle = .inline
        logButton.font = NSFont.systemFont(ofSize: 10)
        logButton.frame = NSMakeRect(420, 22, 80, 24)
        contentView.addSubview(logButton)
        
        // --- Sync Button ---
        syncButton = NSButton(title: "Sync to Walkman", target: self, action: #selector(startSync))
        syncButton.frame = NSMakeRect(160, 20, 200, 42)
        syncButton.bezelStyle = .rounded
        syncButton.isEnabled = false
        contentView.addSubview(syncButton)
        
        window.contentView = contentView
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
        
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
                storageLabel?.stringValue = ""
                capacityLabel?.stringValue = ""
                updateSyncButton()
            }
        }
    }
    
    @objc func codecChanged() {
        updateStorageDisplay()
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
        let keyHex = String(format: "0x%08X", key)
        
        storageLabel?.stringValue = "\(freeMB) MB free of \(totalMB) MB (\(percentFree)% available) • Key: \(keyHex)"
        
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
    
    @objc func openLogs() {
        WalkmanLogger.openLogInConsole()
    }
    
    @objc func startSync() {
        guard let source = sourceUrl, let destination = walkmanUrl else { return }
        
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
