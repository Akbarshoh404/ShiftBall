# VoidFall

An elegant, fast-paced endless scroller built entirely with Jetpack Compose for Android. 

<br>

![Banner/Screenshot Placeholder](https://via.placeholder.com/800x300/0D0D1A/6D28D9?text=VOIDFALL)

## Features

- **Pure Jetpack Compose Canvas**: Smooth 60FPS fluid rendering and custom draw loops directly within Compose's Canvas API.
- **DataStore Preferences**: Settings such as sound effects, difficulty modifiers, best scores, and customized aesthetics persist locally.
- **Snappy Physics & Collision**: Rapid responsive side-switching mechanics using interpolation physics.
- **Sleek Aesthetic**: Deep space gradient coloring matched with vibrant retro wave neons, glow effects, and micro-animations.
- **Glassmorphism UI**: Beautiful HUD elements and smooth transition animations across all screens.

## Gameplay Mechanic
1. The ball sits fixed at the bottom region of the screen.
2. Obstacles (glowing pink bars) spawn randomly and descend continuously at an increasing speed.
3. Tap **anywhere** on the screen to swiftly toggle the player's position between the left and right walls.
4. Try to collect as many Golden Gems (`◆`) as possible without colliding. Collecting gems updates your multipliers. Good luck!

## Technologies Used

- `Kotlin` 2.0+
- `Jetpack Compose` (Canvas, Animations API, Activity Integration)
- `Navigation Compose` (Screen transitions)
- `DataStore` (Settings persistence)
- `Lifecycle ViewModel`
- `Coroutines & Flow`
- Base Android `SoundPool` & `Vibrator` services for haptic feedback.

## Building & Running

1. Clone this repository.
2. Open the project in **Android Studio** (Koala or later recommended for Compose support).
3. Ensure you have an Emulator or physical phone with `minSdk 29` capability.
4. Hit **Run** (`Shift + F10`) or sync with Gradle.

Optionally via command line:
```bash
./gradlew assembleDebug
```

## Structure
- `MainActivity.kt`: Entry navigation host keeping ViewModels scoped.
- `GameScreen.kt`: The main rendering `Canvas` powered by a `withFrameNanos` loop.
- `GameViewModel.kt`: Centralized dispatcher containing all physics, game ticking, data generation, and scaling logic.
- `SettingsScreen.kt` & `SettingsViewModel.kt`: Control hub for preferences.
- `MenuScreen.kt` / `GameOverScreen.kt`: Additional animated compose routes.

---
*Created by [angrykitten](https://github.com/angrykitten)*
