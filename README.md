# Helicopter Mod (Fabric, Minecraft 1.21.11)

A tiny Fabric mod that adds **exactly one** small, unarmed, easy-to-fly light transport
helicopter. No weapons, no variants, no fuel micromanagement — right-click to get in,
WASD + Space/Shift to fly, right-click again to get out.

---

## 0. If you're uploading this to GitHub (read this first)

This zip is packed so that extracting it puts `build.gradle`, `settings.gradle`, `gradlew`,
`src/`, etc. **directly** where you extract it — there is no extra wrapping folder to worry
about. When you upload to a GitHub repo:

1. Extract the zip somewhere on your computer.
2. Open that extracted folder so you can see `build.gradle` and `settings.gradle` **directly
   inside it** (not one level up, not one level down).
3. Select **everything inside that folder** (including the hidden `.github` folder — make
   sure your file browser shows hidden files) and upload/drag all of it into your repo's
   root. `build.gradle` should end up at the very top level of the repo, sitting right next
   to `README.md` — not inside any subfolder.
4. A working `.github/workflows/build.yml` is already included, so GitHub Actions will run
   automatically on push — no extra setup needed.

If you use `git` instead of the browser uploader, this is even less error-prone:
```bash
cd your-repo
# copy the extracted contents in, replacing anything old
git add -A
git commit -m "helicopter mod"
git push
```

The included workflow runs `gradle build` via `gradle/actions/setup-gradle` rather than
`./gradlew build`, specifically so a missing/non-executable `gradlew` (a common upload
hiccup) can never break the CI build.


```
helicoptermod/
├── build.gradle                  Gradle/Loom build configuration
├── settings.gradle                Gradle plugin repositories
├── gradle.properties              All version numbers in one place
├── gradlew, gradlew.bat           Gradle wrapper scripts (ready to use)
├── gradle/wrapper/                Wrapper jar + config (real, working, pre-downloaded)
├── .github/workflows/build.yml   GitHub Actions: builds the mod on every push
├── LICENSE                        CC0 (public domain) — change if you'd like
│
└── src/main/
    ├── java/com/helicoptermod/
    │   ├── HelicopterMod.java              Main mod entrypoint (registers everything)
    │   │
    │   ├── entity/
    │   │   ├── HelicopterEntity.java        THE helicopter: flight, seats, damage/breaking
    │   │   └── ModEntities.java             Registers the entity type
    │   │
    │   ├── item/
    │   │   ├── HelicopterItem.java          The placeable "Helicopter" item (like a boat)
    │   │   └── ModItems.java                Registers the item + adds it to creative tab
    │   │
    │   ├── sound/
    │   │   └── ModSounds.java               Registers the engine sound event
    │   │
    │   └── client/                          CLIENT-ONLY code (rendering, sound playback)
    │       ├── HelicopterModClient.java     Client entrypoint: registers renderer, drives engine sound
    │       ├── HelicopterEntityModel.java   The 3D model (cuboids for body/tail/rotors/skids)
    │       ├── HelicopterEntityRenderer.java Positions/rotates/draws the model each frame
    │       ├── HelicopterRenderState.java   Per-frame data snapshot used by the renderer
    │       └── HelicopterEngineSoundInstance.java  Looping engine sound while flying
    │
    └── resources/
        ├── fabric.mod.json               Mod metadata (id, entrypoints, dependencies)
        ├── assets/helicoptermod/
        │   ├── icon.png                        Mod icon
        │   ├── lang/en_us.json                 "Helicopter" display names
        │   ├── textures/entity/helicopter/helicopter.png   3D model texture (UV-mapped)
        │   ├── textures/item/helicopter.png    2D hotbar icon
        │   ├── models/item/helicopter.json     Tells the item to use the 2D icon above
        │   └── sounds.json                     Maps the sound event to the .ogg file
        │   └── sounds/helicopter/engine_loop.ogg   The actual engine sound (synthesized)
        └── data/helicoptermod/recipe/helicopter.json   Optional crafting recipe
```

**Where things go if you're copying pieces into an existing project:** everything under
`src/main/java` goes in your mod's Java source root, everything under `src/main/resources`
goes in your resources root, keeping the same relative paths. The package name
`com.helicoptermod` and mod id `helicoptermod` appear throughout — rename both consistently
(a project-wide find/replace) if you want your own namespace.

---

## 2. Building it

Requirements: **JDK 21** (Minecraft 1.21.11 is the last release built against Java 21).

```bash
cd helicoptermod
./gradlew build          # Windows: gradlew.bat build
```

The compiled mod jar appears at `build/libs/helicoptermod-1.0.0.jar`. Drop that into the
`mods` folder of a Fabric-Loader 1.21.11 instance that also has **Fabric API** installed
(same version family as listed in `gradle.properties`), alongside Fabric Loader itself.

To test it directly from source: `./gradlew runClient`.

The Gradle wrapper included here is real and ready to go — you don't need to install
Gradle yourself, just a JDK.

