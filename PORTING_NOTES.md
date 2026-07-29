# AE2WTLib → Fabric 26.1.2 — Porting Notes

**Fork:** `vobolgus/AE2WirelessTerminalLibrary` (local clone `~/IdeaProjects/AE2WTLib`), branch `fabric-26.1`,
branched from upstream `main` @ `3fe88dea` = tag **`26.1.0-beta`** (released 2026-07-25).
**Upstream:** `github.com/Mari023/AE2WirelessTerminalLibrary` (⚠ NOT `Mari023/AE2WTLib` — that repo does not exist;
the Modrinth slug is `applied-energistics-2-wireless-terminals`).
**License:** MIT (both `LICENSE` and `neoforge.mods.toml`) → **publishable**, no license gate.

**Shape:** dual-loader (playbook Part 8, the AE2/GuideME architecture). Upstream is on 26.1 NeoForge already,
so this is a *loader* port only — **no MC forward-port**, which is the cheapest port shape we have.

---

## 0. Status

| Wave | Scope | State |
|---|---|---|
| **W1** | recon + dual-loader skeleton | ✅ **DONE** (this document + `loader/fabric`) |
| **W2** | foundation seams (registration, config, network, components) | ✅ **DONE** 2026-07-29 — see §8 |
| **W3** | events + mixins (the NeoForge event surface) **+ the client layer** | ✅ **DONE** 2026-07-29 — see §9 |
| W4 | capabilities / transfer (energy + inventories) | ▫ |
| ~~W5~~ | ~~client: screens, GUI, hotkeys, scroll input~~ | **absorbed into W3** (§9.5) |
| W6 | integrations (JEI / REI / EMI, Curios→Trinkets) | ▫ |
| W7 | gates: gametest → Prism in-world → real-network MP; polish | ▫ |

**W1 gates (both green, 2026-07-29):**
- `./gradlew assemble -PruntimeItemlistMod=none` (NeoForge, root + `:ae2wtlib_api`) — **GREEN**, unchanged behaviour.
- `./gradlew :loader:fabric:build` — **GREEN** (skeleton; shared sources still gated off).

**W2 gates (all green, 2026-07-29):**
- `./gradlew build spotlessCheck -PruntimeItemlistMod=none` — NeoForge **GREEN**, behaviour unchanged.
- `./gradlew assemble -Pae2wtlib.skipFabric=true` (NeoForge-only CI job) — **GREEN**.
- `./gradlew :loader:fabric:build` — **GREEN** with the whole shared tree compiled, minus the W3 event surface
  and the W6 recipe-viewer plugins. ⚠ The `-Pae2wtlib.fabric.api/shared` switches now **default to `true`**:
  the platform layer references the shared trees, so a build with them off no longer compiles.
- ~~**Bonus — the W4 gate came free:** `./gradlew :loader:fabric:runServer` boots a dedicated server clean.~~
  ⚠ **RETRACTED in W3.** Re-running `runServer` on commit `e6947731` reproduces a hard boot failure
  (`NullPointerException: Components not bound yet`), so this gate was never actually green — the claim was
  recorded from an earlier state of `AE2wtlibFabric.init()`. Root cause and fix: §9.4. **Lesson: re-run a gate
  from the commit you are claiming it for.**

**W3 gates (all green, 2026-07-29):** see §9.6.

---

## 1. Recon

### 1.1 Size and layout

103 Java files / **5 663 LOC**, 89 resource files. Two Gradle modules upstream:

| Module | Files | Role |
|---|---|---|
| root (`src/main/java`) | 70 | the mod itself (`de.mari_023.ae2wtlib`) |
| `ae2wtlib_api` (`ae2wtlib_api/src/main/java`) | 33 | the addon-facing API (`de.mari_023.ae2wtlib.api`), `jarJar`'d into the mod jar |

There is **no client/server source split** upstream (single source set per module, exactly like AE2's own
dual-loader arrangement). No datagen module — all recipes/advancements/models are **hand-written JSON** under
`src/main/resources` (79 files). That removes the entire "datagen harvest & transform" pipeline (playbook
Part 7) from this port: nothing to regenerate, nothing to transform, no `neoforge:` recipe-key drift guard.

Upstream is **NeoForge-only today** (Modrinth `26.1.0-beta` lists loader `neoforge` only). Older lines
(1.20.1/1.21.1) did ship Fabric, and stale branches `1.21.1`, `trinkets_inventory` exist — but they are
1.21.x-era and pre-date the 26.1 rewrite, so they are reference material at best, not a merge base.

### 1.2 Feature inventory (what has to work at the end)

**Terminals** (each = item + menu + menu-host + screen + a `WTDefinition` registration):

| Terminal | Package | Item id | Notes |
|---|---|---|---|
| Wireless **Crafting** Terminal | `wct` | `ae2:wireless_crafting_terminal` | ⚠ **Not registered by this mod.** `AEItemsMixin` intercepts AE2's own item factory and swaps in `ItemWCT`. |
| Wireless **Pattern Encoding** Terminal | `wet` | `ae2wtlib:wireless_pattern_encoding_terminal` | |
| Wireless **Pattern Access** Terminal | `wat` | `ae2wtlib:wireless_pattern_access_terminal` | |
| Wireless **Universal** Terminal (WUT) | `wut` + `api/terminal/ItemWUT` | `ae2wtlib:wireless_universal_terminal` | combines/upgrades the others; shift-scroll cycles the active terminal |

**Cards / extras:** `quantum_bridge_card` (upgrade for all terminals — infinite range), `magnet_card`
(vacuum pickup straight into the ME network; own `MagnetMenu`/`MagnetScreen`/`MagnetMode`).
There is **no "quantum bracelet"** in this mod.

**Sub-menus / screens:** `TrashMenu`/`TrashScreen` (trash slot in the WCT), `MagnetScreen`,
`WirelessTerminalSettingsScreen`, plus the API widgets `TerminalSelectionPanel`, `ScrollingUpgradesPanel`,
`UpgradeBackground`, `IconButton`, `PlayerEntityWidget`, `ArmorSlot`.

**Behaviours:** *restock* (auto-refill the held/used stack from the ME network — the single most invasive
feature, it hooks 6 different vanilla/NeoForge events), *magnet*, *stow*, plus three AE2 hotkey actions
(`ae2wtlib_restock`, `ae2wtlib_magnet`, `ae2wtlib_stow`).

**Recipes:** 2 custom serializers (`ae2wtlib:upgrade` = add a terminal to a WUT, `ae2wtlib:combine` = merge
two terminals into a WUT) registered with plain `Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, …)`
— already loader-neutral.

**Networking:** 6 payloads — C2S `CycleTerminalPacket`, `SelectTerminalPacket`, `TerminalSettingsPacket`;
S2C `UpdateWUTPackage`, `UpdateRestockPacket`, `RestockAmountPacket`.

