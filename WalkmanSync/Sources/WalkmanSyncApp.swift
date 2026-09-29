import Cocoa

@main
class AppDelegate: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    
    // UI Elements
    var sourcePathLabel: NSTextField!
    var walkmanPathLabel: NSTextField!
    var dvidPathLabel: NSTextField!
    var syncButton: NSButton!
    var statusLabel: NSTextField!
    
    var sourceUrl: URL?
    var walkmanUrl: URL?
    var dvidUrl: URL?
    
    func applicationDidFinishLaunching(_ aNotification: Notification) {
        window = NSWindow(contentRect: NSMakeRect(0, 0, 500, 350),
                          styleMask: [.titled, .closable, .miniaturizable],
                          backing: .buffered,
                          defer: false)
        window.center()
        window.title = "Walkman Sync (Native Mac Port)"
        
        let contentView = NSView(frame: window.contentRect(forFrameRect: window.frame))
        
        let titleLabel = NSTextField(labelWithString: "Walkman Sync")
        titleLabel.font = NSFont.boldSystemFont(ofSize: 24)
        titleLabel.frame = NSMakeRect(20, 290, 460, 30)
        contentView.addSubview(titleLabel)
        
        // --- Source Folder ---
        let sourceTitle = NSTextField(labelWithString: "Music Source:")
        sourceTitle.frame = NSMakeRect(20, 240, 120, 20)
        contentView.addSubview(sourceTitle)
        
        sourcePathLabel = NSTextField(labelWithString: "Not Selected")
        sourcePathLabel.frame = NSMakeRect(140, 240, 240, 20)
        sourcePathLabel.textColor = .gray
        contentView.addSubview(sourcePathLabel)
        
        let sourceBtn = NSButton(title: "Select", target: self, action: #selector(selectSource))
        sourceBtn.frame = NSMakeRect(390, 235, 90, 30)
        contentView.addSubview(sourceBtn)
        
        // --- Walkman Folder ---
        let walkmanTitle = NSTextField(labelWithString: "Walkman Volume:")
        walkmanTitle.frame = NSMakeRect(20, 190, 120, 20)
        contentView.addSubview(walkmanTitle)
        
        walkmanPathLabel = NSTextField(labelWithString: "Not Selected")
        walkmanPathLabel.frame = NSMakeRect(140, 190, 240, 20)
        walkmanPathLabel.textColor = .gray
        contentView.addSubview(walkmanPathLabel)
        
        let walkmanBtn = NSButton(title: "Select", target: self, action: #selector(selectWalkman))
        walkmanBtn.frame = NSMakeRect(390, 185, 90, 30)
        contentView.addSubview(walkmanBtn)
        
        // --- DvID.DAT ---
        let dvidTitle = NSTextField(labelWithString: "DvID.DAT Key:")
        dvidTitle.frame = NSMakeRect(20, 140, 120, 20)
        contentView.addSubview(dvidTitle)
        
        dvidPathLabel = NSTextField(labelWithString: "Optional (3rd Gen)")
        dvidPathLabel.frame = NSMakeRect(140, 140, 240, 20)
        dvidPathLabel.textColor = .gray
        contentView.addSubview(dvidPathLabel)
        
        let dvidBtn = NSButton(title: "Select", target: self, action: #selector(selectDvID))
        dvidBtn.frame = NSMakeRect(390, 135, 90, 30)
        contentView.addSubview(dvidBtn)
        
        let dvidNote = NSTextField(labelWithString: "Provides the encryption key required by 3rd Gen Walkmans (e.g. NW-E40x).")
        dvidNote.font = NSFont.systemFont(ofSize: 10)
        dvidNote.textColor = .secondaryLabelColor
        dvidNote.frame = NSMakeRect(140, 120, 340, 15)
        contentView.addSubview(dvidNote)
        
        // --- Status ---
        statusLabel = NSTextField(labelWithString: "Ready")
        statusLabel.alignment = .center
        statusLabel.textColor = .secondaryLabelColor
        statusLabel.frame = NSMakeRect(20, 70, 460, 20)
        contentView.addSubview(statusLabel)
        
        // --- Sync Button ---
        syncButton = NSButton(title: "Sync to Walkman", target: self, action: #selector(startSync))
        syncButton.frame = NSMakeRect(150, 20, 200, 40)
        syncButton.bezelStyle = .rounded
        syncButton.isEnabled = false
        contentView.addSubview(syncButton)
        
        window.contentView = contentView
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }
    
    @objc func selectSource() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        if panel.runModal() == .OK {
            sourceUrl = panel.url
            sourcePathLabel.stringValue = sourceUrl?.path ?? ""
            sourcePathLabel.textColor = .labelColor
            updateSyncButton()
        }
    }
    
    @objc func selectWalkman() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        if panel.runModal() == .OK {
            walkmanUrl = panel.url
            walkmanPathLabel.stringValue = walkmanUrl?.path ?? ""
            walkmanPathLabel.textColor = .labelColor
            updateSyncButton()
        }
    }
    
    @objc func selectDvID() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        if panel.runModal() == .OK {
            dvidUrl = panel.url
            dvidPathLabel.stringValue = dvidUrl?.path ?? ""
            dvidPathLabel.textColor = .labelColor
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
                
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Transferring \(titles.count) files..."
                }
                
                // 2. Install DvID.DAT if provided
                if let dvid = self.dvidUrl {
                    try SyncEngine.installDVID(sourceDVID: dvid, destination: destination)
                }
                
                // 3. Copy MP3s to OMGAUDIO
                try SyncEngine.copyFilesToWalkman(titles: titles, destination: destination)
                
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Building Walkman Database..."
                }
                
                // 4. Generate the DB files
                let generator = WalkmanDBGenerator(dvidFile: self.dvidUrl)
                try generator.generateDatabase(titles: titles, destination: destination)
                
                DispatchQueue.main.async {
                    self.statusLabel.stringValue = "Sync Complete!"
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
}
