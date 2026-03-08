# How to Use the Hidden Password Manager

## 🎮 Quick Access Guide

### Method 1: Fail Immediately (Recommended)
This is the fastest way to access the password manager:

1. **Launch the game**
   ```bash
   java -jar target/FlappyBird-1.0-SNAPSHOT.jar
   ```

2. **Press SPACE rapidly** (5-10 times)
   - This will make the bird fly straight up into the ceiling
   - You'll hit the top of the screen immediately
   - Game over screen appears instantly

3. **Type the secret code: 3973**
   - Just type the numbers - they won't appear on screen
   - The Flappy Bird window will close automatically
   - The Password Manager will open in a new window

### Method 2: Fail During Normal Play
1. Launch the game
2. Play normally and fail (hit a pipe, ground, or ceiling)
3. On the game over screen, type: **3973**
4. Password manager opens

## 🔐 Using the Password Manager

Once the password manager is open, you have a full-featured interface:

### Adding a New Password
1. Click the **"Add Password"** button
2. Fill in the form:
   - **Website/Service**: e.g., "gmail.com"
   - **Username**: Your username or email
   - **Password**: Your actual password
   - **Notes**: Optional notes (e.g., "Personal account")
3. Click **"Save"**

### Viewing a Password
1. Click on any row in the table
2. Click the **"View Password"** button
3. A popup will show the full details including the actual password

### Editing a Password
1. Select a row in the table
2. Click **"Edit"**
3. Modify any fields you want
4. Click **"Save"**

### Deleting a Password
1. Select a row in the table
2. Click **"Delete"**
3. Confirm the deletion

### Copying a Password
1. Select a row in the table
2. Click **"Copy Password"**
3. The password is now in your clipboard
4. Paste it wherever you need (Cmd+V on Mac, Ctrl+V on Windows)

## 💡 Tips & Tricks

### Fast Access
- **Pro tip**: Press SPACE 10 times rapidly as soon as the game starts
- The bird will shoot up and hit the ceiling in about 1 second
- Game over screen appears instantly
- Type 3973 and you're in!

### Security
- The password manager is completely hidden during normal gameplay
- Only accessible when you deliberately fail AND know the code
- Perfect for hiding your password manager in plain sight

### Sample Data
The password manager starts with an **empty database**.
- No pre-loaded examples
- You need to add your own passwords
- All passwords you add are saved permanently to the database

## ⚠️ Important Notes

### Data Persistence
- **Passwords ARE saved to disk**
- All data is stored in `passwords.db` SQLite database
- The database file is created in the same directory as the JAR
- When you close the password manager, your data is preserved
- **Your passwords persist between sessions** - they will be there when you reopen

### Returning to Flappy Bird
- Close the password manager window
- Run the game again to play or access the password manager again
- **Your passwords are saved** and will be loaded when you access the manager again

### Changing the Secret Code
If you want to change the code from 3973:
1. Edit `src/main/java/com/kartersanamo/flappyBird/Main.java`
2. Find line 24: `private static final String SECRET_CODE = "3973";`
3. Change "3973" to your desired code (any 4 digits)
4. Rebuild: `mvn clean package`

## 🎯 Example Workflow

```
1. Double-click FlappyBird JAR
   ↓
2. Window opens, press SPACE 10x rapidly
   ↓
3. Bird hits ceiling, game over screen appears
   ↓
4. Type: 3 9 7 3
   ↓
5. Password Manager opens!
   ↓
6. Add/view/edit your passwords
   ↓
7. Close window when done
```

Total time: **~3 seconds** from launch to password manager!

## 🛠️ Troubleshooting

**Problem**: I typed 3973 but nothing happened
- **Solution**: Make sure you're on the game over screen (red overlay)
- **Solution**: Type slower, one digit at a time
- **Solution**: Make sure the game window has focus (click on it first)

**Problem**: The password manager closed but I lost my data
- **Solution**: Data is now saved! Check for `passwords.db` in the same directory as the JAR
- **Solution**: If the database file was deleted, your data is gone (backup important data!)

**Problem**: Where is my data stored?
- **Solution**: In a file called `passwords.db` in the same directory as the JAR file
- **Solution**: You can backup this file to preserve your passwords

**Problem**: I want to change the secret code
- **Solution**: See "Changing the Secret Code" section above

## 📱 Platform-Specific Notes

### macOS
- Works perfectly on macOS
- Use Cmd+V to paste passwords
- May need to allow Java in Security & Privacy settings

### Windows
- Use Ctrl+V to paste passwords
- Make sure Java is installed and in PATH

### Linux
- Should work on all desktop environments
- Requires Java 22+ installed

---

**Enjoy your hidden password manager! 🎉**

