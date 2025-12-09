```markdown
# Hide Personal Boat Sail (RuneLite plugin)

Hides the player's personal boat sail visually while salvaging.

Target RuneLite version: 1.12.7

Features
- Automatic detection: enters "salvaging mode" when you click a menu option containing "Salvage".
- Keeps sail hidden while the player animation is running and for a short configurable timeout after the last animation tick.
- Visual hide using an overlay (safer than removing objects from the scene).
- Configurable sail object ID and timeout.

Files to add
- src/main/java/com/example/hideboatsails/HideBoatSailsConfig.java
- src/main/java/com/example/hideboatsails/HideSailOverlay.java
- src/main/java/com/example/hideboatsails/HideBoatSailsPlugin.java

Build & test
1. Add the Java files above to your plugin project structure.
2. Ensure your project's pom.xml targets RuneLite 1.12.7 dependencies.
3. Build:
   mvn -DskipTests package
4. Run RuneLite in dev mode and enable the plugin in the plugins list.
5. Test:
   - Approach your boat sail.
   - Click the menu action that says "Salvage" on the sail.
   - While salvaging, the overlay will cover the sail tile so you no longer see the sail.

Configuration
- Sail Object ID: the object id to hide (default 60473) — set this to match your client's sail object if different.
- Salvaging timeout (s): how many seconds to keep hiding after salvage click if no animation is detected.
- Enable debug logging for more diagnostic output.

Notes
- If you have exact animation IDs for the salvaging action, those can be used to replace the generic menu-click detection for even more precise timing; provide them and I can update the plugin.