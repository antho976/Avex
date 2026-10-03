# Multi-week arcs and life events (journey): deload week, block end, holiday, layoff return, sick/sore days

Scope: Home's hero and THIS WEEK strip, the session's load cue, Coach (account, Where you stand, BLOCK), the check-in, the daily and Monday pushes, the watch Today tile; `domain/coach` (LifeEvents, TodayDirective, PreSessionBrief, BlockPlanner, AutoCoachPlanner, WeeklyReview), `domain/adapt` (ReadinessAdvisor, ProgressionAdvisor), `DirectiveRepository`, `CoachRepository`, `BlockRepository`, `StatsRepository`, both workers. 13 raised, 12 kept (9 revised), 1 dropped.

The engine models every life event: holidays, layoffs and the return ramp, sickness, sore muscles, deloads and four block phases. The debt is that each surface reads a different slice of that model. Holidays reach the streak and the Monday pass but not the one answer Home, the watch and the reminder give. The easing Home describes and the weight on the bar come from three separate computations that disagree. The block's milestones (the deload week, the test week, the block's end) show only on Coach's advanced BLOCK section, or nowhere. Most fixes route the arc through `TodayDirective`, which every surface already reads, and through one load function. None of them needs a new surface.

## Findings

### ARC-01 · A declared holiday never reaches Today: Home, the week strip, the watch tile and the Monday pushes still say train
missing-state · impact high · effort M · Home hero and THIS WEEK strip; Wear Today tile; Monday pushes; Coach hold line

