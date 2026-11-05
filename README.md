# Minecraft AI Builder

Type what you want in Minecraft chat and an AI builds it in front of you.

```
/bot aibuild small wizard tower with a balcony
```

A builder bot (an NPC) follows you around. When you ask for a build, the request goes
to a small local AI service, OpenAI designs the structure, and WorldEdit pastes it
into the world — usually 30–120 seconds.

Included: the Minecraft server settings (Paper 1.21.10), the BuilderBot plugin, the AI
service, and one `Start.bat` that sets it all up. Paper, WorldEdit and Citizens are
separate downloads (see Setup).

---

## What you need

| You need | Where to get it |
|---|---|
| Windows | — |
| Java 21 or newer | https://adoptium.net |
| Python 3.12 (tick "Add to PATH" / keep the `py` launcher) | https://www.python.org/downloads/ |
| An OpenAI API key **with credits** | https://platform.openai.com/api-keys |
| Minecraft Java Edition 1.21.10 | https://www.minecraft.net |

The AI uses `gpt-4.1-mini`; each build costs a fraction of a cent.

---

## Download the server files

These aren't included in this repository, so download them before you start:

| File | Where to get it | Save it as |
|---|---|---|
| Paper 1.21.10 | https://papermc.io | `server\paper.jar` |
| WorldEdit 7.3.17 for Bukkit | https://enginehub.org/worldedit | `server\plugins\` (keep its file name) |
| Citizens 2.0.40 | https://citizensnpcs.co | `server\plugins\` (keep its file name) |

---

## Setup (3 steps)

1. **Double-click `Start.bat`.** On the first run Notepad opens a `.env` file.
   Replace `YOUR_API_KEY_HERE` with your OpenAI key, save, and close Notepad.
2. **Double-click `Start.bat` again.** The first time it installs the Python
   packages (a few minutes) and asks you to accept the
   [Minecraft EULA](https://aka.ms/MinecraftEULA) — type `Y`.
   Two windows then run: **AI Service** and the **Minecraft server**. Leave both open.
3. **Give yourself permission.** When the server says `Done`, type this in the
   server window (use your Minecraft name):
   ```
   op YourMinecraftName
   ```

Then open Minecraft → Multiplayer → Add Server → address `localhost` → join.

After the first time, just double-click `Start.bat`.

---

## In-game commands

```
/bot summon                      spawn your builder bot
/bot follow on | off             bot follows you / stays put
/bot aibuild <description>       AI builds it in front of you (ops only)
/bot build <type>                build a built-in structure (e.g. treehouse)
/bot despawn                     remove your bot
/testorientation                 show which way is front/left/right
```

Ideas: `desert fortress with two towers`, `spooky treehouse with cobwebs`,
`modern house with glass windows`.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| "BuilderGPT service is not running" | The **AI Service** window must be open. Run `Start.bat`. |
| AI Service window says `API_KEY not configured` | Put your real key in `.env` (next to `Start.bat`). |
| Build fails with `insufficient_quota` / `no credits` | Add credits to your OpenAI account. |
| "You don't have permission" for `aibuild` | Run `op YourMinecraftName` in the server window. |
| Python setup failed | Delete `service\.venv` and run `Start.bat` again. |
| Check the AI service | Open http://127.0.0.1:8001/health in a browser — look for `"status":"healthy"`. |

---

## What's in this folder

```
Start.bat        one-click setup + launch
.env.example     template for your settings (your real .env is never uploaded)
server/          Paper 1.21.10 server settings + BuilderBot plugin
service/         AI service (Python, FastAPI) on http://127.0.0.1:8001
  components/BuilderGPT/   the AI structure generator (see Credits)
  generated/     AI-made .schem files land here
plugin/          Java source for the BuilderBot plugin
docs/            original design plan and debugging notes
```

The world is created fresh on first run and is not uploaded (see `.gitignore`).

---

## For developers

**Rebuild the plugin** (needs Maven 3.9+ and JDK 21):
```
cd plugin
mvn clean package
copy target\builder-bot-plugin-1.0.0-SNAPSHOT.jar ..\server\plugins\
```

**Run the AI service by hand:**
```
cd service
.venv\Scripts\activate
set API_KEY=sk-...
python main.py
```

**API:** `POST /generate` with
`{"prompt": "small wooden house", "minecraft_version": "JE_1_21_1", "output_format": "schem"}`
returns `{"status": "ok", "schem_path": "...", "metadata": {...}}`. `GET /health` reports status.

**Plugin settings:** `server/plugins/BuilderBot/config.yml` (follow distance, build speed,
structure sizes). The AI service address defaults to `http://127.0.0.1:8001`.

---

## Credits

- **BuilderGPT** by CyniaAI — https://github.com/CyniaAI/BuilderGPT (Apache 2.0, see
  `service/components/BuilderGPT/LICENSE`). Bundled from commit `9e27953` with small
  changes so generated files save into `service/generated/`.
- **CyniaAgents** framework — installed from PyPI (`cynia-agents`).
- **Paper** — https://papermc.io · **Citizens** — https://citizensnpcs.co ·
  **WorldEdit** — https://enginehub.org/worldedit
- **OpenAI** — structure generation.
