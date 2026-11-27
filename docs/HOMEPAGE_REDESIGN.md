# Arcade application UI redesign

The application now opens into a native JavaFX arcade dashboard with seven screens: Home, Leaderboards, Statistics, Profile, Settings, Login, and Registration. The five dashboard pages share one persistent sidebar. All eight existing game routes are retained, with original artwork and working library controls.

This is a frontend implementation. No authentication or shared database service was present in the fetched repository branches at implementation time. Player-dependent sections therefore show guest, unavailable, or empty states. They do not contain the illustrative players or scores from the design references.

## Repository baseline and scope

- Branch: `feature/homepage`, created from `origin/master` at `a8d4fe4`.
- Remote: `https://github.com/LoganARay/CSC311-Capstone.git`.
- The original working tree was clean. Master and the existing game branches were not modified.
- Fetched remote branches and recent PRs were inspected. Pong PR #13 and Pac-Man PR #14 were already merged into master. No database/authentication implementation contract was found in the available branches.
- Java 25.0.4.1, JavaFX 25, and Maven 3.10.0 were used locally on macOS.
- No changes were made to the POM, IntelliJ configuration, existing game FXML, game controllers, mechanics, game artwork, or existing tests.

## Screens and behavior

| Screen | Implemented behavior |
| --- | --- |
| Home | Featured Pac-Man banner; Play Now; Surprise Me; eight illustrated game cards; combined text/category search; six category filters; featured/alphabetical/reverse alphabetical sorting; clear/reset actions; no-results state; unavailable summary metrics; recent-history empty state. |
| Leaderboards | Reference-style banner; all eight games in the selection control; unavailable overall/mode/time-period controls where applicable; gold/silver/bronze podium placeholders; ranking table; disabled pagination without records; personal-standing empty state. |
| Statistics | Summary cards; game/period selection layout; disabled unavailable mode control; native AreaChart without invented data; empty category ring; most-played and record empty states; individual-game section; match-history table. |
| Profile | Honest guest identity; sign-in navigation; unavailable profile editing; summary cards; all eight personal-record placeholders with artwork; favorite-games and activity empty states; history navigation. Levels and XP are not simulated. |
| Settings | General, Account, Display, About tabs; saved motion/animation preferences; preferred dashboard size; start-maximized preference; current maximize/windowed action; dirty indicator; save/reset feedback; explicit unavailable audio/account features; credits and version information. |
| Login | Split layout; original cabinet illustration; username/password fields; password visibility; required-field checks; error/loading presentation hooks; unavailable-service feedback; registration and guest navigation. |
| Registration | Matching split layout; username/password/confirmation fields; visibility controls; required-field and exact confirmation checks; backend-unavailable feedback; login and guest navigation. No invented password policy or email requirement. |

The Bomberman route currently opens the teammate's existing skeleton screen. Its library card is marked **Preview**. This redesign does not claim to finish that game.

## Design and architecture

`hello-view.fxml` is now the shell rather than a grid of launch buttons. Its sidebar stays in place while `ArcadeShellController` replaces only the central page. Page controllers are cached per shell. Each page owns its FXML layout; shared styles and components live in `arcade-theme.css`, `Ui`, `Icons`, and `GameCard`.

The palette follows the supplied references: near-black surfaces, a purple selected-navigation indicator, subtle bordered cards, cyan status, and gold highlights. Typography uses the platform system font. The login and registration pages use separate split layouts without a sidebar.

Stylesheets are attached to the new roots and scoped by `.arcade-root` / `.auth-root`. Individual game scenes do not inherit this theme. Page fades stop during navigation and when leaving the shell. Reduced motion disables them. The game-card hover treatment uses CSS and does not start background timers.

`GameDefinition` and `GameCatalog` hold immutable catalog metadata and all eight existing routes. Search, filtering, sorting, and random selection use that catalog. `DashboardPreferences` stores only non-sensitive dashboard options using Java Preferences. Tests use isolated temporary preference nodes.

`DashboardData` contains presentation records as a starting point for backend adapters, not a competing service or database schema. The production view currently renders unavailable data. The reporting/profile controllers still require adapters to the team's eventual service contract; supplying a snapshot alone does not implement complete report rendering or authentication. The shell snapshot hook currently updates the account summary. Backend integration remains explicit future work.

## Original artwork

