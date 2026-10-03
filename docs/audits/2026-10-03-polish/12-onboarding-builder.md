# Cold launch, onboarding and program builder

Scope: first run and the program screen (`ui/onboarding/`, `ui/programbuilder/`, `ui/launch/`, `ui/common/AvexIntro.kt`, `ui/common/LaunchScenes.kt`), plus the shared selectable tiles and drag list they draw with. 16 raised, 12 kept (10 revised), 4 dropped.

First run is short, honest and resumable. The debt sits at its edges and in the editor behind it. Several marks and controls show less than they could: the week page doesn't say which weekday each workout falls on, though the flow has just asked; a picked tile's reading falls below AA; a re-roll can't be taken back; and the day editor has a nested remove button and a colour picker whose colour no screen shows. Nothing in first run lets someone bring history they already have, and the program screen's names and entry points don't agree with each other. The bigger structural items the finder raised are already owned elsewhere: rebuilding without a confirm (SEP-03), staged Settings inputs (SEP-02) and starting a chosen day (TRF-01).

## Findings

### ONB-12 · A picked tile's reading turns accent and drops to 3.4:1 on the default Red
accessibility · impact medium · effort S · Onboarding mode, goal, experience, weekday, gym-preset and sore-spot tiles; Goal editor tiles

- **Now:** `ForgeChoiceList` draws its mono meta in `primary` when picked ([ForgeGroups.kt:311](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L311), [:320](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L320)), and so does `ForgeLabelTile` ([:424](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L424)). Underneath is `surfaceContainerHigh` `#221C16` ([ForgeTheme.kt:100](../../../forge-android/app/src/main/java/com/forge/app/ui/theme/ForgeTheme.kt#L100)) with the accent@0.15 wash from `memberFill` ([ForgeGroups.kt:189](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L189)). Measured contrast: Red 3.44:1, Ember 4.16, Gold 2.59, Olive 2.21, Navy 1.89. Muted text on the same picked tile measures 7.25:1.
  - **Callers:** onboarding's choice lists ([OnboardingSteps.kt:84](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingSteps.kt#L84), [:113](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingSteps.kt#L113), [:129](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingSteps.kt#L129), [:219](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingSteps.kt#L219)), the gym presets ([OnboardingGymSteps.kt:62](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingGymSteps.kt#L62)), the sore spots ([OnboardingSoreSpots.kt:84](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingSoreSpots.kt#L84)) and the goal editor ([GoalEditorScreen.kt:261](../../../forge-android/app/src/main/java/com/forge/app/ui/goals/GoalEditorScreen.kt#L261)).
  - **Already marked:** the 1.5dp accent ring, the wash and the accent `ForgeGlyphBadge` all show the pick.
- **Why it matters:** On every question in first run, the line that says what a choice does ("8-12 REPS", "12 MOVEMENTS") becomes the hardest text to read once it is picked. It fails AA on every accent, the default included.
- **Proposal:** Draw the meta in `onSurfaceVariant` in both states at those three lines, and let the ring, wash and badge mark the pick. `ForgeOptionCard` and `ForgePresetTile` in `Selectables.kt` do the same ([:170](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Selectables.kt#L170), [:238](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Selectables.kt#L238)), but nothing calls them. Change them to match, or delete them. Revised: corrects the call sites. Settings → Program doesn't use these tiles; the goal editor does.
- **Doctrine:** §14 and SETTLED's open contrast decision: no new accent-coloured body text, and onBg text beside an accent mark is the one treatment that reads under every accent ([DESIGN.md:361-362](../../../.claude/DESIGN.md#L361-L362), [SETTLED.md:400-404](../../../.claude/design/SETTLED.md#L400-L404)). §6: rank mono labels by size, never colour ([DESIGN.md:158](../../../.claude/DESIGN.md#L158)). The §3 one-tile formula (outline, then accent border and accent@0.15 wash) is unchanged ([DESIGN.md:87](../../../.claude/DESIGN.md#L87)).

### ONB-11 · Exercises reorder only by drag, so TalkBack users can't reorder a day
accessibility · impact medium · effort S · Your program → day editor exercise list (and the freestyle log)

- **Now:** The day editor reorders through `dragContainer` and `DraggableItem` long-press drag ([ProgramBuilderDayDetail.kt:131](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L131), [:267-278](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L267-L278)), under the caption "Hold to reorder" ([:262](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L262)).
  - `DraggableItem` adds no semantics ([DragReorder.kt:150-160](../../../forge-android/app/src/main/java/com/forge/app/ui/common/DragReorder.kt#L150-L160)), and `ExerciseRow` carries only its click label ([ProgramBuilderDayDetail.kt:367](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L367)).
  - The day rail above gives each bar "Move earlier" and "Move later" actions ([WeekBarRail.kt:110-116](../../../forge-android/app/src/main/java/com/forge/app/ui/common/WeekBarRail.kt#L110-L116)).
  - The freestyle log's drag list has the same gap. It has been open since the 2026-09-26 audit ([02, P3 "Freestyle reorder is drag-only"](../2026-09-26/02-gym-freestyle-stats-history.md)).
- **Why it matters:** With TalkBack on, nothing can change the order of a day's exercises, and that order decides what gets lifted fresh. The editor's two reorder surfaces also behave differently for the same user.
- **Proposal:** Give `DraggableItem` an optional `onMove` and item count. When `onMove` is set, it adds "Move up" and "Move down" custom actions, leaving out whichever can't apply at the ends of the list, as `WeekBarRail`'s `DayBar` does.
  - Put the actions on the row's own merged node, the clickable `ExerciseRow`, rather than a wrapping Box, so TalkBack reads them with the row.
  - Pass `onMove` from the day editor and from `FreestyleLogScreen` ([FreestyleLogScreen.kt:409-410](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/freestyle/FreestyleLogScreen.kt#L409-L410)).

  One change closes the builder gap and the 2026-09-26 freestyle P3. Revised: the shared fix comes first.
- **Doctrine:** PRODUCT confirms TalkBack support as a requirement ([PRODUCT.md:132-137](../../../PRODUCT.md#L132-L137)). §14 semantics ([DESIGN.md:367-370](../../../.claude/DESIGN.md#L367-L370)). It copies the pattern `WeekBarRail` already ships, with no visual change.

### ONB-10 · Each exercise row nests a red remove button inside a tappable row
consistency · impact medium · effort S · Your program → day editor exercise list

- **Now:** `ExerciseRow` is a whole-row `bounceCombinedClick` that opens the sets × reps sheet. It also holds a `ForgeChromeIconButton(Icons.Filled.Close)` tinted `error` that removes the exercise ([ProgramBuilderDayDetail.kt:360-386](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L360-L386)). That is two tap targets in each row and a column of red Material X glyphs down a six-to-eight-row day.
  - The sheet the row opens has "Swap exercise" and "Done" ([:499-502](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L499-L502)).
  - On the same page, "Remove day" is already an outlined destructive capsule with Undo ([:149-155](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L149-L155), [ProgramBuilderScreen.kt:106-112](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderScreen.kt#L106-L112)).
- **Why it matters:** A thumb near the row's right edge removes the exercise instead of opening it, and TalkBack users get two stops on every row.
- **Proposal:** Drop the X. In `SetsRepsSheet`, add `ForgeSecondaryCapsule("Remove from day", destructive = true)` beside "Swap exercise".
  - It calls the existing `onRemoveExercise`. The sheet already closes itself once the exercise's uid is gone ([:314](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L314)), and the existing Undo then shows.
  - The row becomes one tap target, with sets × reps as its right meta.

  This is the same shape SEP-08 gives cardio activities in [09-settings-prefs.md](09-settings-prefs.md).
- **Doctrine:** One tap target per row: §2③ and FAILURES "Fake tap / nested tap" ([DESIGN.md:66](../../../.claude/DESIGN.md#L66), [FAILURES.md:176](../../../.claude/design/FAILURES.md#L176)). §8: a destructive act is an outlined capsule tinted error, paired with Undo ([DESIGN.md:201-203](../../../.claude/DESIGN.md#L201-L203)). §8 icons: content glyphs never come from Material stock, and chrome buttons belong to the top bar ([DESIGN.md:227-230](../../../.claude/DESIGN.md#L227-L230)). The sheet keeps exactly one filled action, Done.

### ONB-04 · The week page doesn't say which weekday each workout falls on, one step after asking
missing-state · impact medium · effort S · Onboarding "Here's your week"

- **Now:** The weekdays step pins workouts to days: "Pick your days and Home shows the right workout on each one" ([OnboardingSteps.kt:217](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingSteps.kt#L217)). On finish, `WeeklySchedule.fromWeekdays` assigns them ([OnboardingViewModel.kt:195-197](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingViewModel.kt#L195-L197), [WeeklySchedule.kt:37-41](../../../forge-android/app/src/main/java/com/forge/app/domain/schedule/WeeklySchedule.kt#L37-L41)).
  - `StepWeek` gets only the archetypes, sets and days ([OnboardingScreen.kt:418](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingScreen.kt#L418)), so the open day reads "6 MOVES · 18 SETS" with no weekday ([OnboardingGymSteps.kt:205-211](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingGymSteps.kt#L205-L211)).
  - The program screen shares this rail so that "the approved week and the saved one can't read as two different weeks", yet it leads the same meta with the weekday ([ProgramBuilderScreen.kt:370-380](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderScreen.kt#L370-L380), [ProgramBuilderModels.kt:125-128](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderModels.kt#L125-L128)).
- **Why it matters:** A Mon/Wed/Fri user approves Push, Pull and Legs without seeing that Legs is on Friday. The first screen after first run shows it.
- **Proposal:** Pass `weekdays.takeIf { fixedDays == true }` into `StepWeek`. Get each day's weekday by inverting `WeeklySchedule.fromWeekdays(weekdays, archetypes.map { it.key })`, and lead `WeekDay`'s meta with it: "FRI · 6 MOVES · 18 SETS", the builder's weekday-first order. Users who picked "Whenever I can" see no change.

  Revised: the time estimate is cut.
  - The "~N min" the finder cited lives on `DayCard`, which renders only in the Train tab that never mounts (see ONB-02).
  - SETTLED bans a time estimate on the warm-up as a number with no decision attached.
  - The day-count page's meter already redraws when the minutes answer trims a day.
- **Doctrine:** §2① puts a number that qualifies an existing row in its right meta ([DESIGN.md:42](../../../.claude/DESIGN.md#L42)). §4.9 ([DESIGN.md:103](../../../.claude/DESIGN.md#L103)). The SETTLED 2026-08-23 week-page shape holds: one mark plus one open day, no new line ([SETTLED.md:188-199](../../../.claude/design/SETTLED.md#L188-L199)).

### ONB-08 · Re-roll on the week page discards the previous week with no way back
ux-friction · impact medium · effort S · Onboarding "Here's your week" bottom bar

- **Now:** `ForgeSecondaryCapsule("Re-roll", onClick = { previewSeed = Random.nextLong() })` ([OnboardingScreen.kt:352](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingScreen.kt#L352)) overwrites the seed held at [:214](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingScreen.kt#L214). The preview is a pure function of the seed and the answers ([OnboardingViewModel.kt:97-106](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingViewModel.kt#L97-L106)), so the old week is one seed away, but nothing keeps that seed.
  - `SnackbarControllerHost` is already composed over onboarding ([MainActivity.kt:648](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L648)).
  - `SnackbarController.showUndo` is the app's one Undo ([SnackbarController.kt:67-68](../../../forge-android/app/src/main/java/com/forge/app/ui/common/SnackbarController.kt#L67-L68)).
- **Why it matters:** Re-rolling is for trying options, and "the last one was better" is the usual second thought. Today that's a dead end, so users either stop re-rolling early or approve a week they liked less.
- **Proposal:** Keep the old seed, then call `snackbarController.showUndo("Week re-rolled") { previewSeed = old }` through `OnboardingViewModel`, with the controller injected there. The seed already rides the draft, so the restored seed is saved too. Check that the snackbar sits above the bottom bar rather than over the CTA.
- **Doctrine:** §12 and §8: reversible acts get Undo through `SnackbarController`, never a dialog ([DESIGN.md:323](../../../.claude/DESIGN.md#L323), [:195-199](../../../.claude/DESIGN.md#L195-L199)). No dialog and no new button.

### ONB-09 · "+ Add day" makes a generic "Day N", leaves the user one tap short of its editor, and picking a type never names it
ux-friction · impact medium · effort S · Your program (edit) → + Add day → day editor

- **Now:** `addDay` appends "Day N" with archetype `fb` and no exercises ([ProgramBuilderViewModel.kt:274-279](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderViewModel.kt#L274-L279)), and the screen only selects it in the rail ([ProgramBuilderScreen.kt:217-222](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderScreen.kt#L217-L222)). The user then taps the day block to open the editor, where the next step is always "+ Add exercise".
  - `setDayType` changes only the archetype ([ProgramBuilderViewModel.kt:322](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderViewModel.kt#L322)). A day typed "Push" still says "Day 3" on the rail and in Home's hero ([OverviewScreen.kt:501](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L501)).
  - Meanwhile the type quietly sets the subtitle under that name ([ProgramRepository.kt:543-547](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L543-L547), [:560-569](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L560-L569), [OverviewScreen.kt:507](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L507)).
- **Why it matters:** Building days is the whole build-your-own path. Every day costs an extra tap, and the week reads "Day 1 / Day 2 / Day 3" unless the user also finds the rename.
- **Proposal:**
  - After "+ Add day", open the new day's editor (`viewModel.openDay(newUid)`).
  - In `setDayType`, when the name is still an automatic one (`^Day \d+$`), replace it with the type's label from `DAY_TYPES` ("Push"), de-duplicated through the existing `copyName`. A name the user typed is never touched.

  Revised: the finder's caption under Type is cut. The tiles say what they are, and §4.3 allows an explainer only beside a non-obvious control. This also eases the 2026-09-26 P4 where "Day N" names collide after a deletion.
- **Doctrine:** §4.3 prose budget ([DESIGN.md:97](../../../.claude/DESIGN.md#L97)) and §13 ([DESIGN.md:329](../../../.claude/DESIGN.md#L329)). It removes a tap and adds no control. Nothing in SETTLED covers the builder's add flow.

### ONB-03 · First run offers no way to restore a backup or bring history from another app
feature-gap · impact medium · effort M · Onboarding first page (plan-mode fork)

- **Now:**
  - **Onboarding only:** while `ONBOARDING_DONE` is false, `MainActivity` composes only `OnboardingScreen` ([MainActivity.kt:602-603](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L602-L603)), and onboarding has no restore or import entry.
  - **Shared files wait:** a backup or CSV shared into the app is held until onboarding finishes ([MainActivity.kt:674-678](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L674-L678)).
  - **No system backup:** `allowBackup="false"` ([AndroidManifest.xml:95](../../../forge-android/app/src/main/AndroidManifest.xml#L95)), so a new phone never restores the app's data automatically.
  - **The only route:** skip setup and accept a bodyweight program ([OnboardingScreen.kt:445-485](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingScreen.kt#L445-L485)), then go to Settings → Export data → Restore, which replaces what was just set up.
- **Why it matters:** Two groups arrive with history: returning users on a new phone, and lifters switching from Strong, Hevy or FitNotes. First run asks them to build a plan that the restore then throws away. Imported history also counts toward the coach's baseline, so importing it at first run makes the coach useful from week one.
- **Proposal:** On the plan-mode page only, put an outlined `ForgeSecondaryCapsule("Restore or import")` beside the CTA, the way the week page pairs Re-roll with its CTA ([OnboardingScreen.kt:350-357](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingScreen.kt#L350-L357)). It opens a small sheet with two rows.
  - **Restore a backup:** the same `OpenDocument` → `BackupRepository.restoreFromUri` → `RestoreRestart` path Settings uses ([BackupRepository.kt:1027](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L1027), [RestoreRestart.kt:19-33](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/RestoreRestart.kt#L19-L33), [SettingsViewModel.kt:730](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L730)). It includes the password prompt for encrypted backups and SED-04's blocking busy state. A ZIP backup carries the preferences, `ONBOARDING_DONE` included ([BackupRepository.kt:926](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BackupRepository.kt#L926)), so the relaunch opens the restored app. An older database-only backup returns to the fork with its history in place, which is acceptable.
  - **Import workouts:** the existing importer pipeline ([SettingsViewModel.kt:620](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L620)), then back to the fork with a one-line count in the sheet.

  Revised: covers password-protected and database-only backups, and reuses the restore pieces from SED-01 and SED-04 in [10-settings-data.md](10-settings-data.md) instead of a second restore flow.
- **Doctrine:**
  - **Opening beat:** SETTLED 2026-09-25 rules out a welcome screen, and the fork stays the opening beat ([SETTLED.md:218-220](../../../.claude/design/SETTLED.md#L218-L220)).
  - **No launch dialog:** SETTLED removed the backup-restored confirmation and every permission ask at cold launch ([SETTLED.md:89-92](../../../.claude/design/SETTLED.md#L89-L92)). This adds neither: the user starts it, and the relaunch is the result.
  - **Buttons:** one filled CTA with an outlined sidekick (§8, [DESIGN.md:201](../../../.claude/DESIGN.md#L201)).

### ONB-05 · After a rebuild, Settings says "Open Gym to see it", and there is no Gym
copy-voice · impact low · effort S · Settings → Program & equipment → Rebuild status line

- **Now:** Generate, Re-roll and Deload end with "… Open Gym to see it." in five strings ([SettingsViewModel.kt:487-490](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L487-L490), [:522-525](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L522-L525), [:541](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsViewModel.kt#L541)). The bottom bar is Cardio · Stats · Home · Coach · Academy ([ForgeBottomBar.kt:34-41](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/ForgeBottomBar.kt#L34-L41)), and the program opens from Plan on Home ([OverviewScreen.kt:581](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L581)).
- **Why it matters:** The only feedback after the page's biggest act sends the user to a tab that doesn't exist.
- **Proposal:** Name the real place:
  - "New 4-day plan ready. See it from Plan on Home."
  - "Exercises re-rolled, same split. See it from Plan on Home."
  - "Deload week set at lighter volume. See it from Plan on Home."

  Do it together with SEP-03 (the confirm) and SEP-11 (the freestyle variants) in [09-settings-prefs.md](09-settings-prefs.md), which touch the same three actions. Revised: the "drop the message after a preview" option went with ONB-01. Impact is lowered because Home updates either way.
- **Doctrine:** §11 translate the machine. "Gym" is the internal module name (`ui/gym`) showing up in copy, which FAILURES calls "Machine leak" ([DESIGN.md:278-281](../../../.claude/DESIGN.md#L278-L281), [FAILURES.md:140](../../../.claude/design/FAILURES.md#L140)). A pointer names its real referent ([DESIGN.md:283](../../../.claude/DESIGN.md#L283)).

### ONB-02 · Home's Plan opens the program on its first day, not the day Home just named
ux-friction · impact low · effort S · Home → Plan → Your program

- **Now:** Home's hero names `sessionDayKey` ([OverviewScreen.kt:496](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L496)), and its Plan capsule opens `Routes.programBuilder(view = true)` ([OverviewScreen.kt:530](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L530), [HubScreen.kt:178](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/HubScreen.kt#L178)). The route has no day argument ([Routes.kt:52](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/Routes.kt#L52), [:60-61](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/Routes.kt#L60-L61)), and the screen opens at `pickedIndex = 0` ([ProgramBuilderScreen.kt:171-175](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderScreen.kt#L171-L175)). So Home says "Pull B" and Plan opens on Push A.
- **Why it matters:** The usual reason to tap Plan beside Start is to see what's in today's session, and every time that takes a second tap on the rail.
- **Proposal:** Add `day: String? = null` to `Routes.programBuilder`. Pass `sessionDayKey` from Home, gym day keys only. On first load, set `pickedUid` to the day with that key. TRF-01 in [01-train-flow.md](01-train-flow.md) puts "Train <day>" on this screen's open day, so that button would then default to Home's day too.

  Revised: the finder's other claims don't hold in the current tree. The Stats day cards, their long-press dialog (single-day re-roll, change colour, edit this day) and the "Pick your day" list all live in `DayListScreen`'s `TrainTab`. That tab never renders, because the Stats tab mounts `DayListScreen` with `initialTab = 1` ([HubScreen.kt:168](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/HubScreen.kt#L168), [DayListScreen.kt:99-117](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/DayListScreen.kt#L99-L117)). The start action belongs to TRF-01, which also ruled that single-day re-roll needs no new home.
- **Doctrine:** No new control, and the top bar is unchanged (§4.6). Nothing in SETTLED covers which day the program screen opens on.

### ONB-14 · The program screen goes by four names, and the week page points at "the editor", which nothing is called
consistency · impact low · effort S · Week page caption, Home, builder title, Settings → Program, skip and freestyle dialogs

- **Now:** The week page says "Change any of it in the editor" ([OnboardingGymSteps.kt:166-167](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingGymSteps.kt#L166-L167)). The screen it means has three other names:
  - **Its title:** "Your program" ([ProgramBuilderScreen.kt:245](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderScreen.kt#L245)).
  - **On Home:** "Plan", or "Build plan" when there is no plan ([OverviewScreen.kt:513](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L513), [:581](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L581)).
  - **In Settings:** "Plan builder", inside a group called "Your plan" whose own "Plan" row opens the days-and-schedule section instead ([SettingsProgramPage.kt:98-107](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsProgramPage.kt#L98-L107)).

  Four dialog strings send users to "Settings → Program" ([OnboardingScreen.kt:452-458](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingScreen.kt#L452-L458), [ProgramBuilderScreen.kt:335-336](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderScreen.kt#L335-L336)), but the page is titled "Program & equipment" ([SettingsScreen.kt:41](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsScreen.kt#L41)).
- **Why it matters:** The first instruction a new user gets about changing their plan names something they can't find. Later, the different entry points read as different tools.
- **Proposal:** Copy only, and one noun: "plan", which Home, the custom-path CTA ("Build my plan") and the Settings group already use.
  - **Week caption:** "Tap a day to read it. Change any of it later from Plan on Home." For a one-day week: "Built from your answers. Change it later from Plan on Home."
  - **Builder title:** "Your plan".
  - **Settings row:** rename "Plan builder" to "Edit days & exercises", so it stops reading as a sibling of the "Plan" row above it. Add the new label to SEP-01's search index.
  - **Dialog pointers:** "Settings → Program & equipment".

  Revised: the Stats "Edit plan" button is in the dead Train tab (ONB-02), and the finder's "Your plan" for the Settings row would have clashed with the group of the same name.
- **Doctrine:** §11: a pointer names its real referent ([DESIGN.md:283-286](../../../.claude/DESIGN.md#L283-L286)). No layout change.

### ONB-15 · The day editor's Color picker sets a colour nothing in the app draws
consistency · impact low · effort S · Your program → day editor → Color

- **Now:** The day editor offers six swatches that write `ProgramDay.accentHex` ([ProgramBuilderDayDetail.kt:229-251](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderDayDetail.kt#L229-L251), [ProgramBuilderViewModel.kt:323](../../../forge-android/app/src/main/java/com/forge/app/ui/programbuilder/ProgramBuilderViewModel.kt#L323)).
  - **Only readers are dead:** `DayCard` and `CompactCard` are the only readers ([DayCard.kt:62](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/components/DayCard.kt#L62), [DayCardComponents.kt:55](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/components/DayCardComponents.kt#L55)), and they render only in the Train tab that never mounts (ONB-02).
  - **No other surface:** since the 2026-09-25 redraw, the program screen draws the week with `WeekBarRail`, in accent for the open day and muted for the rest ([WeekBarRail.kt:195-196](../../../forge-android/app/src/main/java/com/forge/app/ui/common/WeekBarRail.kt#L195-L196)). Onboarding's week page dropped day colour on 2026-08-23. The widget and the watch use the app accent, not the day's.
  - **Stale docs:** SETTLED still says "The Program Editor keeps its dot … where the colour is doing work" ([SETTLED.md:197-199](../../../.claude/design/SETTLED.md#L197-L199)), and MAP still describes a program screen with a colour dot per day ([MAP.md:431-437](../../../.claude/design/MAP.md#L431-L437)).
  - **The finder's second store:** the Stats long-press "Change day color" override it describes is in the same dead tab.
- **Why it matters:** A user picks a colour, saves, and sees no change anywhere. A control whose effect no screen shows is a fake affordance.
- **Proposal:** Antho's call, since SETTLED names the dot. Recommended:
  - Remove the Color row. Keep the column, which generation still fills.
  - Correct SETTLED's 2026-08-23 line and MAP's `PROGRAM_BUILDER` entry to say day colour currently has no surface.
  - The `dayColors` override and `DayColorPickerDialog` go when TRF-01 deletes `TrainTab`.

  If Antho wants day colour back, it needs a place to show first. `WeekBarRail` is the wrong place, because its accent already means "the day you're reading". Revised: the finder's merge of two stores is moot while neither store is visible anywhere.
- **Doctrine:** §2③: "Can't actually run: render passive. Nothing looks tappable while doing nothing." ([DESIGN.md:68](../../../.claude/DESIGN.md#L68)). FAILURES "Fake tap" ([FAILURES.md:176](../../../.claude/design/FAILURES.md#L176)). §5 colour scarcity ([DESIGN.md:119-121](../../../.claude/DESIGN.md#L119-L121)). SETTLED 2026-08-23 ([SETTLED.md:197-199](../../../.claude/design/SETTLED.md#L197-L199)) has been out of date since the 2026-09-25 redraw.

### ONB-16 · The closing step doesn't offer the training reminder, right after the user picked fixed weekdays
feature-gap · impact low · effort S · Onboarding "Anything else?" → Turn on

- **Now:** "Turn on" offers Weekly coach and Lock Avex ([OnboardingExtras.kt:107-118](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingExtras.kt#L107-L118)).
  - **Off by default:** the daily training reminder starts off ([SettingsRepository.kt:240](../../../forge-android/app/src/main/java/com/forge/app/data/prefs/SettingsRepository.kt#L240)) and its switch is in Settings → Notifications ([SettingsSubPages.kt:333-342](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsSubPages.kt#L333-L342)).
  - **Weekday mode:** `TrainingReminderWorker` names the day's workout, and on scheduled rest days it sends a rest-day note instead ([TrainingReminderWorker.kt:47-90](../../../forge-android/app/src/main/java/com/forge/app/service/TrainingReminderWorker.kt#L47-L90)).
  - **Feed promise:** the notification feed's "Turn on notifications" row promises "Training nudges" ([NotificationFeed.kt:282](../../../forge-android/app/src/main/java/com/forge/app/data/repo/NotificationFeed.kt#L282)), which stay off unless the user finds that switch.
- **Why it matters:** The reminder is most useful at the moment a user commits to "Mon, Wed, Fri", and first run never offers it.
- **Proposal:** For generated-plan users who chose fixed weekdays, add a third `ForgeSwitchRow`, "Training reminder", described as "6 pm on your training days, naming the workout. Skipped once you've logged." Save it in `complete()` with `setTrainingReminderEnabled` ([SettingsRepository.kt:790](../../../forge-android/app/src/main/java/com/forge/app/data/prefs/SettingsRepository.kt#L790)).
  - **No permission prompt:** don't request `POST_NOTIFICATIONS` from the switch. The app's settled route is the feed row that opens the OS notification screen ([MainActivity.kt:431-434](../../../forge-android/app/src/main/java/com/forge/app/MainActivity.kt#L431-L434)), which the user meets on Home.
  - **Ship after the copy fix:** do this after the 2026-09-26 fix to `TrainingReminder.kt`, which removes the em dashes and the 🔥 emoji in every nudge ([11, P4](../2026-09-26/11-domain-services-core.md)), so a new user's first reminder isn't in the wrong voice.

  Revised: the runtime permission request is dropped. The row is limited to fixed-weekday users, because sequence mode has no rest days and would nudge on every untrained day. "Quiet on rest days" was inaccurate and is corrected.
- **Doctrine:** SETTLED 2026-08-22: settings belong on the optional closing step, after the plan exists ([SETTLED.md:152-158](../../../.claude/design/SETTLED.md#L152-L158)). SETTLED 2026-07-27: no permission dialog, and notices live in the feed ([SETTLED.md:89-92](../../../.claude/design/SETTLED.md#L89-L92)). One more switch in an existing group adds no block to a step DECISIONS already warned reads as a wall ([DECISIONS.md:110-115](../../../.claude/design/DECISIONS.md#L110-L115)).

## Considered and dropped

| ID | Idea | Why dropped |
|---|---|---|
| ONB-01 | Show a preview sheet before Settings' Generate, Re-roll and Deload commit | Duplicate of SEP-03 in [09-settings-prefs.md](09-settings-prefs.md): same three rows, same problem. SEP-03 chose `SettingsConfirmDialog` and recorded why a faithful undo is effort L. If SEP-03's dialog is ever replaced, onboarding's seeded `buildPreview` drawn with `WeekBarRail` is the shape to reuse. |
| ONB-06 | Goal, experience, emphasis and length changes wait for a rebuild, and nothing says the plan is out of date | Duplicate of SEP-02: the same snapshot of generation params, the same Rebuild footer reading, the same fold of Equipment's local `remember` flag and the same footer rewording. |
| ONB-07 | Tap a movement on the week page to strike it and re-deal that day | Re-dealing the whole open day to remove one movement is the bluntness SEP-12 rejected for `rerollDay`. The saved week is rebuilt from the seed ([OnboardingViewModel.kt:243-256](../../../forge-android/app/src/main/java/com/forge/app/ui/onboarding/OnboardingViewModel.kt#L243-L256)), so keeping struck days as shown needs a new save path for previewed days (effort L, not M). It also adds editing to the approval page that DECISIONS 2026-08-22 kept lean. ONB-08's Undo covers the re-roll regret. |
| ONB-13 | Sets-per-muscle bars under the builder's edit view | §3 bans one long multi-block scroll for the editor archetype ([DESIGN.md:87](../../../.claude/DESIGN.md#L87)). The program screen is deliberately one mark plus one open day (the SETTLED 2026-08-23 week-page shape, which the 2026-09-25 redraw adopted), and about 12 muscle rows under a 6–8 row day would double it. Planned per-muscle sets already draw as track lengths in Stats' Volume lens ([StatsContent.kt:273](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/StatsContent.kt#L273)). |
