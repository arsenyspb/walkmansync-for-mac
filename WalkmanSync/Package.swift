// swift-tools-version: 5.7
import PackageDescription

let package = Package(
    name: "WalkmanSync",
    platforms: [
        .macOS(.v12)
    ],
    products: [
        .executable(name: "WalkmanSync", targets: ["WalkmanSync"])
    ],
    dependencies: [],
    targets: [
        .executableTarget(
            name: "WalkmanSync",
            dependencies: [],
            path: "Sources"
        )
    ]
)
