# Group Booking Bug Fixes

## Issues Fixed

### Issue 1: Leave Button Not Decreasing Member Count
**Problem:** When a member clicked "Leave", the UI showed a success message but the member count didn't update.

**Root Cause:** The `leaveGroup()` function was closing the modal immediately after leaving, without refreshing the group list to show the updated member count.

**Solution:**
- Updated `leaveGroup(groupId, apartmentId)` to accept `apartmentId` parameter
- After successfully leaving, the function now calls `showGroupModal(apartmentId)` to refresh the entire group list
- This shows the updated member count and group status immediately

**Files Modified:**
- `src/main/resources/static/js/app.js`:
  - `leaveGroup()` function now refreshes the group modal after leaving
  - All calls to `leaveGroup()` now pass the `apartmentId` parameter

---

### Issue 2: Pay Button Not Appearing After 4th Member Joins
**Problem:** When the 4th member joined a group, the Pay Now button didn't appear even though the group had 4/4 members.

**Root Cause:** Two issues:
1. **Backend:** The group status wasn't being updated from `FORMING` to `READY` when the 4th member joined
2. **Frontend:** After joining, the UI wasn't refreshing to show the updated status and Pay button

**Backend Fix:**
In `RoommateGroupService.joinGroup()`:
- Added `memberRepository.flush()` after saving new member to ensure the database commit happens immediately
- Changed member count check to query the database directly: `memberRepository.findByGroup_GroupId(group.getGroupId()).size()`
- Added `groupRepository.flush()` after updating status to READY
- This ensures the transaction is committed and the status change is persisted immediately

**Frontend Fix:**
In `joinExistingGroup()`:
- Updated to accept `apartmentId` parameter
- After successfully joining, now calls `showGroupModal(apartmentId)` instead of just `viewMyGroup()`
- This refreshes the entire group modal, ensuring:
  - Updated member count is displayed (e.g., 4/4)
  - Group status badge shows "READY" in green
  - Pay Now button appears if group is full and ready
  - All group members can see the Pay button

**Files Modified:**
- `src/main/java/com/example/project/aloy/service/RoommateGroupService.java`:
  - Added flush operations to ensure immediate database commits
  - Fixed member count detection logic
- `src/main/resources/static/js/app.js`:
  - `joinExistingGroup()` now refreshes the entire modal after joining
  - All calls to `joinExistingGroup()` now pass `apartmentId`

---

## Testing Instructions

### Test 1: Verify Leave Functionality Decreases Member Count
1. Login as Alice (alice.tenant@example.com)
2. Go to Apartment 9 and click "Book in a Group"
3. Join the existing group (Invite Code: CFW513)
4. You should see "4/4 members" with a Pay button
5. Click "Leave" button
6. **Expected:** Group modal refreshes and shows "3/4 members" with no Pay button
7. Verify the group status changes from READY to FORMING

### Test 2: Verify Pay Button Appears When 4th Member Joins
1. Create a fresh test by deleting existing groups:
   ```sql
   DELETE FROM roommate_group_members WHERE group_id=2;
   DELETE FROM roommate_group WHERE group_id=2;
   ```
2. Login as Alice and create a new group for Apartment 9
3. Copy the invite code
4. Login as Bob and join using the invite code → Should show 2/4 members
5. Login as Charlie and join → Should show 3/4 members
6. Login as David and join → **Expected:**
   - Modal automatically refreshes
   - Shows 4/4 members
   - Status badge shows "READY" (green)
   - **Pay Now button appears** for all 4 members
7. Any of the 4 members can click "Pay Now & Book Apartment"

### Test 3: Verify Leave and Re-join Flow
1. With a group of 4 members (READY status):
2. One member clicks "Leave"
3. **Expected:** 
   - Member count drops to 3/4
   - Status changes from READY to FORMING
   - Pay button disappears for remaining members
4. A new person joins (or the same person re-joins)
5. **Expected:**
   - Member count increases to 4/4
   - Status changes to READY
   - Pay button appears again

---

## Technical Details

### Database Schema
```sql
-- Group status must be READY for Pay button to appear
SELECT * FROM roommate_group WHERE group_id=2;
-- status should be 'READY' when member_count = 4

-- Member count should match database records
SELECT COUNT(*) FROM roommate_group_members WHERE group_id=2;
-- Should return 4 when group is READY
```

### API Endpoints Behavior
- `POST /api/groups/join`: Now returns updated group with correct status
- `POST /api/groups/{groupId}/leave`: Deletes member and updates status if needed
- `GET /api/groups/{groupId}`: Returns current group with all members and status

### Frontend Logic
```javascript
// Pay button only shows when BOTH conditions are true:
if (isFull && isReady) {
    // isFull: memberCount >= 4
    // isReady: group.status === 'READY'
    // Show Pay Now button
}
```

---

## Current Database State
```
Group ID: 2
Apartment: 9 (Baridhara)
Status: READY
Invite Code: CFW513
Members: 4/4
```

All 4 test accounts (Alice, Bob, Charlie, David) can now see the Pay button and proceed with booking.

---

## Files Changed Summary
1. **Backend:** `RoommateGroupService.java` - Fixed status update logic with flush operations
2. **Frontend:** `app.js` - Fixed UI refresh after leave/join operations

Both issues are now resolved! 🎉
