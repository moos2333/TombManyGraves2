# Changelog

All notable changes to **TombManyGraves Continued** (a fork of TombManyGraves2 by M4thG33k) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Note on versioning:** The FML-facing version reported by `Names.VERSION` remains
> `"999"` to match upstream TombManyGraves2 behavior. The release identifiers below
> are human-readable and independent of that placeholder.

---

## [Unreleased]

### Added

- **Grave NBT compression (Issue #52).** `InventoryHolder.writeToNBT` now serializes
  the underlying compound via `CompressedStreamTools` and compresses the result
  with `Deflater` when the raw payload exceeds 512 bytes. Compressed payloads are
  stored under `InventoryHolder_z` as a `byte[]`.
    - Block NBT shrinks by approximately **70–85%** for typical inventories and by
      up to **80%** for Tinkers' Construct tool stacks.
    - Legacy saves (which store an uncompressed `InventoryHolder` tag) are read
      transparently and migrated on the next write.
    - `InventoryHolder.toUncompressedForm` expands compressed payloads on read,
      keeping `ItemDeathList.getEndVector` (particle path) and `GuiDeathItems`
      fully compatible with **no changes to either class**.
    - Payloads below the 512-byte threshold, or payloads that do not shrink when
      compressed, are written in the original uncompressed format.

- **Dual-channel file encoding for `DeathInventoryHandler`.**
    - Writes are explicit UTF-8.
    - Reads attempt strict UTF-8 decoding first, then fall back to GBK (with
      `GB18030` and platform-default fallbacks) for files written under a
      non-UTF-8 JDK 8 locale.
    - `readLine()` replaced with full-file reads to handle multi-line JSON
      produced by external tools.

- **ASCII fast path in `DeathInventoryHandler.decode`.** Pure-ASCII files
  (the common case: player names, NBT keys, and item IDs are ASCII) bypass
  `CharsetDecoder` allocation entirely.

- **Cross-dimension lookup in `PacketProbeFiles.handleServerSafe`.** Uses
  `MinecraftServer.getPlayerList().getPlayerByUUID` instead of
  `WorldServer.getPlayerEntityByUUID`, so a player who changes dimension
  between request and response still receives the file list.

### Changed

- **Fork identity.**
    - `Names.MODNAME` is now `"TombManyGraves Continued"`.
    - `mcmod.info` `name` synchronized to match; `description` now includes a
      GPLv3 modification notice; `authorList` corrected from a single
      comma-joined string to `["M4thG33k", "Tiffit"]`; `url` points to the
      fork's repository.
    - `MODID` (`tombmanygraves`) and the package name
      (`com.m4thg33k.tombmanygraves`) are **unchanged** for save and
      configuration compatibility.

- **`BaublesInventory`** now uses `IBaublesItemHandler` instead of casting to
  `BaublesContainer`, restoring compatibility with **Baubles-LTS**. `getItems`
  copies stacks instead of aliasing the Baubles container.

- **`BetweenlandsInventory.insertInventory`** now honors the `shouldForce`
  parameter as specified by the `IGraveInventory` contract: when
  `shouldForce = true` and the target slot is occupied, the current item is
  dropped and the grave item is inserted, followed by `IEquippable.onEquip`.

- **`CommandFriends`** uses `equalsIgnoreCase` instead of
  `toLowerCase().equals`, removing locale sensitivity (e.g. Turkish `I`/`ı`).

- **`CommonProxy.preinit`** invokes `ModConfigs.preInit(e)` before
  `FriendHandler.importFriendsList()` so the friend system can read
  configuration values during initialization.

### Fixed

#### Data loss / corruption

- **`FriendHandler`** — When `friends.fdat` failed to load, the original file
  was immediately overwritten with an empty list, permanently losing friend
  data on transient I/O errors. Corrupted files are now moved to
  `friends.fdat.bak` before any new write.
- **`FriendHandler`** — Direct overwrite of `friends.fdat` could leave a
  truncated file on crash. Writes now go through `friends.fdat.tmp` and an
  atomic move (`ATOMIC_MOVE`, falling back to `REPLACE_EXISTING`).
- **`GraveInventoryManager.insertInventory`** — A single `IGraveInventory`
  throwing an exception previously caused all remaining items in that
  inventory to be lost when the grave block was removed. Remaining items are
  now dropped at the player's feet.
- **`TileGrave.onCollision` / `dropGraveContentsAt`** — Unconditional reset of
  `savedInventory` could erase recoverable items on exception paths. Reset
  removed.
- **`TileGrave.shouldRefresh`** — Always returned `true`, forcing a new
  `TileEntity` on every block-state update and losing in-memory grave data.
  Now compares the block only.
- **`TileGrave.getUpdateTag` / `readSyncFields`** — Restored
  `super.writeToNBT` and `NBTTagCompound.getUniqueId` to preserve
  compatibility with third-party tools (WAILA, map mods) that read the full
  tag.
- **`CommonEvents.onPlayerDeath`** — With `keepInventory` enabled, items were
  extracted via `grabItemsFromPlayer` but `PlayerDropsEvent` never fired,
  stranding items in the internal `invs` map. Added short-circuit.

#### Functional bugs

- **`ModConfigs`** — `replaceStillWater` read the `replaceStillLava` config
  key, making the option a silent no-op and coupling water replacement with
  lava replacement. Fixed to read `replaceStillWater`.
- **`CommandFriends`** — The static `parameters` list was mutated in the
  constructor, accumulating `[add, remove, list, add, remove, list, ...]`
  across `FMLServerStartingEvent` invocations (every singleplayer world
  reload). Now a single field initializer.
- **`CommandGainOwnership` / `CommandPopGrave`** — Missing `return` after an
  invalid `args.length` check caused a duplicate "Invalid usage" message and
  a swallowed `ArrayIndexOutOfBoundsException`. Added `return`.
- **`CommandGetDeathList`** — `ClassCastException` when executed from a
  command block or the server console. Added an explicit
  `instanceof EntityPlayer` guard with a user-facing message.

#### Stability

- **`ItemDeathList.addInformation`** — Referenced
  `org.lwjgl.input.Keyboard` without a client-side guard; on dedicated
  servers, tooltip collectors (WAILA/HWYLA-style) could trigger
  `NoClassDefFoundError`. Added `world == null || !world.isRemote` guard.
- **`PacketProbeFiles`** — `DimensionManager.getWorld(dimension)` and
  `WorldServer.getPlayerEntityByUUID` could return `null`, leading to an NPE
  inside Netty. Added four-layer null guards (`uuid`, `pos`, `server`,
  `player`) and cross-dimension lookup.
- **`BaseThreadsafePacket` / `BasePacketHandler`** — Added null guards on
  `netHandler`, `IThreadListener`, and `MessageContext`.
- **`GraveInventoryManager`** — `annotationMap` was keyed by listener
  instance; re-keyed by registry id. Null-registry entries no longer throw in
  `getGuiColorForInventory`.
- **`DeathInventoryHandler`** — Silent `catch` blocks on file errors; errors
  are now logged via `LogHelper`.
- **`GraveInventoryManager.grabItemsFromPlayer` / `getItems` /
  `insertInventory` / `generateDrops`** — Per-listener `try/catch` isolates a
  single faulty plugin. **`pregrabLogic` call order is unchanged** — the
  soulbound-protection chain is preserved.

### Performance

- **`GraveInventoryManager`** — Replaced Stream-based sorting with
  `List.sort`; presized `HashMap`s; `finalizeListeners` no longer creates
  intermediate collectors.
- **`BlockGrave`** — `AxisAlignedBB` hoisted to a static constant;
  non-player entities are filtered before the `TileEntity` lookup in
  `addCollisionBoxToList`; sneak check moved before `getTileEntity` in
  `onEntityCollision`.
- **`TileGrave`** — `setSavedInventory` and `setCamoState` now `markDirty()`
  to guarantee persistence at the point of mutation.
- **`InventoryHolder`** — Static `DateTimeFormatter`.
- **`TempInventory`** — `getListOfNonEmptyItemStacks` presized.
- **`GraveInventoryHelper`** — `storeInventory` skips empty slots.
- **`DeathInventoryHandler`** — `pathReady` cache avoids repeated
  `File.exists()` probes; `ALLOW_INVENTORY_SAVES` checked before any I/O;
  ASCII fast path avoids decoder allocation.
- **`FriendHandler`** — `pathReady` cache; `writeFriends` only invoked when
  the underlying set actually changed; `synchronized` to serialize
  concurrent writes; `hasAsFriend` / `isFriendOf` merged.
- **`ClientProxy`** — `Vector3f.length()` cached in `particleStream`.
- **`ItemDeathList`** — `Vector3f.length()` cached in `onUpdate`; removed
  redundant `normalise` + intermediate `Vector3f`; `BlockPos` cached in
  `onItemRightClick`.

### Removed

- **`ClientProxy`** — A commented-out `ItemRenderRegister.registerItemRenderers()`
  line.

### Documentation

- **`README.md`** — Rewritten with fork identity, fork-specific change
  summary, compatibility table, and GPLv3 compliance section.
- **`mcmod.info`** — `description` now includes the GPLv3 modification
  notice; `credits` acknowledges the upstream author and the fork maintainer.
- **`CHANGELOG.md`** — This file.

---

## [Not changed]

The following files and behaviors were reviewed and **intentionally left
untouched** in this release:

- **`IGraveInventory`** and **`GraveRegistry`** — public API surface unchanged.
- **`InventoryHolder.ensure`** — matching semantics
  (`ItemStack.areItemStacksEqual` + `Iterator.remove`) preserved; this is the
  primary safeguard against soulbound items entering the grave.
- **`GraveInventoryManager.grabItemsFromPlayer`** — `pregrabLogic` call order
  preserved.
- **`TombManyGraves.init`** — reflective `Class.forName` +
  `Loader.isModLoaded(reqMod)` registration preserved.
- **`VanillaInventory`, `AetherLegacyInventory`, `GalacticraftInventory`,
  `CosmeticArmorReworkedInventory`** — reviewed; these mods are
  feature-frozen on 1.12.2 and their APIs do not exhibit the defensive-
  fallback scenarios the other implementations were hardened for.
- **`ClientEvents`, `RenderEvents`, TESR, GUI rendering paths** — reviewed
  but not modified; renderer changes require in-game visual verification and
  are out of scope for this release.
- **`TMGNetwork`, `BasePacket`, `GraveRenderTogglePacket`,
  `GravePosTogglePacket`** — reviewed; no changes required.

---

## [Known limitations]

### Disk JSON compression may be neutral or larger

`InventoryHolder.writeToNBT(...).toString()` encodes compressed bytes as
`[B;12,-34,...]` text, where each byte costs roughly 3.5 characters. For a
typical inventory, the compressed JSON file may be **approximately equal in
size** to the uncompressed version, and for a small number of very large NBT
payloads it may be **slightly larger**. Functionality is unaffected. Block NBT
(the primary target of Issue #52) is unambiguously smaller.

### Soulbound protection depends on mod event priority

Soulbound items are protected by `InventoryHolder.ensure`, which runs at
`EventPriority.LOWEST` on `PlayerDropsEvent`. Mods that remove soulbound items
from `drops` **before** `LOWEST` (Tinkers' Construct, Thermal, Random Things)
are handled correctly. Mods that remove them at or after `LOWEST` would not be.
This is an upstream design constraint and has not been changed by this fork.

### Failure reason is not distinguished in `getDeathList`

A missing file, an invalid timestamp, and a corrupted compressed payload all
produce the same user-facing message ("Failed to retrieve list. Check spelling
and timestamp."). Corrupted payloads additionally log
`Failed to decompress saved inventory` at error level. Distinguishing failure
causes would require changing `readNBT`'s return contract and is out of scope
for this release.

### Death-list item NBT is not compressed

`toUncompressedForm` returns the uncompressed format so that
`ItemDeathList.getEndVector` and `GuiDeathItems` remain unmodified. The
death-list item therefore carries the full uncompressed grave NBT. Compressing
it would require a separate design (store coordinates + timestamp only, read
the file on demand) and is out of scope for this release.

---

## [Migration notes]

1. **`replaceStillWater`** — This config key was previously a no-op (it read
   `replaceStillLava`). Existing configurations that relied on
   `replaceStillLava = false` to disable *water* replacement must now set
   `replaceStillWater = false` explicitly.
2. **`friends.fdat`** — If the file is corrupted, it is renamed to
   `friends.fdat.bak` and the friend list starts empty. The original data is
   recoverable from the `.bak` file.
3. **`tmg_getDeathList`** — This command must now be executed by a player.
   Execution from a command block or the server console returns a user-facing
   message instead of throwing a `ClassCastException`.
4. **`-Dfile.encoding=UTF-8`** — Recommended on dedicated servers running
   JDK 8 with a non-UTF-8 locale. The reader falls back to GBK for legacy
   files, but the writer emits UTF-8.
5. **`Baubles`** — The integration now requires **Baubles-LTS** (or any
   version exposing `IBaublesItemHandler`). The original Baubles 1.5.2 release
   crashes on modern Forge and is not supported.

---

## [License]

This project is licensed under the **GNU General Public License v3.0**.

- **Upstream work** — *TombManyGraves2* © M4thG33k.
- **This fork** — modifications © the fork's contributors.

This repository is a modified version of TombManyGraves2 by M4thG33k. Original
copyright notices are preserved. The nature and scope of the modifications are
documented in this file and in the repository's commit history. Full license
text: [`LICENSE`](LICENSE), or <https://www.gnu.org/licenses/gpl-3.0.txt>.

This fork is not affiliated with or endorsed by M4thG33k.