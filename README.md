# ShiftBall ⚡

> A sleek, endless mobile runner built with Jetpack Compose — dodge walls, collect gems, and shift till you drop.

---

## Overview

**ShiftBall** is a minimalistic Android endless-runner where a glowing ball plunges through a procedurally generated corridor of obstacles. Tap anywhere to switch sides, collect gems for combo multipliers, and see how long you can survive. The app features a full 4-theme system (including a light mode), a polished pause screen, persistent settings, and buttery 60 FPS canvas rendering.

---

## Screens

| Screen | Highlights |
|---|---|
| **Splash** | Theme-aware animated ball + tagline + pulsing loading dots |
| **Menu** | Best-score card at top, centered title section, big circular play button, pulsing indicator dots, full-width settings row |
| **Game** | Canvas-rendered corridor, working pause with score/best card + Resume/Menu buttons |
| **Game Over** | Star rating, confetti on new best, animated entry |
| **Settings** | Theme picker (Void / Neon / Fire / Light), ball color, difficulty, audio toggles, personal best |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI framework | Jetpack Compose + Material 3 |
| State | `StateFlow` + `ViewModel` |
| Navigation | `androidx.navigation.compose` |
| Persistence | Jetpack `DataStore` (Preferences) |
| Audio | `SoundPool` |
| Build | Gradle KTS |

**Min SDK:** 29 (Android 10)  
**Target SDK:** 35

---

## Project Structure

```
app/src/main/java/uz/angrykitten/shiftball/
├── MainActivity.kt          # App entry, NavHost, CompositionLocal theme provider
├── GameState.kt             # Data classes (Ball, Platform, Gem, Particle, GameStatus)
├── GameViewModel.kt         # Game loop, tick(), physics, scoring, pause
├── GameScreen.kt            # Canvas rendering, pause card overlay, HUD
├── GameOverScreen.kt        # Score card, star rating, confetti on new best
├── MenuScreen.kt            # Best-score card, centered branding, play button, settings row
├── SettingsScreen.kt        # Theme/color/difficulty/audio settings
├── SettingsViewModel.kt     # Settings state + DataStore sync
├── SplashScreen.kt          # Animated entry screen (1.9 s), then auto-navigates
├── DataStoreManager.kt      # DataStore keys + read/write helpers
└── VoidFallColors.kt        # Theme system (VoidFallTheme, AllThemes, CompositionLocal)
```

---

## Theme System

ShiftBall ships with **4 built-in themes**, saved to DataStore and applied instantly from `CompositionLocalProvider`.

| Theme | Palette | Feel |
|---|---|---|
| **Void** (default) | Deep purple, violet walls, gold gems | Dark, mysterious |
| **Neon** | Cyan, teal walls, yellow gems | Electric, futuristic |
| **Fire** | Orange/red walls, green gems | Intense, volcanic |
| **Light** | White background, purple accents | Clean, modern |

### How themes propagate

```
SettingsViewModel.theme (StateFlow<AppTheme>)
    ↓ collected in VoidFallApp (MainActivity)
themeByEnum(AppTheme) → VoidFallTheme data class
    ↓ provided via
CompositionLocalProvider(LocalVoidFallTheme provides currentTheme)
    ↓ consumed everywhere via
val theme = LocalVoidFallTheme.current
```

Canvas helpers (private `DrawScope` functions) receive the theme as a parameter to stay theme-aware without needing a composable context.

### Light mode considerations
- `theme.isLight` flag gates the dark vignette overlay and starfield
- All text references use `theme.score` (dark in light mode, light in dark mode) instead of hardcoded `Color.White`
- `theme.overlayBg` provides a tinted opaque scrim for the pause overlay regardless of theme

### Adding a new theme

1. Add an entry to `AppTheme` enum in `VoidFallColors.kt`
2. Create a `VoidFallTheme` instance filling all fields
3. Add it to `AllThemes`
4. It appears automatically in the Settings theme picker

