[![build](https://github.com/porkyoot/piggy-admin/actions/workflows/build.yml/badge.svg)](https://github.com/porkyoot/piggy-admin/actions/workflows/build.yml)
[![test](https://github.com/porkyoot/piggy-admin/actions/workflows/test.yml/badge.svg)](https://github.com/porkyoot/piggy-admin/actions/workflows/test.yml)
[![release](https://img.shields.io/github/v/release/porkyoot/piggy-admin)](https://github.com/porkyoot/piggy-admin/releases)

# Piggy Admin

A server-side administration mod designed to help manage and enforce settings for compatible client-side mods (like Piggy Build and Piggy Inventory). Currently, it serves as a central point for managing anti-cheat options, logging, and detection, but it will evolve to include more administration tools.

---

## ⚠️ Disclaimer

**This is a personal project.** It comes **AS IS** and might cause issues.
*   **No Support:** Do not expect regular support or bug fixes.
*   **No Forge Port:** There are **NO** plans to port this mod to Forge.
*   **Use at your own risk.**

Feel free to fork the project or submit a Pull Request if you want to contribute fixes or features!

---

## Features

### 🛡️ Anti-Cheat Enforcement
*   **Centralized Control**: Configure whether "cheat" features (like fast placement, tool swapping) are allowed on your server.
*   **Automatic Sync**: When a player joins, the server's anti-cheat configuration is automatically sent to their client.
*   **Strict Enforcement**: If the server disables cheats, client-side overrides are ignored, ensuring fair play.
*   **Granular Control**: Enable/disable specific features individually (e.g., allow tool swapping but forbid fast placement).

### 🕵️ X-Ray Detection
*   **Heuristic Detection**: The server monitors mining patterns to detect suspicious behavior.
*   **Ratio Analysis**: Calculates the ratio of rare ores mined versus common blocks.
    *   **Rare Ores**: Diamond, Ancient Debris, Iron, Gold, Emerald.
    *   **Common Blocks**: Stone, Deepslate, Netherrack, etc.
*   **Configurable Thresholds**: Adjust sensitivity via `xrayMaxRatio` in config.
*   **Alerts**: Admins are notified if a player's mining ratio exceeds the configured threshold.

### 📜 Moderation Engine
*   **AI-Powered Moderation**: Integrates with **Google Gemini AI** to automatically filter toxic or inappropriate chat and sign text.
*   **Dynamic Rule System**: Define custom moderation rules and categories (e.g., Profanity, Harassment, Hate Speech).
*   **Performance Optimized**: Includes a high-performance cache and rate-limiting system to minimize API latency and handle free-tier API limits gracefully.
*   **Hybrid Checking**: Combines fast local **Regex** checks with deep semantic analysis from Gemini.
*   **Categorized Logs**: All blocked messages are logged with their specific category for easy review.

### 🤖 Gemini AI Setup
To use the AI moderation features, you need a Google Gemini API Key.

1.  **Get an API Key**: Visit the **[Google AI Studio](https://aistudio.google.com/app/apikey)** and create a free API key.
2.  **Configure the Mod**:
    *   Start the server once to generate the config file.
    *   Open `config/piggy-admin-server.json`.
    *   Paste your key into the `"geminiApiKey"` field.
    *   Set `"moderationEnabled": true`.
3.  **Advanced Options**:
    *   `geminiModel`: defaults to `gemini-1.5-flash` (recommended for speed and cost).
    *   `geminiSystemPrompt`: You can customize the AI's "personality" and strictness here.

> [!NOTE]
> The free tier of Gemini has rate limits (approx. 15 requests per minute). The mod includes a built-in cache and rate-limiter to handle these limits gracefully without lagging the server.

### 📜 History & Logging
*   **Sign History**: Tracks who placed a sign and what was written on it.
*   **Chat History**: Logs all chat messages for administrative review.
*   **Explosion Logging**: Detailed logs for TNT placement, ignition, and explosions, including player attribution whenever possible.
*   **Blame Tool**: Inspect existing signs/blocks to see who interacted with them and when.
*   **Persistent Storage**: History survives server restarts (saved to `config/piggy-admin-history.json`).

---

## Commands

All commands require **OP level 2** or higher.

### General Administration
*   `/piggy cheats <allow|forbid>`: Globally allow or forbid "cheat" features for all players.
*   `/piggy feature list`: List all controllable features and their current status.
*   `/piggy feature <id> <enable|disable>`: Enable or disable a specific feature (e.g., `flexible_placement`, `tool_swap`).
*   `/piggy sync`: Manually trigger a synchronization of all admin settings and moderation rules to all connected clients.

### Investigation & Logging
*   `/blame`: Look at a sign or block and run this command to see who placed/modified it and when.
*   `/logs <player_name>`: View the recent chat, sign, tnt, and moderation history for a specific player.
    *   **Clickable Coordinates**: Sign logs include coordinates that you can click to teleport to.

---

## Configuration

The mod generates a configuration file at `config/piggy-admin-server.json` (server-side).

```json
{
  "allowCheats": false,
  "features": {
    "flexible_placement": true,
    "fast_place": false,
    "tool_swap": true
  },
  "xrayCheck": true,
  "xrayMaxRatio": 0.15,
  "xrayMinBlocks": 20,
  "moderationEnabled": true,
  "geminiApiKey": "YOUR_KEY",
  "geminiModel": "gemini-1.5-flash"
}
```

*   `"allowCheats": false`: Forces all connected clients to disable restricted features.
*   `"allowCheats": true`: Allows clients to use their own local settings.
*   `"features"`: Granular control over individual features.
*   `"xrayMaxRatio"`: Maximum allowed ratio of rare ores to total blocks (default 15%).
*   `"xrayMinBlocks"`: Minimum blocks mined before X-Ray detection activates (default 20).
*   `"xrayCheck"`: Toggle the X-Ray detection system.
*   `"moderationEnabled"`: Toggle the AI and Regex moderation engine.
*   `"geminiApiKey"`: Your Google Gemini API Key.
*   `"geminiModel"`: The Gemini model to use (default: `gemini-1.5-flash`).

---

## Dependencies & Installation

### Requirements
*   **Minecraft**: ~1.21.1
*   **Fabric Loader**: >=0.18.1
*   **Java**: >=21

### Required Mods
*   **[Fabric API](https://modrinth.com/mod/fabric-api)**: Any version
*   **[YACL (Yet Another Config Lib)](https://modrinth.com/mod/yacl)**: >=3.6.1+1.21-fabric (for clients to view config)
*   **[Piggy Lib](https://github.com/porkyoot/piggy-lib)**: >=1.0.1
*   *(Optional)* **[Mod Menu](https://modrinth.com/mod/modmenu)**: >=11.0.3 - Recommended for clients.

### Installation
1.  Download the `.jar` file from [Releases](https://github.com/porkyoot/piggy-admin/releases).
2.  Install Fabric Loader for Minecraft 1.21.1.
3.  Place the `piggy-admin` jar (along with Fabric API and Piggy Lib) into your server's `mods` folder.
4.  Launch the server!

---

## Todo / Future Features

- [x] **Detect XRay**: Implement heuristics/checks to detect players using XRay texture packs or mods.
- [X] **Moderate Sign Text**: Add tools to inspect and filter text on signs to prevent inappropriate content.

---

## Inspiration

This mod was inspired by:
- **[ItemSwapper](https://modrinth.com/plugin/itemswapper)** - For the concept of server-side item management and swapping mechanics.

---

**License**: CC0-1.0
