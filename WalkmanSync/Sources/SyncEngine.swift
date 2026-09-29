import Foundation
import AVFoundation

public class SyncEngine {
    
    /// Scans directory for MP3s and extracts metadata using AVFoundation
    public static func scanForMusic(in folder: URL) -> [WalkmanDBGenerator.WalkmanTitle] {
        var titles: [WalkmanDBGenerator.WalkmanTitle] = []
        let fm = FileManager.default
        var idCounter = 1
        
        guard let enumerator = fm.enumerator(at: folder, includingPropertiesForKeys: [.isRegularFileKey]) else {
            return titles
        }
        
        for case let fileURL as URL in enumerator {
            if fileURL.pathExtension.lowercased() == "mp3" {
                let asset = AVAsset(url: fileURL)
                
                var titleName = fileURL.deletingPathExtension().lastPathComponent
                var artistName = "Unknown Artist"
                var albumName = "Unknown Album"
                var genre = "Unknown Genre"
                
                let metadata = asset.metadata
                for item in metadata {
                    guard let commonKey = item.commonKey?.rawValue else { continue }
                    let value = item.stringValue ?? ""
                    
                    switch commonKey {
                    case AVMetadataKey.commonKeyTitle.rawValue:
                        titleName = value.isEmpty ? titleName : value
                    case AVMetadataKey.commonKeyArtist.rawValue:
                        artistName = value.isEmpty ? artistName : value
                    case AVMetadataKey.commonKeyAlbumName.rawValue:
                        albumName = value.isEmpty ? albumName : value
                    case AVMetadataKey.commonKeyType.rawValue:
                        genre = value.isEmpty ? genre : value
                    default:
                        break
                    }
                }
                
                let duration = CMTimeGetSeconds(asset.duration)
                let lengthInSeconds = duration > 0 ? Int(duration) : 180
                
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
    
    /// Transfers files to Walkman:
    /// Resolves/generates DvID.DAT, wraps each track into encrypted .OMA, and places in OMGAUDIO/10Fxx/
    public static func transferFilesToWalkman(
        titles: [WalkmanDBGenerator.WalkmanTitle],
        destination: URL,
        progressCallback: ((String) -> Void)? = nil
    ) throws {
        let fm = FileManager.default
        let omgAudioDir = destination.appendingPathComponent("OMGAUDIO", isDirectory: true)
        try fm.createDirectory(at: omgAudioDir, withIntermediateDirectories: true, attributes: nil)
        
        // 1. Resolve or generate device encryption key in MP3FM/DvID.DAT
        let deviceKey = WalkmanKeyManager.resolveOrCreateDeviceKey(deviceURL: destination)
        
        // 2. Process each track into 10Fxx/1000xxxx.OMA
        for title in titles {
            guard let sourceURL = title.originalFile else { continue }
            
            let dirIndex = title.id / 256
            let dirName = String(format: "10F%02X", dirIndex)
            let trackDir = omgAudioDir.appendingPathComponent(dirName, isDirectory: true)
            try fm.createDirectory(at: trackDir, withIntermediateDirectories: true, attributes: nil)
            
            let fileName = String(format: "1000%04X.OMA", title.id)
            let destOMAURL = trackDir.appendingPathComponent(fileName)
            
            progressCallback?("Encrypting & writing \(title.titleName)...")
            let omaData = try OMAContainerBuilder.createEncryptedOMA(
                title: title,
                sourceMP3URL: sourceURL,
                deviceKey: deviceKey
            )
            
            try omaData.write(to: destOMAURL, options: .atomic)
            print("Wrote encrypted OMA (\(omaData.count) bytes) to \(dirName)/\(fileName)")
        }
    }
}