- **Now:** Holidays are read by Settings, the streak and trophy math, readiness spacing, the Monday pass hold and the come-back nudge. Nothing that answers "what do I do today" reads them.
  - **Home and watch:** `TodayDirective.compute` takes no holiday input, and `Kind` has no holiday case ([TodayDirective.kt:28-40](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L28-L40), [:73-87](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L73-L87)). `DirectiveRepository.compute` never touches `vacationDao` ([DirectiveRepository.kt:72-76](../../../forge-android/app/src/main/java/com/forge/app/data/repo/DirectiveRepository.kt#L72-L76)). On day 3 of a declared trip the hero reads "Pull B · It's been 3 days since your last session." over the filled Start session ([TodayDirective.kt:208](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L208)). From day 14 it reads "It's been 14 days. Start light: this one is about showing up." ([:132](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L132)). The watch tile publishes the same headline and reason ([WearStatePublisher.kt:167-179](../../../forge-android/app/src/main/java/com/forge/app/service/wear/WearStatePublisher.kt#L167-L179)).
  - **Week strip:** `weekRestDays` comes only from the schedule history, even though the holiday list is in the same `combine` ([StatsRepository.kt:222-233](../../../forge-android/app/src/main/java/com/forge/app/data/repo/StatsRepository.kt#L222-L233)). Past holiday days therefore draw the full-alpha open fill, the strip's "missed" cell ([SurfaceKit.kt:403](../../../forge-android/app/src/main/java/com/forge/app/ui/experiment/SurfaceKit.kt#L403)). The anchor's "1 / 4 days" counts them as training days ([OverviewScreen.kt:610](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L610)).
  - **Monday:** a vacation pass is a HOLD, so the brief push still fires as "Your coach has an update / Coach · holding steady this week" ([WeeklyRecapWorker.kt:84-89](../../../forge-android/app/src/main/java/com/forge/app/service/WeeklyRecapWorker.kt#L84-L89), [CoachRepository.kt:1062](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L1062)). The deload push has no holiday check ([WeeklyRecapWorker.kt:96-103](../../../forge-android/app/src/main/java/com/forge/app/service/WeeklyRecapWorker.kt#L96-L103)). Only the come-back nudge checks `isOnVacationToday()` ([:116](../../../forge-android/app/src/main/java/com/forge/app/service/WeeklyRecapWorker.kt#L116)).
  - **Coach:** the hold is stored as "On vacation — the coach is paused until you're back. Enjoy it." ([CoachRepository.kt:269-271](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L269-L271)). It has no end date, an em dash, and the word Settings dropped for "Holidays" (DECISIONS, Settings rebuild).
- **Why it matters:** This is the one feature built for time away, and it is invisible on the surfaces a traveller actually glances at. The app spends the trip telling them to train and draws every holiday day as a miss.
- **Proposal:**
  1. `DirectiveRepository` reads `vacationDao.all()` once and passes a `HolidayContext(endsOn, endedOn, range)` into `TodayDirective.compute`.
  2. Add a branch directly after the sick one. It returns REST with the headline "Rest today", the reason "On holiday until Sunday, Oct 12.", and in the existing `secondary` slot "Pull B is first back." (from `WeeklySchedule.upcomingKey`, else next-up). The CTA takes HOM-02's REST treatment ("Train anyway"). The watch tile picks this up unchanged, and so will the widget once HOM-13 lands.
  3. On the first TRAIN day after a holiday ends, the reason becomes "First session since your holiday, Sep 20 to Oct 1." instead of the bare day count. The layoff ramp copy still outranks it at 14 days or more.
  4. `StatsRepository` derives `weekHolidayDays` from the periods already in its `combine`. `WeekStrip` draws them with its existing rest bar, the reading gains ", 2 holiday days", and the anchor's denominator drops planned training days that fall inside the holiday.
  5. `WeeklyRecapWorker` skips the brief and deload pushes when `isOnVacationToday()`, as the come-back nudge already does. SEP-05 keeps the daily reminder skip.
  6. The hold is written as "On holiday until Oct 12. The coach picks up when you're back."

  Revised: the anchor denominator is added, and the copy drops its parentheses.
- **Doctrine:** SETTLED 2026-08-16 (Home): the hero's button follows the coach, so a holiday becomes one more coach answer and draws REST, with no accent fill on a day off (HOM-02). DESIGN §3: the serif headline stays a decision ("Rest today"), and the status sits in the reason line. §4.3: one reason line is replaced and no section is added. §4.6: no strip or banner. The strip reuses its own rest bar, so it gains no new mark. FAILURES *Two units, one section*: the anchor and the strip count the same days. §11: the end date is named. COACH_V3_PLAN "Modes the coach must survive" requires every surface to declare its vacation behaviour, and the directive never did. SEP-05 (Holidays page, reminder skip) and SEP-04 (root row) are untouched. Nothing in SETTLED's removals ledger comes back.

### ARC-02 · Home promises lighter loads (first week back, sore muscles, deload) that the session's "try X" doesn't deliver
consistency · impact high · effort M · Home hero reason vs the live session's load cue; Coach "Readiness today"

- **Now:** Three code paths describe the same easing three ways.
  - **First week back:** Home says "loads hold about 10% under" (`RAMP_SCALE` 0.9, [TodayDirective.kt:134](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L134), [LifeEvents.kt:42](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/LifeEvents.kt#L42)). The session's suggestion receives only readiness and the block phase ([DayViewModel.kt:279-280](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/DayViewModel.kt#L279-L280), [DayViewModelBuilders.kt:131-134](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/DayViewModelBuilders.kt#L131-L134)). Readiness carries only a 3% comeback penalty ([ReadinessAdvisor.kt:75-80](../../../forge-android/app/src/main/java/com/forge/app/domain/adapt/ReadinessAdvisor.kt#L75-L80), [AdaptThresholds.kt:146](../../../forge-android/app/src/main/java/com/forge/app/domain/adapt/AdaptThresholds.kt#L146)), so the "try X" cue eases 3%. Coach says "Targets 3% lighter" ([CoachNow.kt:73](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachNow.kt#L73)). The brief multiplies both factors to about 13% ([PreSessionBrief.kt:86](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/PreSessionBrief.kt#L86)), and HOM-03 is about to put that brief on Home.
  - **Sore check-in:** Home says "You flagged quads as sore, so those come in lighter" ([TodayDirective.kt:214-220](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L214-L220)). Only the brief eases those muscles, by 10% ([PreSessionBrief.kt:129](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/PreSessionBrief.kt#L129)). The cue sees a flat −2% "sore" on every lift ([ReadinessAdvisor.kt:147-149](../../../forge-android/app/src/main/java/com/forge/app/domain/adapt/ReadinessAdvisor.kt#L147-L149)), so a squat on a sore-quads day can still read "try 105 kg".
  - **Deload without a block:** a coach-call or Settings deload cuts sets (`DELOAD_FACTOR` 0.55, [ProgramGenerator.kt:107](../../../forge-android/app/src/main/java/com/forge/app/program/ProgramGenerator.kt#L107)) but leaves `blockPhase` null. The cue can therefore offer a progression jump, while a block-served deload eases loads 15% (`DELOAD` 0.85). Two deloads, two load rules.
  - The card's plan line carries no reason by design ([ExerciseCard.kt:193-199](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/components/ExerciseCard.kt#L193-L199)), so the number is the only place the easing can show.
- **Why it matters:** These are the most fragile weeks: coming back, sore, or deloading. The only explanation the lifter gets is on Home, and the weight they then load contradicts it.
- **Proposal:**
  1. Add one function, `TodayLoad.scale(readiness, life, phase, deloadRunning, muscle)` in `domain/coach`. `PreSessionBrief`, `ProgressionAdvisor.suggestNextLoad` (new `life` and `muscle` parameters) and Coach's readiness line all call it.
  2. Inside it:
     - `life.loadScale` replaces the readiness comeback penalty instead of stacking on it (COACH_V3_PLAN M6, one signal and one computation).
     - A sore muscle's 0.9 applies per slot.
     - A running deload window (`ProgramRepository.deloadWeekRunning()`) counts as `BlockPhase.DELOAD` when no block phase is set.
  3. `DayViewModel` loads `lifeEvents()` and `deloadWeekRunning()` beside readiness at line 279.
  4. TodayDirective's ramp and soreness reasons print the composed percent from the same function. Coach's line reads "Targets 10% lighter" with the parts as its sub ("first week back after 16 days · slept badly").
  5. A unit test pins Home's stated percent to the cue's delta.

  Revised: the deload case is restated against the planner's own copy ("lighter volume across the board", [AutoCoachPlanner.kt:447](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/AutoCoachPlanner.kt#L447)). The problem is the two deload rules, not a promise of lighter loads.
- **Doctrine:** The live-session screen is untouchable (SETTLED) and nothing it draws changes. Only the value of the existing cue moves, and no reason text returns to the card. FAILURES *Verdict without a reading*: the stated reading and the delivered number become one. AUDIT_DEFERRED H-02's 0.80–1.10 composition clamp is kept. Not tracked anywhere else: the 2026-09-26 audit 10 P1 (same-weight branches unscaled) is fixed in the current tree ([ProgressionAdvisor.kt:144](../../../forge-android/app/src/main/java/com/forge/app/domain/adapt/ProgressionAdvisor.kt#L144)) and was a different defect.

### ARC-11 · In the week back, every scheduled rest day reads "Ease back in" with a filled Start session
consistency · impact medium · effort S · Home hero; Wear Today tile

- **Now:** The layoff branch returns TRAIN before the rest-day check runs ([TodayDirective.kt:126-141](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L126-L141) vs [:186-202](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L186-L202)). On a weekday rest slot `nextUpDayKey` is null ([DirectiveRepository.kt:122](../../../forge-android/app/src/main/java/com/forge/app/data/repo/DirectiveRepository.kt#L122)). The result is the headline "Ease back in", kind TRAIN, and `dayKey` null. Home falls back to `state.nextUpDayKey` and draws the accent "Start session" for a later day ([OverviewScreen.kt:496](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L496), [:510-525](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L510-L525)). `DirectiveRepository`'s own comment (H-07) says only a day the resolver places TODAY may become "train" ([DirectiveRepository.kt:114-121](../../../forge-android/app/src/main/java/com/forge/app/data/repo/DirectiveRepository.kt#L114-L121)). The same happens while still away (14 days or more, no session yet).
- **Why it matters:** For a week after a break of two weeks or more, the week the coach should be gentlest, fixed-weekday lifters are told to train on their rest days.
- **Proposal:**
  1. In the layoff branch, when the user isn't in freestyle and `nextUpDayKey` is null, return REST. The headline is "Rest today". The reason is "First week back after 16 days off." (returning) or "16 days since your last session." (away). The `secondary` slot reads "Pull B is next, on Thursday, loads about 10% under." `upcomingDayKey` and `upcomingInDays` are already passed in.
  2. Training days keep TRAIN.
  3. Add a weekday-mode rest-day case inside the ramp to `TodayDirectiveTest`.

  Land it with FWK-02 in [15-first-week.md](15-first-week.md), which fixes the same skipped rest-day check in the cold-start branch of the same function. Revised: covers the away case and names its sibling.
- **Doctrine:** Restores the H-07 placement rule (AUDIT_DEFERRED, closed for the main path only). SETTLED 2026-08-16: REST gets the non-accent CTA (HOM-02). Uses the existing reason and secondary slots (§4.3).

### ARC-09 · "Feeling unwell" can't be switched off: a better next-day check-in is ignored for three days
ux-friction · impact medium · effort S · Daily check-in; Home hero on the days after

- **Now:** `sick` is true when any check-in in the last 72 hours has the flag ([LifeEvents.kt:112-114](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/LifeEvents.kt#L112-L114)). The next day's check-in opens on a fresh row with the switch off, because `open()` reads only today's entry ([CheckinViewModel.kt:74-91](../../../forge-android/app/src/main/java/com/forge/app/ui/checkin/CheckinViewModel.kt#L74-L91)). Saving it changes nothing: Home keeps "Rest today. You flagged being unwell." ([TodayDirective.kt:89-96](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L89-L96)) until the third morning, and readiness keeps its −5%. The switch's caption promises "Nothing is pushed until it passes" ([CheckinSheet.kt:171-176](../../../forge-android/app/src/main/java/com/forge/app/ui/checkin/CheckinSheet.kt#L171-L176)), and nothing lets the user say it passed.
- **Why it matters:** Someone over a 24-hour bug is told to rest for two more days, and the check-in they just answered appears not to work.
- **Proposal:**
  1. In `LifeEvents.assess`, the newest answered check-in in the window decides. If it is newer than the sick one and has sick off, the user is no longer sick. The cardio "sick" rest reason follows the same newest-wins rule.
  2. `CheckinViewModel.open()` starts the switch on while an earlier day's flag is still live. While it is on, the row reads "Still unwell" with the line "Flagged Tuesday. Switch off once it has passed."
  3. Home's REST reason names the day: "You flagged being unwell on Tuesday."
- **Doctrine:** The check-in stays behind the bell with no new entry point (SETTLED 2026-09-01). HOM-08 owns reopening today's saved answer; this is about the days after. Reuses `ForgeSwitchRow`. §11: a status names its referent (the day). No dialog.

### ARC-10 · On a sick day the reminder still pushes the streak, and resting as the coach says breaks it
consistency · impact medium · effort S · Daily training reminder; streak (Profile, widget, recap)

- **Now:** The reminder worker resolves the schedule itself and never reads the check-in or the directive ([TrainingReminderWorker.kt:47-92](../../../forge-android/app/src/main/java/com/forge/app/service/TrainingReminderWorker.kt#L47-L92)). After a morning "Feeling unwell", Home says "Rest today", and the evening push says "Don't break your streak · 🔥 12-day streak — Pull B is on today. Keep the chain alive." ([TrainingReminder.kt:56-67](../../../forge-android/app/src/main/java/com/forge/app/domain/notify/TrainingReminder.kt#L56-L67)). The streak bridges holidays and planned weekday rests only ([StatsRepository.kt:293-300](../../../forge-android/app/src/main/java/com/forge/app/data/repo/StatsRepository.kt#L293-L300)), so the days the coach told the user to rest reset it. After a layoff the push still says "Pull B today", with nothing about easing in.
- **Why it matters:** The app argues with itself on the day the user is least well, and the streak punishes following the coach.
- **Proposal:**
  1. After its existing guards, `TrainingReminderWorker` reads `AdaptationRepository.lifeEvents()`. That is a few small queries, not the whole-history snapshot `DirectiveRepository.today()` builds (HOM-13 notes that cost).
     - Sick: no push.
     - Layoff returning or away: the title is the day name, and the body is `LifeEvents.explain`'s line.
     - Never the streak branch while sick.
  2. `streakBridge` also bridges days with no session inside a sick window (`sickAtMs` plus `SICK_WINDOW_DAYS`), the same way it bridges holidays. `TrophyRepository`'s max-streak bridge takes the same input so the two counts can't drift.

  RCH-10 keeps the wording and SEP-05 adds holidays to the same skip. Revised: reads life events instead of the full directive, to keep the worker inside its window.
- **Doctrine:** SETTLED 2026-08-16: surfaces follow the coach. `DirectiveRepository`'s KDoc names one answer for every surface. §11: no pressure the data doesn't support. No channel, notice kind or gamification surface is added. The streak already exists, and the bridge mirrors the holiday rule.

### ARC-03 · A deload week or the block's test week reads like any other day on Home
feature-gap · impact medium · effort S · Home hero reason; Wear Today tile; widget once HOM-13 lands

- **Now:** The directive takes no week context. `DirectiveRepository` reads the block phase only to hand it to the unrendered brief ([DirectiveRepository.kt:151-162](../../../forge-android/app/src/main/java/com/forge/app/data/repo/DirectiveRepository.kt#L151-L162)).
  - **Deload week:** on every TRAIN day of a deload, from any of its three entry points, the reason is "It's what today's schedule calls for." ([TodayDirective.kt:209](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/TodayDirective.kt#L209)). The window's end is known (`deloadWeekEndMs`, [WeekMath.kt:72-79](../../../forge-android/app/src/main/java/com/forge/app/core/time/WeekMath.kt#L72-L79)) and never shown. Coach's account does say it: "Deload week of your block" for a block deload ([AutoCoachPlanner.kt:436-441](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/AutoCoachPlanner.kt#L436-L441)), or the applied entry for a coach call. A Settings deload leaves no trace outside its snackbar.
  - **Peak (test) week:** its one ask, "Test week: take one heavy top set on your focus lift", renders only in BLOCK ([CoachBlock.kt:67-74](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachBlock.kt#L67-L74)), which draws only with Advanced tracking on ([CoachScreen.kt:244-256](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachScreen.kt#L244-L256)). `BlockPlanner.describe`'s KDoc says "the coach screen and the directive both use" it ([BlockPlanner.kt:159](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/BlockPlanner.kt#L159)), but the directive never does. COACH_V3_PLAN Phase C asks for test days "announced by the directive", and its status line marks C built.
- **Why it matters:** The screen opened before training doesn't say this week is different, how long it lasts, or what test day asks for.
- **Proposal:** Pass `WeekContext(deloadEndsOn: LocalDate?, phase, testLift: String?)` into `TodayDirective.compute`. It replaces only the routine reason on a TRAIN day with no life event:
  - **Deload:** "Deload week until Sunday. Fewer sets this week." Once ARC-02 lands, add ", loads 15% under", with the percent from ARC-02's function.
  - **Peak:** "Test week. Work up to one heavy top set on Bench Press." The lift is CCH-10's goal lift once it lands, else the tracked lift with the most bouts in the block.

  These outrank HOM-03's brief line and yield to sick, holiday (ARC-01), layoff and the gap of 3 days or more. Revised: the Now no longer claims the deload is unannounced anywhere (Coach's account carries it). Impact lowered to medium, because blocks are an advanced-tracking feature.
- **Doctrine:** Replaces one reason line (§4.3; no *Caption stack*). §11: the countdown names its landing day. Home stays "what do I do now" (SETTLED 2026-08-16). Not a resident strip (§4.6). The serif headline stays the day name, so no status verdict. AUDIT_DEFERRED H-02 still owns Peak's tagged test prescription, TRF-03 owns session-type tagging, and CCH-10 owns the goal; this is copy only.

### ARC-12 · The coach's weekly cue can't tell a holiday or a layoff from training, and the gap hold says "back" while you're still away
missing-state · impact medium · effort S · Coach THIS WEEK quiet entry (CCH-03), the hold-week push (RIT-13)

- **Now:** CCH-03 puts `focusLine` on Coach's quiet weeks, and RIT-13 puts it in the hold-week push. `WeeklyReview.assemble` takes no life events and no holidays ([WeeklyReview.kt:136-146](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/WeeklyReview.kt#L136-L146)).
  - **After a layoff:** `mesocycleFocus` anchors on the last deload and counts calendar weeks straight through the time away, so the Monday after three weeks off can read "A long stretch with no down week. A lighter week soon will bank the gains." ([:190-217](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/WeeklyReview.kt#L190-L217)).
  - **After a holiday week:** a lifter without enough history for that cue gets `quietFocus`'s "Volume dipped 100%. Get the sessions in and finish every range." ([:170-180](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/WeeklyReview.kt#L170-L180)).
  - **Already live:** the gap hold is written once 14 days pass with no session, so the lifter is still away, yet it reads "First week back after a break" ([AutoCoachPlanner.kt:142-145](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/AutoCoachPlanner.kt#L142-L145)). The account shows that first clause today.
- **Why it matters:** The line meant to be the coach's one cue for the week tells a returning lifter to deload, or to make up volume they skipped on purpose. Wiring CCH-03 and RIT-13 as written would put these lines in front of people.
- **Proposal:**
  1. Pass `LifeEvents.State` and last week's holiday-day count into `assemble()`.
  2. After `hasDeloadShadow`, in order:
     - **Still away:** "No sessions since Sep 14. The plan holds until you're back."
     - **Returning:** "First week back after 16 days. Run the plan as written; loads hold 10% under until Oct 9."
     - **Last week had 4 or more holiday days:** "Back from your holiday. Same plan, nothing judged from those days."
  3. `mesocycleFocus` re-anchors at `layoff.returnedAtMs` when that is later than the last deload.
  4. Split the gap hold: "Away 16 days. The plan holds until you're back." while no session has landed, and the existing return wording after one.

  It should land before, or with, CCH-03 and RIT-13.
- **Doctrine:** §11: generated lines vary with the user's numbers and stay grounded. No surface, section or push is added. Audit 10's P3 ("computed but never shown") names latent bugs that reach users once `focusLine` is wired, and this one isn't among them.

### ARC-07 · BLOCK's figure counts weeks while its rail draws four equal phases, and the deload and test weeks have no dates
consistency · impact medium · effort S · Coach > BLOCK (advanced)

- **Now:** In the golden ([coach-ledger-block.png](../../../forge-android/app/src/test/screenshots/coach-ledger-block.png)), "Week 3 of 6" sits over a rail of four `weight(1f)` segments, one per phase ([CoachBlock.kt:47-58](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachBlock.kt#L47-L58), [:131-171](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachBlock.kt#L131-L171)). Halfway through the block one quarter is lit, because a 6-week block is three Accumulate weeks plus one week of each other phase ([BlockPlanner.kt:101-107](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/BlockPlanner.kt#L101-L107)). An early fatigue deload jumps the light to the last quarter. "Deload in 3 weeks" never names the week ([BlockPlanner.kt:171-181](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/BlockPlanner.kt#L171-L181)), the Peak segment never says it is the test week, and TalkBack hears only "Training block phase: Accumulate" ([CoachBlock.kt:137](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachBlock.kt#L137)).
- **Why it matters:** The mark and its own figure disagree, and the two dates a lifter plans around, test day and the deload, aren't on the page.
- **Proposal:**
  1. Draw one cell per planned week: 3dp tall, with 2dp gaps inside a phase and 6dp between phases. Each phase label sits under its own span. Past weeks use `c.secondary`, the current week `c.accent`, weeks ahead `c.track`. An early deload greys the weeks it skipped.
  2. The purpose line becomes "Test week of Oct 19 · deload week of Oct 26", derived from `startedAt` and `weekIndex`.
  3. The `contentDescription` reads "Week 3 of 6, Accumulate. Test week of Oct 19, deload week of Oct 26."
- **Doctrine:** FAILURES *Two units, one section*: the mark counts what the figure counts. §2② progress meter with the real total. §11: a countdown names its landing day. §14: a value-reading description. It stays a thin rail that is unlit at zero, and BLOCK keeps its advanced-only placement (MAP, Advanced tracking 2026-09-11).

### ARC-05 · An applied deload is the one coach change with no Undo, even when autopilot applied it
ux-friction · impact medium · effort M · Coach account, the "Deload week" entry

- **Now:** `applyDeloadLocked` stamps the call applied with `undoData` null ("Not undoable — regenerating again is the way back", [CoachRepository.kt:658-668](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L658-L668)), and undo returns false for the deload type ([:895](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L895)). The entry never draws its Undo pill ([CoachAccount.kt:336-343](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachAccount.kt#L336-L343)). Autopilot applies deloads once the type is earned ([CoachRepository.kt:342](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L342)), and "Apply all" takes them with the rest. The only way back is a Settings regenerate or re-roll, which replaces the whole plan (SEP-03). `restoreAfterDeload()` already rebuilds the same program at full volume ([ProgramRepository.kt:426-431](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProgramRepository.kt#L426-L431)), and the window's natural end runs exactly that.
- **Why it matters:** A mis-tap on "Apply all", an autopilot deload the lifter didn't want, or a lifter who feels fresh by Wednesday is locked into a reduced week. PRODUCT lists undo on every proposed change as one of the four things that must survive.
- **Proposal:**
  1. Store `undoData = "deload"` on apply.
  2. While the window runs, Undo goes through the existing `ProgramChangeGuardHost` (it discards an open workout, exactly as apply does). It then calls `restoreAfterDeload()`, clears the deload marker, `markReverted`s the call and refreshes the brief.
  3. The entry's existing Undo pill and "undone" stamp render it, and the trust ledger counts it as reverted like any other type.
  4. After the window closes there is no Undo, because the week has run.

  This is not the faithful pre-state undo SEP-03 rated L: it ends the deload early through the same path the window's end already takes. Block-served and Settings deloads have no call row and are out of scope. Revised: adds the autopilot case and states the undo's semantics.
- **Doctrine:** PRODUCT "Must survive" #1 and Principle 1 (consent is always reversible). §8: reuses the entry's Undo control, with no new button. §12 prefers undo over confirm. The guard dialog appears only because a workout would be destroyed, the rule apply already follows.

### ARC-06 · A finished block disappears: no closing read, and its deload hold promises a next block that never comes
feature-gap · impact medium · effort M · Coach > BLOCK (advanced); the deload-week hold line

- **Now:** The pass after the deload week stamps `endedAt` and does nothing else ([BlockPlanner.kt:96-97](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/BlockPlanner.kt#L96-L97)). The deload week's own hold reads "...and the next block starts from a fresh baseline" ([AutoCoachPlanner.kt:440](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/AutoCoachPlanner.kt#L440)), but no block starts. BLOCK falls back to the unlit rail and "Start a block →", which always starts a default 5-week block ([BlockRepository.kt:73](../../../forge-android/app/src/main/java/com/forge/app/data/repo/BlockRepository.kt#L73)). Nothing compares where the block's lifts started with where they finished. `TrainingBlockDao.all()` keeps ended blocks, and the snapshot holds every bout's e1RM across the block's dates.
- **Why it matters:** Five or six weeks of periodisation end with no answer to "did it work?". The block was the arc, and it stops without a word.
- **Proposal:**
  1. While no block runs and the last block ended within four weeks, BLOCK's zero state reads that block instead of a bare rail. The figure is the change on its focus lift ("100 → 106 kg"). The caption is "Bench Press over block 1, Sep 1 to Oct 5 · 14 of 15 sessions". The rail draws fully in the secondary rung. The focus lift is CCH-10's goal lift once it lands, else the lift with the most bouts in the block.
  2. The action reads "Start block 2 →" and reuses the ended block's `plannedWeeks` and `focusGoalId`.
  3. The deload hold is reworded so it promises nothing that doesn't happen: "...run it as written and sleep. The block closes when this week ends."

  Revised: the finder proposed a new "block" decision type on the account. That puts a block call in front of advanced-off users, whom the 2026-09-11 decision keeps to the account and NEXT. A new type would also need watcher semantics and autopilot rules (COACH_V3_PLAN: "no unwatched writes, ever"). The closing read lives where the block already lives.
- **Doctrine:** PRODUCT "Must survive" #4 (goals, block, project). DESIGN §3 Coach rule: status states are an eyebrow plus figures, and the figure here is a reading, not a verdict (SETTLED, Coach status serif verdicts removed). §12: BLOCK's zero state gets a real reading. This is not the removed "Coming up" countdown (SETTLED 2026-08-20). CCH-12 (Undo on End) and CCH-10 (goal naming) stay separate.

### ARC-08 · A declared holiday pauses the coach but not the block
consistency · impact medium · effort M · Coach BLOCK and the served deload, after time away

- **Now:** `ensureWeeklyPass` advances the block ([CoachRepository.kt:265](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L265)) before it checks the holiday ([:269](../../../forge-android/app/src/main/java/com/forge/app/data/repo/CoachRepository.kt#L269)). `advance()` deliberately catches up every elapsed ISO week ([BlockPlanner.kt:63-84](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/BlockPlanner.kt#L63-L84)), and the pass runs during the trip anyway from the Monday worker's `pendingBanner()`.
  - **Two weeks away in Intensify:** the lifter comes back to a deload served while away, and the Peak test week is skipped.
  - **Three weeks away:** the block ends while they're gone, and they come back to "Start a block →" with no word why.

  `VacationPeriod`'s own KDoc says "streak and deload counters pause during this window" ([VacationPeriod.kt:7](../../../forge-android/app/src/main/java/com/forge/app/data/db/entities/VacationPeriod.kt#L7)). COACH_V3_PLAN's life-events section plans to "restart or extend the block" after a layoff, and that part was never built.
- **Why it matters:** The lifter who planned ahead loses the block's build and test weeks to a trip, which is exactly the case the holiday feature exists for.
- **Proposal:**
  1. `BlockPlanner.advance` takes `pausedWeeks: Set<String>`. `BlockRepository.advanceForWeek` builds it from `vacationDao.all()`, counting an ISO week as paused when it has 4 or more holiday days.
  2. `advance` steps only the weeks that aren't paused, but still moves `lastAdvancedWeek`.
  3. While paused, BLOCK's purpose line reads "Paused for your holiday · resumes week of Oct 13", and ARC-01's hold line adds "Your block is paused too."
  4. An undeclared layoff keeps today's catch-up, because that time really did pass.

  Revised: the finder's note that ARC-13 lets this be declared afterwards is dropped. The block steps in the background during the trip, so a holiday declared after the fact can't un-advance it.
- **Doctrine:** It honours the contract the entity already states, using data already stored. No new control or section. One caption temporarily replaces the purpose line (§4.3), and §11 names the landing week. The 2026-09-26 audit 08 P1 (unguarded background regeneration) is the correctness half of this same call order and stays there. CCH-12 is unaffected.

### ARC-13 · Holidays can only be declared ahead of time, from deep in Settings
discoverability · impact low · effort M · Notifications feed; Settings > Holidays

- **Now:** The only way in is Settings > Coach & recovery > Holidays, captioned "Pause your streak while you're away" ([SettingsMainList.kt:316](../../../forge-android/app/src/main/java/com/forge/app/ui/settings/SettingsMainList.kt#L316)), plus Settings search. Nothing suggests it when a gap actually happens. `VacationCalendar` handles past dates, so a trip declared afterwards restores the streak and excuses readiness spacing. With ARC-01 and ARC-12 it also changes Home's and the coach's copy. No surface offers it.
- **Why it matters:** Few people open Settings before a flight, so the feature mostly helps people who already know it exists.
- **Proposal:**
  1. Add `NoticeKind.AWAY` after `CHECK_IN` ([NotificationFeed.kt:59](../../../forge-android/app/src/main/java/com/forge/app/data/repo/NotificationFeed.kt#L59)): "Time away", explainer "Mark a gap as a holiday".
  2. Raise one row only once the lifter is back: the first finished session after a gap of 5 or more days that no holiday covers. Its title is "Back after 9 days", and its detail "Away? Mark Sep 15 to Sep 23 as a holiday".
  3. Its action (a new `NoticeAction.OpenHolidays(start, end)`) opens Settings > Holidays with the add dialog pre-filled.
  4. Saving or dismissing clears the row, and it lapses after 7 days. It gets the usual toggle under Settings > Notifications.

  Revised: the finder raised the row during the absence ("No sessions since Sep 14"). That is a lapse notice the come-back nudge already covers gently, and it would sit in the feed for the whole trip. Offering it on return carries no pressure.
- **Doctrine:** §4.6 and SETTLED 2026-07-27: anything waiting on you lives in the feed, so no page strip and no launch prompt (SETTLED 2026-09-01). Declaration order is feed rank. No push is added, and the come-back copy stays RCH-10's. SEP-05 and SEP-04 still own the Holidays page and its root row.

## Considered and dropped

| ID | Idea | Why dropped |
|---|---|---|
| ARC-04 | Restore full volume at the week boundary instead of on the first finish after the deload window, and stamp a closing "18 → 24 sets a week" reading on the deload entry | The restore timing is a correctness defect: `maybeRotateProgram` restores on the first finish after the window, so the following week's first session runs deload volume ([WorkoutRepository.kt:548-551](../../../forge-android/app/src/main/java/com/forge/app/data/repo/WorkoutRepository.kt#L548-L551)), and the Monday pass snapshots deload rows. It belongs in the engine bug list beside the 2026-09-26 audit 08 P1 on background regeneration ([08-data-repositories.md](../2026-09-26/08-data-repositories.md)). The "silent close" half is already covered: a coach-call deload entry carries its 14-day watch bar and then an outcome stamp ([CoachAccount.kt:289-327](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachAccount.kt#L289-L327)), and ARC-03 puts the window's end date on Home. A second right-meta reading on the same entry would be a *Mark echo*. |
