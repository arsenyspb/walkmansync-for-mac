---
name: Bug Report (Markdown)
about: Report an issue, device connection problem, or playback error
title: "[Bug]: "
labels: bug
assignees: ''
---

### Device & System Information
- **Walkman Model & Capacity:** <!-- e.g. Sony Network Walkman NW-E405 (512 MB), NW-E507, NW-HD5 -->
- **macOS Version & Architecture:** <!-- e.g. macOS 14.6 Sonoma (Apple Silicon M2) or macOS 13.5 (Intel Core i7) -->
- **WalkmanSync Version:** <!-- e.g. v0.3.1 (check in About WalkmanSync or run 'walkmansync --version') -->
- **Audio Codec Selected:** <!-- e.g. ATRAC3 LP2 (132k), ATRAC3 LP4 (66k), ATRAC3plus (256k), MP3 (320k CBR) -->
- **Hardware Key Status:** <!-- e.g. Authentic Key (0x08FF8139), Placeholder (0x08DA6D03), or Uninitialized -->

---

### WalkmanSync Doctor Output
<!-- Run `walkmansync --doctor` in Terminal or copy details from the '🩺 Doctor...' button in the app -->
```shell
# Paste output of 'walkmansync --doctor' here
```

---

### Application Logs
<!-- 
How to retrieve logs:
- In the GUI: Click the "View Logs" button at the bottom-right of the window, or choose Help -> Open Log File in Console
- In Finder: Choose Help -> Reveal Log File in Finder (~/Library/Logs/WalkmanSync/walkmansync.log)
- In Terminal: Run `cat ~/Library/Logs/WalkmanSync/walkmansync.log` or run your command with `--verbose`
-->
```text
# Paste ~/Library/Logs/WalkmanSync/walkmansync.log content here
```

---

### Description of the Problem & Expected Behavior
<!-- A clear and concise description of what the bug is and what you expected to happen. -->

---

### Steps to Reproduce
1. Connect Walkman via USB (OLED shows 'USB CONNECT')
2. Launch WalkmanSync
3. Select music source folder containing...
4. Select codec...
5. Click 'Sync to Walkman'
6. See error...