`ArcadeArt` draws the eight thumbnails and cabinet with JavaFX Canvas, original pixel masks, shapes, shading, and grid details. Illustrations preserve aspect ratio as their regions resize. `Icons` draws original line icons from SVG paths. No design screenshot is used as a background or cropped into a thumbnail.

The cabinet marquee reuses the existing Press Start 2P font under its included SIL Open Font License. The dashboard's original artwork and font reuse are documented in `CREDITS.md`. Existing game attributions remain intact.

## Game navigation

| Game | Existing destination |
| --- | --- |
| Pong | `pong-view.fxml` |
| Pac-Man | `pacman-view.fxml` |
| Tetris | `tetris-home.fxml` |
| Snake | `snake-view.fxml` |
| Space Invaders | `spaceinvaders-view.fxml` |
| Blackjack | `blackjack-view.fxml` |
| Breakout | `breakout-view.fxml` |
| Bomberman | `bomberman-view.fxml` |

`SceneSwitcher` retains the dashboard Scene and its window geometry in the owning Stage. Returning through `hello-view.fxml` restores that Scene, including search/filter state, window size, and the shared sidebar. Games retain their own preferred scene sizes. Styles remain local to their roots. Destination FXML is loaded before replacing the current scene, and game-launch errors use a themed dialog.

Tetris's existing homepage has no return button. A small navigation bar is added around that homepage in `SceneSwitcher`; its game files are unchanged. The separate `tetris-view.fxml` gameplay scene remains the teammate's implementation, including its existing lack of an arcade-return/lifecycle contract. The route verification covers the requested Tetris homepage destination, not extended Tetris gameplay or modifications to that implementation.

## Verification

The following required commands passed:

```sh
mvn clean compile
mvn test
mvn clean verify
mvn javafx:run
```

The Maven test suite executes **149 tests, with zero failures, errors, or skips**:

| Test suite | Count |
| --- | ---: |
| Existing Pac-Man mechanics | 67 |
| Existing Pong mechanics | 42 |
| Game catalog, metadata, routes, search, categories, sorting, random selection | 21 |
| Login/registration validation | 7 |
| Preference persistence, defaults, invalid sizes, reduced motion | 6 |
| Empty data and number/duration/outcome formatting | 5 |
| JavaFX subprocess scene/interaction/snapshot integration | 1 |
| **Total** | **149** |

The scene test runs many assertions inside one JUnit test. The final run passed **153 scene/interaction assertions**, with the exact total printed in `arcade-redesign-scene-probe.log`. It verifies all seven screens, all four Settings tabs, actual JavaFX button actions, search and filter behavior, sorting, all eight game launches, dashboard restoration, saved preferences, authentication validation/loading/error presentation, unavailable-data tables, and repeated navigation without duplicate cards. Game returns use a visible native return Button when exposed; Canvas-based menu routes are exercised through the existing public `SceneSwitcher.showMenu` path.

Layout snapshots cover 1440 × 900, 1280 × 800, 1024 × 768, and 900 × 650. The library uses four columns at the larger sizes, three at 1024, and two at 900. The probe checks page width against the viewport and rejects truncated visible labels. Smaller authentication layouts use vertical scrolling. Full-content snapshots also cover sections below the initial fold.

These are actual JavaFX-rendered snapshots, with the toolkit and a live Stage, using programmatic JavaFX actions and explicit viewport sizes. They are **not physical mouse/keyboard playtests or screenshots of the design mockups**. OS window chrome is not part of the snapshots. The scene test requires a desktop environment (or a suitable virtual display on CI). Existing legacy tests under `src/main/test/java` remain untouched and are not included in Maven's standard test-source count.

The application was launched through Maven. IntelliJ settings were inspected but not modified. Physical gameplay, subjective audio quality, and manual acceptance on other displays remain separate acceptance work.

## Visual review and corrections

The implementation preserves the references' sidebar, purple selection treatment, featured/library organization, dark cards, reporting hierarchy, tabbed settings, and split authentication screens. The cabinet and thumbnails are deliberately original geometric pixel artwork rather than copies of the generated artwork. Empty states replace the examples' populated rankings, XP, and history.

Rendered snapshots exposed and drove fixes for:

