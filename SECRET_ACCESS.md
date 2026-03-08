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
- **Persistent storage** - All entries are saved to an SQLite database

## Security Notes

- The secret code (`3973`) provides access to the password manager
- Passwords are displayed as dots (`••••••••`) in the table view
- Use the "View Password" button to see actual passwords
- The password manager remains open until you close the window
- **All passwords are saved to `passwords.db` and persist between sessions**
- To return to Flappy Bird, close the password manager and restart the game
- **Important**: Backup the `passwords.db` file to preserve your passwords

## Development

This feature is designed to hide your password manager behind a harmless-looking game. The secret code must be entered on the game over screen to prevent accidental access.

