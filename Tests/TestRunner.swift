import Foundation

@main
struct TestRunner {
    static func main() {
        testKeyDerivation()
        testScrambleRoundTrip()
        testEA3TagAndHeader()
        testDvidDataGeneration()
        testAuthenticKeyValidation()
        testHardwareKeyParsingFromResponse()
        testMultiDeviceCaching()
        testFullDatabaseSuiteGeneration()
        print("ALL TESTS PASSED!")
    }

    static func testKeyDerivation() {
        print("Testing XOR key derivation...")
        let devKey: UInt32 = 0x08DA6D03
        let trackId = 1
        let keyBytes = WalkmanKeyManager.computeXorKey(trackId: trackId, deviceKey: devKey)
        
        let expected: [UInt8] = [0x5A, 0x4D, 0x65, 0x99]
        assert(keyBytes == expected, "Key derivation failed: got \(keyBytes), expected \(expected)")
        print("XOR key derivation PASSED: \(keyBytes.map { String(format: "%02X", $0) }.joined())")
    }

    static func testScrambleRoundTrip() {
        print("Testing XOR scramble round-trip...")
        let keyBytes: [UInt8] = [0xAA, 0xBB, 0xCC, 0xDD]
        let original: [UInt8] = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]
        var data = Data(original)
        
        WalkmanKeyManager.xorScramble(data: &data, keyBytes: keyBytes)
        assert(Array(data) != original, "Data should be modified after scrambling")
        
