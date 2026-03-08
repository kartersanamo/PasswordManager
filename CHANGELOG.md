# Changelog

## [1.2.0] - 2026-03-07

### Added - Search & Password Generator Features

#### 🔍 Search Functionality
- **Real-time search** across all password fields
  - Search by service/website name
  - Search by username/email
  - Search by notes
  - Case-insensitive filtering
  - Instant results as you type
- **Search UI**
  - Search bar at top of table
  - Clear button to reset filter
  - Tooltip for user guidance
- **Smart filtering** using TableRowSorter
  - Works with filtered/sorted views
  - Maintains proper row selection

#### 🔐 Password Generator
- **Advanced password generator** (NordPass-style)
  - Customizable length: 4-64 characters (slider)
  - Character type options:
    - Uppercase letters (A-Z)
    - Lowercase letters (a-z)
    - Digits (0-9)
    - Special characters (!@#$%^&*...)
  - Live preview of generated password
  - Regenerate button for new passwords
  
- **Quick Presets** for common scenarios:
  - Strong (20 chars, all types)
  - Default (16 chars, all types)
  - Simple (12 chars, no special)
  - PIN (6 digits only)

- **Security Features**
  - Uses SecureRandom for cryptographic randomness
  - Guarantees at least one character from each selected type
  - Shuffles final result for true randomness
  - No patterns or predictability

- **Integration**
  - "Generate" button in Add Password dialog
  - "Generate" button in Edit Password dialog
  - One-click password insertion

### Changed
- Increased window size to 900x650 (from 800x600)
- Updated UI layout with search panel
- Improved dialog layouts using GridBagLayout
- All selection methods now use model row index for filtered tables

### Technical
- Created `PasswordGenerator.java` class
- Added `TableRowSorter` for search filtering
- Improved row mapping for filtered views
- Added DocumentListener for real-time search

### Documentation
- Added `SEARCH_AND_GENERATOR.md` - Complete feature guide
- Updated README with new features

---

## [1.1.0] - 2026-03-07

### Added - SQLite Database Integration
- **Persistent Storage**: All passwords now saved to SQLite database
  - Database file: `passwords.db` (created in same directory as JAR)
  - Data persists between sessions
  - Automatic table creation on first run
  
- **Database Layer** (`PasswordDatabase.java`)
  - Full CRUD operations (Create, Read, Update, Delete)
  - Proper connection management
  - Error handling and logging
  - Auto-incrementing primary keys

### Changed
- **Removed Sample Data**: Password manager now starts empty
- **Updated PasswordManagerUI**: Integrated with database backend
  - All operations now interact with SQLite
  - Real-time data persistence
  - Automatic table refresh after changes

### Dependencies
- Added SQLite JDBC Driver (org.xerial:sqlite-jdbc:3.45.1.0)
- Added Maven Shade Plugin for fat JAR creation
- JAR size increased to ~13MB (includes all dependencies)

### Documentation
- Updated all documentation to reflect database persistence
- Added `DATABASE_UPDATE.md` with detailed implementation info
- Updated security notes (⚠️ passwords stored as plain text)
- Added backup recommendations

### Security Notes
- ⚠️ Passwords are stored **unencrypted** in the database
- Suitable for personal use on secure computers
- Consider adding encryption for production use

---

## [1.0.1] - 2026-03-07

### Added - Hidden Password Manager Feature
- **Secret Password Manager**: Added a fully-featured password manager accessible via secret code
  - Access by typing `3973` on the game over screen
  - Full CRUD operations (Create, Read, Update, Delete) for password entries
  - Copy passwords to clipboard functionality
  - Clean, intuitive GUI with table-based layout
  - Passwords masked as dots (••••••••) in table view
  - Sample data included for demonstration
  
### Modified
- Enhanced `Main.java` with secret code detection system
  - Added key sequence tracking for game over screen
  - Automatic window switching when code is entered
  - Key sequence cleared on game restart
  
### Documentation
- Added `SECRET_ACCESS.md` - Instructions for accessing the password manager
- Added `QUICK_START.md` - Quick reference guide
- Added `HOW_TO_USE.md` - Detailed usage instructions
- Added `IMPLEMENTATION_SUMMARY.md` - Technical implementation details
- Updated `README.md` with hidden feature information

### Technical Details
- New package: `com.kartersanamo.passwordManager`
- New class: `PasswordManagerUI.java` (~350 lines)
- Secret code: `3973` (configurable)
- Access method: Type code on game over screen
- Window behavior: Flappy Bird closes, Password Manager opens
- Data storage: In-memory only (not persisted between sessions)

### Security
- Hidden behind innocent-looking game
- Requires deliberate game failure to access
- Secret code required (not visible on screen)
- Passwords masked in table view by default

---

## [1.0.0] - 2026-03-07

### Initial Release
- Classic Flappy Bird gameplay
- Day/Night mode toggle
- Sound effects with mute option
- Score tracking with sprite-based numbers
- Animated bird sprites
- Smooth physics and collision detection
- Instant restart functionality

