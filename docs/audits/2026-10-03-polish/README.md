# Avex polish pass (2026-10-03)

A whole-app UX and UI polish review of the Avex phone app, its Wear OS app and its widget, at revision **42c5ce2** (`main` after PR #230). This pass adds documentation only. The 23 area reports beside this file hold the detail; this page indexes and ranks them.

## Method

Thirteen surface finders each read one slice of the app in full (areas 01 to 13: the live session, set logging, freestyle, Stats and History, Home and navigation, Coach and Goals, Profile, body and photos, two Settings slices, Cardio, onboarding and the program builder, Academy and Wear OS). Six cross-app journey lenses then followed the app as a user would (14 reachability, 15 a beginner's first week, 16 the gym floor, 17 the Monday ritual and idle browsing, 18 the shared kit, 19 accessibility, font scale, RTL and voice). One adversarial verifier per area checked every finding against the code and against SETTLED.md, FAILURES.md, DECISIONS.md and the 2026-09-12 and 2026-09-26 audits, then revised, trimmed or dropped what didn't hold. A completeness critic looked for gaps; it found them by modality rather than by file, and four more areas were added and verified the same way (20 units and formats, 21 multi-week arcs and life events, 22 what leaves the app, 23 interruption and window changes). Nothing was run on a device or emulator. This is a read of the code and the committed screenshot goldens, and contrast ratios are computed from the theme tokens. Each finding gives `file:line` evidence under **Now**, then **Why it matters**, **Proposal** and **Doctrine**.

**Counts.** 361 findings raised, 292 kept (222 of them revised by the verifier) and 69 dropped whole. The verifiers also cut 13 parts from findings they kept (6 in 04, 3 in 10, 4 in 20), so their own tally reads 82 drops. Eleven kept findings re-find a problem another area already filed. They are merged below under the clearest ID, which leaves **281 distinct items**: 35 high impact, 158 medium, 99 low; 194 S effort, 92 M, 6 L.

**sign-off** marks an item that changes the live-session screen (SETTLED lists `GYM_DAY` as untouchable) or reverses a recorded decision. Those need Antho's call before they are built.

## Verdict

The core loops hold up. Set logging is solid after the 2026-09-26 fixes, the Coach ledger's shape is finished, the 2026-09-26 Settings kit is applied consistently and most screens follow the doctrine. The debt is at the edges. A session is easy to end or discard by accident and hard to start on any day except the one Home picked. The engine computes readings that no screen shows: the pre-session brief, why a change didn't stick, the goal a block should plan around, the next lesson. Home, the widget, the watch and the pushes each work out "today" on their own, so they disagree about holidays, rest days and how heavy to go. Restore and the notification grant, where trust matters most, get the least guidance. Of the 281 items, 33 are high impact at S or M effort, and most of those reuse pieces that already exist (`SnackbarController.showUndo`, `SettingsConfirmDialog`, the feed, `BodyLogDateCapsule`, `RestTimerController`). The largest cluster is in the live session, where every change needs sign-off.

## Do first

High impact at S or M effort: 11 S, then 22 M. Within each effort band, rows roughly follow the user's path, from Home and the session out to Coach, Stats, Settings and the watch.

| ID | Finding | Surface | Impact | Effort | Notes |
|---|---|---|---|---|---|
| [HOM-02](05-home-nav.md) | Rest and finished days still get the accent-filled hero button | Home hero | high | S |  |
| [HOM-03](05-home-nav.md) | A routine day's reason repeats one schedule line while the computed pre-session brief goes unshown | Home hero reason | high | S | Hidden when the coach is off |
| [FWK-02](15-first-week.md) | On a fixed-day rest day in week one, Home's only button is "Open Academy" | Home hero, first 3 sessions | high | S | ARC-11 is the same fix for the layoff branch |
| [TRL-01](02-train-logging.md) | A weight change on set 1 snaps back to last session's weight on every later set | Live session set input | high | S | sign-off |
| [TRL-04](02-train-logging.md) | Swiping a logged set away deletes it with no Undo | Live session set row | high | S | sign-off; GYM-05 reuses the Undo for Skip |
| [UNT-02](20-units-formats.md) | The live weight field opens the letters keyboard | Live session set input | high | S | sign-off (behaviour only) |
| [CCH-01](06-coach-goals.md) | Skip is one tap, can't be undone, and breaks that change type's autopilot streak | Coach open call | high | S |  |
| [CCH-02](06-coach-goals.md) | The call's evidence sparkline shows a shape, not a reading | Coach open call | high | S |  |
| [SEP-03](09-settings-prefs.md) | Generate, Re-roll and Deload replace a hand-built plan in one tap | Settings → Program → Rebuild | high | S | SettingsConfirmDialog already exists |
| [KIT-01](18-consistency-kit.md) | Kit chrome and secondary capsules ripple while the primary beside them bounces, and announce no button role | Every top bar and capsule pair | high | S | also AXV-03 |
| [SYS-01](22-system-surfaces.md) | Android 13+ never asks for the notification grant | First run, feed row, Settings → Notifications | high | S | sign-off (changes a settled row) |
| [TRF-01](01-train-flow.md) | You can only train the day Home picks; the day list never renders | Home Start, Plan view | high | M | Pair with ONB-02 (open Plan on Home's day) |
| [HOM-01](05-home-nav.md) | The month and year Recap has no way in | Recap | high | M | also STH-04; Antho picks door or delete; needs HOM-10 |
| [TRL-03](02-train-logging.md) | Effort (RPE) is asked for after the workout but hidden during it | Live session set rows, card footer | high | M | sign-off |
| [GYM-01](16-gym-floor.md) | A locked phone shows no rest countdown, next set or controls | Session notification, rest-done alert | high | M | Lands with RCH-02; weight text waits for ACW-01 |
| [FRE-01](03-freestyle.md) | Repeat a workout seeds the old workout as already logged | Freestyle start page | high | M |  |
| [FRE-02](03-freestyle.md) | A workout logged after the fact is always saved as today | Freestyle header and save | high | M | Promotes BodyLogDateCapsule, which BOD-14 reuses |
| [FRE-03](03-freestyle.md) | A freestyle PR is never shown: no gold row, no confetti, no receipt | Freestyle ledger and Save | high | M |  |
| [FRE-04](03-freestyle.md) | Freestyle rest is a count-up that scrolls away, with no countdown or alert | Freestyle log | high | M | Reuses RestTimerController and the session service |
| [FWK-01](15-first-week.md) | Coach shows a stale baseline count, then waits behind a gate it never names | Coach THIS WEEK, weeks 1 to ~5 | high | M |  |
| [RIT-02](17-ritual-browse.md) | An undecided call drops out of the bell the first time Coach is opened, then lapses to SKIPPED | Feed, bell, Coach | high | M |  |
| [ARC-01](21-life-arcs.md) | A declared holiday never reaches Today: Home, the week strip, the watch tile and the pushes still say train | Home hero and strip, Wear tile, pushes | high | M |  |
| [ARC-02](21-life-arcs.md) | Home promises lighter loads (week back, soreness, deload) that the session's cue doesn't deliver | Home reason vs live load cue | high | M | One shared load function; live layout unchanged |
| [STH-02](04-stats-history.md) | Strength and Records stop at six lifts, and inert Records rows look tappable | Stats Strength | high | M | Full fix lands on STH-01's lift page |
| [STH-03](04-stats-history.md) | Charts have no dates, no point to open, and deload weeks aren't drawn | Stats Strength and Volume | high | M |  |
| [CAR-01](11-cardio.md) | The cardio meter grades against WHO 150 even when the user set a weekly cardio goal | Cardio hero, weeks page, week page | high | M |  |
| [SEP-01](09-settings-prefs.md) | Settings search misses 18 visible labels and opens some hits in the wrong place | Settings search | high | M |  |
| [SEP-02](09-settings-prefs.md) | Program inputs wait for the next Generate, but their copy says they apply now | Settings → Program | high | M |  |
| [SED-01](10-settings-data.md) | Restore isn't on the Backup page, and two different actions are both called "Back up now" | Settings → Backup, Export sheet | high | M |  |
| [SED-02](10-settings-data.md) | Restore can't be undone, and the app leaves the safety copy to the user | Restore confirm | high | M | With SED-03 in Bigger bets |
| [SED-04](10-settings-data.md) | Backup, restore and import show nothing while they run, and Back mid-restore silently cancels it | Backup page, restore confirm, Import sheet | high | M |  |
| [ACW-01](13-academy-wear.md) | The watch offers a different set target than the phone, and never the coach's step | Watch set view | high | M | Step 2 needs sign-off |
| [ACW-02](13-academy-wear.md) | Watch Log set stays live after the day is done and on timed holds | Watch set view | high | M |  |

## Bigger bets

High impact at L effort, plus medium-impact changes that close several findings at once or are prerequisites for them.

| ID | Change | Impact · effort | What it unlocks |
|---|---|---|---|
| [STH-01](04-stats-history.md) | A page for one lift (Detail archetype), fed by per-session aggregates | high · L | Destination for STH-02's `view all`, STH-15's search results, RIT-14's coach rows and session detail's set table |
| [BOD-01](08-body-photos.md) | A bodyweight page with history, rate and the suppressed goal line | high · L | Gives PRO-08's goal line a home and the WEIGHT row a destination |
| [RCH-02](14-reachability.md) | One `EXTRA_OPEN` destination for every push, both session notifications and the widget, routed through a generalised ExternalOpenRequest | medium · M | INT-06 (morning re-entry), RCH-14 (launcher shortcuts), GYM-01's notification actions; fixes the widget stacking a day on Settings |
| [KIT-02](18-consistency-kit.md) | A kit `ForgeConfirmDialog` replacing 24 hand-rolled confirms | medium · M | One place for TRF-02 and TRF-04's confirms, worded with real counts, and for the destructive capsule whose contrast KIT-07 settles |
| [KIT-08](18-consistency-kit.md) | Route every user-facing week through `userWeekStart` | medium · M | Week starts then holds on goals, Cardio, Profile ACTIVITY and the weekday schedule (also PRO-14, SEP-14) |
| [STH-05](04-stats-history.md) | One session-title helper from the top two muscles | medium · M | History, the Stats day sheet, Home RECENT, session detail and the template lists stop reading "Open workout" (also FRE-12) |
| [AXV-01](19-a11y-voice.md) | Promote Stats' FlowRow as `EditorialFigureRow` and drop `maxLines = 1` | medium · S | Coach, Cardio, Recap and Stats figure rows stop ellipsizing at 200% (also CAR-06, whose cardio golden still applies) |
| [ARC-12](21-life-arcs.md) | Pass life events and holidays into the weekly review | medium · S | Must land before CCH-03 and RIT-13, or the new cues misread time away as a volume dip |
| [CCH-06](06-coach-goals.md) + [CCH-08](06-coach-goals.md) | One migration adding `outcome_reason` and `applied_by` | medium · M | The coach record says why a change didn't stick and who applied it; feeds the revert copy and the weekly feed line |
| [CCH-10](06-coach-goals.md) | Map the Goals screen's top goal into block focus | medium · M | Blocks stop being generic; settles SETTLED's two-goal-systems question (sign-off) |
| [GYM-12](16-gym-floor.md) | Move the focused exercise into DayUiState and publish it to the wrist | medium · M | The watch follows the phone's jumps; prerequisite for GYM-10 supersets |
| [SED-03](10-settings-data.md) | Stage, preview and then publish a restore | medium · M | With SED-02 and SED-04, restore becomes previewable, uncancellable mid-run and reversible |
| [UNT-01](20-units-formats.md) | Split Weight into Lifts (lb, kg) and Bodyweight (lb, kg, st) | medium · L | Removes decimal-stone lifting and 7 lb jumps, and settles UNT-02's typed-stones trade-off (DESIGN §11 change, sign-off) |

## Themes

1. **Built, but no way to reach it.** The engine and data layer already hold things that no screen shows and no route opens: day choice ([TRF-01](01-train-flow.md)), session type ([TRF-03](01-train-flow.md)), Recap ([HOM-01](05-home-nav.md), STH-04), the pre-session brief ([HOM-03](05-home-nav.md)), last session's exercise note ([TRL-12](02-train-logging.md)), plate math and the later-lift warm-up ladder ([TRL-14](02-train-logging.md)), set tags ([TRL-15](02-train-logging.md)), notes search ([FRE-06](03-freestyle.md)), the weekly focus cue and the watcher's fail reasons ([CCH-03](06-coach-goals.md), [CCH-06](06-coach-goals.md)), the goal table the block plans from ([CCH-10](06-coach-goals.md)), the exercise index the watch receives ([ACW-05](13-academy-wear.md)), the lesson Home computes ([ACW-12](13-academy-wear.md)), the talk test ([CAR-14](11-cardio.md)), the per-session PDF ([SYS-06](22-system-surfaces.md)), supersets and assisted sets ([GYM-10](16-gym-floor.md), [GYM-11](16-gym-floor.md)), watch connection state ([SED-09](10-settings-data.md)) and deload weeks on the tonnage chart ([STH-03](04-stats-history.md)). Most of these fixes are wiring. Where nobody wants the feature, deleting the dead path is the honest alternative, and several reports offer that option.

2. **A mark without its reading, and numbers that don't open their source.** Charts, bars and deltas show a shape but not the value, date or referent: [STH-03](04-stats-history.md) (no dates), [CCH-02](06-coach-goals.md) (no values on the call's sparkline), [CCH-04](06-coach-goals.md) (last week's figures under THIS WEEK), [STH-11](04-stats-history.md) and AXV-02 (deltas with no amount, unit or referent), [STH-07](04-stats-history.md) (bars ranked against the strongest lift), [PRO-06](07-profile.md) (unnamed delta windows), [TRL-05](02-train-logging.md) (a PR without the record it beat), [TRF-06](01-train-flow.md) (a time fit without the day's length), [BOD-10](08-body-photos.md) and [SED-08](10-settings-data.md) (readings with no age). One step further, the reading exists but has no page to open: [STH-01](04-stats-history.md), [STH-02](04-stats-history.md), [STH-15](04-stats-history.md) and [RIT-14](17-ritual-browse.md) for lifts, [BOD-01](08-body-photos.md) for bodyweight, [HOM-05](05-home-nav.md) for the week strip, [RCH-07](14-reachability.md) for goal rows.

3. **One tap to permanent.** Apply on a coach call has Undo. Skip, End the block, Clear goal and an applied deload don't ([CCH-01](06-coach-goals.md), [CCH-12](06-coach-goals.md), [CCH-15](06-coach-goals.md), [ARC-05](21-life-arcs.md)). The live session deletes a set, skips an exercise, and finishes or discards a workout with no Undo and no count of what goes ([TRL-04](02-train-logging.md), [GYM-05](16-gym-floor.md), [TRF-02](01-train-flow.md), [TRF-04](01-train-flow.md), [TRF-13](01-train-flow.md)). Settings rebuilds a plan, restores a backup and imports a file with no way back ([SEP-03](09-settings-prefs.md), [SED-02](10-settings-data.md), [SED-05](10-settings-data.md)), and onboarding's Re-roll loses the previous week ([ONB-08](12-onboarding-builder.md)). The opposite also happens: some records can't be removed at all ([STH-06](04-stats-history.md) finished gym sessions, [BOD-04](08-body-photos.md) weigh-ins and body fat, [FRE-11](03-freestyle.md) custom moves). The pieces exist: `SnackbarController.showUndo` for reversible acts, and [KIT-02](18-consistency-kit.md)'s shared confirm for the irreversible ones, worded with the real count.

4. **The same question, answered differently in different places.** Home, the widget, the watch, the pushes and the session each work out "what is today" and "how heavy" on their own: [HOM-13](05-home-nav.md) (the widget's own next-up), [ACW-01](13-academy-wear.md) (the watch's own set target), [ACW-09](13-academy-wear.md) and [ACW-10](13-academy-wear.md) (watch idle screen vs tile, stale after midnight), [ARC-01](21-life-arcs.md) (holidays never reach the directive), [ARC-02](21-life-arcs.md) (three load computations), [ARC-10](21-life-arcs.md) (the reminder ignores sick days), [FWK-02](15-first-week.md) and [ARC-11](21-life-arcs.md) (rest days), [HOM-07](05-home-nav.md) (freestyle ignores the directive). Shared settings and helpers also apply on some screens and not others: Week starts ([KIT-08](18-consistency-kit.md), PRO-14, SEP-14), the weekly cardio goal ([CAR-01](11-cardio.md)), relative day labels ([KIT-09](18-consistency-kit.md)), the load grid ([UNT-03](20-units-formats.md), [UNT-04](20-units-formats.md), [UNT-05](20-units-formats.md)), session titles ([STH-05](04-stats-history.md), FRE-12), which day a midnight-crossing session belongs to ([INT-09](23-interrupt-window.md), [PRO-05](07-profile.md)). Each fix is one function every surface reads, with a test that pins them together.

5. **Outside the app lags behind inside.** The parts Android shows on the app's behalf get less care than the screens. The notification grant is never requested ([SYS-01](22-system-surfaces.md)). Every push and session notification opens wherever the app was left ([RCH-02](14-reachability.md), [INT-06](23-interrupt-window.md)). The locked-phone notification has no countdown or controls ([GYM-01](16-gym-floor.md)). Pushes are fixed one-liners that misread the week ([RIT-13](17-ritual-browse.md), [SYS-02](22-system-surfaces.md), [SYS-04](22-system-surfaces.md), [RCH-10](14-reachability.md)), they are never cleared once dealt with ([SYS-03](22-system-surfaces.md)), and most can only be muted from Android ([SYS-05](22-system-surfaces.md)). Backup failures show only inside Settings ([SED-11](10-settings-data.md)). Watch tiles and complications do nothing when tapped ([ACW-08](13-academy-wear.md)). The widget picker shows "Avex" over "Avex" ([SYS-11](22-system-surfaces.md)). Files in and out read like plumbing: [SYS-06](22-system-surfaces.md), [SYS-07](22-system-surfaces.md), [SYS-08](22-system-surfaces.md), [SYS-10](22-system-surfaces.md), [SED-12](10-settings-data.md).

6. **Each logger has what the other lacks.** Freestyle carries a weight change into the next set, offers Undo on delete, edits a logged hold and tags sets, and the live session does none of these ([TRL-01](02-train-logging.md), [TRL-04](02-train-logging.md), [TRL-13](02-train-logging.md), [TRL-15](02-train-logging.md)). The live session shows PRs, runs a rest countdown with an alert and takes notes, and freestyle doesn't ([FRE-03](03-freestyle.md), [FRE-04](03-freestyle.md), [FRE-05](03-freestyle.md)). Freestyle's Repeat also seeds old sets as already logged ([FRE-01](03-freestyle.md)), and neither logger can record a past date ([FRE-02](03-freestyle.md)). Around both, the exercise pickers have three sets of search rules and layouts ([FRE-07](03-freestyle.md), [FRE-09](03-freestyle.md), [FRE-17](03-freestyle.md), [TRL-16](02-train-logging.md), [CCH-14](06-coach-goals.md)), and the browser runs a second "like" system ([FRE-10](03-freestyle.md)).

7. **Kit gaps, so every feature re-rolls its edges, and 200% breaks with no golden to catch them.** The grouped-surface kit covers forms, lists and top bars. It has no confirm dialog ([KIT-02](18-consistency-kit.md)), sheet scaffold ([KIT-04](18-consistency-kit.md)), range pager ([KIT-16](18-consistency-kit.md)), quiet link ([KIT-06](18-consistency-kit.md)), figure row that wraps ([AXV-01](19-a11y-voice.md)) or page title with heading semantics ([AXV-04](19-a11y-voice.md)). Its chrome ripples beside a bouncing primary and has no button role ([KIT-01](18-consistency-kit.md), AXV-03), and five raw haptics bypass the user's setting ([KIT-11](18-consistency-kit.md)). The hand-rolled copies drift in fill, contrast and RTL ([KIT-07](18-consistency-kit.md), [TRL-09](02-train-logging.md), [FWK-10](15-first-week.md), [AXV-13](19-a11y-voice.md), [AXV-15](19-a11y-voice.md), [PRO-12](07-profile.md)). At 200% font, Profile's BODY rows, the Cardio and Recap figures, Stats rows, browser tiles and the gallery break ([PRO-13](07-profile.md), CAR-06, [AXV-07](19-a11y-voice.md), [AXV-08](19-a11y-voice.md), [BOD-11](08-body-photos.md), [FRE-14](03-freestyle.md)). Only the Coach ledger, the swap picker, the warm-up and the design recipes have screenshot goldens, so nothing caught these.

## Areas

| # | Area | Prefix | Raised | Kept | Dropped |
|---|---|---|--:|--:|--:|
| 01 | [Train: day list, warm-up and live-session shell](01-train-flow.md) | TRF | 15 | 13 | 2 |
| 02 | [Train: set logging components](02-train-logging.md) | TRL | 18 | 18 | 0 |
| 03 | [Freestyle logging, exercise browser, templates, notes search](03-freestyle.md) | FRE | 17 | 17 | 0 |
| 04 | [Stats, session history and session detail](04-stats-history.md) | STH | 18 | 18 | 0 (+6 parts) |
| 05 | [Home, hub navigation, notifications, check-in, recap, trophies, widget](05-home-nav.md) | HOM | 17 | 15 | 2 |
| 06 | [Coach tab and goals](06-coach-goals.md) | CCH | 17 | 15 | 2 |
| 07 | [Profile: header, ledger, rank, trophies, activity](07-profile.md) | PRO | 17 | 14 | 3 |
| 08 | [Body tracking and progress photos](08-body-photos.md) | BOD | 16 | 15 | 1 |
| 09 | [Settings: main list, appearance, program, coach, exercises, format, about](09-settings-prefs.md) | SEP | 18 | 16 | 2 |
| 10 | [Settings: backup, import/export, storage, security, recovery & Health Connect, wearable](10-settings-data.md) | SED | 16 | 16 | 0 (+3 parts) |
| 11 | [Cardio tab](11-cardio.md) | CAR | 16 | 13 | 3 |
| 12 | [Cold launch, onboarding and program builder](12-onboarding-builder.md) | ONB | 16 | 12 | 4 |
| 13 | [Academy and the Wear OS app](13-academy-wear.md) | ACW | 17 | 16 | 1 |
| 14 | [Reachability and wayfinding (cross-app)](14-reachability.md) | RCH | 14 | 7 | 7 |
| 15 | [A beginner's first week (journey)](15-first-week.md) | FWK | 14 | 9 | 5 |
| 16 | [On the gym floor (journey)](16-gym-floor.md) | GYM | 18 | 7 | 11 |
| 17 | [The Monday ritual and idle browsing (journey)](17-ritual-browse.md) | RIT | 17 | 5 | 12 |
| 18 | [Cross-surface consistency and the shared kit](18-consistency-kit.md) | KIT | 17 | 10 | 7 |
| 19 | [Accessibility, font scale, RTL, monochrome and voice (cross-app)](19-a11y-voice.md) | AXV | 15 | 11 | 4 |
| 20 | [Unit and format switching across the app (lb / kg / st, km / mi, cm / in, 12h / 24h)](20-units-formats.md) | UNT | 12 | 12 | 0 (+4 parts) |
| 21 | [Multi-week arcs and life events (journey): deload week, block end, holiday, layoff return, sick/sore days](21-life-arcs.md) | ARC | 13 | 12 | 1 |
| 22 | [What leaves the app: notification channels and content, permission path, PDF and share outputs, widget picker, inbound share](22-system-surfaces.md) | SYS | 13 | 12 | 1 |
| 23 | [Interruption, resume and window changes: rotation, process death, backgrounding, midnight/time-zone, landscape, split-screen, tablet/foldable](23-interrupt-window.md) | INT | 10 | 9 | 1 |
| | **Total** | | **361** | **292** | **69** (+13 parts) |

Each area's "Considered and dropped" table lists dropped findings and cut parts with reasons. Most drops in the journey lenses (14 to 19) are re-finds of a surface finding and point to it.

## All other kept findings

The 234 kept items not in the tables above, by category, medium before low. A merged item shows the higher impact of the pair.

### UX friction (49)

- [TRF-02](01-train-flow.md) FINISH ends a half-done workout in one tap, with no check and no way back *(medium · S)*
- [TRF-04](01-train-flow.md) Leave and cross-day discard delete a workout without saying how much *(medium · S)*
- [TRF-06](01-train-flow.md) The time fit never says how long the day is, and offers choices that do nothing *(medium · S · sign-off)*
- [TRF-08](01-train-flow.md) The engine reorders the day silently, with no note and no way back *(medium · S · sign-off)*
- [TRF-09](01-train-flow.md) Swapping after one logged set dead-ends at "delete your sets first" *(medium · M)*
- [TRL-02](02-train-logging.md) The coach's suggested load is the one number on the card you cannot tap *(medium · S · sign-off)*
- [FRE-07](03-freestyle.md) Tapping "Search 117 exercises" opens a browser where search isn't focused *(medium · S)*
- [STH-12](04-stats-history.md) Session detail's Bars/Line toggle often changes nothing, and RPE uses a different interaction *(medium · S)*
- [STH-13](04-stats-history.md) Cardio's "view all N" opens a mixed log where the N cardio sessions are buried *(medium · S)*
- [STH-14](04-stats-history.md) History filters are opaque: "Heavy" is a fixed 3,000 lb, durations are unstated, and "All" clears unrelated chips *(medium · S)*
- [STH-06](04-stats-history.md) A finished gym workout can't be deleted (a cardio entry can) *(medium · M · sign-off)*
- [STH-15](04-stats-history.md) Searching History for an exercise returns session rows that never show that exercise *(medium · M)*
- [HOM-04](05-home-nav.md) A passive feed row can only be cleared with Clear all, which also sweeps the coach brief and the invites *(medium · S)*
- [HOM-08](05-home-nav.md) Once saved, today's check-in can't be reopened *(medium · M)*
- [CCH-09](06-coach-goals.md) The advanced-tracking pop-up lands on top of the call you came to decide *(medium · S · also [FWK-06](15-first-week.md))*
- [CCH-14](06-coach-goals.md) Setting a lift target is blind: no current best, and the whole library to scroll *(medium · M)*
- [PRO-11](07-profile.md) A photo you pick yourself is centre-cropped with no way to reposition it, so faces land in the faded top strip *(medium · M)*
- [BOD-02](08-body-photos.md) The camera takes one shot per trip and forgets pose and album *(medium · M)*
- [BOD-03](08-body-photos.md) The weigh-in sheet is keyboard-first *(medium · M)*
- [SED-05](10-settings-data.md) A Settings import can't be undone, and its result only shows in a four-second snackbar *(medium · M)*
- [CAR-05](11-cardio.md) Effort, the one field that prices cardio against lifting, is hidden behind "More" *(medium · S)*
- [CAR-02](11-cardio.md) Home's "Log cardio" only switches tabs, so the user taps "Log cardio" twice and gets the wrong activity *(medium · M)*
- [CAR-04](11-cardio.md) "24:37" is read as 24 h 37 min and silently becomes "1440"; durations have no seconds *(medium · L)*
- [ONB-08](12-onboarding-builder.md) Re-roll on the week page discards the previous week with no way back *(medium · S)*
- [ONB-09](12-onboarding-builder.md) "+ Add day" makes a generic "Day N", leaves the user one tap short of its editor, and picking a type never names it *(medium · S)*
- [ACW-11](13-academy-wear.md) The back gesture on the RPE picker exits the app instead of going back *(medium · S)*
- [UNT-03](20-units-formats.md) Off-grid weights stay off-grid: the ± pill adds 2.5 to 61.2 *(medium · S · sign-off)*
- [ARC-09](21-life-arcs.md) "Feeling unwell" can't be switched off: a better next-day check-in is ignored for three days *(medium · S)*
- [ARC-05](21-life-arcs.md) An applied deload is the one coach change with no Undo, even when autopilot applied it *(medium · M)*
- [SYS-03](22-system-surfaces.md) Pushes stay in the shade after the app has dealt with what they announce *(medium · S)*
- [SYS-08](22-system-surfaces.md) A file shared into Avex gets a confirm that names nothing, and an unreadable file fails only after "Import" *(medium · M)*
- [INT-05](23-interrupt-window.md) After Android reclaims a freestyle log, coming back asks "Resume log or Start fresh" and loses the rest readout *(medium · S)*
- [INT-06](23-interrupt-window.md) Opening the app the next morning lands on whatever deep screen was left last night *(medium · S)*
- [TRF-13](01-train-flow.md) A warm-up opt-out can last a week, with no undo and nothing showing it's on *(low · S)*
- [HOM-17](05-home-nav.md) "Hold start to skip warmup" shows forever, even when the warm-up is already off *(low · S)*
- [CCH-12](06-coach-goals.md) "End the block →" throws away the block's position in one tap, with no Undo *(low · S)*
- [CCH-16](06-coach-goals.md) A custom goal can't be renamed or moved to a different timeframe *(low · S)*
- [SEP-09](09-settings-prefs.md) The accent swatches disappear when the icon sets the colour, and the switch that brings them back is two groups down *(low · S)*
- [SED-15](10-settings-data.md) Storage shows "0 B" before it has measured, and its rows lead nowhere *(low · S)*
- [ONB-02](12-onboarding-builder.md) Home's Plan opens the program on its first day, not the day Home just named *(low · S)*
- [ACW-12](13-academy-wear.md) Home's LEARN button opens the Academy tab instead of the lesson it means *(low · S)*
- [ACW-14](13-academy-wear.md) The lesson's Next ignores what you've read, and the last lesson is a dead end *(low · S)*
- [ACW-17](13-academy-wear.md) The watch asks for every permission on launch, before anything explains why *(low · S)*
- [RCH-11](14-reachability.md) Re-tapping the tab you're on does nothing *(low · S)*
- [GYM-05](16-gym-floor.md) Skipping an exercise has no Undo *(low · S · sign-off)*
- [SYS-09](22-system-surfaces.md) The PR-milestone push buzzes while the user is watching the summary sheet, and never reaches the feed *(low · S)*
- [SYS-12](22-system-surfaces.md) The app-icon picker says the home screen updates "a moment after you pick", but the swap waits until you leave and can drop the home-screen icon *(low · S)*
- [INT-04](23-interrupt-window.md) With App lock on, a file picker, Health Connect grant or notification-settings trip ends on the lock screen *(low · S · sign-off)*
- [INT-10](23-interrupt-window.md) Turning the phone in the progress camera resets the pose and lens and cancels the self-timer *(low · S)*

### Consistency (46)

- [TRF-07](01-train-flow.md) The warm-up has no back arrow and never names the day *(medium · S · sign-off)*
- [TRL-15](02-train-logging.md) Live sets cannot be tagged warm-up, drop or failure, and the RPE picker is drawn twice *(medium · M · sign-off)*
- [TRL-07](02-train-logging.md) One PR, four different marks *(medium · S · also [STH-10](04-stats-history.md) · sign-off)*
- [STH-11](04-stats-history.md) Deltas render as raw integers on Stats and as bare arrows on session detail *(medium · S · also [AXV-02](19-a11y-voice.md))*
- [HOM-06](05-home-nav.md) A cardio row in RECENT opens a thin one-off sheet instead of the cardio detail every other surface opens *(medium · S)*
- [CCH-04](06-coach-goals.md) THIS WEEK closes on last week's figures, unlabelled *(medium · S)*
- [CCH-15](06-coach-goals.md) Clearing a lift goal has no Undo, while deleting any other goal does *(medium · S)*
- [PRO-04](07-profile.md) The cover name is drawn in Material's stock sans, not the serif hero voice, and gets cut off at large font sizes *(medium · S)*
- [PRO-05](07-profile.md) ACTIVITY's streak counts only gym sessions, but the grid above it also lights cardio days *(medium · M)*
- [BOD-09](08-body-photos.md) The gallery is half stock Material, with no recorded decision *(medium · M)*
- [SED-10](10-settings-data.md) Backup, Wearable and Storage rows on the Settings root show fixed captions instead of their live value *(medium · S · also [SEP-04](09-settings-prefs.md))*
- [ONB-10](12-onboarding-builder.md) Each exercise row nests a red remove button inside a tappable row *(medium · S)*
- [ACW-09](13-academy-wear.md) The watch's idle screen and the Today tile give different answers for today *(medium · S)*
- [KIT-11](18-consistency-kit.md) Five haptics bypass `forgeHaptic`, so Haptic feedback "Off" still buzzes mid-session *(medium · S)*
- [UNT-05](20-units-formats.md) Equipment → Loading offers kg users converted pound plates *(medium · S)*
- [UNT-04](20-units-formats.md) The coach's lb grid is 2.5 lb, so it asks for a 47.5 lb dumbbell the + pill can't reach *(medium · M)*
- [UNT-06](20-units-formats.md) Stones bodyweight is stone and pounds on one sheet and decimal stones everywhere else *(medium · M)*
- [ARC-11](21-life-arcs.md) In the week back, every scheduled rest day reads "Ease back in" with a filled Start session *(medium · S)*
- [ARC-10](21-life-arcs.md) On a sick day the reminder still pushes the streak, and resting as the coach says breaks it *(medium · S)*
- [ARC-07](21-life-arcs.md) BLOCK's figure counts weeks while its rail draws four equal phases, and the deload and test weeks have no dates *(medium · S)*
- [ARC-08](21-life-arcs.md) A declared holiday pauses the coach but not the block *(medium · M)*
- [SYS-05](22-system-surfaces.md) Settings lists 3 of the 7 phone pushes, and Android shows only 2 of Avex's channels until each push has fired once *(medium · M)*
- [TRF-14](01-train-flow.md) The long-press exercise menu repeats the card's chips and says "Toggle skip" *(low · S)*
- [TRL-08](02-train-logging.md) "Rest done" borrows the green that means "you beat last time" *(low · S · sign-off)*
- [TRL-18](02-train-logging.md) The swap picker's scope moved back to the header against a settled owner call *(low · S · sign-off)*
- [FRE-10](03-freestyle.md) The browser star is a second "like" system, named three ways *(low · S)*
- [FRE-16](03-freestyle.md) The "That day" sheet departs from its own Modal recipe *(low · S)*
- [FRE-17](03-freestyle.md) ExerciseLibraryPicker is a stock-M3 dialog unlike every other exercise list *(low · M)*
- [HOM-14](05-home-nav.md) Coach and Academy tab pages draw a back arrow; Cardio and Stats don't *(low · S)*
- [PRO-06](07-profile.md) The BODY deltas don't say what period they cover, and SIZES covers a different one from its neighbours *(low · S)*
- [PRO-15](07-profile.md) The BODY row glyphs are borrowed from unrelated Settings pages *(low · S)*
- [BOD-14](08-body-photos.md) Measurements can only be logged for today *(low · S)*
- [SEP-15](09-settings-prefs.md) The theme preview calls itself "a sample of Home" but draws shapes Home doesn't use *(low · S)*
- [SED-13](10-settings-data.md) Privacy mode reads off while a lock is already hiding Avex and blocking screenshots *(low · S)*
- [CAR-13](11-cardio.md) The pace trend draws getting faster as a falling line, while the compare bars fill toward your best *(low · S)*
- [CAR-12](11-cardio.md) One cardio session is drawn three ways, and the watch-import action is bare text *(low · M)*
- [ONB-14](12-onboarding-builder.md) The program screen goes by four names, and the week page points at "the editor", which nothing is called *(low · S)*
- [ONB-15](12-onboarding-builder.md) The day editor's Color picker sets a colour nothing in the app draws *(low · S · sign-off)*
- [FWK-13](15-first-week.md) The same coach week is dated two ways *(low · S)*
- [KIT-06](18-consistency-kit.md) The kit's section anchor draws its link in accent 10sp on an 18dp target, so Home and Cardio each built their own *(low · S)*
- [KIT-09](18-consistency-kit.md) "When was this" is written four ways, so a run logged this morning reads TODAY on Home and SAT on Cardio *(low · S)*
- [KIT-15](18-consistency-kit.md) Home RECENT colours "better or worse" in two raw hexes no other screen uses *(low · S)*
- [KIT-04](18-consistency-kit.md) Sixteen bottom sheets repeat one scaffold with no kit version, and four other findings each fix one copy *(low · M)*
- [UNT-08](20-units-formats.md) Editing a logged set calls a plate count "Weight" and drops the unit *(low · S)*
- [UNT-09](20-units-formats.md) Gym session detail never shows the start time, so the Clock setting only reaches cardio *(low · S)*
- [INT-09](23-interrupt-window.md) A workout that crosses midnight lights one day in Profile's activity grid and counts on the next in the streak printed under it *(low · S · sign-off)*

### Feature gaps (37)

- [TRF-03](01-train-flow.md) Session type (Technique, Test) has no control, though the coach acts on it *(medium · S · sign-off)*
- [TRL-12](02-train-logging.md) Last session's note on an exercise is fetched but never shown *(medium · S · sign-off)*
- [TRL-10](02-train-logging.md) The swap picker never shows your own numbers on a move *(medium · M · sign-off)*
- [TRL-16](02-train-logging.md) Mid-session "add exercise" can only add moves already in your program *(medium · M · sign-off)*
- [TRL-06](02-train-logging.md) Bodyweight lifts can never earn a PR *(medium · L · sign-off)*
- [FRE-09](03-freestyle.md) Browser search can't find by equipment or narrow to the user's gear *(medium · S)*
- [FRE-05](03-freestyle.md) Freestyle can't write an exercise note, a session note or a pinned cue *(medium · M)*
- [FRE-11](03-freestyle.md) Custom moves are always weight × reps and can never be fixed or removed *(medium · M)*
- [HOM-07](05-home-nav.md) Freestyle users get a fixed hero; the directive computed for them is thrown away *(medium · S)*
- [HOM-10](05-home-nav.md) Recap shows only the month and year in progress, so the month that just closed can't be seen *(medium · M)*
- [BOD-06](08-body-photos.md) The full-screen photo viewer can't zoom *(medium · M)*
- [SEP-12](09-settings-prefs.md) Exercise likes can't show the moves in your plan, and hiding one doesn't take it out of the plan *(medium · S)*
- [SEP-05](09-settings-prefs.md) Holidays can't be edited, need two date dialogs, and don't say which one is on or what pauses *(medium · M)*
- [SED-12](10-settings-data.md) CSV exports are always in pounds and km, and there's no per-set CSV *(medium · M)*
- [CAR-08](11-cardio.md) Rides and swims read as running pace (min/km) instead of km/h and per 100 m *(medium · M)*
- [ONB-03](12-onboarding-builder.md) First run offers no way to restore a backup or bring history from another app *(medium · M)*
- [ACW-04](13-academy-wear.md) The rest screen doesn't say what's next, and a paused rest looks frozen *(medium · S)*
- [ACW-05](13-academy-wear.md) The watch receives "exercise 3 of 6" but never shows it *(medium · S)*
- [ACW-06](13-academy-wear.md) The ambient mode WEAR.md specifies was never built, so the full UI stays lit for the whole session *(medium · M)*
- [GYM-11](16-gym-floor.md) Bodyweight lifts can't record added weight or assistance *(medium · M · sign-off)*
- [GYM-10](16-gym-floor.md) Supersets are stored, imported and respected by the engine, but can't be run *(medium · L · sign-off)*
- [UNT-07](20-units-formats.md) Swims are logged and read in kilometres or miles, to one decimal *(medium · M)*
- [ARC-03](21-life-arcs.md) A deload week or the block's test week reads like any other day on Home *(medium · S)*
- [ARC-06](21-life-arcs.md) A finished block disappears: no closing read, and its deload hold promises a next block that never comes *(medium · M)*
- [TRF-11](01-train-flow.md) After FINISH there's nowhere to write or fix the session note *(low · S · sign-off)*
- [TRL-13](02-train-logging.md) A logged timed hold cannot be corrected, only deleted *(low · S · sign-off)*
- [PRO-08](07-profile.md) The bodyweight goal is loaded but deliberately not drawn, so the WEIGHT trend has no target *(low · S)*
- [PRO-10](07-profile.md) ACTIVITY knows the plan but never says how many planned training days were hit *(low · S)*
- [BOD-13](08-body-photos.md) The camera self-timer is fixed at 3 seconds *(low · S)*
- [BOD-08](08-body-photos.md) Pose, muscles and tags can only be set one photo at a time *(low · M)*
- [BOD-12](08-body-photos.md) Nothing says a photo check-in is due *(low · M)*
- [CAR-09](11-cardio.md) The weeks chart shows minutes only, so weekly distance can't be compared *(low · M)*
- [ONB-16](12-onboarding-builder.md) The closing step doesn't offer the training reminder, right after the user picked fixed weekdays *(low · S)*
- [ACW-13](13-academy-wear.md) The "Your bodyweight" and "Your working weight" calculators start on someone else's numbers *(low · S)*
- [RCH-14](14-reachability.md) No launcher shortcuts for the quick logging jobs *(low · M)*
- [FWK-07](15-first-week.md) Home's cold-start line repeats for three sessions and never says what weight to pick *(low · S)*
- [RIT-17](17-ritual-browse.md) Reached goals don't say when, and the lens sorts them by the day they were set *(low · M)*

### Missing states and actions (22)

- [TRL-05](02-train-logging.md) A PR row says "record" but never shows the record it beat *(medium · S · sign-off)*
- [STH-09](04-stats-history.md) On first run, Days and Effort show a hint line where doctrine wants the mark at zero *(medium · S)*
- [CCH-05](06-coach-goals.md) First use reads as a waiting room: three "Forming" rows, and a rung that repeats the baseline *(medium · S)*
- [BOD-04](08-body-photos.md) A wrong weigh-in or body-fat reading can never be removed *(medium · S)*
- [BOD-10](08-body-photos.md) A photo's weight can be two weeks old and never says so *(medium · M)*
- [SEP-06](09-settings-prefs.md) A training reminder set inside quiet hours never arrives, and the page doesn't say so *(medium · S)*
- [SEP-11](09-settings-prefs.md) In freestyle the Program menu still offers rotation and deload, and Re-roll quietly leaves freestyle *(medium · S)*
- [SED-07](10-settings-data.md) Once Health Connect has refused a permission, "Connect" does nothing *(medium · S)*
- [SED-08](10-settings-data.md) Signal rows say "Receiving" with no age, and three read signals are never checked *(medium · M)*
- [CAR-03](11-cardio.md) An activity with no distance never compares: PROGRESS stays empty and the compare section is a header over one line *(medium · S)*
- [ONB-04](12-onboarding-builder.md) The week page doesn't say which weekday each workout falls on, one step after asking *(medium · S)*
- [ACW-10](13-academy-wear.md) The watch glances keep yesterday's answer after midnight while the phone app stays alive *(medium · S)*
- [ACW-07](13-academy-wear.md) A lost phone connection shows nothing until a set fails, and +30 and Skip fail silently *(medium · M)*
- [FWK-05](15-first-week.md) Home's first-week strip marks the days before you joined as missed *(medium · S)*
- [SYS-02](22-system-surfaces.md) A week of only cardio gets the come-back nudge and no recap *(medium · S)*
- [INT-02](23-interrupt-window.md) A workout left open overnight comes back under today's name and finishes as a 12-hour session *(medium · M)*
- [TRF-12](01-train-flow.md) A day with no exercises opens to a blank session with no way to add one *(low · S · sign-off)*
- [FRE-13](03-freestyle.md) The Unfinished workout prompt doesn't say when the workout started *(low · S)*
- [FRE-15](03-freestyle.md) First-run freestyle page is three sentences of mechanics instead of a way in *(low · S)*
- [SED-06](10-settings-data.md) Exports found in the folder look the same before and after they're imported *(low · M)*
- [RIT-11](17-ritual-browse.md) Stats asks for a weigh-in and gives no way to log one, under a title Coach also uses *(low · S)*
- [SYS-10](22-system-surfaces.md) Quick export rows don't say how much there is, and an empty export still writes a header-only file and opens the share sheet *(low · S)*

### Discoverability (21)

- [TRL-14](02-train-logging.md) Plate math and the warm-up ladder for later lifts are built but cannot be reached *(medium · M · sign-off)*
- [FRE-06](03-freestyle.md) Notes search is still unreachable, and its hits open nothing *(medium · S)*
- [HOM-05](05-home-nav.md) Home's week strip can't be tapped, unlike every other consistency grid *(medium · S)*
- [CCH-07](06-coach-goals.md) The week-by-week record stops at six weeks, ending on a dead "And N more weeks." *(medium · M)*
- [PRO-02](07-profile.md) BODY rows with data no longer say what a tap does, so Log, Sync and Open look the same *(medium · S)*
- [PRO-03](07-profile.md) The name can only be changed by tapping the name text, which sits inside the photo's tap target, and nothing says so *(medium · S)*
- [BOD-05](08-body-photos.md) "What changed since last time?" is the one comparison the gallery never offers *(medium · S)*
- [SED-09](10-settings-data.md) The phone never shows whether the Avex watch app is installed or in range *(medium · M)*
- [SED-11](10-settings-data.md) A failed weekly backup, or a folder copy that has stopped, is only visible inside Settings → Backup *(medium · M)*
- [ACW-08](13-academy-wear.md) Tapping a tile or complication does nothing *(medium · S)*
- [FWK-09](15-first-week.md) Lessons a session unlocks arrive at the next app open, one receipt after another *(medium · S)*
- [SYS-06](22-system-surfaces.md) Session detail can only share raw JSON, while a per-session PDF sits unused *(medium · M)*
- [BOD-15](08-body-photos.md) A site's past readings sit behind a hidden long-press and a column of red links *(low · S)*
- [SEP-16](09-settings-prefs.md) About's gesture list advertises two gestures that are gone and misses four that exist *(low · S)*
- [SEP-13](09-settings-prefs.md) What's new is found only by going to look, and nothing points to it after an update *(low · M)*
- [RCH-07](14-reachability.md) Home's goal rows open the whole list, and at zero goals "Pin a goal" opens an empty one *(low · S)*
- [RCH-12](14-reachability.md) The "hold to go Home" shortcut exists only on Home *(low · S)*
- [RCH-13](14-reachability.md) A successful import's feed receipt leads nowhere *(low · S)*
- [RIT-14](17-ritual-browse.md) The coach's lift rows lead nowhere, and their overflow ends on a dead "And N more." *(low · S)*
- [ARC-13](21-life-arcs.md) Holidays can only be declared ahead of time, from deep in Settings *(low · M)*
- [SYS-11](22-system-surfaces.md) The widget picker shows "Avex" over "Avex", and a placed widget first flashes a black slab with the retired wordmark *(low · S)*

### UI polish (24)

- [TRL-11](02-train-logging.md) The exercise chart draws a forecast the engine never computed *(medium · S · sign-off)*
- [FRE-14](03-freestyle.md) Session header: three serif figures on a Live screen, and the set count stated twice *(medium · S)*
- [FRE-08](03-freestyle.md) Browser tiles state the muscle three times and nothing a lifter chooses by *(medium · M)*
- [STH-07](04-stats-history.md) Strength bars rank every lift against your strongest one *(medium · S · sign-off)*
- [STH-16](04-stats-history.md) Long histories have no month structure *(medium · M)*
- [HOM-11](05-home-nav.md) Recap is a figure wall with no mark, and shows a shimmer for an instant local read *(medium · M)*
- [HOM-13](05-home-nav.md) The widget reads like a text dump and works out "next up" on its own, so it contradicts Home *(medium · M)*
- [PRO-07](07-profile.md) The WEIGHT figure is rounded to a whole kg or lb, while its delta and the rows beside it show tenths *(medium · S)*
- [PRO-09](07-profile.md) The ACTIVITY calendar has no day numbers, no marker for today, and no quick way back to this month *(medium · S · sign-off)*
- [BOD-07](08-body-photos.md) The before/after share card crops the arms off and uses the retired palette *(medium · S)*
- [FWK-10](15-first-week.md) Session summary: figure labels below the contrast floor, the wrong sheet fill, and a coach called "He" *(medium · S)*
- [GYM-15](16-gym-floor.md) The session's top row spends its width on a passive date and a second accent button *(medium · S · sign-off)*
- [SYS-04](22-system-surfaces.md) The weekly recap is one run-on line, with a per-set volume format, no comparison, and the "On this day" memory last and stripped of its numbers *(medium · S)*
- [INT-08](23-interrupt-window.md) Onboarding draws under the status bar and the 3-button navigation bar *(medium · S)*
- [INT-01](23-interrupt-window.md) The dark-only app still follows the phone's light/dark switch: dark status icons in light mode, and a theme flip rebuilds the screen *(medium · S)*
- [TRL-17](02-train-logging.md) The rest-timer controls dialog is off-kit *(low · S · sign-off)*
- [STH-18](04-stats-history.md) The Balance bar's 50/50 tick contradicts its own verdict, and its window is unnamed *(low · S)*
- [CAR-11](11-cardio.md) The week page's figure row is a five-figure wall that repeats its own marks *(low · S)*
- [CAR-14](11-cardio.md) The HR zone picker is a bare Z1-Z5 with nothing saying what each zone feels like *(low · S)*
- [CAR-16](11-cardio.md) Session-detail steps show the whole day without marking the session, and the tab's steps header doesn't say "today" *(low · S)*
- [GYM-16](16-gym-floor.md) The rest countdown is drawn twice, and the floating copy clips at large text sizes *(low · S · sign-off)*
- [UNT-10](20-units-formats.md) The Units & format preview shows conversion leftovers, and two rows don't say what they set *(low · S)*
- [SYS-07](22-system-surfaces.md) The session PDF stops at one page, splits words mid-line, prints em dashes and raw codes, and doesn't mark warm-ups or PRs *(low · M)*
- [INT-07](23-interrupt-window.md) On a tablet, an unfolded foldable or a phone in landscape, every screen stretches edge to edge *(low · S · sign-off)*

### Accessibility (22)

- [TRL-09](02-train-logging.md) The numbers you read mid-set are the dimmest text on the card *(medium · S · sign-off)*
- [STH-17](04-stats-history.md) Session detail's muscle map carries no numbers, and enlarging it opens a dialog *(medium · S · also [AXV-05](19-a11y-voice.md))*
- [PRO-12](07-profile.md) ACTIVITY is hard to navigate with TalkBack and its chevrons point the wrong way in RTL; ALL TIME is read in fragments *(medium · S)*
- [PRO-13](07-profile.md) At 200% font the BODY row cannot fit, and Profile has no screenshot goldens at any scale *(medium · M)*
- [BOD-11](08-body-photos.md) At large font sizes the gallery drops its photo count and every thumbnail date *(medium · S)*
- [SEP-08](09-settings-prefs.md) Cardio activity rows put a delete button inside a tappable row, and delete doesn't say what happens to logged sessions *(medium · S)*
- [ONB-12](12-onboarding-builder.md) A picked tile's reading turns accent and drops to 3.4:1 on the default Red *(medium · S)*
- [ONB-11](12-onboarding-builder.md) Exercises reorder only by drag, so TalkBack users can't reorder a day *(medium · S)*
- [ACW-03](13-academy-wear.md) "undo" sits next to "rate →", the reps toggle doesn't look tappable, and status text is 8sp *(medium · S)*
- [KIT-07](18-consistency-kit.md) Destructive capsule labels are error red on a filled capsule, at 3.2:1 and 3.0:1 *(medium · S · sign-off)*
- [AXV-13](19-a11y-voice.md) On the default Red accent, a failed change and a working one are the same red mark *(medium · S)*
- [AXV-04](19-a11y-voice.md) Pushed screens have no name for TalkBack, and their serif titles are not headings *(medium · S)*
- [AXV-08](19-a11y-voice.md) Two recipes teach a 200% break, and their goldens pin it *(medium · S)*
- [AXV-07](19-a11y-voice.md) Fixed widths and heights around text break Stats rows and browser tiles at 200% *(medium · S)*
- [CCH-17](06-coach-goals.md) TalkBack hears "Apply", "Skip this change", "Undo this change", with no subject *(low · S)*
- [SEP-17](09-settings-prefs.md) The colour wheel and brightness slider can't be used with TalkBack, and icon tiles have no radio role *(low · S)*
- [SED-16](10-settings-data.md) Backup password fields can't be shown *(low · S)*
- [ACW-16](13-academy-wear.md) Whether a lesson is read is shown by colour only, so TalkBack can't hear it *(low · S)*
- [KIT-16](18-consistency-kit.md) Earlier/later pagers are built three ways, and the Stats heatmap's is stock M3 with chevrons that don't mirror *(low · S)*
- [AXV-06](19-a11y-voice.md) TalkBack reads the Coach change line and trend words as symbol names *(low · S)*
- [AXV-15](19-a11y-voice.md) In RTL the Coach ledger spine stays on the left, and the Stats calendar arrows don't mirror *(low · S)*
- [UNT-11](20-units-formats.md) TalkBack hears "Increase PL" and "Decrease KG" on the live steppers, and bare unit letters on body rows *(low · S)*

### Copy and voice (13)

- [STH-08](04-stats-history.md) "Where you stand" stamps a tier word on any lift from one generic bodyweight scale *(medium · S)*
- [HOM-15](05-home-nav.md) RECENT marks a planned deload as a below-average session in the negative hue *(medium · S)*
- [CCH-03](06-coach-goals.md) A week with no call drops the coach's instruction *(medium · S)*
- [FWK-03](15-first-week.md) Each day's first session ends on "Consistency is what moves the needle" *(medium · S)*
- [RIT-13](17-ritual-browse.md) The Monday push says "Your coach has an update" and never names the change, and deload weeks get two pushes *(medium · S)*
- [TRF-15](01-train-flow.md) The beat-last-session line uses a ⚡ and the reserved △ LAST green *(low · S · sign-off)*
- [HOM-16](05-home-nav.md) Two milestone lines are praise with no numbers *(low · S)*
- [SEP-18](09-settings-prefs.md) Backup uses a cloud glyph in an app that has no internet permission *(low · S)*
- [SED-14](10-settings-data.md) Error messages show raw exception text, an em dash and "schema" *(low · S)*
- [ONB-05](12-onboarding-builder.md) After a rebuild, Settings says "Open Gym to see it", and there is no Gym *(low · S)*
- [RCH-10](14-reachability.md) The daily reminder and come-back nudge repeat fixed poster lines, with em dashes and an emoji *(low · S)*
- [AXV-11](19-a11y-voice.md) Profile's "Closest:" trophy line builds broken sentences *(low · S · parked UI, see Not covered)*
- [UNT-12](20-units-formats.md) The Academy's double-progression example is always in kilograms *(low · S)*

## FEATURES.md claims to reconcile

FEATURES.md lists things the app doesn't ship. These were not filed as findings:

- "Shareable workout card": session detail shares raw JSON only ([SYS-06](22-system-surfaces.md) proposes PDF and text).
- "Trophies & Ranks", "Shareable rank card" and "Goal-achievement trophies": parked behind `Features.SHOW_GAMIFICATION = false`.
- "Date & time formats": the date format and timezone settings were removed (see the FormatPage comment in `SettingsSubPages.kt`). Only Clock and Week starts remain.
- "On this day memories": exists only as a clause in the Monday recap push ([SYS-04](22-system-surfaces.md)).
- "Quick-log your bodyweight from the Stats Body tab": contradicted by [RIT-11](17-ritual-browse.md); [RCH-14](14-reachability.md) also asks for the line to be fixed.

## Not covered

- **Parked gamification.** `Features.SHOW_GAMIFICATION = false` hides `ui/trophies/**`, `profile/RankSection.kt`, `RankEmblem.kt`, `RankCardRenderer.kt`, `TrophyCaseSection.kt`, `TrophyIconMotion.kt`, the Profile XP and standing cells, the rank-up celebration and the weekly recap's trophy line. The Trophies route is registered but nothing links to it. Nothing was proposed for these. [AXV-11](19-a11y-voice.md) (the "Closest:" trophy line) sits on this parked UI; it is excluded from the ranking and should be downgraded or dropped.
- **`ui/experiment/SurfaceKit.kt`** is production code despite its package name: Home, Profile and Goals import it. It got no finding of its own and is covered indirectly by [HOM-05](05-home-nav.md), [KIT-06](18-consistency-kit.md), [KIT-15](18-consistency-kit.md) and [FWK-05](15-first-week.md).
- **Localisation.** There are no `values-*` translations, so nothing beyond RTL ([AXV-15](19-a11y-voice.md), [PRO-12](07-profile.md), [KIT-16](18-consistency-kit.md)) applies.
- **Thin spots.** Areas 05 and 14 together covered seven surfaces, and the check-in, the notifications screen and the widget got only one finding each. Areas 21 and 22 cover the parts of those surfaces that face outside the app or span weeks, but these three have had less scrutiny than the rest of the app.
- **File coverage** is otherwise complete: every file under `ui/`, `widget/` and the Wear UI maps to at least one of areas 01 to 19. Every area has a verified result.
- **Correctness bugs** are outside this pass. The 2026-09-26 review and BUG_SCAN.md cover them, and findings here point to them where they overlap: UNT-02 leaves its typo case to a 2026-09-26 P3, and GYM-11 asks for BUG_SCAN's assisted-set item to be fixed first.
- **Nothing was observed on a device.** Font-scale breaks, RTL, motion and contrast were read from code and goldens. The items most worth checking on hardware before building are [PRO-09](07-profile.md) (day numbers on a grid Antho tuned), [INT-01](23-interrupt-window.md) (system bar icons), [INT-07](23-interrupt-window.md) (wide windows) and [ACW-06](13-academy-wear.md) (ambient mode).
