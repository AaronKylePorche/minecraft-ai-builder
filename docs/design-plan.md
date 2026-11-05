# Minecraft Java “Builder Bot” – Technical Game Plan

## High-Level Concept

We want an **in-game bot** in **Minecraft Java Edition** that:

1. **Follows the player** around (like a pet / NPC).
2. **Listens to natural-ish chat commands**, e.g.:
   - `bot build me a treehouse`
   - `bot build me a bridge`
   - `bot build me a masjid`
3. **Asks clarifying questions** in chat, e.g.:
   - “Where should I build it? Stand at the center and type `here`.”
   - “How big? (small / medium / large)”
4. **Builds a structure oriented toward the player’s facing**, assuming:
   - Player stands like they’re in the driveway looking at their house.
   - “Front” of the structure should face the player.

### Non-Goals (for v1)

- No voice recognition (we’ll use in-game chat).
- No external AI structure generation (v1 uses hand-designed / parametric patterns).
- Single-player or small server focus (not massive multi-tenant complexity).

---

## Target Platform / Stack

### Minecraft Platform

- **Minecraft: Java Edition**
- **Server Type**: Paper (recommended) or Spigot
  - Paper = better performance + Spigot plugin compatibility.

### Primary Language

- **Java 17+**
  - Native for Spigot/Paper, huge ecosystem, good docs.
  - Direct access to Bukkit/Paper API for entities, blocks, world edits.

> If we want scripting for advanced prototyping later, we can embed Kotlin or a small DSL, but v1 = pure Java plugin.

### Core Dependencies (Server-Side)

1. **Paper / Spigot API**
   - For events, commands, entities, locations, vectors, schedulers.

2. **Citizens (optional but recommended)**
   - For spawnable NPCs that represent the “bot”.
   - Easy follow behavior and animations.
   - Alternative: implement our own invisible “brain” with an armor stand or entity, but Citizens simplifies this.

3. **Pathfinding / Behavior**
   - Option A: Use Citizens + Sentinel or built-in path behaviors.
   - Option B: Implement custom A* pathfinding using NMS or a path lib.
   - For v1, keep it simple: follow player via Citizens.

4. **WorldEdit (optional but powerful)**
   - For fast region edits and schematics.
   - Lets us load pre-built schematics (e.g. treehouse/masjid/bridge templates).
   - Also gives brushes and region APIs if we want to handcraft generation.

5. **Config / Data Storage**
   - YAML / JSON config for:
     - Structure presets (sizes, materials, offsets).
     - Bot settings (follow radius, name, chat style).
   - Simple file-based persistence is enough for v1.

---

## Architecture Overview

### Components

1. **BotManager**
   - Tracks the NPC for each player.
   - Handles spawn/despawn, follow toggling, and state.
   - One “bot” per player (to start).

2. **Command & Chat Parser**
   - Intercepts chat (and/or registers `/bot` commands).
   - Example:
     - Player types: `bot build me a treehouse`
     - Parser detects:
       - Keyword: `bot`
       - Intent: `build`
       - Structure type: `treehouse`
   - Simple pattern matching first, not full NLP.

3. **Conversation / Prompt State Machine**
   - Manages the back-and-forth with the player:
     - Ask: “Where should I build it? Stand on the center and type `here`.”
     - Wait for “here”.
     - Ask: “What size? (small / medium / large)”
     - Wait for answer.
   - Stores per-player conversation state until done or timeout.

4. **StructureBuilder Engine**
   - Given:
     - **Origin `Location`** (player’s position or target block).
     - **Facing `Vector` or `Yaw`** (direction the player is looking).
     - **Structure type** (treehouse/bridge/masjid).
     - **Parameters** (size, material theme, etc.).
   - It computes block placements and runs the build:
     - Either via:
       - Direct Bukkit block placement (`Block#setType`), or
       - WorldEdit region / schematic paste.
   - Supports both **procedural** and **schematic-based** structures.

5. **Orientation & Placement Helper**
   - Utility methods:
     - `getFrontOffset(playerYaw)` → returns forward/right vectors.
     - `getBuildOriginFromPlayer(playerLocation, distance)` → where the front of the building goes.
     - Rotates structures/schematics to face the player.

6. **Animation / Progressive Building (Optional v2)**
   - To make it cool, instead of instant paste:
     - Bot “walks” to the spot.
     - Places blocks row-by-row or chunk-by-chunk.
   - Use Bukkit scheduler to pace block placement.

---

## Command & Chat Design

### Basic Commands

1. Typed Commands:
   - `/bot summon` – spawn or teleport bot to player.
   - `/bot follow on|off` – enable/disable following.
   - `/bot build <type>` – start build intent.
     - `type` ∈ `{treehouse, bridge, masjid, custom1, ...}`

2. Natural-ish Chat (v1 simple parsing):
   - Player types: `bot build me a treehouse`
   - Parser logic:
     - Contains token `bot` → bot command.
     - Contains token `build` → build intent.
     - Contains one of known structure tokens:
       - `treehouse`, `bridge`, `masjid`, etc.
   - Then treat it like `/bot build treehouse`.