        // Scrambling again should decode it back
        WalkmanKeyManager.xorScramble(data: &data, keyBytes: keyBytes)
        assert(Array(data) == original, "Data should match original after double XOR")
        print("XOR scramble round-trip PASSED")
    }

    static func testEA3TagAndHeader() {
        print("Testing EA3 tag and audio header...")
        let title = WalkmanDBGenerator.WalkmanTitle(
            id: 1,
            titleName: "Test Song",
            artistName: "Test Artist",
            albumName: "Test Album",
            genre: "Rock",
            length: 210
        )
        
        let tag = OMAContainerBuilder.buildEA3Tag(title: title)
        assert(tag.count == 3072, "EA3 tag must be exactly 3072 bytes")
        assert(tag.prefix(3) == Data("ea3".utf8), "Tag magic must be 'ea3'")
        assert(tag[3] == 0x03, "Tag version must be 3")
        
        let audioHdrCBR = OMAContainerBuilder.buildEA3AudioHeader(title: title, isVBR: false)
        assert(audioHdrCBR.count == 96, "EA3 audio header must be exactly 96 bytes")
        assert(audioHdrCBR.prefix(3) == Data("EA3".utf8), "Audio header magic must be 'EA3'")
        assert(audioHdrCBR[3] == 0x02, "Audio header version must be 2")
        assert(audioHdrCBR[6] == 0xFF && audioHdrCBR[7] == 0xFE, "Protection marker must be 0xFFFE")
        assert(audioHdrCBR[32] == 0x03, "Codec ID must be 3 (MP3)")
        assert(audioHdrCBR[33] == 0x80, "Byte 33 must be 0x80 for CBR")
        
        let audioHdrVBR = OMAContainerBuilder.buildEA3AudioHeader(title: title, isVBR: true)
        assert(audioHdrVBR[33] == 0x90, "Byte 33 must be 0x90 for VBR")
        print("EA3 tag and audio header (CBR & VBR) PASSED")
    }

    static func testDvidDataGeneration() {
        print("Testing DvID.DAT generation...")
        let key: UInt32 = 0x08DA6D03
        let data = WalkmanKeyManager.generateDvidData(key: key)
        assert(data.count == 16, "DvID.DAT must be 16 bytes")
        assert(data[0] == 0x03 && data[1] == 0x01 && data[2] == 0x01 && data[3] == 0x00, "DvID prefix mismatch")
        
        let tempURL = URL(fileURLWithPath: "/tmp/test_DvID.DAT")
        try! data.write(to: tempURL)
        let readKey = WalkmanKeyManager.readDeviceKey(from: tempURL)
        assert(readKey == key, "DvID read key mismatch: got \(String(describing: readKey)), expected \(key)")
        try? FileManager.default.removeItem(at: tempURL)
        print("DvID.DAT generation & parsing PASSED")
    }

    static func testAuthenticKeyValidation() {
        print("Testing Authentic Key Validation...")
        assert(!WalkmanKeyManager.isKeyAuthentic(key: 0), "Key 0 should be invalid")
        assert(!WalkmanKeyManager.isKeyAuthentic(key: WalkmanKeyManager.defaultDeviceKey), "Default placeholder key (0x08DA6D03) must not be authentic")
        assert(WalkmanKeyManager.isKeyAuthentic(key: 0x08FF8139), "Hardware key 0x08FF8139 must be recognized as authentic")
        print("Authentic Key Validation PASSED")
    }

    static func testHardwareKeyParsingFromResponse() {
        print("Testing Hardware Key Parsing from 18-Byte USB Response...")
        // Authentic response captured from physical NW-E405 ASIC register
        let rawResponse: [UInt8] = [
            0x00, 0x10, // Wrapper length (16 bytes)
            0x03, 0x01, 0x01, 0x00, 0x00, 0x00, 0x01, 0x28, 0x00, 0x00,
            0x08, 0xFF, 0x81, 0x39, // ASIC Key: 0x08FF8139
            0x00, 0x00
        ]
        assert(rawResponse.count == 18, "Response must be 18 bytes")
        
        let payload = Data(rawResponse[2..<18])
        assert(payload.count == 16, "Extracted DvID payload must be exactly 16 bytes")
        
        let k0 = UInt32(rawResponse[12])
        let k1 = UInt32(rawResponse[13])
        let k2 = UInt32(rawResponse[14])
        let k3 = UInt32(rawResponse[15])
        let key = (k0 << 24) | (k1 << 16) | (k2 << 8) | k3
        assert(key == 0x08FF8139, "Parsed key must match ground truth 0x08FF8139, got 0x\(String(format: "%08X", key))")
        
        let tempURL = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("Test_DvID_\(UUID().uuidString).DAT")
        try! payload.write(to: tempURL)
        let readBackKey = WalkmanKeyManager.readDeviceKey(from: tempURL)
        assert(readBackKey == 0x08FF8139, "Read back key must match ground truth")
        try? FileManager.default.removeItem(at: tempURL)
        print("Hardware Key Parsing PASSED (Key: 0x08FF8139)")
    }

    static func testMultiDeviceCaching() {
        print("Testing Multi-Device Caching...")
        let tempCacheDir = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("CacheTest_\(UUID().uuidString)")
        let dummyPayload = WalkmanKeyManager.generateDvidData(key: 0x08FF8139)
        
        WalkmanKeyManager.saveKeyToCache(payload: dummyPayload, key: 0x08FF8139, cacheDirectory: tempCacheDir)
        
        let primaryFile = tempCacheDir.appendingPathComponent("DvID.DAT")
        let keyedFile = tempCacheDir.appendingPathComponent("DvID_08FF8139.DAT")
        
        assert(FileManager.default.fileExists(atPath: primaryFile.path), "Primary DvID.DAT must exist in cache")
        assert(FileManager.default.fileExists(atPath: keyedFile.path), "Per-device DvID_08FF8139.DAT must exist in cache")
        
        let keyFromPrimary = WalkmanKeyManager.readDeviceKey(from: primaryFile)
        let keyFromKeyed = WalkmanKeyManager.readDeviceKey(from: keyedFile)
        
        assert(keyFromPrimary == 0x08FF8139, "Key from primary cache must match")
        assert(keyFromKeyed == 0x08FF8139, "Key from keyed cache must match")
        
        try? FileManager.default.removeItem(at: tempCacheDir)
        print("Multi-Device Caching PASSED")
    }

    static func testFullDatabaseSuiteGeneration() {
        print("Testing Full OMGAUDIO Database Suite generation...")
        let tempDir = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("OMGAUDIOTest_\(UUID().uuidString)")
        let titles = [
            WalkmanDBGenerator.WalkmanTitle(id: 1, titleName: "Song A", artistName: "Artist 1", albumName: "Album X", genre: "Rock", length: 200),
            WalkmanDBGenerator.WalkmanTitle(id: 2, titleName: "Song B", artistName: "Artist 1", albumName: "Album X", genre: "Rock", length: 180),
            WalkmanDBGenerator.WalkmanTitle(id: 3, titleName: "Song C", artistName: "Artist 2", albumName: "Album Y", genre: "Pop", length: 240)
        ]
        
        let gen = WalkmanDBGenerator(mp3Bitrate: .kbps192, isVBR: false, isEncrypted3rdGen: true)
        try! gen.generateDatabase(titles: titles, destination: tempDir)
        
        let expectedFiles = [
            "00GTRLST.DAT",
            "01TREE01.DAT",
            "03GINF01.DAT",
            "01TREE02.DAT",
            "03GINF02.DAT",
            "01TREE03.DAT",
            "03GINF03.DAT",
            "01TREE04.DAT",
            "03GINF04.DAT",
            "01TREE22.DAT",
            "03GINF22.DAT",
            "01TREE2D.DAT",
            "03GINF2D.DAT",
            "02TREINF.DAT",
            "04CNTINF.DAT",
            "05CIDLST.DAT"
        ]
        
        let omgDir = tempDir.appendingPathComponent("OMGAUDIO")
        for f in expectedFiles {
            let fileURL = omgDir.appendingPathComponent(f)
            assert(FileManager.default.fileExists(atPath: fileURL.path), "Missing expected DB file: \(f)")
            let attrs = try! FileManager.default.attributesOfItem(atPath: fileURL.path)
            let size = attrs[.size] as? Int ?? 0
            assert(size > 0, "DB file \(f) is empty")
        }
        
        try? FileManager.default.removeItem(at: tempDir)
        print("Full OMGAUDIO Database Suite generation PASSED (all 16 DAT tables verified)")
    }
}
