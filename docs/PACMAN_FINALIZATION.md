# Pac-Man finalization

Pac-Man opens through its existing arcade route on a dedicated five-mode homepage. The original maze and breadth-first search foundation remain; continuous/interpolated movement, ghost state rules and progression are now owned by the JavaFX-independent model.

## Modes and balancing

| Mode | Rules |
|---|---|
| Classic | Three lives; clear the maze to advance, preserving score and increasing speed. |
| Time Attack | Score for 60 seconds or until all lives are lost. Life/level transitions count toward time; opening Ready does not. |
| Speed-Up | Three lives; every 20 pellets adds 0.08× speed, with level progression and a 1.55× cap. |
| Hard Mode | Two lives, 1.22× ghost speed, four-second initial power duration and shorter scatter phases. |
| Ghost Challenge | Capture six ghosts within 90 seconds and three lives. Each consumed power pellet recharges after 12 active seconds. |

Player speed starts at 5.2 tiles/second; normal ghost speed starts at 3.35. Speeds are capped at 8.1/6.6. Frightened ghosts move at 64% of normal speed, returning eyes move at 9, and ghosts wait 1.2 seconds at spawn before rejoining. Starting releases are staggered. These are centralized in `PacmanMode` and mechanics state rules, ready for later human balancing.

## Gameplay and architecture

- Single-tap continuous movement, buffered turns at intersections, direction-aware mouth animation and separate logical/interpolated positions. Unspent movement carries across tile boundaries. Collision checks run at steps no larger than 1/240 second and use visual positions, catching crossing paths.
- The original 17×13 maze now includes a row-seven horizontal tunnel, four power pellets and four valid ghost spawns. Startup validates reachability of all walkable tiles.
- Blinky targets the player; Pinky targets four tiles ahead; Inky reflects a point ahead of the player about Blinky; Clyde pursues from afar and retreats to a corner when near. Targets outside the maze or in walls are projected onto reachable walkable tiles before BFS. Ghosts avoid reversing in open corridors and retain valid paths through tunnels.
- Ghosts alternate Chase and Scatter. Power pellets trigger Frightened, with a flashing warning in the final two seconds. Captures award 200/400/800/1,600, once per returning ghost, resetting the chain on each new power pellet. Returning ghosts cannot harm the player and route back to their individual spawn.
- Normal pellets award 10; power pellets award 50. Fruit appears at 30% and 70% collection, lasts nine seconds and awards 300×level. Collection removes each item once. A complete legal BFS traversal is tested, including level completion and score preservation.
- Life loss resets positions and input, preserves score and pellets, and gives a two-second transition. Level clears have a two-second transition and rebuild a progressively faster maze. Classic/Speed-Up/Hard continue until lives run out; clearing a level is not a final run result.
- Original vector art, blue maze borders, four colored animated ghosts, frightened faces, returning eyes, animated fruit/power pellets, score feedback, life-loss rings and ready/level/result overlays. The board scales uniformly; menus scroll at small sizes.
- Ten original synthesized WAV cues, controlled by a local mute/audio owner. Missing files or media support degrade to silence. Navigation and pause stop clips.
- Controller listeners clear input, stop timers/audio and detach scene/window handlers on navigation or close. Losing focus pauses active gameplay. Full elapsed frame time is passed to the sub-stepped model so slow frames do not silently extend timed games.
- `PacmanResult` freezes game/mode, score, levels completed/reached, captures, pellet/power counts, lives, duration, outcome and challenge target/time. `consumeResult()` returns it once per completed run. A future shared result service can consume that record without coupling the model to authentication, Derby or leaderboards.

## Files and shared integration

Game source: `PacmanMode`, `PacmanMechanics`, `PacmanController`, `PacmanRenderer`, `PacmanAudio`, `PacmanResult`. Resources: `pacman-view.fxml`, `pacman.css` and ten `PacmanSounds/` WAVs. Tests: `src/test/java/edu/farmingdale/csc311capstone/Pacman/PacmanMechanicsTest.java`.

Shared edits are only an original-asset note appended to `CREDITS.md` and explicit Surefire 3.5.4/classpath test configuration in `pom.xml`. The production module and all existing arcade routes remain unchanged. Existing teammates' tests under `src/main/test/java` remain untouched and are not included in the new test count.

Both finalization branches start independently at `72c6af8`. They contain identical Maven additions; those should coalesce. Their credits additions share the same end-of-file location, so preserve both game sections if Git requests a resolution. No game implementation paths overlap.

## Build and launch

Use the existing IntelliJ `HelloApplication` configuration with Java 25, then choose Pacman on the dashboard. Terminal equivalents:

```sh
mvn clean compile
mvn test
mvn clean package
mvn javafx:run
```

Java 25.0.4.1 and Maven 3.10.0 were verified. IntelliJ's existing configuration names `liberica-25`; its console showed a prior successful Liberica 25 launch. No IDE settings were changed. The checked-in wrapper lacks execute permission; installed Maven or `sh mvnw` works around that without changing repository permissions.

## Automated and rendered verification

The 67-test suite covers maze/spawn validation, full reachability and legal maze clear, continuous/buffered/interpolated movement, walls/tunnels, normal/power collection and duplicate prevention, capture chains, frightened expiry/pause, returning and respawn, BFS/personality targets, scatter/chase transitions, crossing collisions, life loss/reset/game over, fruit spawning/collection/expiry, level/score progression, all mode configurations, speed caps, timed transitions/expiry/pause, challenge success/failure/recharge, restart, result statistics (including expiry during a just-cleared level transition) and single delivery.

JavaFX probes exercise actual FXML/controller construction, every mode screen, frightened/returning ghosts, level transition, game-over and challenge-success results, pause/resume, mute toggles, restart and arcade cleanup. Scenario screenshots deliberately use controlled model fixtures for rare states; a level-clear screenshot alone does not claim a human cleared that maze. Screenshots and logs remain outside source control at `/private/tmp/csc311-verification/`.

Desktop automation could inspect IntelliJ but could not target the standalone Java process. Rendered snapshots were visually inspected. Physical keyboard interaction, human difficulty/fun/fairness balancing and audible sound quality remain manual acceptance items. Audio resources loaded and mute/stop code paths were exercised; no claim is made that a person listened to them.

## Manual acceptance checklist

- Classic: single-tap steering, queue turns, reverse, use tunnels, collect each item type, capture blue ghosts, watch returning eyes, lose lives, clear a maze and advance.
- Time Attack: watch 60 seconds, pause, restart, lose a life and clear a level; verify time/score persist correctly and time expiry ends the run.
- Speed-Up: collect at least 20 pellets, inspect the multiplier, continue toward the cap and test turns/crossings at higher speeds.
- Hard: verify two lives, faster ghosts and shorter power windows; assess difficulty with a human player.
- Ghost Challenge: use replenishing power pellets, capture six ghosts, inspect success; separately exhaust lives or time for failure.
- All modes: mute/unmute, Tab/Enter navigation, resize, switch focus while steering, reopen and restart repeatedly, and return to the arcade. Listen for cue clarity and comfortable volume.