- Hero/statistics wrapping caused by rounding at desktop widths.
- Minimum-content widths preventing the main pane from shrinking at laptop sizes.
- Search-field contrast and missing search icon.
- Stretched artwork; illustrations now preserve their aspect ratio.
- Default grey authentication viewport background and incomplete panel height.
- A squeezed profile avatar and clipped supporting descriptions.
- Unbalanced three-plus-one summary-card layouts; summaries use four or two columns.
- Wrapped empty-state descriptions losing height; labels now retain their preferred height.
- Captures taken before a tab's rendering pulse; tab navigation and capture are separated and the actual tab content is asserted.

Long pages intentionally scroll. The reporting/profile layouts do not fabricate data to match the populated reference examples.

## Screenshots and logs

The scene probe writes PNGs outside the repository to:

```text
<java.io.tmpdir>/arcade-redesign/screenshots/
```

The subprocess log is:

```text
<java.io.tmpdir>/arcade-redesign-scene-probe.log
```

The delivered local review folder includes an HTML gallery and the seven primary screen screenshots, all Settings tabs, smaller layouts, search/no-results, backend empty states, full-page captures, and the dashboard after a game return. Screenshot binaries are intentionally not committed. Re-run `mvn test` on a desktop to regenerate them.

## Backend and authentication handoff

The team still needs to supply:

1. An actual authentication API, its username/email requirements, password policy, asynchronous request handling, and secure session ownership. Wire that service into the form submission methods. `setBusy` and `showAuthenticationError` provide presentation hooks; they are not an authentication implementation.
2. Player identity, member date, biography/avatar, and editing/sign-out/password-change capabilities.
3. Per-game/per-mode match history, scores, durations, outcomes, and authoritative personal records. Do not aggregate incompatible score scales.
4. Leaderboard queries, filtering, pagination, current-user standing, and a defined overall ranking methodology before enabling overall rankings.
5. Game-specific statistics and reporting adapters. Category counting should assign each session to the catalog's single primary category.

No passwords, accounts, credentials, authentication sessions, rankings, shared score storage, XP database, or migrations were added. Submitting a locally valid form clears password fields and reports the unavailable backend; it never signs the player in or creates an account. There is no Google sign-in, password recovery, Remember Me, or plaintext credential persistence.

## Files and merge considerations

New application code is confined to `src/main/java/edu/farmingdale/csc311capstone/dashboard/`:

- Shell and pages: `ArcadeShellController`, `PageController`, `HomeController`, `LeaderboardController`, `StatisticsController`, `ProfileController`, `SettingsController`.
- Account forms: `AuthController`, `LoginController`, `RegistrationController`, `FormValidation`.
- Catalog and presentation data: `GameDefinition`, `GameCatalog`, `DashboardData`, `DisplayFormats`.
- Shared visuals/preferences: `Ui`, `Icons`, `ArcadeArt`, `GameCard`, `DashboardPreferences`.

New resources are seven page FXML files and `arcade-theme.css` under `src/main/resources/edu/farmingdale/csc311capstone/dashboard/`. New standard Maven tests are under `src/test/java/edu/farmingdale/csc311capstone/dashboard/`, including the standalone probe and JUnit wrapper.

Modified shared files are `HelloApplication.java`, `SceneSwitcher.java`, `module-info.java`, `hello-view.fxml`, and `CREDITS.md`. The old `HelloController.java` is preserved for compatibility but is no longer the shell's controller. This report is new.

Likely future conflicts are in the startup/shell FXML, SceneSwitcher, module declarations, and appended credits if the database teammate changes the same areas. Preserve existing game package opens, the new `java.prefs` requirement and dashboard FXML access, both parties' credits, and the team's eventual session ownership. There is no competing database code to merge.

## Launching in IntelliJ

1. Open the existing project and keep the project SDK and Maven runner on Java 25.
2. Reload the existing Maven project if needed; no new Maven dependencies were added.
3. Run `javafx:run` from the Maven tool window, or use the existing JavaFX-aware HelloApplication run configuration.
4. The app starts on Home as a guest. Use the footer account button or Profile's Sign in button to inspect the account forms. Continue as guest returns to the retained dashboard.
5. Use Settings → Display to save a preferred dashboard size. Games continue to choose their own scene dimensions.

Implementation commit: `532306e`. The final verification/documentation commit and pushed branch hash are supplied in the delivery message. The branch is pushed for review and is not merged into master.
