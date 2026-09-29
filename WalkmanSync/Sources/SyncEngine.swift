import Foundation
import AVFoundation

public class SyncEngine {
    
    // Scans directory for MP3s and extracts metadata natively using AVFoundation
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
                
                // Approximate length in seconds (Walkman DB expects track duration, mocked to 180s for speed if unreadable)
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
    
    // Actually copy the files to the OMGAUDIO folder
    public static func copyFilesToWalkman(titles: [WalkmanDBGenerator.WalkmanTitle], destination: URL) throws {
        let omgAudioDir = destination.appendingPathComponent("OMGAUDIO", isDirectory: true)
        
        // Walkmans typically split files into numbered subfolders, e.g. OMGAUDIO/10F00/10000001.OMA
        // But for generic MP3 dropping, many generations accept them in root OMGAUDIO or just track IDs.
        // We will mock the Sony naming convention: 10000001.OMA (though they are MP3s, Sony renames them)
        
        let fm = FileManager.default
        
        for title in titles {
            guard let originalUrl = title.originalFile else { continue }
            
            // Format ID into 8-digit Sony standard (e.g. 10000001)
            let sonyFileName = String(format: "1%07d.OMA", title.id)
            let destUrl = omgAudioDir.appendingPathComponent(sonyFileName)
            
            if fm.fileExists(atPath: destUrl.path) {
                try fm.removeItem(at: destUrl)
            }
            try fm.copyItem(at: originalUrl, to: destUrl)
            print("Copied \(title.titleName) to \(sonyFileName)")
        }
    }
    
    public static func installDVID(sourceDVID: URL, destination: URL) throws {
        let mp3fmDir = destination.appendingPathComponent("MP3FM", isDirectory: true)
        let destDVID = mp3fmDir.appendingPathComponent("DvID.DAT")
        
        let fm = FileManager.default
        if fm.fileExists(atPath: destDVID.path) {
            try fm.removeItem(at: destDVID)
        }
        try fm.copyItem(at: sourceDVID, to: destDVID)
        print("Successfully injected DvID.DAT to Walkman MP3FM folder.")
    }
}
