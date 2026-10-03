# Settings: backup, import/export, storage, security, recovery & Health Connect, wearable

Scope: Settings → Backup, the Export & back up and Import sheets, Storage, Privacy & security and Wearable (`ui/settings/` SettingsBackupPage, SettingsBackupPassword, ImportDialog, ExportAttempt, RestoreRestart, SettingsStoragePage, SettingsSecurityPage, SettingsRecoveryPage/Components, HealthConnectViewModel, SettingsDialogs, plus the backup, restore and import halves of SettingsViewModel), `ui/security/AppLockScreen`, and the services behind them (BackupRepository, AutoBackupWorker, HealthConnectManager.probeSignalFlow, WearConnection). 16 raised, 16 kept (14 revised), 0 dropped. Three sub-proposals were cut and are listed at the end.

Row by row, these pages are tidy on the 2026-09-26 kit. The debt is in telling the user what happened and what is safe. Restore is kept apart from the Backup page, can't be undone, doesn't say what the file contains, and shows nothing while it runs. Imports have no undo, and their detail disappears with a four-second snackbar. Health Connect rows report that data exists but not how old it is or that a grant was refused, the Wear OS app is invisible on the phone, and weekly backup and folder failures are only visible inside Settings → Backup. Most fixes reuse what already exists: the feed, the one snackbar, the export dialog's progress pattern and the auto-backup slots.

## Findings

### SED-04 · Back up, restore and import show nothing while they run, and leaving mid-restore silently cancels it
missing-state · impact high · effort M · Backup page "Back up now"; restore confirm; Import sheet

