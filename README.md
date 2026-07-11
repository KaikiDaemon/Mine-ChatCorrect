# Mine-ChatCorrect

Mine-ChatCorrect is a client-side NeoForge mod for Minecraft 1.21.1 that provides spell-check assistance while typing in chat.

It highlights likely misspelled words in the chat input, offers replacement suggestions, allows manual replacement text, supports adding accepted custom words, and can import external dictionary files for broader language support. The mod is intended to help players write clearer chat messages without requiring any server-side installation.

## Current features

- Client-side only; servers do not need to install the mod.
- Highlights likely misspelled words in the Minecraft chat input.
- Provides chat-window controls for:
  - selecting highlighted words
  - cycling replacement suggestions
  - typing a manual replacement
  - applying a correction
  - adding a word to the accepted-word list
  - optional auto-correction
- Includes an in-game settings GUI.
- Supports custom additional words.
- Supports importing and managing external dictionaries.
- Allows imported dictionaries to be enabled, disabled, or removed.
- Includes a separate GUI for browsing, editing, and removing additional words.

## Intended use

Mine-ChatCorrect is meant for players who want lightweight spell-check support directly inside Minecraft chat. It is especially useful for multiplayer chat, server messages, and players who frequently mistype while playing.

The mod operates locally on the client before a message is sent. It does not change server behavior and does not require other players or the server to install it.

## Dictionary imports

Mine-ChatCorrect can import external dictionary files or dictionary archives. Third-party dictionaries are not part of this project's license.

Users are responsible for ensuring they have permission to use any dictionary files they import.

Supported import sources:

- Local plain word-list files such as `.txt`, `.words`, and `.dic`.
- Local dictionary directories.
- `.zip` archives.
- `.tar.gz` and `.tgz` archives.
- `.gz` and `.dz` compressed dictionary files.
- `.tar.bz2`, `.tbz2`, and `.tbz` archives when a local `tar` command is available.
- StarDict-style dictionaries using `.ifo`, `.idx`, and `.idx.gz` files.

Dictionary import behavior:

- Word-list files are read as UTF-8 when possible, with ISO-8859-1 fallback.
- Blank lines, comments, numeric affix counts, one-letter entries, hyphenated entries, underscore entries, and words with digits/symbols are ignored.
- Imported dictionaries are stored in the client config directory under `mine_chatcorrect/dictionaries/imports/`.
- Imported dictionary enabled/disabled state is stored beside the imported dictionary in a `.mine-chatcorrect.properties` metadata file.
- Failed imports attempt to clean up their temporary import directory.
- Dictionary source files and extracted archive entries are size-limited to reduce the risk of large files or zip-bomb style archives.

Current safety notes and limitations:

- Import dictionaries only from trusted sources.
- Plain `http://` downloads are supported but are not encrypted; prefer `https://` sources when possible.
- `.tar.bz2` imports are extracted through the system `tar` command after path pre-validation and post-extraction size validation.
- Network download behavior and full archive integration behavior still need broader automated or manual validation.

## Default dictionary URL

The in-game import URL field defaults to the English Speller Database / SCOWL Aspell English release:

`https://github.com/en-wl/wordlist/releases/download/rel-2026.02.25/aspell6-en-2026.02.25-0.tar.bz2`

This was selected because it is a current, broad, spellchecker-focused English word source with an open-license source family and a small tested archive.

## Suggested dictionaries

These links are suggested for user import/testing. Check upstream licenses before redistributing third-party dictionaries.

| Dictionary | URL | Why pick it? |
|---|---|---|
| en-wl / SCOWL Aspell English | `https://github.com/en-wl/wordlist/releases/download/rel-2026.02.25/aspell6-en-2026.02.25-0.tar.bz2` | Best default: broad, current, spellchecker-focused, small, tested. |
| en-wl Hunspell English ZIP | `https://github.com/en-wl/wordlist/releases/download/rel-2026.02.25/hunspell-en_AU-2026.02.25.zip` | ZIP path; modern SCOWL/Hunspell dictionary. |
| LibreOffice English `en_US.dic` | `https://raw.githubusercontent.com/LibreOffice/dictionaries/master/en/en_US.dic` | Simple direct Hunspell `.dic`; good compatibility test. |
| dwyl `words_alpha.txt` | `https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt` | Very broad plain-text list; useful for stress testing. |
| dwyl `words.zip` | `https://raw.githubusercontent.com/dwyl/english-words/master/words.zip` | Broad ZIP archive; useful for archive testing. |
| FreeDict English-German dictd | `https://download.freedict.org/dictionaries/eng-deu/0.3.6/freedict-eng-deu-0.3.6.dictd.tar.bz2` | DICT/FreeDict archive; tests `.index` parsing. |

## Custom accepted words

Accepted/custom words are stored in the client config directory as `mine_chatcorrect/additional_words.txt`.

Behavior:

- Words are normalized to lowercase.
- Blank entries are ignored.
- Duplicate entries are collapsed.
- Added, removed, and replaced accepted words are persisted to the config file.
- Accepted words suppress spell-check false positives after the spell checker reloads its dictionaries.

## License

Mine-ChatCorrect is licensed under the MIT License. See [`LICENSE`](LICENSE).

The MIT License applies to Mine-ChatCorrect’s own source code, project configuration, and original resources. It does not grant rights to Minecraft, NeoForge, third-party dictionaries, external dictionary archives, or any other third-party content.

Mine-ChatCorrect is an unofficial Minecraft mod and is not affiliated with Mojang, Microsoft, or NeoForge.

## Build output

Current public release: [0.1.2](https://github.com/KaikiDaemon/Mine-ChatCorrect/releases/tag/v0.1.2)

Current release/build: `0.1.2`

Release download: `https://github.com/KaikiDaemon/Mine-ChatCorrect/releases/download/v0.1.2/mine_chatcorrect-0.1.2.jar`

Local Gradle build output: `build/libs/<artifact-name>.jar`

Current local jar: `build/libs/mine_chatcorrect-0.1.2.jar`

For version `0.1.2`, the local jar is expected at `build/libs/mine_chatcorrect-0.1.2.jar`.

## Compatibility

- Minecraft: `1.21.1`
- NeoForge: tested with `21.1.172`
- Java: `21`

Mine-ChatCorrect is a client-side mod. Servers and other players do not need to install it.

## Installation

1. Install Minecraft `1.21.1`.
2. Install a compatible NeoForge `1.21.1` client.
3. Download the release jar from GitHub Releases, or build locally with `./gradlew build`.
4. Place the jar in your Minecraft `mods` folder.
5. Launch Minecraft with the NeoForge profile.

Typical mods folder locations:

- Linux: `~/.minecraft/mods`
- Windows: `%APPDATA%\.minecraft\mods`
- macOS: `~/Library/Application Support/minecraft/mods`

Local build artifacts are generated under:

```text
build/libs/<artifact-name>.jar
```
