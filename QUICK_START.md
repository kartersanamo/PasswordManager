# Quick Start Guide

## Running the Hidden Password Manager

### Step 1: Run the Game
```bash
java -jar target/FlappyBird-1.0-SNAPSHOT.jar
```

### Step 2: Trigger Game Over
- Press **SPACE** repeatedly to make the bird fly upward
- Hit the ceiling to trigger game over immediately
- You should see the "Game Over" screen

### Step 3: Enter Secret Code
- While on the game over screen, type: **3973**
- The numbers won't appear on screen
- The Flappy Bird window will close automatically
- The Password Manager will open in a new window

## Password Manager Usage

Once opened, you can:

1. **Add Password**
   - Click "Add Password" button
   - Fill in Service, Username, Password, and Notes
   - Click "Save"

2. **View Password**
   - Select a row in the table
   - Click "View Password" to see the actual password in a popup

3. **Edit Password**
   - Select a row in the table
   - Click "Edit"
   - Modify fields and click "Save"

4. **Delete Password**
   - Select a row in the table
   - Click "Delete"
   - Confirm deletion

5. **Copy Password**
   - Select a row in the table
   - Click "Copy Password"
   - Password is copied to your clipboard

## Sample Data

The password manager starts with an **empty database**. You'll need to add your own password entries using the "Add Password" button.

## Exiting

- Close the Password Manager window to exit
- Your passwords are **saved to a database file** (`passwords.db`)
- To return to Flappy Bird, run the game again
- **Your passwords persist between sessions** - they'll be loaded when you access the manager again

## Troubleshooting

**Q: I typed 3973 but nothing happened**
- Make sure you're on the game over screen (not during gameplay)
- Try typing the numbers slower
- Make sure you're using the number keys (not numpad if it's causing issues)

**Q: Can I change the secret code?**
- Yes, edit the `SECRET_CODE` constant in `Main.java` and rebuild

**Q: Are my passwords saved?**
- Yes! Passwords are stored in an SQLite database (`passwords.db`)
- The database file is created in the same directory as the JAR
- Your passwords persist between sessions
- **Backup the `passwords.db` file to preserve your data**

