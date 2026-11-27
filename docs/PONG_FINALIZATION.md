# Pong finalization

Pong now opens on its own neon court homepage through the existing `pong-view.fxml` route. No dashboard or navigation source changes are needed.

## Rules and implementation

- Local Multiplayer: W/S versus Up/Down, first to seven.
- Player vs AI: first to seven against Easy, Medium or Hard.
- Blitz: 60 seconds against a human or any AI difficulty; highest score wins, ties draw. The opening ready countdown is excluded; between-point countdowns count toward the clock. Pause freezes everything.
- Ball physics use 240 Hz substeps, impact-based angles, increasing rally speed capped at 780 units/second, separation from paddles, wall reflection, alternating serves and a minimum horizontal speed.
- Easy/Medium/Hard use speeds of 215/300/385, reaction delays of .34/.19/.10 seconds, and target errors of 75/34/15 units. Hard folds its predicted trajectory across wall bounces. Every CPU moves continuously within the same paddle bounds.
- Original JavaFX court art, trails, collision particles, score pulses, ready countdowns and result rings. The court scales uniformly inside the available area.
- Eight original synthesized WAV cues, throttled and muted through a game-local audio owner. Pause/navigation stop clips. No downloaded assets.
- Scene, window, focus and close listeners stop the loop and release handlers when leaving. Focus loss pauses play and clears held movement keys.
- `PongResult` exposes mode, opponent, difficulty, score, outcome, active match duration, hits and longest rally. `getResult()` is stable; `consumeResult()` returns it once per completed match. Connect a future result service at that boundary; no database exists here.

## Files

The Pong package contains `PongConfig`, `PongMechanics`, `PongController`, `PongRenderer`, `PongAudio` and `PongResult`. The FXML, `pong.css`, eight `PongSounds/` WAVs and `PongMechanicsTest` complete the implementation.

Shared edits are limited to an original-asset attribution appended to `CREDITS.md` and an explicit Surefire 3.5.4 configuration in `pom.xml`. Surefire runs JUnit 5 on the classpath while the application remains modular. Existing tests under `src/main/test/java` are unchanged and are not included in the new test count.

## Launch and verify

Use JDK 25 and the existing IntelliJ `HelloApplication` run configuration. Select Pong on the arcade dashboard. Alternatively:

```sh
mvn clean compile
mvn test
mvn clean package
mvn javafx:run
```

The checked-in wrapper is not executable on this checkout; installed Maven is equivalent. `sh mvnw` is another option. No IntelliJ configuration was changed.

Forty-one deterministic JUnit tests cover movement, paddle bounds, wall and paddle collisions, angles, speed caps, tunnelling, score/reset/serve rules, first-to-seven, pause/restart, AI timing/differences/bounds/prediction, Blitz timers/outcomes and single-delivery results. Scene probes additionally exercise FXML construction, every mode and difficulty, overlays, mute toggles, restart and arcade navigation. Verification screenshots are kept outside the source tree in `/private/tmp/csc311-verification/`.

For manual acceptance, play a standard local match to seven with simultaneous keys, replay, try each CPU difficulty, and run Blitz against both opponent types. Pause midway and check the clock; restart and verify 60 seconds. Resize the window, tab through menus, leave and reopen repeatedly, switch focus while holding movement, and listen to each cue with sound on/off.

Desktop automation could inspect IntelliJ but could not address the standalone Java process. JavaFX scene snapshots were visually reviewed and programmatic JavaFX checks were performed. Human gameplay feel, physical keyboard rollover and audible sound quality still need manual acceptance; do not equate automated checks with those checks.

## Recorded verification (2026-11-27)

- `mvn clean compile`: success.
- `mvn test` and `mvn clean package`: 41 discovered/executed/passed, 0 failures/errors/skipped; packaging success.
- Baseline and finalized application launches succeeded. No FXML or application exceptions occurred in the scene probe. JavaFX emits JDK native-access warnings with the existing Maven launcher; these are baseline warnings.
- Real animation-timer Blitz versus Hard ran to completion: 0–12, 60.0 active seconds, 13 paddle hits, best rally 5. The left paddle was unattended; this confirms timing/results, not human difficulty balance.
- Programmatic JavaFX checks passed every mode/difficulty, pause/resume, restart, mute toggles, stable result display and arcade cleanup. Snapshots of menu, difficulty, local, all CPU difficulties, both Blitz opponents, pause and results were reviewed. Separate 660×660 menu/gameplay snapshots confirmed the court remains visible and buttons wrap correctly.
- Final fixes included continuous Blitz timing across serves, priority for result sounds over score-cue throttling, and additional score/result animations.

Final review also removed controller frame-delta clipping so slow rendering frames cannot silently extend Blitz. Physics still sub-step safely. Opening/serve countdowns now last exactly three/one seconds, and the match-start cue takes priority over the last countdown tick. The additional minimum-horizontal-speed regression brings the final suite to 41 tests.

The final isolated JavaFX run passed after the clock/countdown refinements: Blitz versus Hard ended 0–12 at 60.0 seconds, with 14 hits and best rally 7. Complete fixed-time simulations also reached first-to-seven results against Easy, Medium and Hard; these used an unattended left paddle and do not establish human difficulty balance. A direct 250 ms animation-frame check confirmed that all 250 ms reached the clock. Final resized rendering and timer cleanup checks passed.

A temporary combined source build containing both finalized games compiled and packaged with **108 tests passed (41 Pong + 67 Pac-Man), zero failures/errors/skips**. A Git merge simulation found only an end-of-file conflict in `CREDITS.md`; retain both original-asset sections. No actual branch merge was performed. Shared navigation, the production module, IDE files and teammates' games were unchanged.

The executable named by IntelliJ's existing SDK configuration was also checked directly: `liberica-25.0.4.1/bin/java -version` reports OpenJDK 25.0.4.1. The terminal builds used Oracle Java 25.0.4.1 and Maven 3.10.0.
