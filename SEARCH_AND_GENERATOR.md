# Search & Password Generator Features

## 🔍 Search Feature

### Overview
The password manager now includes a powerful real-time search feature that filters across all fields in your password table.

### How to Use

1. **Locate the Search Bar**
   - At the top of the password table
   - Contains a text field and "Clear" button

2. **Search Across All Fields**
   - Type anything in the search box
   - Search is **case-insensitive**
   - Searches across:
     - Website/Service names
     - Usernames/Emails
     - Notes (but not passwords, since they're masked)
   
3. **Real-Time Filtering**
   - Results update as you type
   - No need to press Enter
   - Table instantly shows only matching entries

4. **Clear Search**
   - Click the "Clear" button
   - Or delete all text from the search field
   - Table shows all entries again

### Examples

**Search by service:**
- Type: `gmail` → Shows all Gmail-related passwords
- Type: `github` → Shows GitHub accounts

**Search by username:**
- Type: `john@` → Shows all entries with "john@" in username
- Type: `myuser` → Shows entries with that username

**Search by notes:**
- Type: `work` → Shows all work-related passwords
- Type: `personal` → Shows personal accounts

**Partial matches work:**
- Type: `ama` → Finds "amazon.com"
- Type: `@gmail` → Finds all Gmail addresses

---

## 🔐 Password Generator

### Overview
Generate strong, random passwords with customizable options - just like NordPass!

### Accessing the Generator

**From Add Password Dialog:**
1. Click "Add Password" button
2. Click the "Generate" button next to the password field
3. Password generator dialog opens

**From Edit Password Dialog:**
1. Select a password entry
2. Click "Edit"
3. Click the "Generate" button next to the password field
4. Password generator dialog opens

### Generator Features

#### 1. **Custom Length Slider**
- Adjustable from 4 to 64 characters
- Default: 16 characters
- Slider shows real-time value
- Major ticks every 10 characters

#### 2. **Character Type Options**
Choose which character types to include:

- ✅ **Uppercase (A-Z)** - Capital letters
- ✅ **Lowercase (a-z)** - Small letters
- ✅ **Digits (0-9)** - Numbers
- ✅ **Special (!@#$%^&*...)** - Special characters

**All enabled by default** for maximum security.

#### 3. **Live Preview**
- See the generated password in real-time
- Monospaced font for clarity
- Password displayed in center preview box

#### 4. **Regenerate Button** 🔄
- Click to generate a new password
- Uses current slider and checkbox settings
- Generates instantly

#### 5. **Quick Presets**
One-click password generation for common use cases:

**Strong (20 chars)**
- Length: 20 characters
- All character types enabled
- Maximum security
- Best for: Banking, email, important accounts

**Default (16 chars)**
- Length: 16 characters
- All character types enabled
- Good balance of security and usability
- Best for: Most websites and services

**Simple (12 chars, no special)**
- Length: 12 characters
- Uppercase, lowercase, and digits only
- No special characters
- Best for: Sites that don't accept special chars

**PIN (6 digits)**
- Length: 6 characters
- Digits only (0-9)
- Best for: PIN codes, numeric passwords

### How to Use

#### Method 1: Custom Generation
1. Open password generator
2. Adjust length slider to desired value
3. Check/uncheck character type options
4. Click "Regenerate" to create new passwords
5. When satisfied, click "Use This Password"

#### Method 2: Quick Presets
1. Open password generator
2. Click one of the preset buttons
3. Password is generated instantly
4. Click "Regenerate" if you want a different one
5. Click "Use This Password" to use it

### Password Strength Guide

**Weak (< 8 characters)**
- Not recommended
- Easy to crack

**Moderate (8-12 characters)**
- Acceptable for low-security accounts
- Use multiple character types

**Strong (13-16 characters)**
- Good for most accounts
- All character types recommended

**Very Strong (17-20 characters)**
- Excellent for important accounts
- Maximum security

**Extreme (21+ characters)**
- Overkill for most uses
- Best for master passwords

### Security Features

1. **SecureRandom**
   - Uses Java's `SecureRandom` class
   - Cryptographically secure random generation
   - Not predictable

2. **Guaranteed Character Types**
   - At least one character from each selected type
   - Prevents weak passwords
   - Shuffled after generation

3. **No Patterns**
   - Completely random order
   - No sequential characters
   - No dictionary words

### Tips & Best Practices

**DO:**
- ✅ Use at least 16 characters for important accounts
- ✅ Enable all character types when possible
- ✅ Generate unique passwords for each account
- ✅ Regenerate if you don't like the result
- ✅ Use "Strong" preset for banking/email

**DON'T:**
- ❌ Reuse passwords across multiple sites
- ❌ Use passwords shorter than 12 characters
- ❌ Disable all character types
- ❌ Write passwords down on paper

### Examples

**Generated Passwords:**

**Strong (20):**
```
K9#mP2$xQ5!vN8@dL3&w
```

**Default (16):**
```
aB3#kL9@mN2$pR7!
```

**Simple (12, no special):**
```
Hj8KmP2nQ5Lv
```

**PIN (6):**
```
847392
```

### Customization Examples

**For maximum security:**
- Length: 32+ characters
- All options enabled
- Perfect for master password

**For compatibility:**
- Length: 12-16 characters
- Disable special characters if site doesn't allow
- Enable uppercase, lowercase, digits

**For memorable (but still strong):**
- Length: 12-14 characters
- Enable only lowercase and digits
- Easier to type on mobile

### Keyboard Shortcuts

- **Enter**: Use the generated password (when focused on dialog)
- **Escape**: Cancel and close dialog
- **Space**: Regenerate (when Regenerate button is focused)

---

## 🎯 Combined Workflow Example

### Adding a New Password with Search & Generator

1. **Click "Add Password"**
2. Enter website: `amazon.com`
3. Enter username: `john@example.com`
4. **Click "Generate"** button
5. In generator:
   - Click "Strong (20 chars)" preset
   - Click "Use This Password"
6. Add notes: `Shopping account`
7. Click "Save"
8. **Search for it:**
   - Type `amazon` in search box
   - Entry appears instantly!

### Updating a Password

1. **Search for the account:**
   - Type service name in search box
   - Entry is filtered and visible
2. Select the entry
3. Click "Edit"
4. **Click "Generate"** for new password
5. Choose preset or customize
6. Click "Use This Password"
7. Click "Save"

---

## 📊 Feature Comparison

### Before v1.2.0
- ❌ No search functionality
- ❌ Manual password creation only
- ❌ Hard to find specific entries
- ❌ Weak passwords possible

### After v1.2.0
- ✅ Real-time search across all fields
- ✅ Advanced password generator
- ✅ Instant filtering
- ✅ Strong passwords guaranteed
- ✅ Quick presets for convenience
- ✅ Customizable generation options

---

## 🔧 Technical Details

### Search Implementation
- Uses `TableRowSorter` for efficient filtering
- Case-insensitive regex matching
- Searches all visible columns
- Updates in real-time via DocumentListener

### Generator Implementation
- Uses `SecureRandom` for cryptographic security
- Builder pattern for options
- Guarantees at least one character from each type
- Shuffles final result for randomness

### Character Sets
- **Uppercase:** 26 characters (A-Z)
- **Lowercase:** 26 characters (a-z)
- **Digits:** 10 characters (0-9)
- **Special:** 24 characters (!@#$%^&*()-_=+[]{}|;:,.<>?)

Total possible combinations with all types:
- 16 chars: ~3.2 × 10²⁸ combinations
- 20 chars: ~2.7 × 10³⁵ combinations

Time to crack (at 1 trillion guesses/second):
- 16 chars: Billions of years
- 20 chars: Longer than age of universe

---

## 🎉 Summary

**Search Feature:**
- ✅ Real-time filtering
- ✅ Case-insensitive
- ✅ Searches all fields
- ✅ One-click clear

**Password Generator:**
- ✅ Customizable length (4-64)
- ✅ 4 character type options
- ✅ 4 quick presets
- ✅ Secure random generation
- ✅ Live preview
- ✅ Regenerate on demand

**Together they make password management:**
- Faster ⚡
- Easier 😊
- More secure 🔒
- More powerful 💪

