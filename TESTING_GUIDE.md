# Testing Guide - Hidden Password Manager

## ✅ Pre-Test Checklist

Before testing, verify:
- [x] Java 22+ installed
- [x] JAR file exists at `target/FlappyBird-1.0-SNAPSHOT.jar`
- [x] All source files compiled without errors
- [x] Documentation files created

## 🧪 Test Scenarios

### Test 1: Basic Access (Critical Path)
**Objective**: Verify the secret code works on game over screen

**Steps**:
1. Run the JAR: `java -jar target/FlappyBird-1.0-SNAPSHOT.jar`
2. Press SPACE key 10 times rapidly
3. Verify bird hits ceiling and game over screen appears
4. Type: `3` `9` `7` `3` (one at a time)
5. Verify Flappy Bird window closes
6. Verify Password Manager window opens

**Expected Result**: ✅ Password manager opens successfully

**Actual Result**: _____________

---

### Test 2: Password Manager UI
**Objective**: Verify all password manager features work

**Steps**:
1. Access password manager using Test 1
2. Verify sample data is loaded (3 entries visible)
3. Click on first row (example.com)
4. Click "View Password" button
5. Verify popup shows full password details
6. Click "Copy Password" button  
7. Paste into text editor (Cmd+V / Ctrl+V)
8. Verify password was copied correctly

**Expected Result**: ✅ All view and copy functions work

**Actual Result**: _____________

---

### Test 3: Add Password
**Objective**: Verify adding new passwords works

**Steps**:
1. Access password manager
2. Click "Add Password" button
3. Enter test data:
   - Service: `test.com`
   - Username: `testuser`
   - Password: `testpass123`
   - Notes: `Test entry`
4. Click "Save"
5. Verify new row appears in table
6. Select the new row and click "View Password"
7. Verify all data was saved correctly

**Expected Result**: ✅ New password added successfully

**Actual Result**: _____________

---

### Test 4: Edit Password
**Objective**: Verify editing passwords works

**Steps**:
1. Access password manager
2. Select first row (example.com)
3. Click "Edit" button
4. Change username to: `newemail@example.com`
5. Click "Save"
6. Verify table shows updated username
7. Click "View Password" to confirm

**Expected Result**: ✅ Password edited successfully

**Actual Result**: _____________

---

### Test 5: Delete Password
**Objective**: Verify deleting passwords works

**Steps**:
1. Access password manager
2. Note the total number of rows
3. Select last row
4. Click "Delete" button
5. Click "Yes" in confirmation dialog
6. Verify row count decreased by 1
7. Verify deleted entry is gone

**Expected Result**: ✅ Password deleted successfully

**Actual Result**: _____________

---

### Test 6: Secret Code During Gameplay
**Objective**: Verify code doesn't trigger during active game

**Steps**:
1. Run the game
2. Press SPACE once to start
3. During gameplay, type: `3973`
4. Verify nothing happens (game continues)
5. Fail the game (hit pipe or ground)
6. On game over screen, type: `3973`
7. Verify password manager opens

**Expected Result**: ✅ Code only works on game over screen

**Actual Result**: _____________

---

### Test 7: Wrong Secret Code
**Objective**: Verify wrong codes don't open password manager

**Steps**:
1. Run the game and fail immediately
2. On game over screen, type: `1234`
3. Verify nothing happens
4. Type: `5678`
5. Verify nothing happens
6. Type: `3973`
7. Verify password manager opens

**Expected Result**: ✅ Only correct code works

**Actual Result**: _____________

---

### Test 8: Game Restart
**Objective**: Verify restarting game clears key sequence

**Steps**:
1. Run game and fail
2. On game over screen, type: `39` (incomplete code)
3. Press SPACE to restart game
4. Fail again to reach game over screen
5. Type: `73` (rest of code - should NOT work)
6. Verify password manager doesn't open
7. Type: `3973` (complete code)
8. Verify password manager opens

**Expected Result**: ✅ Restart clears key sequence

**Actual Result**: _____________

---

### Test 9: Multiple Access
**Objective**: Verify password manager can be accessed multiple times

**Steps**:
1. Access password manager
2. Add a test password entry
3. Close password manager window
4. Run game again and fail
5. Type: `3973` to access password manager again
6. Verify sample data is loaded (test entry is gone)

**Expected Result**: ✅ Can access multiple times, data resets

**Actual Result**: _____________

---

### Test 10: Clipboard Functionality
**Objective**: Verify copy to clipboard works correctly

**Steps**:
1. Access password manager
2. Select example.com entry
3. Click "Copy Password"
4. Open a text editor
5. Paste (Cmd+V or Ctrl+V)
6. Verify "********" appears (the sample password)
7. Select github.com entry
8. Click "Copy Password"
9. Paste in text editor
10. Verify new password replaced old one

**Expected Result**: ✅ Clipboard updates correctly

**Actual Result**: _____________

---

## 📊 Test Results Summary

| Test | Status | Notes |
|------|--------|-------|
| 1. Basic Access | ☐ Pass ☐ Fail | |
| 2. Password Manager UI | ☐ Pass ☐ Fail | |
| 3. Add Password | ☐ Pass ☐ Fail | |
| 4. Edit Password | ☐ Pass ☐ Fail | |
| 5. Delete Password | ☐ Pass ☐ Fail | |
| 6. Code During Gameplay | ☐ Pass ☐ Fail | |
| 7. Wrong Secret Code | ☐ Pass ☐ Fail | |
| 8. Game Restart | ☐ Pass ☐ Fail | |
| 9. Multiple Access | ☐ Pass ☐ Fail | |
| 10. Clipboard | ☐ Pass ☐ Fail | |

**Overall Result**: _____ / 10 tests passed

---

## 🐛 Known Issues

_Document any issues found during testing:_

1. 
2. 
3. 

---

## 💡 Suggestions for Improvement

_Document any ideas for enhancement:_

1. 
2. 
3. 

---

## ✅ Final Sign-off

**Tester**: _________________  
**Date**: _________________  
**Build Version**: 1.0-SNAPSHOT  
**Platform**: _________________  
**Overall Status**: ☐ Approved ☐ Needs Work  

**Comments**:
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________

---

## 🔧 Quick Commands

### Build Project
```bash
mvn clean package
```

### Run Game
```bash
java -jar target/FlappyBird-1.0-SNAPSHOT.jar
```

### Quick Test Access
1. Launch game
2. Spam SPACE 10x
3. Type `3973`
4. Test features

---

**Testing completed on**: _______________

