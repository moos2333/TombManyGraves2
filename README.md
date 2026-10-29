# TombManyGraves Continued

**A maintained fork of [TombManyGraves2](https://github.com/M4thG33k/TombManyGraves2) by M4thG33k for Minecraft 1.12.2 / Forge 14.23.5.2847.**

TombManyGraves Continued preserves the original grave-mod experience — a
configurable grave block placed at the point of death, a recoverable inventory,
a death-list item with a particle trail, and a friend-based access system —
while hardening the parts that matter most on long-running servers:

- **Stability** — friend-list corruption no longer wipes data, command crashes
  are fixed, and dedicated-server tooltip NPEs are gone.
- **Performance** — grave NBT is Deflater-compressed (70–85% smaller block NBT),
  with a GBK/UTF-8 dual-channel reader for legacy saves.
- **Compatibility** — Baubles-LTS, Betweenlands, Aether, Galacticraft, Techguns,
  Cosmetic Armor Reworked, and Backpacks, all loaded reflectively as soft
  dependencies.
- **Soulbound protection** — Tinkers' Construct Soulbound, Thermal Soulbound,
  and Random Things anchors are never absorbed into the grave.

Licensed under **GPLv3**. This fork is a modified version of TombManyGraves2;
see [`LICENSE`](LICENSE) and [`CHANGELOG.md`](CHANGELOG.md) for details.