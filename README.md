# Flappy Bird (with Hidden Password Manager)

A modern Java implementation of the classic Flappy Bird game with enhanced features, polished gameplay, and a secret password manager hidden within.

![Java](https://img.shields.io/badge/Java-25-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Version](https://img.shields.io/badge/Version-1.0.0-green.svg)

> **🔐 Secret Feature**: This game contains a hidden password manager accessible via a secret code. See [SECRET_ACCESS.md](SECRET_ACCESS.md) for details.

## Features

### Gameplay
- **Classic Flappy Bird mechanics** - Navigate through pipes by tapping space
- **Physics-based movement** - Realistic gravity and jump mechanics
- **Smooth animations** - Dynamic bird sprites that change based on velocity
- **Collision detection** - Accurate hitboxes for fair gameplay
- **Score tracking** - Keep track of your score

### Visual Features
- **Day/Night Mode** - Toggle between beautiful day and night backgrounds
- **Sprite-based graphics** - Authentic Flappy Bird visuals
- **Number sprites** - Classic pixel-art score display
- **Animated bird** - Three bird sprites (upflap, midflap, downflap) based on velocity

### Audio
- **Sound effects** - Wing flaps, scoring, collision sounds
- **Mute toggle** - Easily enable/disable all game sounds
- **Multiple audio clips** - Wing, point, hit, die, and swoosh sounds

### User Interface
- **On-screen controls** - Day/Night and Sound toggle buttons
- **Game states** - Start screen, gameplay, and game over screens
- **Instant restart** - Press SPACE to restart after game over
- **Quick exit** - Window closes instantly when clicking the X button

### Hidden Features
- **🔒 Secret Password Manager** - Access a full-featured password manager by entering a secret code on the game over screen
  - Store and manage passwords securely
  - **SQLite database persistence** - passwords saved permanently
  - **🔍 Real-time search** - Filter across all fields instantly
  - **🔐 Password generator** - Generate strong passwords with customizable options
  - Add, edit, delete, and view password entries
  - Copy passwords to clipboard
  - No sample data - clean start
  - See [SECRET_ACCESS.md](SECRET_ACCESS.md) or [QUICK_START.md](QUICK_START.md) for access instructions
  - See [DATABASE_UPDATE.md](DATABASE_UPDATE.md) for database details
  - See [SEARCH_AND_GENERATOR.md](SEARCH_AND_GENERATOR.md) for search & generator guide

## Requirements

- **Java 22 or higher**
- **Maven 3.6+** (for building from source)
- **Operating System**: Windows, macOS, or Linux

## Installation

### Download Release
1. Download the latest release from the [Releases](https://github.com/kartersanamo/FlappyBird/releases) page
2. Extract the archive
3. Run the JAR file:
   ```bash
   java -jar FlappyBird-1.0.0.jar
   ```

### Build from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/kartersanamo/FlappyBird.git
   cd FlappyBird
   ```

2. Build with Maven:
   ```bash
   mvn clean package
   ```

3. Run the game:
   ```bash
   java -jar target/FlappyBird-1.0.0.jar
   ```

## How to Play

### Controls
- **SPACE** - Jump / Start game / Restart after game over
- **Night Toggle Button** (top-right) - Switch between day and night backgrounds
- **Sound Toggle Button** (top-right) - Mute/unmute game sounds

### Objective
Navigate the bird through the pipes by tapping SPACE to flap. Each successful pass earns you a point. The game ends when you hit a pipe, the ground, or the ceiling.

### Tips
- **Timing is key** - Tap space at the right moment to maintain altitude
- **Watch the bird sprite** - The bird's animation indicates its velocity
- **Practice makes perfect** - Learn the rhythm of jumping

## Project Structure

```
FlappyBird/
├── src/
│   ├── main/
│   │   ├── java/com/kartersanamo/
│   │   │   ├── flappyBird/
│   │   │   │   ├── Main.java           # Main game loop and logic
│   │   │   │   ├── FlappyBird.java     # Rendering and UI panel
│   │   │   │   ├── Bird.java           # Bird sprite and animations
│   │   │   │   ├── Pipe.java           # Pipe objects
│   │   │   │   └── AudioManager.java   # Sound management
│   │   │   └── passwordManager/
│   │   │       └── PasswordManagerUI.java  # Hidden password manager
│   │   └── resources/
│   │       ├── sprites/            # Game graphics
│   │       └── audio/              # Sound effects
│   └── test/java/                  # Unit tests
├── docs/                           # Documentation
│   └── v1.0.0/                     # Release notes
├── SECRET_ACCESS.md                # Password manager access guide
├── QUICK_START.md                  # Quick start guide
├── IMPLEMENTATION_SUMMARY.md       # Technical implementation details
├── pom.xml                         # Maven configuration
├── LICENSE                         # MIT License
└── README.md                       # This file
```

## Technical Details

### Architecture
- **Language**: Java 25
- **GUI Framework**: Java Swing
- **Audio**: javax.sound.sampled
- **Build Tool**: Maven
- **Design Pattern**: MVC (Model-View-Controller)

### Game Constants
- **Screen Size**: 460 × 819 pixels (1.6x scale of background image)
- **Update Interval**: 22ms (~45 FPS)
- **Pipe Speed**: 4 pixels/update
- **Gravity**: 1 pixel/update²
- **Jump Strength**: -13 pixels/update (upwards)
- **Pipe Gap**: ~178 pixels

### Performance
- Smooth 45 FPS gameplay
- Low CPU usage (~5-10%)
- Instant window close
- Minimal memory footprint (~50MB)

## Credits
- Stratofortress at https://www.instructables.com/Java-Game-Programming-Tutorial-Flappy-Bird-Redux/ for inspiration & file structure
- All code written by Karter Sanamo

### Assets
- Samuel Custodio at https://github.com/samuelcust/flappy-bird-assets for the assets

### Development
- Developer: Karter Sanamo
- Initial Release: March 2026
- Version: 1.0.0

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Changelog

See [CHANGELOG.md](docs/v1.0.0/RELEASE_NOTES.md) for detailed release notes.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Support

For issues, questions, or suggestions:
- **GitHub Issues**: [Create an issue](https://github.com/kartersanamo/FlappyBird/issues)
- **Email**: kartersanamo@gmail.com

## Acknowledgments

- Inspired by the original Flappy Bird by Dong Nguyen
- Built with Java Swing
- Sound effects from the original game

---

**Enjoy the game!** 🐦

