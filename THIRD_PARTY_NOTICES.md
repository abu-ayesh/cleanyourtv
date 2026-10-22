# MIHRAB TV — source attribution

The root `LICENSE` retains CleanYourTV's MIT license and copyright © 2026 Gathin.
The inherited `launcher/`, tools, tests and documentation remain as upstream reference material;
the new Android application is `app/`, built from the root Gradle project.

`app/.../launcher/AppRepository.java` adapts the PackageManager discovery and explicit
LEANBACK_LAUNCHER launch flow from CleanYourTV's `launcher/.../AppAdapter.java`, including
excluding the launcher's own package, sanitizing labels and preserving task flags.
MIHRAB's XML interface and its focus navigator are independent implementations of the approved design.
Upstream: https://github.com/Gathin23/cleanyourtv

Wallpaper artwork and Thmanyah Serif Text/Display fonts were supplied in `../mihrab-html`.
Original JPEG files are copied byte for byte. Fonts were converted from WOFF2 to TTF without
editing their glyphs, shaping tables or naming metadata. No font license file was supplied
with those assets; this local implementation does not assert redistribution rights.
CleanYourTV's separate `launcher/FONT-LICENSE-OFL.txt` is not a license for the Thmanyah fonts.

`content.json` preserves the supplied Arabic content object without text edits:
HadeethEnc.com (hadith text, explanations, grading and references), AlQuran Cloud
`quran-uthmani` (Quran text), as recorded in the design source on 2026-10-22.
Source attribution and retrieval date are available in the native detail reader.
No network retrieval is performed by this Phase 1 APK.

Development-only Python packages (fontTools, Brotli and Pillow), the Java formatter and JUnit
are not packaged into the APK.