> **Update:** this project originally shipped pointing at `id 'fabric-loom' version '1.14'`,
> which fails with *"Plugin [id: 'fabric-loom', version: '1.14'] was not found"* — that
> plugin coordinate had already gone stale. As of 1.21.11, Loom's Gradle plugin ID for
> obfuscated versions is `net.fabricmc.fabric-loom-remap`, not `fabric-loom`, and the
> current version is a moving target (verified live against Fabric's own
> `fabric-example-mod` "1.21.11" branch at the time of writing: Loom `1.17-SNAPSHOT`,
> Fabric Loader `0.19.3`). `build.gradle` and `gradle.properties` here have been corrected
> to match. If a future Loom release changes the ID or version again, check
> `https://github.com/FabricMC/fabric-example-mod/blob/1.21.11/gradle.properties` for the
> current values Fabric themselves are using.

---

## 3. A transparent note on mappings and verification

This code is written and **cross-checked against the actual published Yarn javadocs for
`1.21.11+build.1`/`+build.4`** (method signatures for `damage`, `updatePassengerPosition`,
`initDataTracker`, `interact`, `isJumping`, etc. were individually verified against those
docs while building this). A few things worth knowing:

- **1.21.11 changed some core APIs very recently**, and this mod already accounts for the
  ones most likely to bite a hand-written entity: entity NBT saving moved from
  `NbtCompound` to a `WriteView`/`ReadView` API, and `Entity#getWorld()` was renamed to
  `Entity#getEntityWorld()`. Both are already handled correctly in the code here.
- **Fabric itself switched its default documentation and examples to Mojang's official
  mappings starting with the 1.21.11 release**, with Yarn now positioned as the legacy/
  alternative option (still fully published and buildable — this project uses it because
  it's what the overwhelming majority of existing Fabric tutorials and my own knowledge
  are grounded in, which meant far more of this code could be verified against real,
  current documentation rather than guessed). If you'd rather build against Mojang
  mappings going forward, Fabric's migration guide is at
  `https://docs.fabricmc.net/1.21.11/develop/porting/mappings/` — it's a mostly mechanical
  rename (`World`→`Level`, `getYaw()`/`getPitch()` stay the same, etc.), but it isn't a
  drop-in for this specific file set.
- I don't have the ability to actually invoke Minecraft/Fabric's Maven repositories from
  where this was built, so **this has not been compiled end-to-end**. Everything has been
  checked for structural correctness (balanced braces, valid JSON, consistent types) and
  the riskiest API calls were individually verified against live javadocs, but if you hit
  a compile error on something I couldn't verify, it's almost always a one-line fix —
  the compiler error will name the exact method it expected.

The two spots most likely to need a tweak if Minecraft's rendering internals have shifted
slightly by the time you build this are marked with `NOTE:` comments in
`HelicopterEntityRenderer.java`.

---

## 4. Controls

| Input | Effect |
|---|---|
| Right-click the helicopter | Get in (pilot seat first, then up to 3 passenger seats) |
| W | Accelerate forward |
| S | Brake / reverse (slower than forward) |
| A / D | Turn left/right |
| Mouse | Looking around also gently steers the nose that way over a second or two |
| Space (hold) | Ascend |
| Shift (hold) | Descend |
| No input | Auto-hover — it holds position and altitude by itself |
| Right-click again while seated | Get out |
| Punch it while empty | Breaks it back into a "Helicopter" item after enough hits |

**Why both "A/D turns" and "mouse steers" at once?** Minecraft's default Sneak key both
descends (per your spec) *and* is the engine's built-in "dismount vehicle" key — those two
uses of Shift genuinely conflict, and reliably overriding the built-in one requires a mixin
fragile enough that I chose not to gamble your build on it. Instead: right-click-to-exit is
the primary way out (and always works), and if Shift's built-in dismount does kick in while
you're descending, `HelicopterEntity` gives you a few seconds of Slow Falling automatically
so it's never a punishing fall — just fly/walk back and hop in again. This is the same
trade-off real vanilla flying mounts in Minecraft have run into.

## 5. Design decisions worth knowing about

- **No gravity, ever.** The helicopter never falls — occupied or not. That's what makes
  "hovers with no input" and "keeps its position after you leave" both true for free, and
  it's why there's no fall-related crash mechanic to worry about.
- **No fuel system**, per the "skip it if it's simpler" option in the spec.
- **Up to 4 seats** (1 pilot + 3 passengers) since it's described as a light *transport*
  helicopter — only the pilot (first passenger) controls it.
- **Health is a plain 60-point pool**, reduced by any damage source; hitting 0 drops a
  "Helicopter" item and removes the entity, so it can be broken and re-placed like a boat.
- **Texture is a normal editable PNG** (`textures/entity/helicopter/helicopter.png`) — open
  it in any image editor to reskin it; the UV layout is documented via comments in
  `HelicopterEntityModel.java` so you know which region paints which part.

## 6. Easy things to tune

All in `HelicopterEntity.java`, top of the class:
- `MAX_HORIZONTAL_SPEED` / `MAX_VERTICAL_SPEED` — top speeds
- `HORIZONTAL_ACCEL` / `VERTICAL_ACCEL` — how "floaty" vs "snappy" it feels
- `TURN_SPEED_PER_TICK` — how fast A/D turns it
- `TURN_INPUT_SIGN` — flip to `-1.0f` if A/D ever feel backwards in your build

Enjoy the flight.