### Conversation Flow Example

**User**: `bot build me a masjid`  
**Bot**: `Okay, a masjid! Stand where you want the FRONT door to be, face the building, then type "here".`  
*(player moves and types `here`)*  
**Bot**: `Got it. How big? (small / medium / large)`  
**User**: `medium`  
**Bot**: `Medium masjid coming up! Building in front of you...`

---

## Structure Design Strategy

We’ll combine **procedural logic** and optionally **schematics**.

### Orientation Basics

- Use player yaw (`float yaw`) to get facing:
  - Convert to quadrant (N/E/S/W) or full vector.
- Define a local coordinate system:
  - `forward` = direction player is facing.
  - `right` = perpendicular to `forward`.
  - `up` = +Y.
- All structure templates are defined in **local coordinates**:
  - Example: front door at (0, 0, 0),
  - building extends +forward and ±right.

At build time:
- Map local coordinates `(fx, fy, fz)` to world coordinates using:
  - `worldLoc = origin + forward * fx + right * fz + up * fy`.

### Example Structures

#### 1. Treehouse

- Inputs:
  - Size: small/medium/large.
  - Material theme: oak/spruce/jungle.
- Algorithm:
  1. Generate a trunk (vertical log column).
  2. Generate simple organic leaf blob around the top (sphere/ellipse).
  3. Build a platform near the crown.
  4. Add ladder up the trunk.
  5. Optionally, railings + small interior.

#### 2. Bridge

- Inputs:
  - Length (or auto-detect between two points).
  - Width (2–5 blocks).
  - Style (wooden / stone).
- Algorithm v1:
  1. Ask player to stand at start and type `start`.
  2. Then stand at end and type `end`.
  3. Draw straight line between points and build:
     - Deck blocks.
     - Side rails.
  4. Later: add arches, decorative supports.

#### 3. Masjid

- Inputs:
  - Size: small/medium/large.
  - Style: simple rectangular masjid with:
    - Prayer hall.
    - Entrance facade.
    - At least one dome-like roof (simplified).
    - One or more minarets.
- Algorithm:
  - Layout in 2D grid:
    - Basic rectangular footprint.
    - Qibla wall indicated (we can just visually distinguish a side).
  - Build walls with arches (doorways).
  - Add dome:
    - Approximate with concentric circles shrinking as you go up.
  - Add minaret:
    - Slim tower with small balcony and roof.
  - Add minimal interior (carpet lines, lights).

We can encode these as either:

- **Hardcoded procedural patterns in Java**, OR
- Offload to **schematics** created manually in a creative world and exported with WorldEdit.

For more complexity / aesthetics, schematics are faster; for parametric scaling, procedural is better.

---

## Technical Implementation Details

### 1. Project Setup

- **Build Tool**: Maven or Gradle.
- Target:
  - Java 17+
  - Paper API dependency.

**Steps:**
1. Setup a Paper test server locally.
2. Create a Maven/Gradle project with:
   - `plugin.yml` defining main class, name, version.
3. Install Citizens + (optionally) WorldEdit plugins on the dev server.

---

### 2. Bot NPC / Following Behavior

Option A (Citizens):

- Spawn NPC linked to player:
  - On `/bot summon`:
    - If no NPC yet, create one at player location with Citizens API.
    - Store mapping: `UUID(player) -> NPC(bot)`.
  - Enable follow mode:
    - Citizens has follow traits or we can periodically update NPC’s goal to follow the player.
- Bot chat:
  - Use `player.sendMessage` for player-only messages.
  - Optionally `npc.say()` style messages if we want global chat.

Option B (Custom Entity):

- Spawn e.g. an invisible armor stand or custom mob.
- On every tick/interval:
  - Calculate vector from bot to player.
  - If distance > desired radius:
    - Move bot using velocity or teleport with smoothing.
- More work, but avoids external dependency.

For v1, **use Citizens** to simplify.

---

### 3. Command & Chat Handling

- Register `/bot` command in `plugin.yml`.
- Implement `onCommand` to handle:
  - `/bot summon`
  - `/bot follow on|off`
  - `/bot build <type>`

- For natural chat:
  - Listen to `AsyncPlayerChatEvent`.
  - If the message contains `bot` + `build`:
    - Parse structure type by scanning for known keywords.
    - Start a `ConversationState` for that player:
      - `state = AWAITING_LOCATION`.
      - `pendingStructureType = TREEHOUSE|BRIDGE|MASJID`.

- Conversation State:
  - Store in a map:
    - `Map<UUID, ConversationContext>`.
  - `ConversationContext` may include:
    - `StructureType`
    - `Stage` (enum: IDLE, AWAITING_LOCATION, AWAITING_SIZE, AWAITING_EXTRA, BUILDING)
    - `Intermediate data` (origin location, start/end, size, etc.)
  - On every new chat message from that player:
    - If context.stage != IDLE:
      - Treat message as answer instead of a new command.

---

### 4. Structure Builder Module

Create a `StructureBuilder` interface:

```java
public interface StructureBuilder {
    void build(World world, Location origin, float yaw, StructureParameters params);
}
