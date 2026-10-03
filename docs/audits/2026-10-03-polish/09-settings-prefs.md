# Settings: main list, appearance, program, coach, exercises, format, about

Scope: the Settings root and search, Appearance, Units & format, Notifications' reminder and quiet hours, Program & equipment, Coach, Exercise likes, Cardio activities, Holidays, What's new and About (`ui/settings/` SettingsScreen, SettingsMainList, SettingsKit, SettingsViewModel, AccentColorPicker, AppIconPicker, SettingsSubPages, SettingsProgramPage, SettingsCoachPage, SettingsExercisePrefsPage, SettingsCardioActivitiesPage, SettingsVacationPage, SettingsQuietHours, SettingsWhatsNewPage, Changelog, SettingsAboutPage, SettingsPreviews, SettingsIcons). 18 raised, 16 kept (12 revised), 2 dropped.

The 2026-09-26 kit is applied consistently, and rows, slabs and confirms look and behave the same on every page. The debt is in finding things and in what the pages say about state. Search runs on a hand-kept index that no longer matches the row labels. Program inputs only take effect at the next Generate, yet most of their copy reads as immediate, and the three Rebuild actions can replace a hand-built plan with one tap. Several pages draw controls that can't act in the current state: the freestyle Program menu, and a reminder hour that falls inside quiet hours. Others put two tap targets in one row. Backup, Wearable and Storage live values, and where restore lives, belong to [10-settings-data.md](10-settings-data.md) and aren't repeated here.

## Findings

### SEP-03 · Generate, Re-roll and Deload replace the whole plan, Plan builder edits included, in one tap
ux-friction · impact high · effort S · Settings → Program & equipment → Rebuild

