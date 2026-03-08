# Quick Reference Card - Password Manager v1.2.0

## 🎮 Access
1. Run: `java -jar FlappyBird-1.0-SNAPSHOT.jar`
2. Press SPACE 10x (fail immediately)
3. Type: `3973`
4. Password manager opens!

---

## 🔍 Search

**Location**: Top of table

**Usage**:
- Type anything → See filtered results
- Click "Clear" → Show all

**Examples**:
- `gmail` → Gmail accounts
- `@work` → Work emails  
- `bank` → Banking sites

**Tips**:
- Case-insensitive
- Searches service, username, notes
- Real-time filtering

---

## 🔐 Password Generator

**Access**:
- Click "Add Password" → "Generate" button
- Click "Edit" → "Generate" button

**Options**:
- **Length**: 4-64 chars (slider)
- **Types**: Uppercase, Lowercase, Digits, Special
- **Regenerate**: New password anytime

**Quick Presets**:

| Preset | Length | Use For |
|--------|--------|---------|
| **Strong** | 20 | Banking, Email |
| **Default** | 16 | Most sites |
| **Simple** | 12 | No special chars |
| **PIN** | 6 | Numeric only |

**How to Use**:
1. Open generator
2. Click preset OR customize
3. Click "Use This Password"
4. Done!

---

## 💾 Database

**File**: `passwords.db`  
**Location**: Same directory as JAR  
**Backup**: `cp passwords.db backup.db`  
**Restore**: `cp backup.db passwords.db`

---

## ⌨️ Main Actions

| Action | How |
|--------|-----|
| **Add** | Click "Add Password" |
| **Edit** | Select row → "Edit" |
| **Delete** | Select row → "Delete" |
| **View** | Select row → "View Password" |
| **Copy** | Select row → "Copy Password" |
| **Search** | Type in search box |
| **Generate** | Click "Generate" in Add/Edit |

---

## 🔒 Security Tips

✅ **DO**:
- Use 16+ characters for important accounts
- Enable all character types
- Generate unique passwords per site
- Backup `passwords.db` regularly

❌ **DON'T**:
- Reuse passwords
- Use < 12 characters
- Share database file
- Disable all character types

---

## 📊 Password Strength

| Length | Strength | Use For |
|--------|----------|---------|
| 4-8 | Weak | ❌ Not recommended |
| 8-12 | Moderate | Low security |
| 13-16 | Strong | ✅ Most accounts |
| 17-20 | Very Strong | ✅ Important accounts |
| 21+ | Extreme | Master passwords |

---

## 🐛 Troubleshooting

**Q**: Search not working?  
**A**: Clear search box, type again

**Q**: Generator button missing?  
**A**: Look next to password field in Add/Edit

**Q**: Can't find password?  
**A**: Use search feature at top

**Q**: Database location?  
**A**: Same folder as JAR file

**Q**: Lost passwords?  
**A**: Check for `passwords.db` file, restore from backup

---

## 📱 Quick Workflow

### Add New Password
```
Add Password → Enter details → 
Generate → Pick preset → Use → Save
```

### Find Password
```
Type in search → Select → 
View Password OR Copy Password
```

### Update Password
```
Search → Select → Edit → 
Generate → Use → Save
```

---

## 🎯 Pro Tips

1. **Tag with notes** for better searching:
   - "Work", "Personal", "Banking", etc.

2. **Use search before adding** to avoid duplicates

3. **Copy-paste passwords** instead of typing

4. **Backup weekly**:
   ```bash
   cp passwords.db ~/Backups/passwords_$(date +%Y%m%d).db
   ```

5. **Strong passwords** for:
   - Email (can reset other accounts)
   - Banking/Financial
   - Work accounts

6. **Simple passwords** for:
   - Low-value sites
   - Sites that reject special chars

---

## 📞 Quick Help

| Need | Solution |
|------|----------|
| Strong password | Use "Strong" preset (20 chars) |
| Find account | Use search box |
| Backup data | Copy `passwords.db` file |
| Reset search | Click "Clear" button |
| New password | Click "Generate" button |

---

## ⚡ Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Type in search | Filter entries |
| Enter | Confirm dialog |
| Escape | Close dialog |
| Tab | Navigate fields |

---

## 📈 Version Info

**Version**: 1.2.0  
**Released**: March 7, 2026  
**Features**:
- ✅ SQLite Database
- ✅ Real-time Search  
- ✅ Password Generator
- ✅ 4 Quick Presets

---

## 🔗 More Info

- `SEARCH_AND_GENERATOR.md` - Detailed guide
- `HOW_TO_USE.md` - Complete manual
- `CHANGELOG.md` - Version history
- `README.md` - Project overview

---

**Remember**: 
- 🔒 Strong passwords = Strong security
- 🔍 Search makes life easier
- 💾 Backup prevents data loss
- 🔐 Unique passwords per site!

**Happy password managing!** 🎉

