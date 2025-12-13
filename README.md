[![build](https://github.com/porkyoot/piggy-admin/actions/workflows/build.yml/badge.svg)](https://github.com/porkyoot/piggy-admin/actions/workflows/build.yml)

# Piggy Admin

Piggy Admin is a server-side administration mod designed to help manage and enforce settings for compatible client-side mods (like Piggy Build). Currently, it serves as a central point for managing anti-cheat options, but it will evolve to include more administration tools such as moderation and detection.

## ⚠️ Disclaimer

**This is a personal project.** It comes **AS IS** and might cause issues.
*   **No Support:** Do not expect regular support or bug fixes.
*   **No Forge Port:** There are **NO** plans to port this mod to Forge.
*   **Use at your own risk.**

Feel free to fork the project or submit a Pull Request if you want to contribute fixes or features!

---

## Features

### 🛡️ Anti-Cheat Enforcement
*   **Centralized Control**: Configure whether "cheat" features (like fast placement or flight in other Piggy mods) are allowed on your server.
*   **Automatic Sync**: When a player joins, the server's anti-cheat configuration is automatically sent to their client.
*   **Strict Enforcement**: If the server disables cheats, client-side overrides are ignored, ensuring fair play.

### 🕵️ X-Ray Detection
*   **Heuristic Detection**: The server monitors mining patterns to detect suspicious behavior.
*   **Ratio Analysis**: Calculates the ratio of rare ores (Diamond, Ancient Debris) mined versus common blocks (Stone, Netherrack).
*   **Alerts**: Admins are notified if a player's mining ratio exceeds the configured threshold.

### 📜 History & Logging
*   **Sign History**: Tracks who placed a sign and what was written on it.
*   **Chat History**: Logs all chat messages for administrative review.
*   **Blame Tool**: inspect existing blocks to see who interacted with them.

---

## 💻 Commands

### General Administration
*   `/piggy cheats <allow|forbid>`: Globally allow or forbid "cheat" features (like Fast Place) for all players.
*   `/piggy feature list`: List all controllable features and their current status.
*   `/piggy feature <id> <enable|disable>`: Enable or disable a specific feature (e.g., `fast_place`) for all players.

### Investigation & Logging
*   `/blame`: Look at a block (e.g., a sign) and run this command to see who placed/modified it and when.
*   `/logs <player_name>`: View the recent chat and sign history for a specific player.
    *   **Clickable Coordinates**: Sign logs include coordinates that you can click to teleport to.

---

## 🚀 Todo / Future Features

- [x] **Detect XRay**: Implement heuristics/checks to detect players using XRay texture packs or mods.
- [ ] **Moderate Sign Text**: Add tools to inspect and filter text on signs to prevent inappropriate content.

---

## Configuration

The mod generates a configuration file at `config/piggy-admin-server.json` (server-side).

```json
{
  "allowCheats": false
}
```

*   `"allowCheats": false`: Forces all connected clients to disable restricted features (e.g., Fast Place).
*   `"allowCheats": true`: Allows clients to use their own local settings.

---

## Dependencies & Installation

### Requirements
*   **Minecraft**: ~1.21.1
*   **Fabric Loader**: >=0.18.1
*   **Java**: >=21

### Required Mods
*   **[Fabric API](https://modrinth.com/mod/fabric-api)**
*   **[YACL (Yet Another Config Lib)](https://modrinth.com/mod/yacl)** (Required for clients to view config)
*   *(Optional)* **[Mod Menu](https://modrinth.com/mod/modmenu)** - Highly recommended for accessing config.

### Installation
1.  Download the `.jar` file.
2.  Install Fabric Loader for Minecraft 1.21.1.
3.  Place the `piggy-admin` jar (along with Fabric API) into your `.minecraft/mods` folder (or server `mods` folder).
4.  Launch the game/server!

---

**License**: CC0-1.0
