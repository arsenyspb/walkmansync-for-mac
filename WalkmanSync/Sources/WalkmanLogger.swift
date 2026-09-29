import Foundation
import AppKit

public class WalkmanLogger {
    public static let shared = WalkmanLogger()
    
    private let fileManager = FileManager.default
    public let logFileURL: URL
    private let logQueue = DispatchQueue(label: "com.arsenyspb.walkmansync.logger")
    private var fileHandle: FileHandle?
    
    private init() {
        let libraryLogs = fileManager.urls(for: .libraryDirectory, in: .userDomainMask).first!
            .appendingPathComponent("Logs", isDirectory: true)
            .appendingPathComponent("WalkmanSync", isDirectory: true)
        
        try? fileManager.createDirectory(at: libraryLogs, withIntermediateDirectories: true, attributes: nil)
        
        logFileURL = libraryLogs.appendingPathComponent("walkmansync.log")
        
        if !fileManager.fileExists(atPath: logFileURL.path) {
            fileManager.createFile(atPath: logFileURL.path, contents: nil, attributes: nil)
        }
        
        fileHandle = try? FileHandle(forWritingTo: logFileURL)
        fileHandle?.seekToEndOfFile()
        
        log("=== WalkmanSync Session Started ===", level: "SYSTEM")
    }
    
    deinit {
        try? fileHandle?.close()
    }
    
    public func log(_ message: String, level: String = "INFO") {
        let timestamp = ISO8601DateFormatter().string(from: Date())
        let line = "[\(timestamp)] [\(level)] \(message)\n"
        print(line, terminator: "")
        
        logQueue.async { [weak self] in
            guard let self = self, let handle = self.fileHandle else { return }
            if let data = line.data(using: .utf8) {
                handle.write(data)
            }
        }
    }
    
    public static func info(_ message: String) {
        shared.log(message, level: "INFO")
    }
    
    public static func debug(_ message: String) {
        shared.log(message, level: "DEBUG")
    }
    
    public static func warn(_ message: String) {
        shared.log(message, level: "WARN")
    }
    
    public static func error(_ message: String) {
        shared.log(message, level: "ERROR")
    }
    
    public static func openLogInFinder() {
        NSWorkspace.shared.activateFileViewerSelecting([shared.logFileURL])
    }
    
    public static func openLogInConsole() {
        NSWorkspace.shared.open(shared.logFileURL)
    }
}
