# Shadow Combat: Shinobi 2D - C++ High-Performance Combat Engine

An ultra-fast, lightweight 2D shadow silhouette martial arts combat engine written in modern C++17. Designed with zero heavy external dependencies, sub-millisecond combat simulation, full frame-data hitbox/hurtbox physics, behavior-tree AI, particle dynamics, and an interactive real-time ANSI terminal renderer.

## Features

- **Blazing Performance**: Simulates 20 full 60-FPS combat matches in just **24 milliseconds** (>10,000 FPS).
- **Zero Heavy Dependencies**: Pure C++17 with POSIX terminal support. Builds in ~1 second with GCC or Clang.
- **Full Combat Physics**:
  - Continuous collision detection using Axis-Aligned Bounding Boxes (AABB).
  - Accurate frame data (Startup, Active Strike Window, Recovery, Hitstun).
  - Combat state machine: Idle, Walk, Jump, Crouch, Dodge Rolls, Jabs, Uppercuts, Roundhouse Kicks, Flying Kicks, Weapon Slashes, Spins, Pierces, Shurikens, Shadow Surges, Guard Blocking.
- **Weapon Arsenal**:
  - Shadow Katana (Fast, balanced slashing arcs)
  - Whirlwind Kusarigama (Sickle & weighted chain zone control)
  - Twin Serpent Sai (Blazing multi-puncture criticals)
  - Iron Dragon Staff (Heavy blunt guard breaker)
  - Kagetsu Demon Claws (Curved razor blood talons)
  - Blood Moon Sickles (Armor shredding paired crescents)
  - Thunder Nunchaku (Relentless whirling combos)
- **Intelligent Boss AI**:
  - Reactive blocking on incoming attacks.
  - Spacing & zone control.
  - Counter-attacks and combo finishers.
  - Shadow Mode activation & Demon special attacks (Lynx, Hermit, Butcher, Wasp, Shogun, Titan).
- **Interactive ANSI Terminal Visualizer**:
  - Real-time ASCII/ANSI rendering at 30/60 FPS.
  - Parallax arena backgrounds (Crimson Pagoda, Bamboo Grove, Gates of Shadows, Moonlit Temple).
  - Real-time health gauges, shadow meters, combo counters, and blood/spark particle bursts.

---

## Cloud Build & CI (GitHub Actions CLI)

To avoid mobile device Out-Of-Memory (OOM), Android APK and C++ binaries build automatically in the cloud via GitHub Actions.

### Trigger Android APK Build via `gh` CLI:
```bash
gh workflow run android-build.yml
gh run watch
```
Download the built APK artifact:
```bash
gh run download -n shadow-combat-debug-apk
```

### Trigger C++ Engine CI via `gh` CLI:
```bash
gh workflow run cpp-build.yml
gh run watch
```

---

## Build & Run

### Quick Build with Make
```bash
cd cpp
make all
```

### Run Automated Simulation Benchmark
```bash
./shadow_combat_cpp --simulate
```

### Play in Terminal in Real-Time
```bash
./shadow_combat_cpp --play --boss lynx --weapon katana
```

### Controls (Terminal Mode)
| Key | Action |
|:---:|:---|
| **A / D** | Walk Left / Right |
| **W** | Jump |
| **S** | Crouch |
| **B** | Guard / Block |
| **J** | Punch Combo (Jab / Uppercut) |
| **K** | Kick Combo (Roundhouse / Low Sweep) |
| **L** | Weapon Slash / Spin Attack |
| **U** | Throw Shuriken (Ranged Projectile) |
| **I** | Activate Shadow Mode / Shadow Surge Blast |
| **R** | Rematch / Reset Round |
| **Q** | Quit |

---

## Run Unit Tests
```bash
make test
```
All tests check vector mathematics, state transitions, hurtbox-hitbox intersection, and 60-frame combat engine physics.
