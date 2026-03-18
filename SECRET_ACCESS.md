# Secret Password Manager Access

This Flappy Bird game contains a hidden password manager feature.

## How to Access

1. **Launch the game** - Run the Flappy Bird executable
2. **Fail immediately** - Press space repeatedly to make the bird fly into the top of the screen
3. **Enter the secret code** - Once on the game over screen, type: `3973`
4. **Access granted** - The Flappy Bird window will close and the Password Manager will open

## Password Manager Features

Once you've accessed the password manager, you can:

- **View passwords** - See all your stored credentials
- **Add new passwords** - Store new login information
- **Edit passwords** - Update existing entries
- **Delete passwords** - Remove entries you no longer need
- **Copy passwords** - Quick copy to clipboard
- **Toggle View** - Reveal/hide passwords directly in the table
- **Change Secret Code** - Update your secret code with automatic password migration
- **Persistent storage** - All entries are saved to MySQL
- **Auto proxy startup** - Cloudflare TCP access is started automatically when possible

## Changing Your Secret Code

### IMPORTANT: Your Passwords Are Safe!

**You can now change your secret code without losing your passwords!** The system automatically migrates all data.

### Method 1: Via Password Manager UI (Recommended)

1. Open Password Manager (enter code `3973` on game over screen)
2. Click "Change Secret Code" button
3. Enter current code, new code, and confirm
4. All passwords automatically re-encrypted
5. **Remember to update** `SECRET_CODE` in `Main.java`

### Method 2: Via Code (Auto-Migration)

1. Edit `Main.java`: Change `SECRET_CODE = "3973"` to your new code
2. Launch game and enter NEW code
3. System detects change and prompts for OLD code
4. Enter old code (`3973`) when prompted
5. Passwords automatically migrated to new code

## Security Notes

- The secret code (`3973`) provides access to the password manager
- **Secret code hash stored** - Only an Argon2 hash is saved (unbreakable)
- **AES-256-GCM encryption** - Military-grade password protection
- **Change code anytime** - Migration preserves all passwords
- Passwords are displayed as dots (`••••••••`) in the table view
- Use "Toggle View" button to reveal passwords in the table
- The password manager remains open until you close the window
- **All passwords are saved in MySQL and persist between sessions**
- To return to Flappy Bird, close the password manager and restart the game
- **Important**: Backup/export your MySQL data to preserve your passwords

## Troubleshooting


### Cloudflare Access / Database Connection
The app now attempts to start Cloudflare access automatically when the hidden
password manager opens.

If Cloudflare Access needs an interactive login or `cloudflared` is not on your
PATH, run this manually and then reopen the password manager:

`cloudflared access tcp --hostname mysql.kartersanamo.com --url localhost:13306`

### "Password Migration Required" Dialog
Appears when you change SECRET_CODE in Main.java:
- Enter your OLD secret code to decrypt existing passwords
- System re-encrypts everything with the NEW code
- If you forgot the old code, you'll need to start fresh

### "Corrupted Password Entries" Dialog  
Appears if you have passwords from the old encryption system:
- Click "Yes" to remove corrupted entries
- Re-add your passwords (they'll use the new secure system)

## Development

This feature is designed to hide your password manager behind a harmless-looking game. The secret code must be entered on the game over screen to prevent accidental access.