- **Now:** `importData`, `restore` and `backupNow` have no busy state ([SettingsViewModel.kt:620-635](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L620-L635), [:765-797](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L765-L797), [:1021-1038](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L1021-L1038)). "Back up now" stays enabled and unchanged while a ZIP of the database and every photo is written ([SettingsBackupPage.kt:66-72](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPage.kt#L66-L72)). The restore confirm closes on tap (`viewModel.restoreDatabase(uri); onClearRestore()`, [SettingsScreen.kt:449](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L449)), and nothing is drawn until `RestoreRestart.relaunch` calls `Runtime.exit(0)` ([RestoreRestart.kt:28-33](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/RestoreRestart.kt#L28-L33)). The restore runs on the Settings ViewModel's scope, so pressing Back in that gap cancels it and discards the staged set without any message ([SettingsViewModel.kt:771-775](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L771-L775)). A found-file import closes the sheet on tap ([ImportDialog.kt:66](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/ImportDialog.kt#L66)), and the only feedback is a snackbar when it ends. The patterns this needs already exist: the export dialog blocks dismissal and draws `SettingsProgressBar` ([SettingsScreen.kt:458-483](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L458-L483)), and the restore-password dialog has a busy label, "Opening" ([SettingsBackupPassword.kt:175](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPassword.kt#L175)).
- **Why it matters:** During a restore the app seems to do nothing and then vanishes, which reads as a crash. A user who gives up waiting and presses Back has cancelled the restore they just confirmed, and nothing says so. A second tap on "Back up now" starts a second backup.
- **Proposal:** Add one busy `StateFlow` per operation in `SettingsViewModel`.
  - **Restore:** keep the `SettingsConfirmDialog` open in a busy state (`confirmEnabled = false`, label "Restoring", body "Avex restarts when it's done"), with `onDismissRequest = {}` as the export dialog has, so Back can't cancel it.
  - **Back up now:** label "Backing up" with `enabled = false`, and the status row reads "Backing up".
  - **Import:** keep the sheet open. The tapped found-file row's supporting line reads "Importing", then the result. MAP already uses that pattern for Wearable's import rows ([MAP.md:582-584](../../../.claude/design/MAP.md#L582-L584)).

  Revised: the restore dialog now blocks dismissal, which closes the silent cancel. The 2026-09-26 audit suggested "a blocking Restoring… state" as part of its P1 fix ([04-settings-security-notifications.md:17-22](../2026-09-26/04-settings-security-notifications.md)). The cancellation half shipped; this half did not. For the bug list, not this pass: two overlapping `autoBackupWithFolderStatus` runs write the same temp file with no lock ([BackupRepository.kt:523](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L523)).
- **Doctrine:** §12 allows loading feedback for real latency ([DESIGN.md:307](../../../.claude/DESIGN.md#L307)), and zipping a photo gallery is real latency. No spinner is added: labels and rows change, as in the existing export and password dialogs. No success toast either (§12).

### SED-01 · Restore isn't on the Backup page, and two different actions are both called "Back up now"
discoverability · impact high · effort M · Settings → Backup; Export & back up sheet; Settings search

- **Now:** The Backup page has the auto-backup switch, the status row, the folder row, the password section and "Back up now", and no restore ([SettingsBackupPage.kt:43-75](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPage.kt#L43-L75)). Both restore paths live in the "Export & back up" sheet: "Restore from a file", and "Restore an auto-backup" with its kept-copies picker and confirm ([SettingsDialogs.kt:86-103](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsDialogs.kt#L86-L103), [:118-173](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsDialogs.kt#L118-L173)). That sheet opens from a root row named "Export data". The sheet has its own "Back up now" ([SettingsDialogs.kt:92](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsDialogs.kt#L92)), which opens a save-as picker ([SettingsScreen.kt:359](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L359)). The page's "Back up now" writes the internal slot and the folder instead. The search entry "Backup & restore" opens the sheet, not the page ([SettingsScreen.kt:67](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L67)). The Export row's caption still lists "full backup" ([SettingsMainList.kt:104](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L104)), the name the JSON export lost on 2026-09-07 because it can't be restored ([MAP.md:855](../../../.claude/design/MAP.md#L855)).
- **Why it matters:** Someone who has lost data opens Settings → Backup, the obvious place, and finds no way back. Two buttons with the same label and different behaviour leave the user unsure which copy exists where.
- **Proposal:** Make the Backup page the one home for restore.
  - **Backup page:** add a "Restore" group with two `SettingsActionRow`s. "Restore an auto-backup" ("3 kept · newest Sep 28") opens the existing picker and confirm, moved out of `DataExportDialog` into a shared composable. "Restore from a file" uses the existing `restoreLauncher`, passed in as a callback. The group footer reads "Restoring replaces all current data and restarts Avex."
  - **Export sheet:** rename its save-as row "Save a backup file". Replace its two restore rows with one `SettingsNavigationRow`, "Restore", that opens the Backup page.
  - **Search:** point "Backup & restore" at `SettingsPage.Backup`.
  - **Root caption:** change the Export row's caption to "Sessions · PRs · cardio · PDF".

  Revised: the finder made `BackupStatusRow` the restore entry. That was cut (see the table at the end), so the status row stays passive and restore gets its own labelled rows.
- **Doctrine:** §3 Settings ("split a dense area into focused sub-pages") and §4.3 one home ([DESIGN.md:87](../../../.claude/DESIGN.md#L87), [:97](../../../.claude/DESIGN.md#L97)). DECISIONS 2026-09-26 keeps Export as a sheet, and it stays one ([DECISIONS.md:32-35](../../../.claude/design/DECISIONS.md#L32-L35)). The picker and confirm behaviour from MAP 2026-09-27 move unchanged ([MAP.md:41-42](../../../.claude/design/MAP.md#L41-L42)). The additions are rows, not buttons, so this is not a FAILURES "Button wall". The Import sheet's found-backup rows still route to the same confirm.

### SED-02 · Restore can't be undone, and the app leaves the safety copy to the user
ux-friction · impact high · effort M · Restore confirm (picked file, found backup, auto-backup)

- **Now:** The confirm says "It can't be undone, so back up first if you're unsure" ([SettingsScreen.kt:442-446](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L442-L446)). `RestoreApply` keeps its `.prerestore` snapshots only until the restored database validates, then retires them ([RestoreApply.kt:46-49](../../../forge-android/app/src/main/java/com/forge/app/RestoreApply.kt#L46-L49), [:250](../../../forge-android/app/src/main/java/com/forge/app/RestoreApply.kt#L250)). Nothing the user can reach keeps the data a restore replaced. The app already writes verified, rotating internal copies (`autoBackupWithFolderStatus`, [BackupRepository.kt:520-576](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L520-L576)) and has a picker that restores any of them.
- **Why it matters:** Picking the wrong file or an older copy wipes everything logged since, with no way back. The app could keep that copy itself, but asks the user to remember to make one.
- **Proposal:**
  - Before staging, write the current data to a dedicated `forge_auto_backup.prerestore.zip` through the same snapshot, `writeBackupZip` and `verifyBackup` path. A separate slot means it never pushes out a weekly copy.
  - The auto-backup picker lists it as "Before restore · Oct 3".
  - The confirm body becomes "Your current data is kept as a copy you can restore from Backup."
  - If the copy can't be written, the confirm says so and still lets the user go ahead: "There isn't room to keep a copy of your current data, so this can't be undone." No space is the likely cause, since a restore also stages a full copy.
  - Include the slot in `deleteLocalCopies` (factory reset) and in Storage's "Local backup" sum.
  - The restore notice ([MainActivity.kt:387](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L387)) can name the copy.

  Revised: the low-space path and the factory-reset and Storage accounting were added.
- **Doctrine:** §12: undo over confirm, and dialogs only for irreversible acts ([DESIGN.md:323](../../../.claude/DESIGN.md#L323)). The confirm stays, because replacing everything still needs consent, but the act becomes reversible. No new dialog or banner. Nothing in SETTLED covers restore safety, and no audit tracks it.

### SED-07 · Once Health Connect has refused a permission, "Connect" does nothing
missing-state · impact medium · effort S · Settings → Wearable, signal rows

- **Now:** All ten permission launchers ignore the granted set they return and only refresh ([SettingsRecoveryPage.kt:53-84](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryPage.kt#L53-L84)). A refused row keeps its drawn "Connect" pill and the same explainer ([SettingsRecoveryComponents.kt:36-50](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryComponents.kt#L36-L50)). After an app has been refused twice, Health Connect stops showing its consent screen. From then on Connect returns at once and nothing changes. The route that still works, "Manage in Health Connect", is the last row of a long page ([SettingsRecoveryPage.kt:296-305](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryPage.kt#L296-L305)). Only the weight-history grant tells "refused" apart from "never asked" ([AUDIT_DEFERRED.md:238](../../AUDIT_DEFERRED.md)).
- **Why it matters:** A user who said no by mistake, or has changed their mind, taps Connect repeatedly, gets no response, and concludes the integration is broken.
- **Proposal:**
  - In each launcher callback, check the returned set against the permissions that row's `connected` state depends on. For Sleep & heart rate that is `viewModel.permissions`, not the HRV extras.
  - If any are missing, set a per-signal `declined` flag in `HealthConnectViewModel.UiState`.
  - A declined row reads "Not allowed in Health Connect. Allow it there", and its drawn pill reads "Allow". The whole-row tap calls `openHealthConnectSettings(context)` instead of asking again.
  - Clear the flag when a refresh finds the grant.

  Revised: the check covers only the permissions the row's state depends on, so a partial grant that skips optional extras doesn't read as a refusal.
- **Doctrine:** DECISIONS 2026-07-27 made this call for notifications: open the OS screen instead of asking again, because "that keeps working after any number of denials, which a re-request does not" ([DECISIONS.md:364-365](../../../.claude/design/DECISIONS.md#L364-L365)). §2③: the whole row is the target, with one drawn pill and no nested tap. The wording never says "unsupported" (§12).

### SED-10 · Backup, Wearable and Storage rows on the Settings root show fixed captions instead of their live value
consistency · impact medium · effort S · Settings root, Data and Coach & recovery groups

- **Now:** `rowSubtitle` gives Appearance, Format, Notifications, Security, Program and Coach their live state ([SettingsMainList.kt:270-308](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L270-L308)). Backup always reads "Weekly copy · back up now", Wearable "Health Connect · sleep, weight, workouts" and Storage "Space used · clear cache" ([:309](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L309), [:317-318](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L317-L318)). The VM already holds `autoBackupSavedAt`, `autoBackupFailed` and `noBackupWarning` ([SettingsViewModel.kt:823-832](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L823-L832)). The Wearable page already computes "n of 9 connected" ([SettingsRecoveryPage.kt:133](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryPage.kt#L133)), and the storage total is one `refreshStorage()` call away.
- **Why it matters:** The root list is where a user would notice that backups failed or never ran, or that no signal is connected. Today these rows read the same on a healthy setup and on a broken one.
- **Proposal:**
  - **Backup:** "Last backup Sep 28", "Last backup failed" or "No backup yet". The icon tile takes `TileTone.Danger` in the last two cases, the rule `BackupStatusRow` already uses ([SettingsBackupPage.kt:78-99](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPage.kt#L78-L99)).
  - **Wearable:** "3 of 9 connected" or "Not connected", from one `grantedPermissions()` read.
  - **Storage:** "48 MB used".

  Refresh all three when the root list shows, as the Backup page does when it opens.
- **Doctrine:** §3 and the §15 Settings checklist ("each row carries its live value", [DESIGN.md:395-396](../../../.claude/DESIGN.md#L395-L396)), and the MAP Settings redesign ("every row showing its live value", [MAP.md:579-580](../../../.claude/design/MAP.md#L579-L580)). No new rows. The danger tone appears only for a true failure (§5 reserved colours).

### SED-03 · The restore confirm lists what you lose but not what the backup contains
feature-gap · impact medium · effort M · Restore confirm; post-restart restore notice

- **Now:** The confirm body describes only the data on the phone, through `restoreImpact` ("your 12 sessions · 5 progress photos", [SettingsScreen.kt:440-446](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L440-L446), [BackupRepository.kt:823-830](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L823-L830)). The auto-backup confirm gives only the save date ([SettingsDialogs.kt:160-163](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsDialogs.kt#L160-L163)). The incoming file is never described, although `restoreFromIncoming` already opens the staged database with the production Room builder before it publishes ([BackupRepository.kt:1078](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L1078), `opensWithRoom` at [:1365](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L1365)). After the restart the feed notice says "Your backup was restored. Everything in it is back on this device." with no date or numbers ([MainActivity.kt:384-388](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L384-L388)).
- **Why it matters:** Choosing between a picked file, a copy found in the folder and three auto-backups is guesswork. A backup older than your last session quietly drops everything logged since, and the receipt can't confirm which backup was restored.
- **Proposal:** Split the restore into two steps, stage then publish.
  1. Pick the file, and ask for the password if it has one.
  2. Validate and stage as today, but don't publish the set.
  3. Read the session count, latest finished session date and photo count from the staged files.
  4. Show the confirm with both sides, for example "Backup: 140 sessions, latest Sep 20, 12 photos. This phone: 152, latest Sep 30." When the backup's latest session is older than the phone's, add "12 sessions logged since Sep 20 won't be in it."
  5. Publish on confirm, and discard the staged set on dismiss.

  The receipt becomes "Restored the backup from Sep 21 · 140 sessions." Revised: the password prompt moves ahead of the confirm (today it appears after the user has confirmed), and dismissing the confirm now discards the staged set, so none is left behind. Build it with SED-02, which edits the same dialog.
- **Doctrine:** PRODUCT principle 2, "Show the reading, not just the verdict" ([PRODUCT.md:123-124](../../../PRODUCT.md#L123-L124)), and §11 voice (grounded in the user's numbers). SETTLED removed the backup-restored OK dialog ([SETTLED.md:90-92](../../../.claude/design/SETTLED.md#L90-L92)), so the receipt stays a feed notice. The existing dialog gains figures, not a caption stack.

### SED-05 · A Settings import can't be undone, and its result only shows in a four-second snackbar
ux-friction · impact medium · effort M · Settings → Import data (found files and "Choose a file")

- **Now:** `importData` sends `result.userMessage()` to the snackbar and nowhere else ([SettingsViewModel.kt:620-635](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L620-L635)). That message can run to five sentences: counts, duplicates, corrections, unmatched exercises and skipped rows ([ImportModels.kt:260-312](../../../forge-android/app/src/main/java/com/forge/app/data/importer/ImportModels.kt#L260-L312)). The snackbar window is 4 seconds ([SnackbarController.kt:103](../../../forge-android/app/src/main/java/com/forge/app/ui/common/SnackbarController.kt#L103)). The share-to-Avex path posts the same message to the feed as `NOTICE_IMPORT` ([MainActivity.kt:186](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L186)), so a shared import can be re-read and a Settings import can't. There is no undo. When a file has no unit column, weights are read in the user's Avex unit (`assumeKg`, [WorkoutImportRepository.kt:89](../../../forge-android/app/src/main/java/com/forge/app/data/importer/WorkoutImportRepository.kt#L89)), and the result doesn't say so.
- **Why it matters:** Importing from Strong or Hevy is something a user does once, and the stakes are high. If it goes wrong (wrong file, wrong unit), the only fix is resetting all sessions, and the detail about what was skipped or unmatched is gone in seconds.
- **Proposal:**
  - Call `settingsRepo.addSystemNotice(NOTICE_IMPORT, userMessage())` from the Settings path, as MainActivity does, so the full summary waits in the feed.
  - Shorten the snackbar to the headline and offer Undo, for example `snackbar.showUndo("Imported 412 workouts from Strong") { delete them }`, with `insert()` returning the ids it created.
  - Undo removes only what the import added. In-place corrections ([WorkoutImportRepository.kt:407](../../../forge-android/app/src/main/java/com/forge/app/data/importer/WorkoutImportRepository.kt#L407)) and phantom-workout conversions stay, and the notice says so when there were any.
  - When the importer had to assume a unit, have `read` report it, and add "Weights read as kg, the file didn't say" to the notice.

  Revised: impact lowered to medium because the Undo window is short and the feed notice carries most of the value. Undo is limited to rows the import added.
- **Doctrine:** §12 undo over confirm, through the one `SnackbarController` (§8). §4.6: notices live in the feed. SETTLED 2026-07-27 made the feed the home of import results ([SETTLED.md:90-92](../../../.claude/design/SETTLED.md#L90-L92)). The snackbar gets shorter and the detail moves to the feed, so this adds no caption stack and no confirm dialog.

### SED-08 · Signal rows say "Receiving" with no age, and three read signals are never checked
missing-state · impact medium · effort M · Settings → Wearable, signal rows

- **Now:** `probeSignalFlow` returns four booleans: sleep or heart rate, weight, steps and route ([HealthConnectManager.kt:1119-1128](../../../forge-android/app/src/main/java/com/forge/app/data/health/HealthConnectManager.kt#L1119-L1128)). Each probe reads one record from a 30-day window, oldest first, and discards its time ([:1140-1153](../../../forge-android/app/src/main/java/com/forge/app/data/health/HealthConnectManager.kt#L1140-L1153)). A connected row shows "Receiving" or "Nothing yet" beside its pre-connect explainer ([SettingsRecoveryComponents.kt:42-48](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryComponents.kt#L42-L48)). Body fat, Muscle mass and Watch workouts pass no `receiving` value, so they read "On" forever ([SettingsRecoveryPage.kt:196-204](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryPage.kt#L196-L204), [:220-227](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryPage.kt#L220-L227), [:262-269](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryPage.kt#L262-L269)), although MAP keeps plain "On" for rows still being probed and write-only rows ([MAP.md:718-721](../../../.claude/design/MAP.md#L718-L721)). "Nothing yet" usually means the companion app's Health Connect sharing is off, but the row names no fix, although `WearableBrand.sourceApp` knows which app that is.
- **Why it matters:** Checking why the coach ignored last night's sleep, the user sees "Receiving" whether the newest record is from today or from three weeks ago. Smart-scale and heart-rate users never learn whether their data arrives at all.
- **Proposal:**
  - Give `SignalFlow` a `lastAtMs: Long?` per signal, read newest first, and add probes for `BodyFatRecord`, `LeanBodyMassRecord` and `HeartRateRecord`.
  - For weight and body fat, read a few records and skip the ones Avex wrote itself (`isSelfWritten`), as `hasAnyRoute` already does ([HealthConnectManager.kt:1163](../../../forge-android/app/src/main/java/com/forge/app/data/health/HealthConnectManager.kt#L1163)). Otherwise Avex's own write-back counts as "receiving".
  - A connected row's supporting line becomes the reading's age: "Last reading 2h ago", or "Last night" for sleep.
  - A silent row reads "Nothing in 30 days. Turn on Health Connect sharing in Samsung Health", with the app name taken from `brand.sourceApp`.
  - The status word stays.

  Revised: the self-write filter was added, and the finder's whole-row tap on a silent row was cut (see the table at the end). The current probe's self-write gap also belongs on the bug list.
- **Doctrine:** §12 stale/denied, "last-known reading + its age". Health Connect exposes presence, never capability, so nothing says "unsupported" and nothing is greyed out by brand ([DESIGN.md:309-312](../../../.claude/DESIGN.md#L309-L312)). MAP's RECEIVING / NOTHING YET / ON states are kept, and only the supporting line gains the reading. This fixes a FAILURES "Verdict without a reading".

### SED-09 · The phone never shows whether the Avex watch app is installed or in range
discoverability · impact medium · effort M · Settings → Wearable

- **Now:** `WearConnection` exposes `reachableWearNodeId()` and the live `pairedWearApp()` flow ([WearConnection.kt:28-39](../../../forge-android/app/src/main/java/com/forge/app/service/wear/WearConnection.kt#L28-L39), [:76-91](../../../forge-android/app/src/main/java/com/forge/app/service/wear/WearConnection.kt#L76-L91)). Only services read them ([WearStatePublisher.kt:74](../../../forge-android/app/src/main/java/com/forge/app/service/wear/WearStatePublisher.kt#L74), [WorkoutSessionService.kt:213](../../../forge-android/app/src/main/java/com/forge/app/service/WorkoutSessionService.kt#L213)), and nothing under `ui/` does. The page titled "Wearable" covers Health Connect only. Its Galaxy / Pixel / Other picker changes the setup wording, not the watch link ([SettingsRecoveryPage.kt:110-129](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsRecoveryPage.kt#L110-L129)). The only mention of the watch app on the phone is a changelog line ([Changelog.kt:31](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/Changelog.kt#L31)). FEATURES lists a full Wear OS app.
- **Why it matters:** Galaxy and Pixel Watch owners, the people this page is for, are never told that Avex runs on the watch. Those who installed it can't tell from the phone whether the watch is paired or in range when sets don't arrive.
- **Proposal:** Open the Wearable page with an "Avex on your watch" group, fed by `pairedWearApp()` and `reachableWearNodeId()` through `HealthConnectViewModel` or a small sibling VM. It holds one status row:
  - Reachable: `SettingsStatus("Connected", live = true)`, with the supporting line "Logs sets, rest and heart rate from your wrist".
  - Paired but unreachable: `SettingsStatus("Out of range", live = false)`.
  - A watch is connected (`NodeClient.connectedNodes`) but none has the Avex capability: "Install Avex on your watch from Google Play".
  - No Wear OS watch at all: the group isn't drawn.

  Revised: a capability lookup can't tell "no watch" from "watch without Avex", so the finder's install line would have shown on every phone. It now requires a connected watch, and phones without one see no change.
- **Doctrine:** WEAR_OS_PLAN principle 3: "No watch paired ⇒ zero behavior change on the phone" ([WEAR_OS_PLAN.md:55-57](../../WEAR_OS_PLAN.md)). §12 hides what was never set up. SETTLED 2026-08-23 keeps the device question out of onboarding ([SETTLED.md:179-186](../../../.claude/design/SETTLED.md#L179-L186)), and this is a Settings status, not a question. SETTLED 2026-07-27 removed the Cardio connect-a-watch banner, and nothing here is a banner or strip. The Wear data layer runs over Bluetooth, not the network, so the offline promise holds. Settings kit: a status is a dot and a word.

### SED-11 · A failed weekly backup, or a folder copy that has stopped, is only visible inside Settings → Backup
discoverability · impact medium · effort M · Auto-backup worker → notifications feed; Backup page folder row

- **Now:** After three failed attempts the worker writes a marker file ([AutoBackupWorker.kt:46-53](../../../forge-android/app/src/main/java/com/forge/app/service/AutoBackupWorker.kt#L46-L53)), which only the Backup page and the Export sheet read. If the phone has lost the backup-password key, every run throws ([BackupRepository.kt:524-526](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L524-L526)) and ends with the same marker. The worker calls `autoBackup(folder)` ([AutoBackupWorker.kt:38](../../../forge-android/app/src/main/java/com/forge/app/service/AutoBackupWorker.kt#L38)), which drops the folder result ([BackupRepository.kt:509](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L509), see its KDoc at [:514-519](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L514-L519)). So a revoked or removed folder fails silently every week, while the folder row still says "Each backup also lands here" ([SettingsBackupPage.kt:53-61](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPage.kt#L53-L61)). The feed has an "Imports and backups" kind and a BACKUP glyph ([NotificationFeed.kt:71](../../../forge-android/app/src/main/java/com/forge/app/data/repo/NotificationFeed.kt#L71), [:79](../../../forge-android/app/src/main/java/com/forge/app/data/repo/NotificationFeed.kt#L79)), but nothing ever posts a backup failure.
- **Why it matters:** The folder copy is the one that survives losing or wiping the phone, and it can stop updating for months without the user ever opening Settings.
- **Proposal:**
  - On the final failure, have the worker call `settingsRepo.addSystemNotice("backup", …)`, worded by cause: "The weekly backup failed. Free up storage, then back up now." or "Set your backup password again so weekly backups can run."
  - Add `NoticeAction.OpenBackup`, routed to `Routes.settings(SettingsPage.Backup.name)` ([Routes.kt:77](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/Routes.kt#L77)). Clear the notice on the next success.
  - Switch the worker to `autoBackupWithFolderStatus` and store the folder's last successful write. The folder row then reads "Last copy Sep 28", or "Last copy failed · choose the folder again".

  Revised: the structured folder result itself is the 2026-09-26 audit's D6, still open ([08-data-repositories.md:72-75](../2026-09-26/08-data-repositories.md)). It is fixed for "Back up now" but not for the worker. This finding covers only what the user sees.
- **Doctrine:** §4.6: anything "waiting on you" goes in the feed, never in a strip on the page ([DESIGN.md:100](../../../.claude/DESIGN.md#L100)). SETTLED 2026-07-27 removed the banners and made the feed their home. The notice uses the existing RESULT kind, so the user can still switch it off. The folder row gains a reading, not a button.

### SED-12 · CSV exports are always in pounds and km, and there's no per-set CSV
feature-gap · impact medium · effort M · Export sheet → Quick export

- **Now:** The headers `volumeLb`, `bestWeightLb` and `weightLb` hold pounds whatever the user's unit, and cardio is always `distanceKm` ([BackupRepository.kt:421-434](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L421-L434), [:443-453](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L443-L453), [:456-462](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L456-L462), [:471-487](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L471-L487)). The sessions CSV writes the raw `dayKey` and has one row per session. No export has one row per set, although `GenericCsvImporter` reads date, exercise, weight, reps and unit, and Strong, Hevy and FitNotes all export that shape. Avex's own importers depend on the pound headers: `ForgeBodyweightCsvImporter.canParse` needs `weightlb` ([ForgeBodyweightCsvImporter.kt:20-25](../../../forge-android/app/src/main/java/com/forge/app/data/importer/ForgeBodyweightCsvImporter.kt#L20-L25)), and the PR-list check needs `bestweightlb` ([GymImporter.kt:280-281](../../../forge-android/app/src/main/java/com/forge/app/data/importer/GymImporter.kt#L280-L281)).
- **Why it matters:** A kg lifter who opens the spreadsheet sees numbers 2.2 times what they lift. Anyone who wants to analyse their lifts in a spreadsheet, or move to another app, can only get set-level data out as JSON.
- **Proposal:**
  - For kg users, write weight columns in kg and name them `volumeKg`, `bestWeightKg` and `weightKg`. Lb and stone users keep lb, since a stones-and-pounds value isn't a number a spreadsheet can use.
  - Write distance in miles or km according to `useMiles`.
  - Write the day's display name instead of `dayKey`.
  - In the same change, teach the two Avex importers the kg headers.
  - Add one `ExportRow("All sets", "CSV", …)` with date, workout, exercise, set, weight, unit, reps, RPE and warm-up, in a shape `GenericCsvImporter` can read back.

  Revised: effort raised to M because the importers must change too, and stones are handled explicitly.
- **Doctrine:** §11: "Never hardcode lb/kg anywhere", and translate the machine ([DESIGN.md:278-293](../../../.claude/DESIGN.md#L278-L293)). DECISIONS 2026-09-26 tags each export row with its format, and this adds one more row of the same shape. Files still leave through the share sheet, so nothing goes online.

### SED-13 · Privacy mode reads off while a lock is already hiding Avex and blocking screenshots
consistency · impact low · effort S · Settings → Privacy & security → Screen

- **Now:** MainActivity sets FLAG_SECURE when `privacyMode || appLockEnabled || galleryLockEnabled` ([MainActivity.kt:485](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L485), [:510](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L510)). With either lock on, the Privacy mode switch still shows off and still says "Hides Avex in recent apps and blocks screenshots", and toggling it changes nothing ([SettingsSecurityPage.kt:64-71](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSecurityPage.kt#L64-L71)).
- **Why it matters:** A user who turned on the gallery lock and now can't screenshot a PR finds Privacy mode off. They toggle it on and off, screenshots still fail, and nothing explains why.
- **Proposal:** While either lock is on, draw the row with `checked = true` and `enabled = false`, using `SettingsSwitchRow`'s existing parameter ("its value stays shown"). The supporting line reads "On while a lock is on". The stored value is untouched and comes back when the locks are off. Revised: the finder's extra text on the root Security caption was cut, so the fact lives in one place, this row.
- **Doctrine:** The Settings kit rule "A denied permission disables the rows it silences, values kept" ([MAP.md:579](../../../.claude/design/MAP.md#L579)), and §3 (each row shows its live value). No new control.

### SED-14 · Error messages show raw exception text, an em dash and "schema"
copy-voice · impact low · effort S · Import result; backup and crash-log failure snackbars; restore failure line

- **Now:** The import result renders "7 exercises weren't in the library — kept under their original names." ([ImportModels.kt:305](../../../forge-android/app/src/main/java/com/forge/app/data/importer/ImportModels.kt#L305)), an em dash still allowed by the design allowlist ([design-allowlist.txt:101](../../../forge-android/app/src/test/resources/design-allowlist.txt#L101)). Backup failures render `"Backup failed: ${it.message}"` ([SettingsViewModel.kt:722](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L722), [:1036](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L1036)), and crash-log export renders `"Crash log export failed: ${it.message}"` ([:1049](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L1049)), so the user sees ENOSPC or a storage provider's exception. A too-old backup reads "That backup is too old to restore safely (made before this app's schema)." ([:808](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L808)).
- **Why it matters:** These lines appear just when something has gone wrong, which is when the user most needs plain words.
- **Proposal:**
  - **Import:** "7 exercises weren't in the library, so they keep their original names."
  - **Backup:** classify the failure. No space: "There isn't enough free space. Free some up, then back up again." Key unavailable: "Set your backup password again on this page, then back up." Anything else: "The backup didn't finish. Try again."
  - **Crash logs:** "Couldn't save the crash logs. Try another folder."
  - **Too-old backup:** "That backup is from a version of Avex too old to restore."
  - Delete the allowlist line in the same change.

  Revised: the finder pointed at `ExportAttempt` for the classification, but it doesn't classify failures ([ExportAttempt.kt:20-28](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/ExportAttempt.kt#L20-L28)), so the backup path needs its own small mapping.
- **Doctrine:** PRODUCT Brand Commitments ban em dashes in any rendered string ([PRODUCT.md:98-101](../../../PRODUCT.md#L98-L101)). §11: translate the machine. This fixes a FAILURES "Machine leak".

### SED-15 · Storage shows "0 B" before it has measured, and its rows lead nowhere
ux-friction · impact low · effort S · Settings → Storage

- **Now:** Until the first measurement lands, `total` defaults to 0, so the page states "Space used 0 B" ([SettingsStoragePage.kt:38-46](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsStoragePage.kt#L38-L46)). Photos, Workout data, Local backup and Exports are passive rows, and only Cache can be cleared ([:47-61](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsStoragePage.kt#L47-L61)). Exports are app-private copies of files already handed to the share sheet ([StorageRepository.kt:55](../../../forge-android/app/src/main/java/com/forge/app/data/repo/StorageRepository.kt#L55)), including one `avex_session_<id>.json` per session ever exported ([BackupRepository.kt:374](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L374)), and they can't be cleared. `StorageRow` draws its own bar ([SettingsStoragePage.kt:66-95](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsStoragePage.kt#L66-L95)) instead of using `SettingsProgressBar`.
- **Why it matters:** A user short on space sees photos taking 300 MB and backups 120 MB, and nothing to do about either. The one button frees the smallest category.
- **Proposal:**
  - Draw no total until `storage != null`.
  - Make "Local backup" a `SettingsNavigationRow` to the Backup page, where the kept copies are explained, and make "Photos" a navigation row to the progress gallery through the existing gallery-lock gate.
  - Fold `exports/` into the clear action, labelled "Clear cache and exports · 12 MB", with the footer "Exports are copies of files you've already shared." Skip files written in the last hour, so a share that is still running keeps its file.
  - Draw the bars with `SettingsProgressBar`, giving it the 2% minimum width.

  Revised: the recent-file guard was added and the button wording tightened.
- **Doctrine:** §12 bans ghost data ("fake numbers", [DESIGN.md:318](../../../.claude/DESIGN.md#L318)). §2③: a row either does something or renders passive. The rows use the Settings kit's navigation rows, and §2⑥ asks for reuse. The page still has one button.

### SED-16 · Backup password fields can't be shown
accessibility · impact low · effort S · Set a backup password and Enter the backup password dialogs

- **Now:** `PasswordField` always masks its text (`PasswordVisualTransformation()`, [SettingsBackupPassword.kt:93-120](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPassword.kt#L93-L120)). The set-password dialog warns that a forgotten password makes every backup made with it impossible to open ([:137](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPassword.kt#L137)), and the restore dialog takes the password once ([:172-189](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPassword.kt#L172-L189)). The file's KDoc cites NIST SP 800-63B ([:122-125](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPassword.kt#L122-L125)), which also recommends letting users see what they typed.
- **Why it matters:** For screen-reader users, large-font users and anyone typing a long passphrase, an unseen typo here produces backups that can never be opened.
- **Proposal:** Add a trailing `IconButton` (Visibility / VisibilityOff icon, labelled "Show password" / "Hide password", 48dp) that switches the field to `VisualTransformation.None`. Mask the field again when the dialog closes. Keep the double entry. Revised: the AppLockScreen half was cut (see the table at the end).
- **Doctrine:** §14: 48dp touch targets and TalkBack labels. §13 already puts a trailing icon in a text field (the search field's clear button). Settings draws Material Rounded icons (DECISIONS 2026-09-26).

### SED-06 · Exports found in the folder look the same before and after they're imported
missing-state · impact low · effort M · Import sheet → In your folder

- **Now:** A found-file row reads "Strong · 412 workouts · Sep 28" with an Import pill, both before and after the file has been imported ([ImportDialog.kt:87-95](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/ImportDialog.kt#L87-L95)). After an import the VM rescans "so its found-file counts reflect what's now imported" ([SettingsViewModel.kt:629-634](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L629-L634)). But `foundImportSummary` counts what the file holds, not what is new ([WorkoutImportRepository.kt:1082-1092](../../../forge-android/app/src/main/java/com/forge/app/data/importer/WorkoutImportRepository.kt#L1082-L1092)). Tapping a file Avex already has returns "No new workouts found in that file."
- **Why it matters:** People who export from Strong every few weeks into Download/Avex end up with several near-identical rows, and the only way to tell which ones Avex already has is to tap each one.
- **Proposal:** During the folder scan, run the import's own duplicate check without writing anything (the `WorkoutIdentity` comparison, [AUDIT_DEFERRED.md:277-286](../../AUDIT_DEFERRED.md)), and carry a `newSessionCount` on `FoundImport`. The row reads "Strong · 412 workouts · 37 new". When nothing is new, it reads "All in your log", ends in `SettingsStatus("Imported", live = false)`, and has no tap target, instead of offering a pill that does nothing. "Choose a file" can still re-import a file on purpose.
- **Doctrine:** §2③: "Can't actually run → render passive". §4.9: show the reading. MAP 2026-09-27 keeps the list's shape, and the reading goes on the existing supporting line. The scan already parses every file in full (BUG_SCAN area 2, MED), so this adds indexed lookups, not more parsing.

## Considered and dropped

No finding was dropped outright. These three parts of revised findings were cut:

| ID | Idea | Why dropped |
|---|---|---|
| SED-16 (part) | Show AppLockScreen's lockout note in onBg with an error glyph, and change its subtitle's 0.7 alpha | The note uses the "quiet inline line in error color" that §12 requires. Its 3.67:1 contrast failure is SETTLED open decision #2 ([SETTLED.md:434-437](../../../.claude/design/SETTLED.md#L434-L437)), an app-wide decision about the reserved `error` colour that is still open. Changing one screen would leave the app's error lines inconsistent. The 0.7 alpha is on DesignDoctrineTest's allowed list and measures 5.18:1 (§14). |
| SED-08 (part) | Make a silent "Nothing yet" row a whole-row tap into Health Connect settings | The row's end slot already holds the status, so the tap would have no drawn pill: a FAILURES "Fake tap / nested tap" (§2③). "Manage in Health Connect" already opens that screen. The fix stays as text on the supporting line. |
| SED-01 (part) | Turn `BackupStatusRow` into the restore entry | A row titled "Last backup" whose tap replaces all your data hides a destructive act behind a status line. Restore gets its own labelled rows instead. |
