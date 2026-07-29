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
| W2 | foundation seams (registration, config, network, components) | ▫ next |
| W3 | events + mixins (the NeoForge event surface) | ▫ |
| W4 | capabilities / transfer (energy + inventories) | ▫ |
| W5 | client: screens, GUI, hotkeys, scroll input | ▫ |
| W6 | integrations (JEI / REI / EMI, Curios→Trinkets) | ▫ |
| W7 | gates: runServer → gametest → runClient → Prism; polish | ▫ |

**W1 gates (both green, 2026-07-29):**
- `./gradlew assemble -PruntimeItemlistMod=none` (NeoForge, root + `:ae2wtlib_api`) — **GREEN**, unchanged behaviour.
- `./gradlew :loader:fabric:build` — **GREEN** (skeleton; shared sources still gated off).

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

The coupling groups into 9 seams:

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

**W1 workaround (in place):** the production jar `~/IdeaProjects/Applied-Energistics-2/dist/appliedenergistics2-fabric-26.1.10-beta.jar`
was hand-installed into `~/.m2/repository/org/appliedenergistics/appliedenergistics2-fabric/26.1.10-beta/`
together with a minimal POM, so `loader/fabric` consumes proper maven coordinates and loom sees a normal
module dependency (and applies AE2's transitive access widener).

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
| R1 | **`ItemDefinition` ctor + `AEItems.DR` divergence** between upstream AE2 (`:neoforge`) and our fork (`:fabric`) forces a per-loader item-registration factory | **high** (touches the mod's registration core) | seam #1 in W2; fallback = publish the fork's `:neoforge` jar to mavenLocal and compile both loaders against the fork (costs the upstream-rebase regression signal) |
| R2 | **AE2 Fabric jar is hand-installed in mavenLocal** — no reproducible publish, silently stale after an AE2 rebase | **high** (build reproducibility) | §3.1: add `maven-publish` to the AE2 fork's `:fabric`; until then re-copy after every AE2 rebuild and keep `ae2Version` in lock-step |
| R3 | **Restock is the feature, and it is 100% event-driven** — 6 NeoForge events with priority semantics (`EventPriority.LOWEST`, `event.isCanceled()`) that Fabric callbacks do not reproduce | **high** | W3: prefer TAIL/RETURN mixins over fabric-api callbacks where ordering matters; verify in-world, not just headless |
| R4 | `@Local(name=…)` in two mixins (`selected`, `slotWithExistingItem`) | medium | javap/`--debug` verify against the merged jar before W3; unobfuscated 26.1 keeps parameter names but locals can still drift |
| R5 | **Multiplayer packet desync** — 6 payloads, one enum codec, item stacks | medium | playbook Part 10; format-pinning tests + a real-network join in W7. Singleplayer and gametests will NOT catch it |
| R6 | `AEItemsMixin` replaces an **AE2-owned item class** — if AE2's Fabric registration path constructs items differently, the swap may not take | medium | signature verified identical by javap; but AE2's fork changed *how* items are registered (`AEItemEntry`) — test that `AEItems.WIRELESS_CRAFTING_TERMINAL instanceof ItemWCT` early in W2 |
| R7 | EMI has no 26.1 Fabric artifact | low | drop the EMI plugin from the Fabric jar (W6) |
| R8 | REI entrypoint-timing crash (empty-ctor rule) | low | known + documented; AE2 fork hit and fixed it |
| R9 | AW field-widening flakiness (`ItemEntity.target`) | low | `@Accessor` mixin fallback already noted in the AW file |
| R10 | Upstream `ServerGamePacketListenerImplMixin` sits in the `client` mixin list → pick-block restock probably dead on dedicated servers | low (a *fix*, not a regression) | move it to the common list on Fabric; report upstream |
| R11 | No automated tests anywhere in this repo | medium | W7: crib AE2's Fabric gametest runner; at minimum place/spawn-and-tick every registered item + a recipe-presence test (playbook rules #6, #8) |

---

## 7. Command crib

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25 2>/dev/null \
  || echo ~/.gradle/jdks/eclipse_adoptium-25-aarch64-os_x.2/jdk-25.0.3+9/Contents/Home)

# NeoForge regression harness (must stay green at EVERY commit)
./gradlew assemble -PruntimeItemlistMod=none
./gradlew spotlessCheck -PruntimeItemlistMod=none

# Fabric
./gradlew :loader:fabric:build
./gradlew :loader:fabric:build -Pae2wtlib.fabric.api=true -Pae2wtlib.fabric.shared=true   # from W2 on
./gradlew :loader:fabric:runServer      # W4 gate
./gradlew :loader:fabric:runClient      # W5 gate

# NeoForge-only CI job (loom resolves at configuration time)
./gradlew assemble -Pae2wtlib.skipFabric=true

# The W1 todo list
grep -rn "W1-STUB" loader/fabric
```

Binary truth:
`javap -classpath ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1.2/minecraft-merged-deobf-26.1.2.jar <fqcn>`
AE2 fork jar: `~/IdeaProjects/Applied-Energistics-2/dist/appliedenergistics2-fabric-26.1.10-beta.jar`
Upstream AE2 jar: `~/.gradle/caches/modules-2/files-2.1/org.appliedenergistics/appliedenergistics2/26.1.10-beta/*/appliedenergistics2-26.1.10-beta.jar`
