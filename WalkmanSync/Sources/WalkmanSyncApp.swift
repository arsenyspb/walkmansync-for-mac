import Cocoa

@main
struct WalkmanSyncMain {
    static func main() {
        let app = NSApplication.shared
        let delegate = AppDelegate()
        app.delegate = delegate
        app.setActivationPolicy(.regular)
        app.run()
    }
}

class AppDelegate: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    
    // UI Elements
    var sourcePathLabel: NSTextField!
    var walkmanPathLabel: NSTextField!
    var syncButton: NSButton!
    var statusLabel: NSTextField!
    
    var sourceUrl: URL?
    var walkmanUrl: URL?
    
    func applicationDidFinishLaunching(_ aNotification: Notification) {
        setupMainMenu()
        
        // Set Dock Icon dynamically
        if let iconUrl = Bundle.main.url(forResource: "AppIcon", withExtension: "icns"),
           let icon = NSImage(contentsOf: iconUrl) {
            NSApp.applicationIconImage = icon
        }
        
        window = NSWindow(contentRect: NSMakeRect(0, 0, 520, 320),
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
            let logoView = NSImageView(frame: NSMakeRect(20, 246, 75, 40))
            logoView.image = logo
            logoView.imageScaling = .scaleProportionallyUpOrDown
            // Iconic Walkman Signature Orange #F26522
            logoView.contentTintColor = NSColor(red: 0.95, green: 0.40, blue: 0.13, alpha: 1.0)
            contentView.addSubview(logoView)
        }
        
        let titleLabel = NSTextField(labelWithString: "Walkman Sync")
        titleLabel.font = NSFont.boldSystemFont(ofSize: 22)
        titleLabel.frame = NSMakeRect(105, 258, 395, 28)
        contentView.addSubview(titleLabel)
        
        let subTitle = NSTextField(labelWithString: "Zero-friction native music sync for Sony Network Walkman")
        subTitle.font = NSFont.systemFont(ofSize: 11)
        subTitle.textColor = .secondaryLabelColor
        subTitle.frame = NSMakeRect(105, 240, 395, 18)
        contentView.addSubview(subTitle)
        
        // --- Source Folder ---
        let sourceTitle = NSTextField(labelWithString: "Music Source:")
        sourceTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        sourceTitle.frame = NSMakeRect(20, 185, 115, 20)
        contentView.addSubview(sourceTitle)
        
        sourcePathLabel = NSTextField(labelWithString: "Not Selected")
        sourcePathLabel.frame = NSMakeRect(140, 185, 260, 20)
        sourcePathLabel.textColor = .secondaryLabelColor
        contentView.addSubview(sourcePathLabel)
        
        let sourceBtn = NSButton(title: "Select", target: self, action: #selector(selectSource))
        sourceBtn.frame = NSMakeRect(410, 180, 90, 30)
        contentView.addSubview(sourceBtn)
        
        // --- Walkman Folder ---
        let walkmanTitle = NSTextField(labelWithString: "Walkman Volume:")
        walkmanTitle.font = NSFont.systemFont(ofSize: 13, weight: .medium)
        walkmanTitle.frame = NSMakeRect(20, 140, 115, 20)
        contentView.addSubview(walkmanTitle)
        
        walkmanPathLabel = NSTextField(labelWithString: "Not Selected")
        walkmanPathLabel.frame = NSMakeRect(140, 140, 260, 20)
        walkmanPathLabel.textColor = .secondaryLabelColor
        contentView.addSubview(walkmanPathLabel)
        
        let walkmanBtn = NSButton(title: "Browse...", target: self, action: #selector(selectWalkman))
        walkmanBtn.frame = NSMakeRect(410, 135, 90, 30)
        contentView.addSubview(walkmanBtn)
        
        // Auto-detect connected Walkman across /Volumes
        autoDetectWalkman()
        
        // --- Status & Logs ---
        statusLabel = NSTextField(labelWithString: "Ready")
        statusLabel.alignment = .center
        statusLabel.textColor = .secondaryLabelColor
        statusLabel.frame = NSMakeRect(20, 85, 480, 20)
        contentView.addSubview(statusLabel)
        
        let logButton = NSButton(title: "View Logs", target: self, action: #selector(openLogs))
        logButton.bezelStyle = .inline
        logButton.font = NSFont.systemFont(ofSize: 10)
        logButton.frame = NSMakeRect(420, 25, 80, 24)
        contentView.addSubview(logButton)
        
        // --- Sync Button ---
        syncButton = NSButton(title: "Sync to Walkman", target: self, action: #selector(startSync))
        syncButton.frame = NSMakeRect(160, 25, 200, 42)
        syncButton.bezelStyle = .rounded
        syncButton.isEnabled = (sourceUrl != nil && walkmanUrl != nil)
        contentView.addSubview(syncButton)
        
        window.contentView = contentView
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }
    
    private func autoDetectWalkman() {
        let fm = FileManager.default
        let volumesURL = URL(fileURLWithPath: "/Volumes")
        if let volumes = try? fm.contentsOfDirectory(at: volumesURL, includingPropertiesForKeys: nil) {
            for vol in volumes {
                let name = vol.lastPathComponent.uppercased()
                let hasOmgAudio = fm.fileExists(atPath: vol.appendingPathComponent("OMGAUDIO").path)
                if name == "WALKMAN" || hasOmgAudio {
                    walkmanUrl = vol
                    walkmanPathLabel.stringValue = "\(vol.path) (Connected)"
                    walkmanPathLabel.textColor = .systemGreen
                    return
                }
            }
        }
        
        // Fallback check
        let defaultWalkman = URL(fileURLWithPath: "/Volumes/WALKMAN")
        if fm.fileExists(atPath: defaultWalkman.path) {
            walkmanUrl = defaultWalkman
            walkmanPathLabel.stringValue = "/Volumes/WALKMAN (Connected)"
            walkmanPathLabel.textColor = .systemGreen
        }
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
                    sourcePathLabel.stringValue = "\(url.lastPathComponent) (\(count) track\(count == 1 ? "" : "s"))"
                    sourcePathLabel.textColor = .labelColor
                    statusLabel.stringValue = "Found \(count) audio track\(count == 1 ? "" : "s") ready to sync."
                } else {
                    sourcePathLabel.stringValue = "\(url.lastPathComponent) (0 tracks found)"
                    sourcePathLabel.textColor = .systemRed
                    statusLabel.stringValue = "No supported audio files (MP3, FLAC, M4A) found."
                }
            }
            updateSyncButton()
        }
    }
    
    @objc func selectWalkman() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.directoryURL = URL(fileURLWithPath: "/Volumes")
        panel.prompt = "Select Volume"
        panel.message = "Select your mounted Walkman drive from /Volumes"
        if panel.runModal() == .OK {
            if let url = panel.url {
                walkmanUrl = url
                let hasOmgAudio = FileManager.default.fileExists(atPath: url.appendingPathComponent("OMGAUDIO").path)
                let suffix = hasOmgAudio ? " (Walkman Detected)" : " (Selected)"
                walkmanPathLabel.stringValue = "\(url.path)\(suffix)"
                walkmanPathLabel.textColor = .systemGreen
                updateSyncButton()
            }
        }
    }
    
    func updateSyncButton() {
        syncButton.isEnabled = (sourceUrl != nil && walkmanUrl != nil)
    }
    
    @objc func openLogs() {
        WalkmanLogger.openLogInConsole()
    }
    
    @objc func startSync() {
        guard let source = sourceUrl, let destination = walkmanUrl else { return }
        
        WalkmanLogger.info("Sync started from source: \(source.path) to Walkman: \(destination.path)")
        syncButton.isEnabled = false
        statusLabel.stringValue = "Scanning files..."
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                // 1. Scan for Music and Read ID3 Tags
                let titles = SyncEngine.scanForMusic(in: source)
                guard !titles.isEmpty else {
                    WalkmanLogger.warn("No audio files found in: \(source.path)")
                    DispatchQueue.main.async {
                        self.statusLabel.stringValue = "No supported audio files (MP3, FLAC, M4A) found."
                        self.syncButton.isEnabled = true
                    }
                    return
                }
                
                WalkmanLogger.info("Scanned \(titles.count) tracks from source")
                
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Starting sync for \(titles.count) tracks..."
                }
                
                // 2. Transfer files with automated DvID key resolution & XOR scramble
                try SyncEngine.transferFilesToWalkman(titles: titles, destination: destination) { msg in
                    DispatchQueue.main.async {
                        self.statusLabel.stringValue = msg
                    }
                }
                
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Building hardware-accurate Walkman database..."
                }
                
                // 3. Generate all 8 OMGAUDIO database files
                let generator = WalkmanDBGenerator(isEncrypted3rdGen: true)
                try generator.generateDatabase(titles: titles, destination: destination)
                
                WalkmanLogger.info("Sync completed successfully for \(titles.count) tracks!")
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Sync Complete (\(titles.count) tracks synced)!"
                    self.syncButton.isEnabled = true
                }
                
            } catch {
                WalkmanLogger.error("Sync error: \(error.localizedDescription)")
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Error: \(error.localizedDescription)"
                    self.syncButton.isEnabled = true
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