**Guidebook:** 7 GuideME markdown pages under `assets/ae2wtlib/ae2guide/` + one page injected into AE2's own
guide namespace (`assets/ae2/ae2guide/items-blocks-machines/wireless_terminals.md`). Pure data — should port
untouched (our GuideME Fabric fork already serves AE2's guide).

**Localisation:** 13 languages for the mod + 5 for the API. Untouched by the port.

### 1.3 Dependencies

| Dependency | Upstream pin | Fabric-side answer |
|---|---|---|
| **AE2** | `org.appliedenergistics:appliedenergistics2:26.1.10-beta` | our fork's Fabric build, `org.appliedenergistics:appliedenergistics2-fabric:26.1.10-beta` (see §3) |
| NeoForge | `26.1.2.87` | fabric-loader `0.19.3` + fabric-api `0.151.0+26.1.2` |
| Curios | `15.0.0+26.1.2`, **commented out** in `build.gradle.kts` | Trinkets — see §5 |
| JEI | `29.19.0.51` (`compileOnly`) | JEI ships Fabric 26.1.2 (`maven.blamejared.com`) |
| REI | `26.1.819` (`compileOnly`) | REI ships Fabric 26.1.819 |
| EMI | `1.1.22+1.21.1` (`compileOnly`, **api classifier**) | ⚠ no 26.1 Fabric EMI artifact known — likely dropped, see §5 |
| architectury | `20.0.10` (REI runtime only) | Fabric build exists |
| GuideME | transitively via AE2 | `org.appliedenergistics:guideme-fabric:26.1.12-beta` (mavenLocal, `compileOnly`) |

No Registrate, no Porting-Lib, no architectury *shim* layer in the sources — the mod talks to NeoForge
directly. Nothing to un-Registrate.

### 1.4 NeoForge coupling surface — small and concentrated

Only **19 of 103 files** import `net.neoforged.*`. With the shared trees compiled against our AE2 Fabric jar
(`-Pae2wtlib.fabric.api=true -Pae2wtlib.fabric.shared=true`) javac reports errors in exactly **21 files**
(the 19 + the JEI and EMI plugins, which just lack their artifacts): **82 of 103 files compile clean today.**

The coupling groups into 9 seams — **plus a tenth found in W2** (`Ae2wtlibItemHooks`, §8.2), which this
import-based scan structurally could not see: NeoForge patches its extension methods straight into vanilla
classes, so a call like `stack.canEquip(...)` carries no `net.neoforged.*` import. **Lesson for the next
dual-loader port: the import scan gives a floor, not the true surface — the real number only appears when the
shared tree first compiles against the Fabric classpath.**

| # | Seam | Files | NeoForge mechanism | Fabric answer |
|---|---|---|---|---|
| 1 | **Item registration** | `AE2wtlibItems` | `DeferredRegister.Items` | plain `Registry.register` + golden rule #7 (`Properties.setId` before construction, `registerBlocks`/`bindComponents`) |
| 2 | **Config** | `AE2wtlibConfig`, `AE2wtlibClientConfig`, `AE2wtlibClient` | `ModConfigSpec` + `ModContainer.registerConfig` | night-config store behind an `AE2wtlibConfigStore` seam — crib AE2's `appeng.core.config.ConfigStore` + `appeng.fabric.config.FabricConfigStore` (identical TOML output) |
| 3 | **Networking** | `AE2wtlibForge`, `AE2wtlibEvents`, `AE2wtlibAPIImplementation`, `MagnetHandler`, `TerminalSelectionPanel`, `IUniversalTerminalCapable`, `WirelessTerminalSettingsScreen`, `AE2wtlibClient` | `PayloadRegistrar`, `PacketDistributor`, `ClientPacketDistributor` | `PayloadTypeRegistry` + `Server/ClientPlayNetworking`, behind a static-holder `Ae2wtlibNet` seam (AE2's `NetworkAdapter` pattern) |
| 4 | **Stream codecs** | `WCTMenu`, `AE2wtlibComponents`, `WTDefinitionBuilder`, `AE2wtlibAdditionalComponents` | `NeoForgeStreamCodecs.enumCodec(…)` (4 call sites, all the same helper) | one shared `EnumStreamCodec` helper — smallest seam in the port |
| 5 | **Data components** | `AE2wtlibComponents` | AE2's `AEComponents.DR` (a `DeferredRegister`) | our fork replaced it with `AEComponents.init()`; register through vanilla `Registry.register` |
| 6 | **Attachments** | `AE2wtlib`, `AE2wtlibAdditionalComponents` (`CT_HANDLER`) | `AttachmentType` + `NeoForgeRegistries.ATTACHMENT_TYPES` | fabric-api `AttachmentRegistry`, or a weak map (AE2's `FabricPlayerCtrlAttachment` precedent — the handler is transient) |
| 7 | **Capabilities / energy** | `AE2wtlibForge#registerPowerStorageItem` | `Capabilities.Energy.ITEM` + AE2's `PoweredItemCapabilities` | `EnergyStorage.ITEM.registerForItems(…)` with AE2's `appeng.fabric.transfer.PoweredItemEnergyStorage` (crib: AE2 `InitApiLookup.java:255`) |
| 8 | **Item transfer** | `WrappedPlayerInventory` | overrides `InternalInventory#toResourceHandler()` using `PlayerInventoryWrapper` | our fork removed that method from the shared API (§3); Fabric answer = `appeng.fabric.transfer.FabricResources` |
| 9 | **Lifecycle + events** | `AE2wtlibForge`, `AE2wtlibClient`, `AE2wtlibAPIEntrypoint`, `AE2wtlibAPI` (`ModList.isLoaded`) | `@Mod`, `IEventBus`, 9 event classes | Fabric entrypoints (already stubbed) + callbacks/mixins, see §4 |

### 1.5 Mixins — all loader-neutral targets

`ae2wtlib.mixins.json` (5) + `ae2wtlib_api.mixins.json` (1). Every target is vanilla or AE2 — **no NeoForge
internals**, so all six should port as-is. ⚠ Re-verify each descriptor against the merged jar before W3
(playbook golden rule #1: recon line numbers and `@Local` names drift).

| Mixin | Target | Purpose | Fabric risk |
|---|---|---|---|
| `AEItemsMixin` | `appeng.core.definitions.AEItems#item(String,Identifier,Function)` | swaps AE2's wireless crafting terminal for `ItemWCT` | ✅ signature **verified identical** in our fork's Fabric jar (javap) |
| `GuiMixin` (client) | `Gui#extractSlot(...)` @ `GuiGraphicsExtractor#itemDecorations` | restock count overlay on the hotbar | vanilla; MixinExtras is JiJ'd by loader ≥0.15 |
| `ServerPlayerGameModeMixin` | `ServerPlayerGameMode#useItemOn` RETURN | restock after use-on-block | vanilla |
| `ServerPlayerMixin` | `ServerPlayer#drop(Z)` TAIL, `@Local(name="selected")` | restock after drop | `@Local` **by name** — must be re-verified; names survive only because 26.1 is unobfuscated |
| `ServerGamePacketListenerImplMixin` | `ServerGamePacketListenerImpl#tryPickItem`, `@Local(name="slotWithExistingItem")` | restock on pick-block | ⚠ **upstream bug candidate**: it is listed under `"client"` in the mixin config although the target is a dedicated-server class → the feature is likely dead on dedicated servers. Move it to `"mixins"` on Fabric and report upstream. |
| `WidgetContainerAccessor` (client) | `appeng.client.gui.WidgetContainer` | accessor | AE2 class; present in our fork |

### 1.6 Access transformers → access widener

Both ATs carry the *same* three entries; translated once into `loader/fabric/src/main/resources/ae2wtlib.accesswidener`:

| AT entry | javap on 26.1.2 | AW |
|---|---|---|
| `public-f Slot x` / `y` | already `public final int` | `mutable field` only (used by `ScrollingUpgradesPanel:130-131`) |
| `public ItemEntity target` | `private UUID target`, public setter, **no getter** | `accessible field` — ⚠ AW field-widening is flaky in this workspace; fall back to an `@Accessor` mixin if it does not take |

---

## 2. Build / toolchain changes made in W1

- **Gradle wrapper 9.2.1 → 9.5.1** (fabric-loom 1.17.x hard-requires ≥ 9.5). NeoForge/ModDevGradle is fine on it — verified by a green `assemble`.
- `settings.gradle.kts`: added the FabricMC plugin repository (the loom marker is not on the Gradle Plugin
  Portal, and declaring `pluginManagement.repositories` replaces the implicit default → the portal had to be
  re-added), pinned `net.fabricmc.fabric-loom` **1.17.8**, and `include("loader:fabric")` behind
  `-Pae2wtlib.skipFabric=true` (loom resolves dependencies at *configuration* time, so NeoForge-only CI jobs
  must be able to drop the subproject — same switch as `-Pae2.skipFabric` in the AE2 fork).
- `gradle.properties`: new Fabric block — `minecraft_version=26.1.2`, `fabric_loader_version=0.19.3`,
  `fabric_api_version=0.151.0+26.1.2`, `guideme_fabric_version=26.1.12-beta`, `trinketsVersion=4.0.0-beta.3`.
- **New subproject `loader/fabric`** (`archivesName = ae2wtlib-fabric`):
  - loom 1.17.8, JDK 25 toolchain, **no `mappings` line** (26.1 is unobfuscated) and plain `implementation`
    for every dependency (loom 1.17 dropped `modImplementation` & friends).
  - Full repository set declared *inside the project* — loom injects project-level repos which shadow the
    settings-level list under `PREFER_PROJECT`.
  - `splitEnvironmentSourceSets()` deliberately **not** used: the shared tree mixes client and server classes
    exactly like the NeoForge jar (`AE2wtlib.registerScreens`, `GuiMixin`, …), which is the same call the AE2
    fork made. The dedicated `runServer` boot is the client-leak gate instead.
  - `accessWidenerPath` → `ae2wtlib.accesswidener`.
  - Shared-source gates, **both OFF in W1**: `-Pae2wtlib.fabric.api=true` (the `ae2wtlib_api` tree) and
    `-Pae2wtlib.fabric.shared=true` (the root tree). Flip them per wave as the seams land.
  - `processResources` excludes `META-INF/neoforge.mods.toml`, `META-INF/accesstransformer.cfg` and the two
    NeoForge mixin configs from the Fabric jar (Fabric copies of the mixin configs live in this project).
  - Fabric metadata: `fabric.mod.json` (entrypoints `main`/`client`, AW, `depends` on ae2), `icon.png`, and
    Fabric copies of both mixin configs named `ae2wtlib.fabric.mixins.json` / `ae2wtlib_api.fabric.mixins.json`
    (deliberately renamed so the `processResources` exclude of the NeoForge-named originals cannot swallow them).
    ⚠ `fabric.mod.json` deliberately carries **no `"mixins"` key yet** — the mixin classes live in the shared
    tree, which is still gated off; loader hard-fails on a `"mixins"` entry whose classes cannot load. Add the
    two entries in W3 together with the shared flip.
  - `data/curios/**` is excluded from the Fabric jar (NeoForge-only tag; see section 5).
  - Spotless is scoped to `loader/fabric/src` only (the shared trees keep the root project's formatting).
- **Stubs (2 files, ~25 `// W1-STUB:` markers)** — `de.mari_023.ae2wtlib.fabric.AE2wtlibFabric` (main) and
  `…fabric.client.AE2wtlibFabricClient` (client). Every marker names the exact NeoForge call it replaces and
  the intended Fabric mechanism, in `AE2wtlibForge`'s constructor order, so W2/W3 can tick them off top-down.
  `grep -rn "W1-STUB" loader/fabric` is the todo list.

### Single Fabric jar (vs. NeoForge's two)

NeoForge ships `ae2wtlib_api` as a separate jar `jarJar`'d into the mod. The Fabric project compiles **both
trees into one jar** — the same call the AE2 fork made for its own single-jar arrangement, and it avoids a
second loom project just for 33 files. If an addon ever needs the API standalone on Fabric, split it later
with `include()` (jar-in-jar).

---

## 3. AE2 dependency — mavenLocal status and the API divergence table

### 3.1 What exists today

- Our AE2 fork's **`:neoforge`** module *does* have `maven-publish` (`org.appliedenergistics:appliedenergistics2`).
- Our AE2 fork's **`:fabric`** module has **no publishing at all** — there is no `:fabric:publishToMavenLocal`
  task, and no root/`buildSrc` convention applies one. Only GuideME's Fabric module publishes
  (`org.appliedenergistics:guideme-fabric`, already in mavenLocal at `26.1.10-alpha` and `26.1.12-beta`).

**RESOLVED 2026-07-29 (before W2):** the AE2 fork's `:fabric` module now publishes properly — mavenLocal holds
`org.appliedenergistics:appliedenergistics2-fabric:26.1.10-beta` with a real POM, Gradle `.module` metadata and
sources/javadoc jars. The W1 hand-installed POM is superseded. Re-run `./gradlew :fabric:publishToMavenLocal` in
the AE2 fork after every rebase and bump `ae2Version` here in lock-step. (R2 closed.)

*Historic — the W1 workaround:* the production jar `…/Applied-Energistics-2/dist/appliedenergistics2-fabric-26.1.10-beta.jar`
was hand-installed into `~/.m2/repository/org/appliedenergistics/appliedenergistics2-fabric/26.1.10-beta/`
together with a minimal POM.

**Durable fix (recommended, needs a change in the AE2 fork — deliberately NOT made from this port's session):**
add to `Applied-Energistics-2/loader/fabric/build.gradle.kts`

```kotlin
plugins { /* … */ `maven-publish` }
group = "org.appliedenergistics"          // already implied by the base name
publishing {
    publications { create<MavenPublication>("maven") {
        artifactId = "appliedenergistics2-fabric"
        version = project.version.toString()
        from(components["java"])
    } }
}
```

then `./gradlew :fabric:publishToMavenLocal` — exactly the shape GuideME's `loader/fabric/build.gradle`
already uses. Re-run it after every AE2 rebase; bump `ae2Version` here in lock-step.

### 3.2 ⚠ Our AE2 fork's API ≠ upstream AE2's API

`:neoforge` resolves upstream AE2 `26.1.10-beta` (from the moddev repositories), `:fabric` resolves our fork.
A `javap` diff over all 111 `appeng.*` classes AE2WTLib imports found **11 diverging classes** — every one of
them a *deliberate* de-NeoForging in our fork (commit `b87e74993`):

| Class | Upstream | Our fork |
|---|---|---|
| `ItemDefinition` | ctor takes `DeferredItem<T>` | ctor takes `AEItemEntry<T>` (new `appeng.core.registration`) |
| `AEItems` / `AEBlocks` / `AEComponents` | `public static final DeferredRegister DR` | `public static void init()` |
| `InitScreens` | `register(RegisterMenuScreensEvent, …)` | `register(InitScreens.MenuScreenRegistrar, …)` ← loader-neutral seam, **use this** |
| `AEConfig` | `register(ModContainer)` | `register(ConfigStore, ConfigStore)` |
| `InternalInventory` | `toResourceHandler()`, `wrapExternal(...)` | both removed → `NeoForgeResources`/`FabricResources` |
| `AEItemKey` | `toResource()`, `of(ItemResource)` | removed |
| `ConfigMenuInventory`, `SupplierInternalInventory` | `toResourceHandler()`; `getDelegate()` protected | removed; `getDelegate()` now **public** |
| `PoweredItemCapabilities` | present | **absent** (NeoForge overlay only) → `appeng.fabric.transfer.PoweredItemEnergyStorage` |
| `QuantumBridgeBlockEntity`, `QuantumCluster` | `ModelData`/`LevelEvent.Unload` | `getRenderData()` / `onLevelUnload(LevelAccessor)` |

**Impact:** every one of these divergences sits in a file that is *already* NeoForge-coupled upstream, so they
cost nothing extra — they will be absorbed by the same seams listed in §1.4. Do **not** be tempted to "fix"
them by publishing our fork's `:neoforge` jar to mavenLocal (which would shadow upstream for the root
project): the NeoForge module's job is to stay green against **upstream** AE2 so the fork remains
upstream-rebaseable and PR-able. Revisit only if the seam count from divergences alone exceeds ~3 extra files.

---

## 4. Wave plan

Order follows the playbook: **registration → logic → datagen → renderers → integrations**, renderers last.
Each wave ends with a checkpoint commit and both loader gates green.

### W2 — Foundation seams *(largest wave; ~1 day)*
Goal: `-Pae2wtlib.fabric.api=true -Pae2wtlib.fabric.shared=true` compiles for everything except the event
surface and the recipe-viewer plugins.
1. `EnumStreamCodec` helper replacing the 4 `NeoForgeStreamCodecs.enumCodec` calls (seam #4).
2. `AE2wtlibComponents.register` → vanilla registry (seam #5); `AE2wtlibAdditionalComponents` follows.
3. Item registration seam (#1): `AE2wtlibItems` behind a `Ae2wtlibRegistrar` holder — NeoForge impl keeps
   `DeferredRegister`, Fabric impl uses `Registry.register` + `ItemDefinition`/`AEItemEntry` on the fork side.
   ⚠ This is the one place the §3.2 `ItemDefinition` ctor divergence bites; expect a per-loader factory.
4. Config seam (#2): `AE2wtlibConfigStore` copied in shape from `appeng.core.config.ConfigStore`;
   NeoForge impl = `ModConfigSpec` wrapper (byte-identical TOML), Fabric impl = night-config.
5. Attachment seam (#6) for `CT_HANDLER`.
6. Networking seam (#3): `Ae2wtlibNet` static holder with `sendToServer` / `sendToPlayer` / registration;
   both impls. **Register payload *types* identically on both loaders** — this is packet-format-sensitive
   (playbook Part 10: raw ids are not synced on 26.1; the payloads here carry enums and item stacks —
   `MagnetMode` uses `NeoForgeStreamCodecs.enumCodec` = a var-int ordinal, so keep the wire shape).
   *Gate:* `:loader:fabric:compileJava` with both flags, minus the event/integration files.

### W3 — Events + mixins *(~0.5 day)*
1. Re-home the 9 NeoForge events (seam #9). Mapping table to fill in as they land:
   `LivingEntityUseItemEvent.Finish` → mixin `LivingEntity#completeUsingItem`;
   `PlayerInteractEvent.RightClickBlock` / `EntityInteractSpecific` → `UseBlockCallback`/`UseEntityCallback`
   (⚠ upstream runs them at `EventPriority.LOWEST`, i.e. *after* everything else — fabric-api callbacks are
   registration-ordered, so a TAIL mixin is likely the faithful answer);
   `ItemEntityPickupEvent.Pre` → mixin on `ItemEntity#playerTouch` (also the consumer of the `target` AW);
   `ArrowNockEvent`/`ArrowLooseEvent` → mixins on `BowItem`/`CrossbowItem`;
   `BuildCreativeModeTabContentsEvent` → `ItemGroupEvents`;
   `ClientTickEvent.Post` → `ClientTickEvents.END_CLIENT_TICK`;
   `InputEvent.MouseScrollingEvent` → cancellable mixin on `MouseHandler#onScroll`.
2. Port the 6 mixins: javap-verify every descriptor, add the `"mixins"` key to `fabric.mod.json`,
   move `ServerGamePacketListenerImplMixin` out of the `client` list (§1.5).
3. Delete/exclude `AE2wtlibForge` + `AE2wtlibClient` from the Fabric source set (they become the NeoForge
   overlay); their logic now lives in the two Fabric entrypoints.
   *Gate:* `:loader:fabric:build` green with both shared flags on.

### W4 — Capabilities / transfer *(~0.25 day)*
Seams #7 and #8: energy capability for the 3 powered terminals via `EnergyStorage.ITEM`, and
`WrappedPlayerInventory` re-expressed through `appeng.fabric.transfer.FabricResources`.
⚠ Playbook Part 8: duplicate `BlockApiLookup`/`ItemApiLookup` providers — **first registration wins**;
register specific before generic and after AE2's own `InitApiLookup`.
*Gate:* dedicated `runServer` boots (`Done (` + 0 ERROR) — the client-class-leak canary.

### W5 — Client *(~0.5 day)*
Screens via `InitScreens.MenuScreenRegistrar` (our fork's seam), the API widgets, hotkeys via AE2's
`Hotkeys`/`HotkeyActions` (already loader-neutral), scroll input, the restock HUD overlay mixin.
Deferred by design: the NeoForge config screen (`IConfigScreenFactory`) has no in-tree Fabric equivalent —
ModMenu is optional and not a parity requirement.
*Gate:* `runClient` boots to title + a world; terminals open.

### W6 — Integrations *(~0.25 day)*
- **JEI**: Fabric discovery is the `jei_mod_plugin` **entrypoint** (the `@JeiPlugin` annotation is NOT
  scanned on Fabric). JEI 26.1.2 Fabric exists on `maven.blamejared.com`.
- **REI**: `rei_common` / `rei_client` entrypoints. ⚠ REI instantiates entrypoints during *its own* init,
  before ours — plugin constructors must be EMPTY and mod-loaded checks lazily memoized (this exact bug was
  found and fixed in the AE2 fork on 07-04).
- **EMI**: no 26.1 Fabric artifact is known (upstream pins `emi 1.1.22+1.21.1`). Recommend **dropping the EMI
  plugin from the Fabric jar** (exclude `recipeviewer/AE2wtlibEmiPlugin.java`) until one ships.
- **Curios → Trinkets**: see §5.

### W7 — Gates and polish
`runServer` → gametests (this mod has **no test harness upstream** beyond `AE2wtlibTestPlots`; AE2's fork has
a working Fabric gametest runner to crib) → `runClient` → the Prism in-world ladder → **real-network
multiplayer join** (playbook Part 10 — singleplayer never serializes packets, and this mod has 6 payloads).

---

## 5. Curios → Trinkets mapping surface

**Good news: the surface is currently empty.** Upstream's only Curios code is *commented out*:
`WUTHandler#findTerminal` (`ae2wtlib_api/.../terminal/WUTHandler.java:130-144`) carries
`// FIXME reintroduce curio compat once the ae2 curio integration is updated to work properly`,
and `build.gradle.kts` has the Curios dependency commented out. The only live Curios artefacts are
`src/main/resources/data/curios/tags/item/curio.json` (a datapack tag — harmless, and it should be
**excluded from the Fabric jar** or mirrored as a Trinkets tag) and a soft `[[dependencies]] modId = "curios"`
entry in `neoforge.mods.toml`.

The commented block needs, from AE2, exactly two things — both of which AE2 already abstracts:
1. an accessory-inventory view → `appeng.integration.modules.curios.CuriosSupport` (shared seam,
   `@Nullable Inventory getCuriosInventory(Player)`),
2. a locator → `appeng.menu.locator.MenuLocators.forCurioSlot(int)` — **present in the shared AE2 sources**,
   so it exists on Fabric too.

⚠ **On our Fabric AE2, `CuriosSupport` is a no-op**: `appeng.fabric.FabricCuriosSupport` returns `null`
("a Trinkets integration could replace this later"). So the honest dependency chain is:

> AE2WTLib wireless-terminal-in-a-trinket-slot **requires AE2's `FabricCuriosSupport` to be implemented
> against Trinkets first.** That is a change in the *AE2 fork*, not here.

Recommendation: keep Curios/Trinkets **out of scope for this port** (matching upstream, where it is disabled
on NeoForge too), and file it as a follow-up on the AE2 fork: implement `FabricCuriosSupport` over
`trinkets_updated 4.0.0-beta.3` (the pack's pin; `dev.emi:trinkets` API). Local crib for the Trinkets API
surface: Create Fly's `compat/trinkets/` (`GoggleTrinket`) in `~/IdeaProjects/refs/Create-Fly-26.1`.
`trinketsVersion` is already parked in `gradle.properties`.

---

## 6. Risk register

| # | Risk | Severity | Mitigation / current read |
|---|---|---|---|
| R1 | **`ItemDefinition` ctor + `AEItems.DR` divergence** between upstream AE2 (`:neoforge`) and our fork (`:fabric`) forces a per-loader item-registration factory | ~~high~~ **CLOSED** (W2) | seam #1 landed; 3 of 11 predicted divergences bit, all in already-coupled files (§8.4). Fallback not needed. |
| R2 | **AE2 Fabric jar is hand-installed in mavenLocal** — no reproducible publish, silently stale after an AE2 rebase | ~~high~~ **CLOSED** | the AE2 fork now publishes `org.appliedenergistics:appliedenergistics2-fabric:26.1.10-beta` to mavenLocal properly (POM + `.module` + sources/javadoc). Re-run `:fabric:publishToMavenLocal` after every AE2 rebase and keep `ae2Version` in lock-step. |
| ~~R3~~ | **Restock is the feature, and it is 100% event-driven** — 6 NeoForge events with priority semantics (`EventPriority.LOWEST`, `event.isCanceled()`) that Fabric callbacks do not reproduce | ~~high~~ **CLOSED (headless)** | W3 mapped all 9 listeners - §9.1. `LOWEST` + `isCanceled` map exactly onto a fabric-api late **phase** + the array-backed invoker's non-`PASS` short-circuit, so the two interaction events are callbacks; the other four have no fabric-api equivalent at all and became javap-verified mixins. In-world verification still open (W7). |
| ~~R4~~ | `@Local(name=…)` in two mixins (`selected`, `slotWithExistingItem`) | ~~medium~~ **CLOSED - and it BIT** | `slotWithExistingItem` is real; **`selected` does not exist in vanilla** (NeoForge patch adds it) and `removed` is not a substitute. Shared `ServerPlayerMixin` excluded from the Fabric tree, `ServerPlayerDropMixin` added. All 11 mixins confirmed applied at runtime with `-Dmixin.debug.verbose=true`. §9.2 |
| R5 | **Multiplayer packet desync** — 6 payloads, one enum codec, item stacks | medium | playbook Part 10; format-pinning tests + a real-network join in W7. Singleplayer and gametests will NOT catch it |
| R6 | `AEItemsMixin` replaces an **AE2-owned item class** — if AE2's Fabric registration path constructs items differently, the swap may not take | ~~medium~~ **CLOSED** (W2) | verified **live**: the swap takes against the fork's `AEItemEntry` path. Asserted at init by `AE2wtlibFabric.verifyWirelessCraftingTerminalSwap()`, and a dedicated server boots past it (§8.3). |
| **R12** | **Fabric Loader does not order entrypoints by mod dependency**, and AE2 registers content from a different entrypoint per dist → wrong item raw ids on one side = silent MP corruption | ~~critical~~ **MITIGATED** (W2) | registration is driven from a TAIL mixin on `AppEngFabric#init` (§8.3). ⚠ `AppEngFabric` exists only in our AE2 fork — **re-verify the target on every AE2 rebase**. Durable fix = an `ae2:registration` addon entrypoint in the AE2 fork. |
| **R13** | **NeoForge extension-method surface** was invisible to the W1 import scan; more may still be hiding in the W3/W5 files | medium | 3 found and sealed behind `Ae2wtlibItemHooks` (§8.2); one carries a real behaviour gap (`PreventRemoteMovement`). Expect more when the event surface and the client tree compile. |
| **R14** | **`new ItemStack(...)` during mod init is illegal on 26.1** - item data components are bound lazily by `DataComponentInitializers`, with the reloadable server resources | ~~unknown~~ **CLOSED (W3)** | It was a hard `runServer` crash, inherited from W2 (`Components not bound yet`). Creative-tab contents are now built lazily in `buildDisplayItems`. §9.4 - audit any other eager `ItemStack` in a registration path. |
| **R15** | **Fabric client entrypoint ordering** - our `ClientModInitializer` can run before AE2's, i.e. before our own payload types/menus exist | ~~unknown~~ **MITIGATED (W3)** | Hit immediately on the first `runClient`. `FabricClientBootstrap` rendezvous; §9.5. Same root cause as R12 - the durable fix is an AE2-fork addon entrypoint. |
| R7 | EMI has no 26.1 Fabric artifact | low | drop the EMI plugin from the Fabric jar (W6) |
| R8 | REI entrypoint-timing crash (empty-ctor rule) | low | known + documented; AE2 fork hit and fixed it |
| ~~R9~~ | AW field-widening flakiness (`ItemEntity.target`) | ~~low~~ **MOOT** | W3's `ItemEntityMixin` `@Shadow`s the field instead of relying on the AW; the AW line is kept only as a faithful AT translation. |
| R10 | Upstream `ServerGamePacketListenerImplMixin` sits in the `client` mixin list → pick-block restock probably dead on dedicated servers | low (a *fix*, not a regression) | ✅ common list on Fabric, confirmed applying on a dedicated server (§9.3). **Still to report upstream.** |
| R11 | No automated tests anywhere in this repo | medium | W7: crib AE2's Fabric gametest runner; at minimum place/spawn-and-tick every registered item + a recipe-presence test (playbook rules #6, #8) |

---

## 7. Command crib

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25 2>/dev/null \
  || echo ~/.gradle/jdks/eclipse_adoptium-25-aarch64-os_x.2/jdk-25.0.3+9/Contents/Home)

# NeoForge regression harness (must stay green at EVERY commit)
./gradlew assemble -PruntimeItemlistMod=none
./gradlew spotlessCheck -PruntimeItemlistMod=none

# Fabric (from W2 the shared trees are ON by default - the flags are optional/legacy)
./gradlew :loader:fabric:build
./gradlew :loader:fabric:runServer      # W4 gate - already GREEN as of W2
./gradlew :loader:fabric:runClient      # W5 gate

# NeoForge-only CI job (loom resolves at configuration time)
./gradlew assemble -Pae2wtlib.skipFabric=true

# The todo list
grep -rn "W2-STUB" loader/fabric
```

Binary truth:
`javap -classpath ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1.2/minecraft-merged-deobf-26.1.2.jar <fqcn>`
AE2 fork jar: `~/IdeaProjects/Applied-Energistics-2/dist/appliedenergistics2-fabric-26.1.10-beta.jar`
Upstream AE2 jar: `~/.gradle/caches/modules-2/files-2.1/org.appliedenergistics/appliedenergistics2/26.1.10-beta/*/appliedenergistics2-26.1.10-beta.jar`

---

## 8. W2 — foundation seams (landed 2026-07-29)

### 8.1 Seam map — what mapped to what

Every seam follows the same shape, cribbed from AE2's `appeng.core.network.NetworkAdapter`: a loader-neutral
interface in the shared tree with a static `get()` / `init(impl)` and a private `Holder`. The NeoForge halves live in
**`src/main/java/de/mari_023/ae2wtlib/neoforge/`** (excluded from the Fabric source set — see the `java.exclude`
block in `loader/fabric/build.gradle.kts`); the Fabric halves in `loader/fabric/src/main/java/.../fabric/`.

| # | Seam | Upstream shape | Shared seam | NeoForge impl | Fabric impl | Crib |
|---|---|---|---|---|---|---|
| 4 | enum stream codecs | `NeoForgeStreamCodecs.enumCodec(C)` ×4 | `api.EnumStreamCodec.of(C)` | *(same class — both loaders use it)* | *(same)* | `javap` on `NeoForgeStreamCodecs$3` |
| 5 | data components | `AE2wtlibComponents.DR` (`HashMap`) flushed by the API `@Mod` | `api.AE2wtlibAPIRegistration.register()`; `DR` → **`LinkedHashMap`** | `AE2wtlibAPIEntrypoint` (unchanged, now delegating) | called inline from `AE2wtlibFabric.init()` | — |
| 1 | item registration | `DeferredRegister.Items` + `new ItemDefinition<>(name, DeferredItem)` | `registration.Ae2wtlibItemFactory` | `NeoForgeItemFactory` (identical to upstream) | `FabricItemFactory`: `AERegistries.registerItem` → `entry.create()` → `Registry.register` → `entry.bind(...)` | AE2 `FabricRegistrar` |
| 2 | config | `ModConfigSpec` + `modContainer.registerConfig` | `config.Ae2wtlibConfigStore` (+`AE2wtlibConfig/ClientConfig.register(store)`) | `NeoForgeConfigStore` (thin `ModConfigSpec.Builder` adapter — TOML byte-identical) | `fabric.config.FabricConfigStore` (night-config 3.8.3, JiJ'd) | AE2 `ConfigStore`/`FabricConfigStore` |
| 6 | attachments | `AttachmentType` + `player.getData(CT_HANDLER)` | `attachment.Ae2wtlibAttachments` | `NeoForgeAttachments` (the `ct_handler` `DeferredRegister`, moved verbatim) | `FabricAttachments` — synchronized `WeakHashMap` (attachment is transient + per-entity, so this is faithful) | AE2 `FabricPlayerCtrlAttachment` |
| 3 | networking | `PacketDistributor` / `ClientPacketDistributor` | `api.Ae2wtlibNet` (typed to `CustomPacketPayload` — it must also carry AE2's `HotkeyPacket`, and it lives in the API module which sits *below* the mod module) | `NeoForgeNet` | `FabricNet` + `FabricNetworkInit`; the C2S sender is injected by the client entrypoint | AE2 `NetworkAdapter`/`FabricNetworkAdapter` |
| 9 | lifecycle (partial) | `ModList.get().isLoaded` | `api.Ae2wtlibPlatform` (a `Predicate<String>` holder — it must answer inside `AE2wtlibAPIImpl`'s static initializer) | `ModList.get()::isLoaded` | `FabricLoader…::isModLoaded` | — |
| 8 | item transfer (compile half) | `WrappedPlayerInventory` `record` overriding `toResourceHandler()` | class made **abstract** + `WrappedPlayerInventory.of(...)` factory hook | `NeoForgeWrappedPlayerInventory` re-adds the override | `FabricWrappedPlayerInventory` (empty) | §3.2 |
| — | screens (compile half) | `AE2wtlib.registerScreens(RegisterMenuScreensEvent)` | moved out of the shared `AE2wtlib` class | `NeoForgeScreens` | **W5** | §3.2 |
| **10** | **NeoForge extension methods** | see §8.2 | `Ae2wtlibItemHooks` | `NeoForgeItemHooks` | `FabricItemHooks` | — |

Two things deliberately did **not** need a seam:
- `CreativeModeTab.builder()` → the vanilla overload `CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)`, which is
  exactly what NeoForge's no-arg builder delegates to (same crib AE2's `MainCreativeTab` used).
- `Player#closeContainer()` → protected in vanilla, public on NeoForge; one `accessible method` line in our
  accesswidener. (AE2's AW already carries it and loom applies dependency AWs transitively, but relying on another
  mod's AW is fragile, so it is declared here too.)

### 8.2 ⚠ Seam group 10 — NeoForge extension methods (not in the W1 inventory)

Three call sites in the shared tree use methods NeoForge **patches into vanilla classes**, so they carry no
`net.neoforged.*` import and the W1 import scan could not find them. They only surfaced when the shared tree first
compiled against the Fabric classpath. All three now sit behind `Ae2wtlibItemHooks`:

| Call site | NeoForge extension | Fabric answer | Parity |
|---|---|---|---|
| `StowHotkeyAction` | `ItemStack#isNotReplaceableByPickAction(Player,int)` | `false` | exact for vanilla items (that is NeoForge's default) |
| `ArmorSlot#mayPlace` | `ItemStack#canEquip(EquipmentSlot,Entity)` | `player.getEquipmentSlotForItem(stack) == slot` | exact — that *is* NeoForge's default impl |
| `MagnetHandler` | `ItemEntity#getPersistentData().contains("PreventRemoteMovement")` | `false` | ⚠ **behaviour gap**: Fabric has no per-entity persistent NBT, so nothing can opt out of the magnet card there. Interop nicety, not a feature of this mod. |

### 8.3 ⚠⚠ The W2 headline: Fabric Loader does **not** order entrypoints by mod dependency

The first `runServer` crashed in `AE2wtlibItems.<clinit>` with
`NullPointerException: … appeng.core.AEConfig.instance() is null`. The `debug.log` entrypoint-registration dump is
conclusive: `de.mari_023.ae2wtlib.fabric.AE2wtlibFabric` was registered (and therefore invoked) **before**
`appeng.fabric.AppEngFabric`, despite `"depends": {"ae2": "*"}` in our `fabric.mod.json`.

Three independent reasons AE2WTLib must run strictly after AE2:

1. `ItemWT`'s constructor reads `AEConfig.instance().getWirelessTerminalBattery()` — null until AE2's
   `FabricConfigStore.initConfigs()` ran. *(This is the crash.)*
2. `AEItems.WIRELESS_CRAFTING_TERMINAL` must be bound and `AEItemsMixin`'s swap must have happened before this mod
   builds its terminals.
3. **Part 10 / raw ids.** AE2 registers all of its content from `AppEngFabric.init(base)`, which it calls from its
   `main` entrypoint on a dedicated server but from its ***client*** entrypoint on a client (`AppEngFabric.onInitialize`
   is a no-op there — the dist-specific `AppEngClient` has to exist first). Fabric runs every `main` entrypoint before
   any `client` entrypoint, so even a *correct* entrypoint order would place this mod's items **before** AE2's on a
   client and **after** them on a dedicated server → different item raw ids on the two sides → silent MP `ItemStack`
   corruption that singleplayer can never reproduce.

**Fix:** `de.mari_023.ae2wtlib.fabric.mixin.AppEngFabricMixin` injects at the **TAIL of
`appeng.fabric.AppEngFabric#init(Lappeng/core/AppEngBase;)V`** and calls `AE2wtlibFabric.init()` (idempotent guard).
That pins all three at once — AE2 is fully registered, and the call rides whatever dist-appropriate entrypoint AE2
itself chose, so both sides see the identical order "AE2 content, then AE2WTLib content". The `main`/`client`
entrypoints remain declared; the client one now carries only order-independent wiring (client config, the C2S sender
injection, the three S2C receivers).

> **This generalises to every future AE2 addon on Fabric** — the dual-loader recipe should not put addon registration
> in a `ModInitializer` at all. The durable fix is an **`ae2:registration` addon entrypoint in the AE2 fork**
> (it already has a client-only, internal `ae2:client_registration`), invoked at the end of `AppEngFabric.init`;
> switching to it here is then a one-line change. Filed as a follow-up on the AE2 fork.
>
> ⚠ `AppEngFabric` exists **only in our fork** — re-verify this mixin target on every AE2 rebase.

**R6 is answered:** `AE2wtlibFabric.verifyWirelessCraftingTerminalSwap()` asserts at init that
`AEItems.WIRELESS_CRAFTING_TERMINAL.asItem() instanceof ItemWCT` and throws otherwise. The dedicated server boots
past it, so `AEItemsMixin` **does** take against our fork's `AEItemEntry`-based registration path.

### 8.4 R1 — the AE2 API divergences actually hit

| §3.2 row | Hit? | Resolution |
|---|---|---|
| `ItemDefinition` ctor (`DeferredItem` vs `AEItemEntry`) | ✅ yes, as predicted | seam #1. `AEItemEntry`'s ctor is package-private, so the Fabric side must go through the public `AERegistries.registerItem(id, factory)` and then create/register/bind itself. Appending to AE2's pending list is harmless *only because AE2 has already flushed* — which §8.3's mixin now guarantees structurally. |
| `AEItems`/`AEComponents` `DR` vs `init()` | ✖ not hit | AE2WTLib never touches AE2's own registers. |
| `InitScreens.register(event, …)` vs `(MenuScreenRegistrar, …)` | ✅ yes | `registerScreens` moved out of the shared `AE2wtlib` class into `NeoForgeScreens`; Fabric twin in W5. |
| `InternalInventory#toResourceHandler()` | ✅ yes, and **worse than predicted** — it is *abstract* upstream, so the shared class cannot simply drop it | `WrappedPlayerInventory` became abstract + a `factory` hook, with a concrete subclass per loader. |
| `AEConfig.register(ModContainer)` vs `(ConfigStore, ConfigStore)` | ✖ not hit directly | AE2WTLib has its own config; but it means AE2's `ConfigStore` is *not* available on the NeoForge side, so seam #2 had to be a private copy rather than a reuse. |
| `AEItemKey`, `ConfigMenuInventory`, `PoweredItemCapabilities`, `QuantumBridgeBlockEntity`… | ✖ not hit in W2 | `PoweredItemCapabilities` is W4. |

**Net: 3 of the 11 predicted divergences bit, all inside files that were already NeoForge-coupled — the §3.2
"costs nothing extra" prediction held.** No need for the R1 fallback (compiling both loaders against the fork).

### 8.5 Stub inventory

| | before W2 | after W2 |
|---|---|---|
| stub markers | ~25 `W1-STUB` in 2 files | **5 `W2-STUB`**, every one tagged with its wave |

Remaining, verbatim:
- `AE2wtlibFabric` — W3: the restock event surface (6 NeoForge events); W4: `RegisterCapabilitiesEvent` →
  `EnergyStorage.ITEM.registerForItems(...)` with `appeng.fabric.transfer.PoweredItemEnergyStorage`.
- `AE2wtlibFabricClient` — W5: screens via `InitScreens.MenuScreenRegistrar`; W3: `ClientTickEvent.Post` →
  `ClientTickEvents.END_CLIENT_TICK`; W3: `InputEvent.MouseScrollingEvent` → cancellable `MouseHandler#onScroll` mixin.
- Deferred by design (W7 polish): NeoForge's `IConfigScreenFactory` config screen — ModMenu is the usual Fabric host
  and a config screen is not a parity requirement.

`BuildCreativeModeTabContentsEvent` did **not** need a stub: because §8.3's hook already runs at the end of all
registration, `AE2wtlib.addToCreativeTab()` is simply called directly.

### 8.6 Mixin state after W2

`fabric.mod.json` now declares three mixin configs. **Only what is verified is active** (playbook golden rule #1):

| Config | Active | Waiting for W3 |
|---|---|---|
| `ae2wtlib.fabric.mixins.json` | `AEItemsMixin` (verified live, §8.3) | `ServerPlayerGameModeMixin`, `ServerPlayerMixin`, `ServerGamePacketListenerImplMixin` (compile, but their descriptors/`@Local` names are unverified — R4), `GuiMixin` (excluded from the source set: it references the still-excluded `AE2wtlibClient`) |
| `ae2wtlib.fabric.platform.mixins.json` **(new)** | `AppEngFabricMixin` | — |
| `ae2wtlib_api.fabric.mixins.json` | `WidgetContainerAccessor` | — |

### 8.7 Smaller decisions worth remembering

- **`AE2wtlibComponents.DR`: `HashMap` → `LinkedHashMap`.** Data component types are a *static* registry and
  `ItemStack`'s stream codec addresses them by **raw registry id**, so their registration order is part of the network
  contract. `HashMap` order is deterministic for a fixed key set, so this was not a live bug — but insertion order
  makes the wire contract equal to the *source* order, which is reviewable. Hardening, not a fix.
- **night-config 3.8.3** is `implementation` + `include` (JiJ) in `loader/fabric`. AE2's Fabric jar already nests the
  same coordinates and loader de-duplicates by version; shipping our own avoids depending on another mod's nested
  libraries.
- `processResources` needed `duplicatesStrategy = EXCLUDE` — all three resource roots ship an `icon.png`, and
  `loader/fabric`'s (the one `fabric.mod.json` points at) comes first.
- `AE2wtlibItems.init()` / `AE2wtlibAdditionalComponents.init()` are empty class-load triggers (AE2's `AEItems.init()`
  pattern). On NeoForge the class load used to be a side effect of `AE2wtlibItems.DR.register(modEventBus)`.
- The seam implementations must be injected **before** anything class-loads `AE2wtlibItems` — its static initializer
  builds every `ItemDefinition` through the factory. Both entrypoints therefore `init(...)` first, `AE2wtlibItems.init()`
  second.

### 8.8 W3 scope — confirmed, with amendments

The §4 plan for W3 stands. Amendments from W2:

1. **Un-exclude** `de/mari_023/ae2wtlib/AE2wtlibForge.java`, `AE2wtlibClient.java`? **No — the opposite.** Both stay
   excluded permanently; they *are* the NeoForge overlay now. W3 only has to re-home their remaining bodies
   (`AE2wtlibClient.clientTick()` / `mouseScroll(...)` need a loader-neutral home, since the Fabric client entrypoint
   cannot see `AE2wtlibClient`) and un-exclude `mixin/GuiMixin.java` once that home exists.
2. The 6 restock events, per §4 — plus **javap-verify every descriptor and `@Local` name first** (R4); the three
   restock mixins compile today but are deliberately not listed in the Fabric mixin config.
3. Move `ServerGamePacketListenerImplMixin` to the common list (R10) — already the case in the Fabric config, which
   has an empty `client` list.
4. W6, not W3: the recipe-viewer plugins are excluded as a package (`recipeviewer/**`), EMI included (R7).

---

## 9. W3 — event surface + client layer (landed 2026-07-29)

W3 absorbed the planned W5 (client), because the two are the same seam: the client entrypoint and the
`GuiMixin`/`MouseHandlerMixin` all need the same loader-neutral home for `AE2wtlibClient`'s bodies.

### 9.1 The event mapping table (R3 answered)

Nine NeoForge listeners lived on `AE2wtlibForge`/`AE2wtlibClient`. **Rule applied: semantic fidelity beats callback
convenience.** A fabric-api callback was chosen only where it reproduces *both* the priority and the cancellation
semantics; everywhere else a javap-verified mixin, injected at the instruction where NeoForge fires its own event.

| # | NeoForge event | Fabric path | Why |
|---|---|---|---|
| 1 | `PlayerInteractEvent.RightClickBlock` (LOWEST, skip if `isCanceled`) | **callback** — `UseBlockCallback` registered in a custom **late phase** (`ae2wtlib:restock_last`), ordered after `Event.DEFAULT_PHASE` | Exact match on both properties. Phases are a property of the singleton `Event`, so the ordering binds **all** mods = `EventPriority.LOWEST`; fabric-api's array-backed invoker stops at the first non-`PASS` return, so a cancelled interaction never reaches a late listener = `isCanceled()`. A mixin could reproduce **neither** (it sees no other mod's handlers). Bonus: fabric-api fires it from `@Inject(HEAD)` on `ServerPlayerGameMode#useItemOn` — the very method NeoForge fires `RightClickBlock` from (offset 41). |
| 2 | `PlayerInteractEvent.EntityInteractSpecific` (LOWEST, skip if `isCanceled`) | **callback** — `UseEntityCallback`, same late phase | Same reasoning. fabric-api fires it from `ServerGamePacketListenerImpl#handleInteract`, exactly where NeoForge calls `CommonHooks.onInteractEntityAt`. ⚠ **MC 26.1 merged NeoForge's two entity-interact events at the vanilla level**: `Entity#interactAt` no longer exists and `ServerboundInteractPacket` is a flat record with a single `interactOn` path — so there is one callback and no `hitResult`-based disambiguation is needed. |
| 3 | `LivingEntityUseItemEvent.Finish` | **mixin** — `LivingEntityMixin`, `@WrapOperation` on the `ItemStack#finishUsingItem` INVOKE inside `LivingEntity#completeUsingItem` | fabric-api has no such event, and the NeoForge one is *result-rewriting* (`event.setResultStack`). `@WrapOperation` is the only shape that gives both the pre-call copy and a settable result — which is literally NeoForge's patch (`EventHooks.onItemUseFinish(this, copy, ticks, useItem.finishUsingItem(...))`). |
| 4 | `ItemEntityPickupEvent.Pre` (LOWEST) — the magnet card | **mixin** — `ItemEntityMixin`, `@Inject(HEAD)` on `ItemEntity#playerTouch` | fabric-api has **no item-entity pickup event at all** (verified: zero classes matching `pickup`/`ItemEntity`; `PlayerPickItemEvents` is creative middle-click). HEAD is where NeoForge fires it (offset 26, before the pickup-delay/`target` checks and before `Inventory#add`); upstream's handler re-implements those two vanilla checks in its `canPickup().isDefault()` branch, which is the only reachable branch on Fabric. |
| 5 | `ArrowNockEvent` | **mixin** — `BowItemMixin`, `@Inject(HEAD)` on `BowItem#use` | No fabric-api event; NeoForge patches the item itself (offset 33). `event.hasAmmo()` == `!player.getProjectile(bow).isEmpty()`, recomputed rather than captured, so no `@Local`. `BowItem` is the **only** class that fires `onArrowNock` in NeoForge 26.1.2.87. |
| 6 | `ArrowLooseEvent` | **mixin** ×2 — `BowItemMixin` (`releaseUsing` HEAD, offset 66) + `CrossbowItemMixin` (`performShooting` HEAD, offset 36) | Same. ⚠ For the crossbow the fire site is `performShooting`, **not** `releaseUsing` (which only reports readiness), and NeoForge hard-codes `hasAmmo = true` there. |
| 7 | `BuildCreativeModeTabContentsEvent` | **lazy generator** — `AE2wtlibCreativeTab#buildDisplayItems` fills the list on first use | See §9.4. W2's "just call it inline" answer was wrong. |
| 8 | `ClientTickEvent.Post` | **callback** — `ClientTickEvents.END_CLIENT_TICK` | Exact counterpart; no ordering or cancellation involved. |
| 9 | `InputEvent.MouseScrollingEvent` (cancellable) | **mixin** — `client/MouseHandlerMixin`, cancellable `@Inject` at the `LocalPlayer#isSpectator()` INVOKE in `MouseHandler#onScroll` | fabric-api has **no raw scroll event**. The only two scroll APIs are `ClientHotbarScrollEvents` (hotbar *slot change* only) and per-`Screen` `ScreenMouseEvents` — neither covers "shift+scroll with no screen open". |

Server-side authority: every mutation is gated on `instanceof ServerPlayer`, matching upstream. Both fabric-api
callbacks also fire client-side (fabric-api mirrors them onto `MultiPlayerGameMode` / `Minecraft#startUseItem`), so
that guard is load-bearing for the pack's live MP server, not decoration.

### 9.2 R4 — mixin descriptor verification (javap, both jars)

Every target was checked against **both** `minecraft-merged-deobf-26.1.2.jar` (Fabric) and
`build/moddev/artifacts/minecraft-patched-26.1.2.87.jar` (NeoForge). Result: **1 of the 2 `@Local` captures is a
NeoForge-only fiction.**

| Mixin | Target | Verdict |
|---|---|---|
| `ServerPlayerMixin` (shared) | `ServerPlayer#drop(Z)V`, `@Local(name = "selected")` | ❌ **FAILS on Fabric.** LVT: vanilla = `this, all, inventory, removed`; NeoForge = `this, all, inventory, **selected**, removed`. `selected` is created by a NeoForge patch. **And `removed` is not a substitute**: `Inventory#removeFromSelected` → `removeItem` → `ContainerHelper.removeItem` → `ItemStack#split`, which always returns a *copy* — `removed` is the stack handed to the dropped `ItemEntity`, while `selected` is the live inventory stack. Restocking `removed` would top up an item already flying through the air. → shared mixin **excluded from the Fabric source set**; `ServerPlayerDropMixin` reads `inventory.getSelectedItem()` at TAIL, which *is* NeoForge's `selected` (same object; both sides see `isEmpty()` when the slot empties). `drop(Z)V` has a single exit, so TAIL is unambiguous. |
| `ServerGamePacketListenerImplMixin` (shared) | `ServerGamePacketListenerImpl#tryPickItem`, `@Local(name = "slotWithExistingItem")` | ✅ **present in vanilla** (slot 3, `I`, scope 32..101). The `send(Lnet/minecraft/network/protocol/Packet;)V` INVOKE is unique in the method (offset 87) and is inside that scope. Applies at runtime. |
| `ServerPlayerGameModeMixin` (shared) | `ServerPlayerGameMode#useItemOn`, `@At("RETURN")` | ✅ identical `ACC_PUBLIC` descriptor on both loaders. ⚠ Its handler is `private **static**` while the target is an instance method — legal: `CallbackInjector.sanityCheck` calls `checkTargetModifiers(target, **false**)` (verified by javap on `sponge-mixin-0.17.3`), so only *non-static handler on static target* is rejected. |
| `AEItemsMixin` (shared) | `AEItems#item(String,Identifier,Function)` | ✅ verified live in W2. |
| `GuiMixin` (shared, client) | `Gui#extractSlot(...)` @ `GuiGraphicsExtractor#itemDecorations` | ✅ descriptor unchanged; applies at runtime. **The W2 build-script comment claiming it references `AE2wtlibClient` was wrong** — it only uses `CraftingTerminalHandler` + `ReadableNumberConverter`. Un-excluded and activated. |
| `LivingEntityMixin` (Fabric) | `LivingEntity#completeUsingItem` @ `ItemStack#finishUsingItem` | ✅ exactly one call site (offset 69), same descriptor both loaders. |
| `ItemEntityMixin` (Fabric) | `ItemEntity#playerTouch` HEAD | ✅. `target` is `private UUID` — **`@Shadow`ed rather than access-widened**, which sidesteps R9 entirely (the AW line is kept only as a faithful translation of the AT). |
| `BowItemMixin` (Fabric) | `BowItem#use`, `#releaseUsing` HEAD | ✅ neither is overloaded. |
| `CrossbowItemMixin` (Fabric) | `CrossbowItem#performShooting` HEAD | ✅ not overloaded. |
| `client/MouseHandlerMixin` (Fabric) | `MouseHandler#onScroll(JDD)V` @ `LocalPlayer#isSpectator()` | ✅ `isSpectator()` is invoked **exactly once** in the method (offset 257) and is the instruction immediately after NeoForge's `ClientHooks.onMouseScroll` (offset 287). |

**All eleven were confirmed APPLIED at runtime**, not merely compiled, by running both `runServer` and `runClient`
with `JAVA_TOOL_OPTIONS=-Dmixin.debug.verbose=true` and grepping `Mixing … from ae2wtlib`. That is now the standard
way to close R4-class risks in this repo — descriptor checking proves the target exists, only a verbose run proves
the injection resolved.

### 9.3 R10 — the upstream `ServerGamePacketListenerImplMixin` bug stands

Upstream lists it under `"client"` although `ServerGamePacketListenerImpl` is a dedicated-server class, so pick-block
restock is almost certainly dead on NeoForge dedicated servers. The Fabric config lists it under `"mixins"`; the
runtime log confirms it applies on a dedicated server. **Still to report upstream.**

### 9.4 ⚠⚠ The W3 headline: `new ItemStack(...)` is illegal during mod init on 26.1

The dedicated-server gate failed with:

```
java.lang.NullPointerException: Components not bound yet
  at net.minecraft.core.Holder$Reference.components(Holder.java:278)
  at net.minecraft.world.item.ItemStack.<init>(ItemStack.java:266)
  at de.mari_023.ae2wtlib.AE2wtlibCreativeTab.addTerminal(AE2wtlibCreativeTab.java:49)
  at de.mari_023.ae2wtlib.AE2wtlib.addToCreativeTab(AE2wtlib.java:77)
  at de.mari_023.ae2wtlib.fabric.AE2wtlibFabric.init(AE2wtlibFabric.java:93)
```

MC 26.1 binds item data components **lazily**: the only callers of `Holder.Reference#bindComponents` are
`DataComponentInitializers`, which bakes them together with the reloadable server resources — long after mod init.
So an `ItemStack` may not be constructed during registration **at all**, on either loader. W2's §8.5 note ("because
the hook already runs at the end of ALL registration, `addToCreativeTab()` can simply be called directly") was
wrong; NeoForge gets away with the same code only because `BuildCreativeModeTabContentsEvent` fires much later.

**Fix:** `AE2wtlibCreativeTab#buildDisplayItems` now calls `AE2wtlib.addToCreativeTab()` itself before
`output.acceptAll(items)`. The generator runs with an `ItemDisplayParameters`, i.e. strictly after the component
bake — the same moment the NeoForge event fires. `registrationHappened()` makes it a no-op on NeoForge, so that
build is unchanged. The tab's `.icon(...)` was already a `Supplier` and needed nothing.

> **Generalises to every Fabric port on 26.1:** any eager `new ItemStack(...)` / `ItemStack.EMPTY`-adjacent work in a
> registration path is a boot crash waiting to happen. Build display lists lazily.

**Process lesson (recorded because it cost the wave a re-verification):** the W2 notes claimed a green
`runServer`. Re-running the gate *from commit `e6947731` in a throwaway worktree* reproduced the crash — the claim
had been written from an earlier state of the file. **Re-run a gate from the commit you are claiming it for.**

### 9.5 The client layer

- **`AE2wtlibClientEvents` (new, shared tree)** — the loader-neutral home for `clientTick()` and `mouseScroll()`.
  `mouseScroll` now takes a `double scrollDeltaY` and *returns* whether to cancel, so both a NeoForge cancellable
  event and a Fabric `CallbackInfo` can drive it. `AE2wtlibClient` keeps both method names as thin NeoForge
  adapters, so `AE2wtlibForge`'s `@SubscribeEvent`s are untouched. ⚠ Like `GuiMixin`, this is a **client-only class
  in the shared source set** — reachable only from client entry points and client mixins; `runServer` is the guard.
- **Screens** — `FabricScreens` feeds `MenuScreens::register` to our AE2 fork's `InitScreens.MenuScreenRegistrar`
  (the loader-neutral seam of §3.2), the same call AE2's own `AppEngFabricClient` makes. The five registrations and
  their style paths are a verbatim copy of `NeoForgeScreens`. `MenuScreens#register` is `private static` in vanilla;
  fabric-api widens it transitively, but the entry is declared in our own accesswidener too (same reasoning as
  `Player#closeContainer`).
- **Hotkeys — nothing to do.** AE2's `HotkeyActions.register` → `AppEng.instance().registerHotkey` →
  `Hotkeys.registerHotkey` creates the `KeyMapping`, and AE2's Fabric client entrypoint registers every accumulated
  mapping centrally via `KeyMappingHelper` (note: **`KeyBindingHelper` does not exist** in fabric-api 0.151 — the
  class is `net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper`). The only constraint is ordering: our
  `registerHotkeyActions()` must run before AE2's `Hotkeys.finalizeRegistration`, and it does — our init rides
  `AppEngFabricMixin` at the TAIL of `AppEngFabric#init`, which AE2 calls from `onInitializeClient` *before* its own
  `registerKeyMappings(...)`.
- **Item models — nothing to do.** All five items use plain JSON item models: no `ItemModel` codec, no
  `RangeSelectItemModelProperty`, no tint source, so there is no `*.ID_MAPPER::put` registration to mirror.
- **⚠ New ordering trap — `FabricClientBootstrap` (the client-side half of §8.3).** The first `runClient` died with
  `IllegalArgumentException: Cannot register handler as no payload type has been registered with name
  "ae2wtlib:update_wut" for CLIENTBOUND PLAY`: our `ClientModInitializer` ran **before** AE2's, and our common init
  (which registers the payload types) is driven from AE2's client entrypoint. Fabric Loader orders neither.
  `FabricClientBootstrap` is a rendezvous — the client entrypoint hands its registration-dependent work over as a
  `Runnable`, `AE2wtlibFabric.init()` signals completion, and whichever finishes last runs the block. It lives in
  the **main** source set and names no client type, so the server path stays clean.
  > This is the same class of bug as §8.3 and reinforces the same conclusion: **an AE2 addon on Fabric must not put
  > order-sensitive work in an entrypoint at all.** The durable fix remains an `ae2:registration` /
  > `ae2:client_registration` addon entrypoint in the AE2 fork.

### 9.6 W3 gates (all green, 2026-07-29)

| Gate | Result |
|---|---|
| `./gradlew :loader:fabric:build` | **GREEN** |
| `./gradlew build spotlessCheck -PruntimeItemlistMod=none` (NeoForge harness) | **GREEN**, behaviour unchanged |
| `./gradlew :loader:fabric:runServer` | **GREEN** — `Done (0.290s)!`, zero ERROR (only fabric's untranslated-item-tag dev WARN) |
| `./gradlew :loader:fabric:runClient` | **GREEN** — boots to the title screen (panorama rendering), zero ERROR |
| mixin application (`-Dmixin.debug.verbose=true`, both runs) | **11/11 applied**, incl. both `@Local` captures, `GuiMixin` and `client.MouseHandlerMixin` |

Not covered by a headless gate and deferred to W7: actually *exercising* restock/magnet in-world, and a real-network
multiplayer join (R5).

### 9.7 Stub inventory after W3

| | after W2 | after W3 |
|---|---|---|
| stub markers | 5 `W2-STUB` | **1** |

The single remaining marker is `W2-STUB (W4)` in `AE2wtlibFabric.init()` — `RegisterCapabilitiesEvent` →
`EnergyStorage.ITEM.registerForItems(...)`. Everything else is either done or a documented, wave-tagged deferral:

- **W4** — seam #7 energy capability (the stub) and seam #8 `WrappedPlayerInventory` via
  `appeng.fabric.transfer.FabricResources`. Crib confirmed: AE2's `appeng.fabric.transfer.PoweredItemEnergyStorage`
  (`PoweredItemEnergyStorage(ContainerItemContext, Item, IAEItemPowerStorage)`), registered exactly as
  `appeng.fabric.init.InitApiLookup#registerPowerStorageItem` does it. `team_reborn_energy` is already on the
  runtime classpath via AE2.
- **W6** — `recipeviewer/**` is excluded as a package (JEI `jei_mod_plugin` + REI `rei_common`/`rei_client`
  entrypoints; EMI dropped, R7); Curios→Trinkets stays out of scope (§5, it is an AE2-fork follow-up).
- **W7 polish** — `IConfigScreenFactory` has no in-tree Fabric equivalent (ModMenu is the usual host).

### 9.8 Smaller decisions worth remembering

- `ae2wtlib.fabric.mixins.json` was bumped `JAVA_21` → `JAVA_25`. The shared mixins compile to class version 69 on
  the Fabric side, which produced `Class version 69 required is higher than the class version supported by the
  current version of Mixin (JAVA_21 supports class version 65)`. The NeoForge copy of the config keeps upstream's
  `JAVA_21` (its classes are compiled for NeoForge's own target).
- The Fabric-only mixins live in `ae2wtlib.fabric.platform.mixins.json` (package
  `de.mari_023.ae2wtlib.fabric.mixin`), client ones under `client.` in its `client` list. The **shared** package
  keeps its own config so the two trees stay independently reviewable.
- `Ae2wtlibFabricEvents.LAST_PHASE` is `AE2wtlibAPI.id("restock_last")`. Note fabric-api's phase API is typed to
  `net.minecraft.resources.Identifier` on 26.1 (not `ResourceLocation`).
- Other fabric-api 26.1 renames found during recon and worth knowing for the next port:
  `ItemGroupEvents`/`FabricItemGroupEntries` → `net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents` /
  `FabricCreativeModeTabOutput`; `KeyBindingHelper` → `KeyMappingHelper`.
