# PvP Learner AI v2 (Fabric 1.21.11)

Client-side mod. The AI plays PvP for you, learns from every death, from other players, and from you.

## Build (no local setup)
1. Make a GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Actions tab -> run "build" (also runs on every push).
3. Download the `pvp-learner-ai` artifact, put the jar in `mods/` with Fabric API.

## Keys
- Right Shift: menu. K: AI on/off. AI is OFF by default and never moves you while a menu is open.

## What is in v2
- Heal: eats golden apples when low, keeps a totem (or shield) in the offhand.
- Combat: shield blocking, W-tap, bow at long range, ender pearl escape when low.
- Crystal mode: places obsidian + end crystals next to the enemy and breaks them from a safe distance.
- Learning: skill evolution after every death, copies the killer's aim/strafe, per-player profiles (remembers who beat it), gear awareness (enemy armor).
- Neural net (11 inputs, 14 hidden, 5 outputs): trains from you ("Watch me play") and from the AI's own skills, then evolves after deaths when Brain = Neural net.
- Menu: toggles, skills, friends list, 3 brain slots, death recap graph, training mode (fights hostile mobs and friends).
- Status line (health, target, K/D) shows above the hotbar while the AI is on.