- **Now:** The Rebuild group is three plain `SettingsActionRow`s, styled like the navigation rows above them ([SettingsProgramPage.kt:126-132](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L126-L132)). When no workout is open, `ProgramChangeGuard.run` executes them immediately ([ProgramChangeGuard.kt:55](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ProgramChangeGuard.kt#L55)). The only warning is the footer "A new plan replaces the current one". In one transaction, `generate` replaces the day and slot rows, deletes every non-custom customization, clears coach swaps and folds applied coach deltas ([ProgramRepository.kt:266-281](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L266-L281), [:585-592](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L585-L592)). The Plan builder saves its edits into those same program rows ([ProgramRepository.kt:114-126](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L114-L126)). Generate, Re-roll and Deload all discard them, Deload included, because it regenerates from the stored seed. The only result is a text snackbar ([SettingsViewModel.kt:472-543](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L472-L543)).
- **Why it matters:** An evening of hand-tuning in the Plan builder is lost to one mis-tap, with no way back.
- **Proposal:** Put all three behind `SettingsConfirmDialog`, shown before the guard runs:
  - Title "Replace your 4-day plan?".
  - Body naming what goes: "Your Plan builder edits and this plan's coach changes are replaced. This can't be undone."
  - Confirm label naming the act: "Generate plan", "Re-roll exercises" or "Start deload week".

  When a workout is open, skip this dialog and add the same line to `ProgramChangeGuardHost`'s body, so the user never meets two dialogs in a row.

  Revised: the finder proposed a snapshot Undo. A faithful undo would have to restore the day and slot rows, the overlay, the coach swaps, the folded decision statuses that feed the trust ledger, the seed, the deload marker and the remapped weekday schedule. That is effort L and touches the coach ledger. The confirm is the kit's own answer for an irreversible act, and an Undo can replace it later.
- **Doctrine:** §8: "every irreversible act confirms through `SettingsConfirmDialog` (a filled button naming the act)" ([DESIGN.md:205-207](../../../.claude/DESIGN.md#L205-L207)). §12 allows a dialog for an irreversible act if it words the consequence ([DESIGN.md:323](../../../.claude/DESIGN.md#L323)). DECISIONS 2026-07-27: "a dialog is for a decision the app cannot proceed without", which keeps `ProgramChangeGuardHost` as the only dialog while a workout is open ([DECISIONS.md:367-370](../../../.claude/design/DECISIONS.md#L367-L370)).

### SEP-01 · Search misses most of the labels on screen, and opens some hits in the wrong place
discoverability · impact high · effort M · Settings root, search pill

- **Now:** Search tests the whole query as one substring against a hand-kept index ([SettingsMainList.kt:176-193](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L176-L193); the index is at [SettingsScreen.kt:64-137](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L64-L137)). I replayed the index with a script.
  - **No results**, though each is a visible row, group or page: "Pure black", "Week starts", "Goals on Home", "Days per week", "Minutes per session", "Problem areas", "Priority muscles", "Advanced tracking", "Backup password", "Privacy policy", "About", "Re-roll", "Plan builder", "rotation", "freestyle", "monochrome", "dark mode", "lbs" ([SettingsSubPages.kt:47](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L47), [:56](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L56), [:140](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L140), [SettingsProgramPage.kt:107](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L107), [:130](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L130), [:197](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L197), [:288](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L288), [:295](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L295), [SettingsCoachPage.kt:77](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsCoachPage.kt#L77), [SettingsBackupPassword.kt:59](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsBackupPassword.kt#L59)). About and Privacy policy have no `PAGE_ENTRIES` entry at all ([SettingsScreen.kt:80-95](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L80-L95)).
  - **Wrong place:** "deload" offers Wearable, Sleep and the coach brief, not Program's Deload week. "sore" offers Notifications, not Problem areas. "schedule" offers Training reminders, not the weekday schedule.
  - **Wrong names:** "AMOLED mode" opens a row labelled "Pure black", "Time format" opens "Clock", and "First day of week" opens "Week starts" ([SettingsScreen.kt:98](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L98), [:104-105](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L104-L105)).
  - **No section:** every Program hit opens the Program menu with no section open, because `onOpenPage` clears `programSection` ([SettingsScreen.kt:280](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L280)).
  - **Freestyle:** the Coach row is hidden from the list ([SettingsMainList.kt:96](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L96)), but search filters out only the brief action ([:188](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L188)), so "coach" still lists the Coach page and Coach mode.
- **Why it matters:** The search pill is the first thing on the root. A user who types a label exactly as it appears on screen and gets "No settings match" concludes the setting doesn't exist.
- **Proposal:**
  1. Rename every index entry to the title its row renders, and keep the old names as tags (for example, "Pure black" tagged "amoled dark mode black"). Add entries for every switch, radio, segmented and group title, and page entries for About and Privacy policy.
  2. Give `SettingsItem` a `section: ProgramSection?` and open hits through `onOpenPage(page, section)`, so "Heaviest dumbbell" lands on Program → Equipment.
  3. Match per word: every whitespace-separated query word must appear in the name or tags. Bold the first matched word.
  4. Apply the freestyle filter to Coach page and item hits too.
  5. Next to `DoctrineParityTest`, add a JVM test that reads the settings page sources for `SettingsSwitchRow`, `SettingsRadioRow`, `SettingsSegmentedRow` and `SettingsGroup` title literals. It fails when one has no index entry. An allow-list covers generic rows ("Restore defaults", "From", "Until").

  Coordinate with SED-01, which points "Backup & restore" at the Backup page ([10-settings-data.md](10-settings-data.md)). Revised: adds the About and Privacy policy page entries and the test's allow-list.
- **Doctrine:** §3 Settings, where search hits render as one group, is unchanged ([DESIGN.md:87](../../../.claude/DESIGN.md#L87)). SETTLED 2026-09-26 removed Date format, Timezone and Compact set logging ([SETTLED.md:27-36](../../../.claude/design/SETTLED.md#L27-L36)). None of them comes back, and the test reads only rows that exist. §16: a rule that can be checked mechanically becomes a test ([DESIGN.md:414-415](../../../.claude/DESIGN.md#L414-L415)).

### SEP-02 · Program inputs wait for the next Generate, but their copy says they apply now and nothing shows what is waiting
ux-friction · impact high · effort M · Settings → Program & equipment (menu, Plan, Goal & experience, Emphasis & priorities, Equipment)

- **Now:**
  - **Staged until a rebuild:** goal, experience, emphasis, priority muscles, problem areas, minutes per session and the days chip are written to prefs only ([SettingsViewModel.kt:423-424](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L423-L424), [:494-502](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L494-L502)). They are read when a plan is generated, re-rolled or rotated ([ProgramRepository.kt:385-399](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L385-L399)). Problem areas have no other reader: the generator's contraindication filter is their only consumer ([ProgramGenerator.kt:216](../../../forge-android/app/src/main/java/com/forge/app/program/ProgramGenerator.kt#L216)). Priority muscles are the exception, since they also reorder a live session ([DayViewModelRefresh.kt:212](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/DayViewModelRefresh.kt#L212)).
  - **Copy:** the Days and Minutes footers already say they wait ([SettingsProgramPage.kt:197](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L197), [:213-214](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L213-L214)). The others read as immediate: "Same exercises, different loading. Switch anytime." ([:267](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L267)), "These get extra volume." ([:288](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L288)) and "Flag a sore joint and the plan steers around it." ([:295](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L295)).
  - **Day count:** the root Program row and the Plan row show the staged day count, not the plan's ([SettingsMainList.kt:298](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L298), [SettingsProgramPage.kt:136-140](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L136-L140)).
  - **Equipment:** this is the only section that prompts a Regenerate, and its flag is a local `remember` that is lost when you leave the section ([:314](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L314), [:370-383](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L370-L383)).
- **Why it matters:** A lifter flags a sore shoulder and finds overhead pressing still in Monday's plan. Goal and emphasis changes seem to do nothing. This is exactly when the plan is meant to adapt.
- **Proposal:**
  - **Snapshot:** at the end of `ProgramRepository.generate`, next to `setProgramGenerationSeed` ([ProgramRepository.kt:295](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L295)), store the inputs the live plan was built from: goal, experience, emphasis, priority muscles, problem areas, minutes, equipment and dumbbell cap. `saveCustomProgram` clears the snapshot, because a hand-built plan wasn't built from these inputs.
  - **Pending list:** the VM compares current prefs with the snapshot to get `pendingPlanChanges`, and takes the day count from `Program.days.size`.
  - **Program menu:** the Rebuild footer becomes that reading, for example "Waiting for the next plan: 5 days (plan has 4) · shoulder flagged · goal Get stronger". While the list is non-empty, "Generate a new 5-day plan" becomes the page's one Primary `SettingsButton`, at the end.
  - **Equipment:** the same reading replaces the local `equipmentEdited` flag.
  - **Copy:** rewrite the four footers in forward voice ("Applies when you generate or re-roll").
  - **Day count:** the root and Plan rows show the plan's own day count, plus "changes waiting" when the list is non-empty.

  Revised: covers the hand-built case and notes the one input that applies live (priority muscles).
- **Doctrine:** §4.9 and FAILURES "Verdict without a reading" ([DESIGN.md:103](../../../.claude/DESIGN.md#L103), [FAILURES.md:122](../../../.claude/design/FAILURES.md#L122)). The VM's choice not to regenerate on every chip tap stays ([SettingsViewModel.kt:494-495](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L494-L495)). This shows the staged state, it doesn't auto-apply it. One filled action per page, at the end (§8, [DESIGN.md:201](../../../.claude/DESIGN.md#L201)). §11: a pending state names its change ([DESIGN.md:283-285](../../../.claude/DESIGN.md#L283-L285)). The 2026-09-26 days-chip item was fixed with copy only, and this finding extends that fix to the other inputs.

### SEP-06 · A training reminder set inside quiet hours never arrives, and the page doesn't say so
missing-state · impact medium · effort S · Settings → Notifications, Remind me at and Quiet hours

- **Now:** `TrainingReminderWorker` returns early when `isQuietNow()`, with the comment "skip; it fires again tomorrow" ([TrainingReminderWorker.kt:50](../../../forge-android/app/src/main/java/com/forge/app/service/TrainingReminderWorker.kt#L50)). It runs at the same hour every day, so a 7 AM reminder under a 22:00–08:00 window is skipped on every day that window covers. The page draws "Remind me at" and the quiet window in neighbouring groups and never connects them ([SettingsSubPages.kt:339-349](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L339-L349), [:366-375](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L366-L375)).
- **Why it matters:** The user turns reminders on, never gets one, and has nothing to explain why.
- **Proposal:** Give `SettingsHourRow` an optional supporting line. When quiet hours are on and `quietHoursSchedule.windowFor(day)` covers `trainingReminderHour` on any day, the "Remind me at" row reads "Inside quiet hours on Mon–Fri, so it won't arrive those days". The days follow Week starts. Draw it in `onSurfaceVariant`, not the error colour.
- **Doctrine:** §12 error state: a quiet inline line that words the consequence ([DESIGN.md:308](../../../.claude/DESIGN.md#L308)). §14 rules out new error-coloured body text, so it uses the muted rung ([DESIGN.md:361-362](../../../.claude/DESIGN.md#L361-L362)). §13 allows one explainer beside a non-obvious control.

### SEP-08 · Cardio activity rows put a delete button inside a tappable row, and delete doesn't say what happens to logged sessions
accessibility · impact medium · effort S · Settings → Cardio activities

- **Now:** Each row is `clickableLabeled("Edit …")` and also holds an `IconButton` that deletes ([SettingsCardioActivitiesPage.kt:57-64](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsCardioActivitiesPage.kt#L57-L64)). Delete removes the activity and offers a short Undo ([SettingsViewModel.kt:247-255](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L247-L255)). Sessions logged with it keep only its code, so once the Undo expires they show as "Other" ([MAP.md:248](../../../.claude/design/MAP.md#L248)).
- **Why it matters:** Edit and delete sit a few pixels apart in one row, and TalkBack users get two focus stops per row. Someone with 20 padel sessions finds them all relabelled "Other" without warning.
- **Proposal:**
  - Make the row one tap target that opens `CustomActivityDialog`.
  - Give the dialog an optional `onDelete`, passed only from Settings and only when `initial != null`, and draw it as an outlined error "Delete activity" action.
  - Show a session count as the row's supporting line ("12 sessions"), from a new `CardioDao.countByType(code)`.
  - Word the snackbar "Padel removed · 12 sessions now show as Other", with Undo.
- **Doctrine:** One tap target per row: §2③, §14 and FAILURES "Fake tap / nested tap" ([DESIGN.md:66](../../../.claude/DESIGN.md#L66), [:365](../../../.claude/DESIGN.md#L365), [FAILURES.md:176](../../../.claude/design/FAILURES.md#L176)). A destructive action is an outlined error action with Undo, never filled red ([DESIGN.md:65](../../../.claude/DESIGN.md#L65)). A count may qualify a row ([DESIGN.md:42](../../../.claude/DESIGN.md#L42)).

### SEP-11 · In freestyle the Program menu still offers rotation and deload, and Re-roll quietly leaves freestyle
missing-state · impact medium · effort S · Settings → Program & equipment (freestyle)

- **Now:** `ProgramMenu` has no freestyle branch ([SettingsProgramPage.kt:92-133](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L92-L133)), although the Plan section already gates on freestyle ([:209](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L209)). In freestyle, "Automatic re-roll" does nothing, because `maybeRotateProgram` returns at once ([WorkoutRepository.kt:528](../../../forge-android/app/src/main/java/com/forge/app/data/repo/WorkoutRepository.kt#L528)). Re-roll and Deload week each generate a plan and switch freestyle off, and only the snackbar says so ([SettingsViewModel.kt:472-491](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L472-L491), [:530-543](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L530-L543)).
- **Why it matters:** The page shows a control that does nothing in this mode. It also lets a row labelled "Same split, new movement picks" switch the user's training mode.
- **Proposal:** When `state.freestyleMode` is on:
  - Hide Rotation and Deload week.
  - Reduce Rebuild to one action, "Start following a plan" (`generateProgram(state.daysPerWeek)`), with the footer "Builds a plan from the settings above. Freestyle turns off."
  - Keep Goal, Emphasis, Equipment and Plan builder, because they shape that plan.
- **Doctrine:** §2③ "Can't actually run: render passive" and §4.5 ([DESIGN.md:68](../../../.claude/DESIGN.md#L68), [:99](../../../.claude/DESIGN.md#L99)). It mirrors the Plan section's existing gate. Nothing removed elsewhere comes back.

### SEP-12 · Exercise likes can't show the moves in your plan, and hiding one doesn't take it out of the plan
feature-gap · impact medium · effort S · Settings → Exercise likes

- **Now:** The scopes are All exercises, Your gear, a muscle and Custom ([SettingsExercisePrefsPage.kt:80-107](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsExercisePrefsPage.kt#L80-L107)), so the twenty or so moves you actually train are scattered through several hundred. The footer says hidden moves "never" come up ([:135](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsExercisePrefsPage.kt#L135)). Dislikes are only read when a plan is generated, re-rolled or rotated ([SettingsViewModel.kt:519](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L519)), so a hidden move already in a day stays there until then.
- **Why it matters:** The most common reason to open this page is "I don't want this move in my plan". The user hides it, and it is still on Push day.
- **Proposal:**
  - Add an "In your plan" scope, built from each day's effective plan (`ProgramCustomizationRepository.effectivePlanForDay`, which includes swaps). Each row's supporting line names its day ("Push A").
  - A hidden exercise that is still in the plan reads "Hidden · still in Push A".
  - Change the footer to "hidden ones stay out of new plans".

  Revised: the finder's per-row "Re-roll Push A" menu item is cut. `rerollDay` re-picks every exercise on that day ([ProgramRepository.kt:455-470](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L455-L470)), which is too blunt for removing one move. The in-session swap with "Make default" already does that, and this page's "Ask to hide after swapping" switch is built around it ([SettingsExercisePrefsPage.kt:139](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsExercisePrefsPage.kt#L139)). Effort drops to S.
- **Doctrine:** It reuses the existing scope filter and row, with no new control type. §11: a status names its referent ([DESIGN.md:283](../../../.claude/DESIGN.md#L283)). It is the same staged-input honesty as SEP-02.

### SEP-05 · Holidays can't be edited, need two date dialogs, and don't say which one is on or what pauses
feature-gap · impact medium · effort M · Settings → Holidays

- **Now:**
  - **No edit:** each row is a passive name and date range with a bin ([SettingsVacationPage.kt:66-74](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsVacationPage.kt#L66-L74)). Fixing a date means deleting and re-adding, and `VacationDao` has no update.
  - **Two pickers:** adding a holiday opens a dialog whose Starts and Ends rows each open their own `DatePickerDialog` ([:83-138](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsVacationPage.kt#L83-L138)).
  - **No current marker:** rows sort by start date, newest first ([VacationDao.kt:20](../../../forge-android/app/src/main/java/com/forge/app/data/db/dao/VacationDao.kt#L20)), and nothing marks the current or next holiday.
  - **Footer:** it mentions only the streak ([SettingsVacationPage.kt:62](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsVacationPage.kt#L62)). A holiday also holds the weekly coach pass ([CoachRepository.kt:269-271](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L269-L271)) and silences the recap's come-back nudge ([WeeklyRecapWorker.kt:115-116](../../../forge-android/app/src/main/java/com/forge/app/service/WeeklyRecapWorker.kt#L115-L116)).
  - **Reminders:** daily training reminders never check holidays ([TrainingReminderWorker.kt:47-52](../../../forge-android/app/src/main/java/com/forge/app/service/TrainingReminderWorker.kt#L47-L52)).
- **Why it matters:** A delayed flight means deleting and re-adding the holiday. Nobody is told the coach will pause, and "train today" nudges still arrive on the beach.
- **Proposal:**
  - Make the whole row open the same dialog, pre-filled, as "Edit holiday". Move delete into that dialog as an outlined error action, keeping the existing Undo ([SettingsViewModel.kt:226-233](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L226-L233)). Add `VacationDao.update`.
  - Replace the two pickers with one M3 `DateRangePicker`.
  - Split the list into a "Now and upcoming" group, oldest start first, and a "Past" group. The current holiday's row ends in `SettingsStatus("Now", live = true)` with "until Oct 12" as its supporting line.
  - Change the footer to "Your streak waits, the weekly coach call holds and the come-back nudge stays quiet."
  - Have `TrainingReminderWorker` skip holiday days, using the same `VacationCalendar` check the recap uses.

  Revised: the finder's "N earlier" collapse is cut, because §12 bans "show more". The accent dot becomes the kit's status element.
- **Doctrine:** One tap target per row (§2③, [DESIGN.md:66](../../../.claude/DESIGN.md#L66)). Undo over confirm is kept ([:323](../../../.claude/DESIGN.md#L323)). §12 overflow never uses "show more" ([:306](../../../.claude/DESIGN.md#L306)). The Settings kit says "Status is a dot and a word" ([MAP.md:578](../../../.claude/design/MAP.md#L578)). No Home strip or banner is added (SETTLED, every page-level banner).

### SEP-04 · The Holidays and Cardio activities rows on the root show fixed text instead of their live value
missing-state · impact low · effort S · Settings root, Training and Coach & recovery groups

- **Now:** `rowSubtitle` returns "Sports the built-in list misses" and "Pause your streak while you're away" whatever the state ([SettingsMainList.kt:315-316](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L315-L316)). Its comment notes that the cardio list lives on its own flow. The VM already holds both lists ([SettingsViewModel.kt:215-217](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L215-L217), [:236-237](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L236-L237)).
- **Why it matters:** Someone checking whether their trip is set up has to open the page to find out.
- **Proposal:**
  - **Holidays:** "On holiday until Oct 12", "Next: Dec 20 to 27" or "None planned", computed with `VacationCalendar`.
  - **Cardio activities:** "3 activities", or the current text at zero.
  - Pass both lists into `MainList` as parameters.

  Revised: Backup, Wearable and Storage are left out because SED-10 in [10-settings-data.md](10-settings-data.md) covers them. Impact lowered with them.
- **Doctrine:** §3 Settings, "Each row shows its live value", and the §15 Settings checklist ([DESIGN.md:87](../../../.claude/DESIGN.md#L87), [:395-396](../../../.claude/DESIGN.md#L395-L396)). No new rows.

### SEP-09 · The accent swatches disappear when the icon sets the colour, and the switch that brings them back is two groups down
ux-friction · impact low · effort S · Settings → Appearance

- **Now:** "Match accent to icon" sits under "Icon & startup" ([SettingsSubPages.kt:93-104](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L93-L104)). When it is on and the icon has a colour of its own, the Accent color group drops the swatches and its header shows "From icon" ([:63-83](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L63-L83)). The unrelated "Goals on Home" group sits between Theme and Accent ([:54-61](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L54-L61)). The switch is off by default ([SettingsRepository.kt:230](../../../forge-android/app/src/main/java/com/forge/app/data/prefs/SettingsRepository.kt#L230)), so only people who turned it on hit this.
- **Why it matters:** A user who wants a different accent sees no swatches, and has to work out that a setting in another group is hiding them.
- **Proposal:** Move the existing "Match accent to icon" switch into the Accent color group, directly under "Use an accent color", so the control sits beside the swatches it hides. Move the Home group after Icon & startup, so Theme, Accent and Icon read in one run.

  Revised: it keeps the existing switch rather than adding a segmented control. Impact is lowered because the setting is opt-in and off by default.
- **Doctrine:** No setting is added or removed. §4.3 gives a control one home, next to what it governs. The accent stays state only (§3 Settings).

### SEP-14 · The weekday schedule ignores Week starts and doesn't flag a plan day that's on no weekday
consistency · impact low · effort S · Settings → Program → Plan → Week (By weekday)

- **Now:** `WEEKDAYS` is fixed to Monday first ([SettingsProgramPage.kt:260](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L260), used at [:241](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L241)). Quiet hours on the Notifications page follow Week starts through `orderedDays` ([SettingsQuietHours.kt:144-147](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsQuietHours.kt#L144-L147)). If a plan day is set to Rest on every weekday, it never comes up, and nothing says so.
- **Why it matters:** A Sunday-first user sees two different week orders in Settings. A plan day can quietly drop out of the week.
- **Proposal:** Order the rows with `orderedDays(state.firstDayMonday)`, moved to a shared spot, and keep storage indexed Monday first. When a `Program.days` key isn't in the schedule, add the group footer "Not on any day: Legs B".

  Revised: the finder's custom-day-name part is cut. `DayNameOverride` has no writer in the tree, and Plan builder renames already flow into `defaultName` ([ProgramRepository.kt:546](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L546)).
- **Doctrine:** One footer caption per group (§4.3). It reuses the existing dropdown rows.

### SEP-15 · The theme preview calls itself "a sample of Home" but draws shapes Home doesn't use
consistency · impact low · effort S · Settings → Appearance, live preview

- **Now:** The preview draws a pill-shaped Start button and a seven-bar chart with bars of varying height ([SettingsPreviews.kt:56-64](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsPreviews.kt#L56-L64), [:33](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsPreviews.kt#L33), [:66-91](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsPreviews.kt#L66-L91)). Its footer reads "A sample of Home in your current look." Home binds its buttons to `CellShape` ([OverviewScreen.kt:93](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L93)) and draws the week as a `WeekStrip` of uniform cells ([SurfaceKit.kt:376](../../../forge-android/app/src/main/java/com/forge/app/ui/experiment/SurfaceKit.kt#L376)).
- **Why it matters:** The one place that shows what the accent and Pure black do shows a Home the user will never see.
- **Proposal:** Build the preview from Home's own components: `WeekStrip(trained = setOf(0, 2, 3), todayIndex = 4, dayLabels, reading = "Sample week")`, with labels that follow Week starts, and a Start capsule clipped to `CellShape`. Keep the serif "Pull B" line and the footer.
- **Doctrine:** SETTLED 2026-08-22 makes Home's buttons the only non-pill buttons in the app ([SETTLED.md:313-317](../../../.claude/design/SETTLED.md#L313-L317)). The live preview remains the one hero the Settings toolkit allows (§3). Reusing the shared component follows §2⑥.

### SEP-16 · About's gesture list advertises two gestures that are gone and misses four that exist
discoverability · impact low · effort S · Settings → About → Gestures & shortcuts

- **Now:** The list ([SettingsAboutPage.kt:113-119](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsAboutPage.kt#L113-L119)) has two dead rows and four gaps.
  - **"Long-press a day on the Gym list":** that list is `DayListScreen`'s Train tab, which never renders ([HubScreen.kt:168](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/HubScreen.kt#L168); TRF-01 in [01-train-flow.md](01-train-flow.md)).
  - **"set its rest timer":** this names a menu item that has been removed (TRF-14).
  - **Missing, hold Start on Home:** skips the warm-up ([OverviewScreen.kt:575-577](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L575-L577)).
  - **Missing, hold the rest-timer bubble:** adds 30 seconds ([DayScreen.kt:159](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/DayScreen.kt#L159)).
  - **Missing, long-press an RPE chip:** clears it ([SetRow.kt:304](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/components/SetRow.kt#L304)).
  - **Missing, long-press a body measurement:** opens its options ([BodyMeasurementsScreen.kt:179](../../../forge-android/app/src/main/java/com/forge/app/ui/profile/BodyMeasurementsScreen.kt#L179)).
- **Why it matters:** This is the only place hidden gestures are written down. Two of its rows promise things that don't happen, and the time-savers between sets aren't listed.
- **Proposal:** Make the list match the tree:
  - Drop the Gym-list row until TRF-01 makes the list reachable.
  - Drop the exercise-card row along with TRF-14, or fix its wording if TRF-14 isn't taken.
  - Add the four missing gestures.
  - Split the list into "In a workout" and "Around the app" so neither group runs long.

  Revised: adds the two dead rows and ties the change to TRF-01 and TRF-14.
- **Doctrine:** Every row names a distinct action, so this isn't FAILURES "Checklist section" ([FAILURES.md:32](../../../.claude/design/FAILURES.md#L32)). No in-context tips are added (§12, where the boxed `FirstTouchTip` is deprecated).

### SEP-17 · The colour wheel and brightness slider can't be used with TalkBack, and icon tiles have no radio role
accessibility · impact low · effort S · Settings → Appearance → Custom color; App icon sheet

- **Now:** `ColorWheel` and `BrightnessSlider` are Canvases driven only by `pointerInput`, with no semantics ([AccentColorPicker.kt:279-307](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/AccentColorPicker.kt#L279-L307), [:368-395](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/AccentColorPicker.kt#L368-L395)). The preset swatches are a proper radio group ([:203-219](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/AccentColorPicker.kt#L203-L219)). App-icon thumbnails put ", selected" in their description, but have no role and no `selectableGroup` ([AppIconPicker.kt:173-195](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/AppIconPicker.kt#L173-L195)). AUDIT.md counts the wheel's two unlabelled clickables as doctrine debt without proposing a fix ([AUDIT.md:130](../../../.claude/design/AUDIT.md#L130)).
- **Why it matters:** TalkBack users can't adjust brightness, and they hear the icon grid as a list of buttons instead of a one-of choice.
- **Proposal:**
  - **Slider:** `semantics { contentDescription = "Brightness"; progressBarRangeInfo = ProgressBarRangeInfo(v, vMin..1f); setProgress { … } }`.
  - **Wheel:** a description that reads the value ("Colour wheel, #E23D3D"). Exact entry stays with the hex field.
  - **Icon thumbnails:** `selectable(selected, role = Role.RadioButton)` inside a `selectableGroup()`, as `AccentSwatch` does.

  Revised: the icon tiles already announce "selected", so the gap there is the role and the group. This also clears the AUDIT.md wheel debt.
- **Doctrine:** §14 semantics ([DESIGN.md:367-370](../../../.claude/DESIGN.md#L367-L370)). This isn't U05, which covers only the hex and search field labels.

### SEP-18 · Backup uses a cloud glyph in an app that has no internet permission
copy-voice · impact low · effort S · Settings root Backup row; Export & back up sheet

- **Now:** `SettingsIcons.Backup` is "a cloud with a down-arrow" ([SettingsIcons.kt:203-221](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsIcons.kt#L203-L221)). It is used on the root Backup row and on the sheet's backup row ([SettingsMainList.kt:227](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L227), [SettingsDialogs.kt:92](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsDialogs.kt#L92)). About states the offline promise next to a crossed-out cloud ([SettingsAboutPage.kt:105](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsAboutPage.kt#L105)).
- **Why it matters:** The cloud suggests sync exists, or that backups leave the phone, which contradicts the product's main promise.
- **Proposal:** Redraw `SettingsIcons.Backup` in the family's 1.7 stroke as a kept copy, with no cloud and no tray arrow (for example, two offset sheets). Export and Import already use the down and up tray arrows ([SettingsIcons.kt:162-183](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsIcons.kt#L162-L183)). Judge it at 22dp in the Data group beside Export, Import and Storage.

  Revised: the finder's "phone with a down arrow into a tray" would collide with Export, and SETTLED records that an arrow over a slab reads as upload or download.
- **Doctrine:** PRODUCT principle 5, "Offline is a promise" ([PRODUCT.md:129](../../../PRODUCT.md#L129)). §8: row glyphs come from the custom families ([DESIGN.md:227-229](../../../.claude/DESIGN.md#L227-L229)). SETTLED 2026-08-24: a glyph is judged in a row, at the size it ships ([SETTLED.md:704-714](../../../.claude/design/SETTLED.md#L704-L714)).

### SEP-13 · What's new is found only by going to look, and nothing points to it after an update
discoverability · impact low · effort M · Settings → What's new; notifications feed

- **Now:** Release notes are passive rows on a page reached from the root's About group ([SettingsWhatsNewPage.kt:20-61](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsWhatsNewPage.kt#L20-L61), [SettingsMainList.kt:123](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L123)). No notice kind covers a new version ([NotificationFeed.kt:50-73](../../../forge-android/app/src/main/java/com/forge/app/data/repo/NotificationFeed.kt#L50-L73)), and nothing stores the last version seen. The changelog still carries its "starting draft" note ([Changelog.kt:9-10](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/Changelog.kt#L9-L10)), an open item from the 2026-09-26 audit.
- **Why it matters:** New features stay unknown unless someone happens to open the page.
- **Proposal:** Once the changelog has been reconciled:
  - Add `NoticeKind.UPDATE` ("What's new", "After Avex updates").
  - Queue it once when `versionCode` rises past a stored last-seen value, never on first install.
  - Title it from the newest release's own counts ("Avex 0.9 · 3 new, 2 improved").
  - Add a `NoticeAction.OpenSettings(page)` that opens `Routes.settings("WhatsNew")` ([Routes.kt:77](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/Routes.kt#L77)).
  - It gets a switch under Notifications → In the app, like every other kind.

  Revised: the finder's links from changelog lines to settings pages are cut, because none of the six 0.9 lines leads to a Settings page. This is also sequenced after the draft fix, because a notice pointing at a draft changelog is worse than none.
- **Doctrine:** §4.6: a notice lives behind the bell ([DESIGN.md:100](../../../.claude/DESIGN.md#L100)). SETTLED removed the launch dialogs and page banners, and this adds neither ([SETTLED.md:85-91](../../../.claude/design/SETTLED.md#L85-L91)). DECISIONS 2026-07-27: a notice is everything that isn't a decision, and kinds are stored as a disabled set, so a new kind defaults on ([DECISIONS.md:352](../../../.claude/design/DECISIONS.md#L352), [:367](../../../.claude/design/DECISIONS.md#L367)).

## Considered and dropped

| ID | Idea | Why dropped |
|---|---|---|
| SEP-07 | Put Restore on the Backup page, and rename the sheet's "Back up now" | Duplicate of SED-01 in [10-settings-data.md](10-settings-data.md): same files, same rename, same new Restore group. One correction for whoever takes SED-01: searching "restore" already offers "Backup & restore", which opens the sheet that holds restore ([SettingsScreen.kt:67](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L67)). |
| SEP-10 | Show each coach feed's reading and the exact trust threshold on Settings → Coach | MAP limits Settings → Coach to "a feeds on/off glance … never a second brief/trust/history home" ([MAP.md:319-320](../../../.claude/design/MAP.md#L319-L320)). The readings already live in the Coach tab's What it reads ([CoachStand.kt:201-216](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachStand.kt#L201-L216)). The mode row already reads "N of M earned so far" ([SettingsCoachPage.kt:70](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsCoachPage.kt#L70)). The threshold differs by change type ([TrustLedger.kt:49-54](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TrustLedger.kt#L49-L54)), so a single "after 4" would be wrong for half of them. |
