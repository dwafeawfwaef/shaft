# Shafting

Fabric client mod for Minecraft **26.1.2**.

## Features

- **Littlefoot ESP** — finds the `RemotePlayer`/player entity named `Littlefoot` and draws a box through walls.
- **Corpse Highlight** — highlights visible Mineshaft corpse armor stands by helmet type.
- **Hide Opened** — stops highlighting corpses after interacting with them when the required key is present.
- **/shafting** — opens the configuration GUI.
- Settings are saved to `.minecraft/config/shafting.json`.

## Build on GitHub

1. Create a new GitHub repository.
2. Upload all files from this repository to the root of the repository.
3. Open **Actions**.
4. Select **Build Shafting** and run it, or push a commit to trigger it.
5. Open the completed workflow run and download the **shafting-jar** artifact.

The project targets Java 25 and uses Fabric's non-remapped Loom mode for Minecraft 26.1.2.

## Important area detection note

The supplied NoFrills snippets did not actually contain a different area detector; both used `Utils.isInArea("Mineshaft")`. This standalone project therefore uses the visible sidebar scoreboard and activates corpse highlighting when the sidebar contains `Mineshaft`.
