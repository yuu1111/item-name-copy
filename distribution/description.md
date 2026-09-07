# ItemNameCopy

Hover over an item in an inventory or container and press **Ctrl+C** to copy its display name to your clipboard
A short action bar message confirms a successful copy

## Features

- Copies the displayed name, including translations, Japanese text and custom names
- Works with inventory slots in chests, crafting tables, furnaces, anvils and standard modded container screens
- Leaves the clipboard unchanged when hovering over an empty slot
- Gives text fields priority so you can still copy selected text from search and rename fields
- Copies once per key press, without repeating while the key is held
- Runs on the client without requiring installation on the server
- Requires no additional shared API mod, including Fabric API or Architectury API

## Installation

- Install the Jar matching your exact Minecraft version and loader
- Choose one of the Fabric, Forge or NeoForge builds and place it in the client's `mods` folder
- Use either Ctrl key and press C while hovering over a nonempty slot

## Scope

- Copies the name as plain text, preserving spaces and line breaks
- Does not copy item IDs, quantities, Lore, NBT or data components
- Does not target recipe viewer pseudo-slots or the item held while no container screen is open
- Does not provide a settings screen or configurable shortcut

Licensed under the [MIT license](https://github.com/yuu1111/item-name-copy/blob/main/LICENSE)
