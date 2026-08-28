# Copperization

Copperization is a Fabric mod that takes Minecraft's copper theme to its logical conclusion: mobs and familiar building blocks can become copper too.

Version 0.1.0 focuses on one complete gameplay loop. Enchant a copper sword, gradually copperize a creature, preserve it as a weathering statue, pick it up, and place it somewhere new. The same weathering language is used by copperized building blocks.

## Core gameplay

- **Copperization Enchantment** — three levels, available only for copper swords. A successful melee hit adds 15%, 22%, or 30% copperization.
- **Living Entity Copperization** — visible copper patches grow in four stages while movement and combat attributes weaken.
- **Copper Statues** — at 100%, the creature is frozen in place with its type, name, age, variant, equipment, hands, and stable pose data intact.
- **Oxidation** — unwaxed statues slowly progress through Fresh, Exposed, Weathered, and Oxidized states on the server.
- **Waxing** — use a honeycomb to lock the current statue state.
- **Axe Scraping** — an axe removes wax first, then reverses oxidation one stage at a time.
- **Statue transport** — use a pickaxe on a statue to obtain one Copper Statue item. Place it on a block to reconstruct the statue.
- **Copperization Wand** — transforms supported building blocks, with durability, a one-second cooldown, particles, and sound.
- **Copperized Blocks** — 11 families with four oxidation stages and matching waxed variants, integrated with vanilla copper weathering.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer compatible release
- Fabric API 0.158.0+26.2 or newer compatible release
- Java 25

No GeckoLib or player animation library is required.

## Installation

1. Install Fabric Loader for Minecraft 26.2.
2. Put Fabric API and the Copperization jar in the instance's `mods` directory.
3. Start Minecraft with the Fabric profile.

The mod is required on both the server and every connecting client.

## How to play

Obtain the Copperization enchantment through the normal enchantment system and apply it to a copper sword. Other swords are not supported. Attack an eligible creature until the copper layer reaches 100%; the creature then becomes a statue.

Use honeycomb to wax a statue, or use an axe to remove wax and scrape oxidation. A pickaxe packs the statue into one item. Its tooltip identifies the stored entity, oxidation stage, and wax state. Right-click a block face to place it again.

Craft the Copperization Wand in a vertical line with a copper ingot, an amethyst shard, and a blaze rod. It supports these source blocks:

- Stone, Cobblestone, Stone Bricks
- Deepslate, Cobbled Deepslate, Deepslate Bricks
- Bricks, Blackstone, Polished Blackstone
- End Stone, Nether Bricks

Containers, redstone devices, portals, pistons, and other complex blocks are intentionally not transformed.

## Current limitations

- Players, the Ender Dragon, the Wither, dead entities, and entities that cannot be serialized are excluded.
- Passengers and leash connections are detached rather than stored in the statue item.
- The generic frozen pose preserves position, body/head rotation, pitch, entity pose, limb animation, attack animation, age animation, and hand-use state. Entity-specific model bones may settle into their renderer's normal pose.
- Copper rendering is a stable four-mask UV-space overlay. It preserves the base model and texture, but its bottom-to-top appearance is approximate on unusual model UV layouts.
- Only the 11 safe full-block families listed above are included in 0.1.0.

## Compatibility

Gameplay state is authoritative on the server and synchronized through Fabric's entity attachment system. Client rendering code remains in the split client source set and is not loaded by a dedicated server. The mod uses Fabric API and vanilla rendering/weathering systems; it does not use raw OpenGL hooks.

## Building from source

Clone the repository, install Java 25, then run:

```text
./gradlew runDatagen
./gradlew build
./gradlew runClientGameTest
```

On Windows, use `gradlew.bat`. The built jar is written to `build/libs/`. Automated server GameTests and JUnit tests are part of `build`.

## Credits

Developed by Shouyun. Original pixel-art resources were produced specifically for Copperization. Fabric and Minecraft provide the underlying modding and game APIs.

Repository: [github.com/ikunkk02-afk/Copperization](https://github.com/ikunkk02-afk/Copperization)

## License

Copperization is licensed under the [MIT License](LICENSE).
