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
        
        window = NSWindow(contentRect: NSMakeRect(0, 0, 500, 300),
                          styleMask: [.titled, .closable, .miniaturizable],
                          backing: .buffered,
                          defer: false)
        window.center()
        window.title = "Walkman Sync (Native Mac Port)"
        
        let contentView = NSView(frame: window.contentRect(forFrameRect: window.frame))
        
        let titleLabel = NSTextField(labelWithString: "Walkman Sync")
        titleLabel.font = NSFont.boldSystemFont(ofSize: 24)
        titleLabel.frame = NSMakeRect(20, 240, 460, 30)
        contentView.addSubview(titleLabel)
        
        let subTitle = NSTextField(labelWithString: "Zero-friction native MP3 sync for Sony Network Walkman (NW-E40x)")
        subTitle.font = NSFont.systemFont(ofSize: 11)
        subTitle.textColor = .secondaryLabelColor
        subTitle.frame = NSMakeRect(20, 220, 460, 18)
        contentView.addSubview(subTitle)
        
        // --- Source Folder ---
        let sourceTitle = NSTextField(labelWithString: "Music Source:")
        sourceTitle.frame = NSMakeRect(20, 175, 120, 20)
        contentView.addSubview(sourceTitle)
        
        sourcePathLabel = NSTextField(labelWithString: "Not Selected")
        sourcePathLabel.frame = NSMakeRect(140, 175, 240, 20)
        sourcePathLabel.textColor = .gray
        contentView.addSubview(sourcePathLabel)
        
        let sourceBtn = NSButton(title: "Select", target: self, action: #selector(selectSource))
        sourceBtn.frame = NSMakeRect(390, 170, 90, 30)
        contentView.addSubview(sourceBtn)
        
        // --- Walkman Folder ---
        let walkmanTitle = NSTextField(labelWithString: "Walkman Volume:")
        walkmanTitle.frame = NSMakeRect(20, 130, 120, 20)
        contentView.addSubview(walkmanTitle)
        
        walkmanPathLabel = NSTextField(labelWithString: "Not Selected")
        walkmanPathLabel.frame = NSMakeRect(140, 130, 240, 20)
        walkmanPathLabel.textColor = .gray
        contentView.addSubview(walkmanPathLabel)
        
        let walkmanBtn = NSButton(title: "Browse...", target: self, action: #selector(selectWalkman))
        walkmanBtn.frame = NSMakeRect(390, 125, 90, 30)
        contentView.addSubview(walkmanBtn)
        
        // Auto-detect connected Walkman across /Volumes
        autoDetectWalkman()
        
        // --- Status ---
        statusLabel = NSTextField(labelWithString: "Ready")
        statusLabel.alignment = .center
        statusLabel.textColor = .secondaryLabelColor
        statusLabel.frame = NSMakeRect(20, 80, 460, 20)
        contentView.addSubview(statusLabel)
        
        // --- Sync Button ---
        syncButton = NSButton(title: "Sync to Walkman", target: self, action: #selector(startSync))
        syncButton.frame = NSMakeRect(150, 25, 200, 40)
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
    
    @objc func startSync() {
        guard let source = sourceUrl, let destination = walkmanUrl else { return }
        
        syncButton.isEnabled = false
        statusLabel.stringValue = "Scanning files..."
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                // 1. Scan for Music and Read ID3 Tags
                let titles = SyncEngine.scanForMusic(in: source)
                guard !titles.isEmpty else {
                    DispatchQueue.main.async {
                        self.statusLabel.stringValue = "No supported audio files (MP3, FLAC, M4A) found."
                        self.syncButton.isEnabled = true
                    }
                    return
                }
                
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
                    self.statusLabel.stringValue = "Building Walkman Database..."
                }
                
                // 3. Generate the DB files with 3rd Gen encryption flags
                let generator = WalkmanDBGenerator(isEncrypted3rdGen: true)
                try generator.generateDatabase(titles: titles, destination: destination)
                
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Sync Complete (\(titles.count) tracks synced)!"
                    self.syncButton.isEnabled = true
                }
                
            } catch {
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
        NSApp.mainMenu = mainMenu
    }
}