---

## Architecture — MVVM

```
UI (Composables)
    ↕  collectAsStateWithLifecycle
GameViewModel / SettingsViewModel
    ↕  StateFlow<GameState> / StateFlow<T>
DataStoreManager (DataStore<Preferences>)
```

- **GameViewModel** drives the game loop via `tick(dt)` called every frame from `withFrameNanos`. It owns all mutable game `StateFlow<GameState>`.
- **SettingsViewModel** exposes DataStore-backed preferences as `StateFlow`s and provides save helpers.
- Both share the same ViewModel instance across the entire NavHost.

---

## Menu Layout

```
┌─────────────────────────────────────────┐
│  ┌───── PERSONAL BEST CARD ─────────┐  │  ← at top
│  │   PERSONAL BEST       ★          │  │
│  │       1,234                      │  │
│  └──────────────────────────────────┘  │
│                                        │
│          · ENDLESS RUNNER ·            │  ← centered labels
│              ShiftBall                 │  ← main title
│       Shift sides, survive.            │
│                                        │
│               ⏵  PLAY                 │  ← big circular button
│              ●  ●  ●                   │  ← pulsing dots
│  ┌─ ≡  SETTINGS ──────────────── → ─┐  │  ← full-width settings row
│  └────────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## Pause System

The pause system uses **independent pointer areas** with proper Z-order:

```
Box (no tap handler)
├── Inner Box  → detectTapGestures { onTap() }     [game canvas area]
├── Pause Overlay → detectTapGestures { togglePause() }  [PAUSED only, full screen]
│   └── Card: PAUSED title + Score/Best + Resume + Main Menu
└── Pause Button (clickable, Z-top) → togglePause()
    └── Canvas-drawn ▶ / ‖ icon
```

The pause overlay is styled like a proper break screen — not a minimal text overlay. It shows the current score and best side by side, and provides explicit **Resume** and **Main Menu** buttons in addition to the tap-anywhere-to-resume gesture.

---

## Navigation

```
splash ──→ menu ⇄ settings
              ↓
           game ──→ gameover/{score}/{gems}
              ↑_______________|
```

- Splash auto-navigates after 1.9 s and is popped from the back stack
- Play Again: `resetGame()` → `syncSettings()` → navigate `"game"` (popped to `"menu"`)
- Main Menu from pause: `resetGame()` → navigate `"menu"` (inclusive pop removes `"game"`)

---

## Game Mechanics

### Ball movement
Exponential easing interpolation — framerate-independent:
```kotlin
val lerp = 1f - exp(-10f * dt)
newX = ball.x + (targetX - ball.x) * lerp
```

### Scoring
- +1 point every 0.5 s of survival
- +3 × combo multiplier per gem

### Gem Combo
| Consecutive gems | Multiplier |
|---|---|
| 2–3 | ×2 |
| 4–5 | ×3 |
| 6+ | ×4 |

Combo resets 2.5 s after last gem collected.

### Speed Scaling
```kotlin
newSpeed = (base + (score / 8f) * 22f).coerceAtMost(base * 4.5f)
```

| Difficulty | Base speed | Platform density |
|---|---|---|
| Easy | 0.75× | 0.7× |
| Normal | 1.0× | 1.0× |
| Hard | 1.5× | 1.5× |

---

## Building & Running

```bash
# Clone
git clone <repo-url>
cd ShiftBall

# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

**Requirements:** Android Studio Hedgehog+, JDK 17, Android SDK 35

---

## DataStore Keys

| Key | Type | Default |
|---|---|---|
| `best_score` | Int | 0 |
| `sound_effects` | Boolean | true |
| `music` | Boolean | true |
| `vibration` | Boolean | true |
| `ball_color_idx` | Int | 0 (Purple) |
| `difficulty` | String | `"NORMAL"` |
| `app_theme` | String | `"VOID"` |

---

## License

MIT — see `LICENSE` for details.
