import Foundation

@main
struct TestRunner {
    static func main() {
        testKeyDerivation()
        testScrambleRoundTrip()
        testEA3TagAndHeader()
        testDvidDataGeneration()
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
        
        let audioHdr = OMAContainerBuilder.buildEA3AudioHeader()
        assert(audioHdr.count == 96, "EA3 audio header must be exactly 96 bytes")
        assert(audioHdr.prefix(3) == Data("EA3".utf8), "Audio header magic must be 'EA3'")
        assert(audioHdr[3] == 0x02, "Audio header version must be 2")
        assert(audioHdr[6] == 0xFF && audioHdr[7] == 0xFE, "Protection marker must be 0xFFFE")
        assert(audioHdr[32] == 0x03, "Codec ID must be 3 (MP3)")
        print("EA3 tag and audio header PASSED")
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
}
