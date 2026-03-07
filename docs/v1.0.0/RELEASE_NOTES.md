# Release Notes - Flappy Bird v1.0.0

**Release Date:** March 7, 2026  
**Release Type:** Initial Release  
**Platform:** Windows, macOS, Linux  
**Java Version Required:** Java 22+

---

## 🎉 First Release

We're excited to announce the first official release of Flappy Bird - a modern Java implementation of the classic mobile game with enhanced features and polished gameplay!

---

## ✨ Features

### Core Gameplay
- **Classic Flappy Bird mechanics** with smooth physics
- **Gravity-based movement** (1 pixel/update acceleration)
- **Space bar controls** for jumping (-13 pixels/update velocity)
- **Accurate collision detection** using rectangular hitboxes
- **Score tracking** with sprite-based number display
- **Infinite scrolling pipes** with random gap positions
- **Game states**: Start screen → Gameplay → Game Over → Restart

### Visual Features

#### Graphics & Animations
- ✅ **Sprite-based rendering** using authentic Flappy Bird graphics
- ✅ **Animated bird sprites**:
  - `yellowbird-upflap.png` - When rising (velocity < -5)
  - `yellowbird-midflap.png` - Neutral flight (-5 ≤ velocity ≤ 5)
  - `yellowbird-downflap.png` - When falling (velocity > 5)
- ✅ **Scrolling ground** with tiled base sprite
- ✅ **Pipe rendering** with proper scaling and flipping for top pipes
- ✅ **Number sprites** for authentic score display (0-9.png)

#### Day/Night Mode
- ✅ **Dual backgrounds**:
  - `background-day.png` - Bright daytime sky
  - `background-night.png` - Dark nighttime atmosphere
- ✅ **On-screen toggle button** (top-right corner)
- ✅ **Instant switching** without game interruption
- ✅ **Button state display**: "Night: OFF" / "Night: ON"

### Audio System

#### Sound Effects
- ✅ **Wing flap sound** (`wing.wav`) - Plays on every jump
- ✅ **Point sound** (`point.wav`) - Plays when passing through pipes
- ✅ **Hit sound** (`hit.wav`) - Plays on collision
- ✅ **Die sound** (`die.wav`) - Plays on game over
- ✅ **Swoosh sound** (`swoosh.wav`) - Plays on game start/restart

#### Audio Controls
- ✅ **Mute/Unmute toggle** button in top-right corner
- ✅ **Persistent mute state** during gameplay
- ✅ **Button state display**: "Sound: ON" / "Sound: OFF"
- ✅ **Clean audio resource management**

### User Interface

#### On-Screen Controls
- ✅ **Sound toggle button** - Mute/unmute all game sounds
- ✅ **Day/Night toggle button** - Switch background themes
- ✅ **Non-focusable buttons** - Don't interfere with space bar control
- ✅ **Transparent overlay UI** - Doesn't obstruct gameplay

#### Game Screens
- ✅ **Start screen** with "Press SPACE to start" message
- ✅ **In-game HUD** with centered score display
- ✅ **Game Over screen** featuring:
  - `gameover.png` sprite
  - Final score display
  - "Press SPACE to restart" prompt
  - Semi-transparent overlay (128 alpha)

### Technical Improvements

#### Performance
- ✅ **45 FPS gameplay** (22ms update interval)
- ✅ **Optimized rendering** with Graphics2D
- ✅ **Efficient collision detection** (Rectangle intersection)
- ✅ **Smooth pipe movement** (4 pixels/update)
- ✅ **Instant window close** - No 3-second delay

#### Code Quality
- ✅ **Clean architecture** with separated concerns:
  - `Main.java` - Game loop and logic
  - `FlappyBird.java` - Rendering and UI
  - `Bird.java` - Bird sprite management
  - `Pipe.java` - Pipe objects
  - `AudioManager.java` - Sound system
- ✅ **Resource loading** via classpath (works in JAR files)
- ✅ **Proper image scaling** to match collision boxes
- ✅ **State management** for game flow
- ✅ **Java 22 features** including unnamed variables

---

## 🎮 How to Play

### Starting the Game
1. Launch the application
2. Press **SPACE** to start
3. Press **SPACE** to make the bird flap

### Controls
| Key/Button | Action |
|------------|--------|
| **SPACE** | Jump / Start / Restart |
| **Night Toggle** | Switch day/night background |
| **Sound Toggle** | Mute/unmute sounds |
| **Red X** | Close game |

### Objective
- Navigate through pipe gaps to earn points
- Avoid hitting pipes, ground, or ceiling
- Beat your high score!

---

## 📦 Installation

### Requirements
- Java Runtime Environment (JRE) 22 or higher
- Operating System: Windows, macOS, or Linux
- Screen Resolution: 460×819 minimum

### Running the Game
```bash
java -jar FlappyBird-1.0.0.jar
```

### Building from Source
```bash
git clone https://github.com/kartersanamo/FlappyBird.git
cd FlappyBird
mvn clean package
java -jar target/FlappyBird-1.0.0.jar
```

---

## 🔧 Technical Specifications

### Game Configuration
| Setting | Value |
|---------|-------|
| Screen Width | 460 pixels |
| Screen Height | 819 pixels |
| FPS | ~45 (22ms update) |
| Pipe Speed | 4 px/update |
| Pipe Width | 115 pixels |
| Pipe Height | 460 pixels |
| Pipe Gap | 178 pixels |
| Gravity | 1 px/update² |
| Jump Strength | -13 px/update |
| Bird X Position | 100 pixels (fixed) |

### Resource Files
- **Sprites**: 28 PNG files (backgrounds, birds, pipes, numbers, UI)
- **Audio**: 5 WAV files (wing, point, hit, die, swoosh)
- **Total Assets Size**: ~500KB

### Dependencies
- Java 22+ Standard Library
  - `javax.swing.*` - GUI framework
  - `java.awt.*` - Graphics and events
  - `javax.sound.sampled.*` - Audio playback

---

## 🐛 Known Issues

None reported for this release.

---

## 🔮 Future Enhancements

Potential features for future versions:
- High score persistence (save to file)
- Multiple bird color options (red, blue birds)
- Red pipe variant toggle
- Difficulty levels (faster pipes, smaller gaps)
- Leaderboard system
- Touch screen support
- Mobile platform ports

---

## 📝 Changelog

### v1.0.0 (March 7, 2026)
- ✅ Initial release
- ✅ Complete Flappy Bird gameplay implementation
- ✅ Day/Night mode toggle
- ✅ Sound system with mute control
- ✅ Sprite-based graphics and animations
- ✅ Velocity-based bird animations
- ✅ Score tracking with number sprites
- ✅ Game Over screen with restart
- ✅ Instant window close
- ✅ Cross-platform compatibility

---

## 🙏 Credits

### Original Game
- **Flappy Bird** created by Dong Nguyen (2013)

### This Implementation
- **Developer**: Karter Sanamo
- **Release Date**: March 7, 2026
- **Version**: 1.0.0
- **License**: MIT License

### Assets
- Sprites and sounds from original Flappy Bird
- Faithful reproduction of classic game feel

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

---

## 🤝 Contributing

We welcome contributions! To contribute:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/kartersanamo/FlappyBird/issues)
- **Email**: karter.sanamo@example.com
- **Documentation**: [README.md](../../README.md)

---

**Thank you for playing Flappy Bird v1.0.0!** 🐦

Enjoy the nostalgia and challenge yourself to beat your high score!

