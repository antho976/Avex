# Accessibility, font scale, RTL, monochrome and voice (cross-app)

Scope: the shared kit in `ui/common/` (press modifiers, capsules, `EditorialFigure`, `ForgeTopBar`), the recipes and goldens that pin it, `BodyHeatmap`, the Coach ledger's marks, Stats and browser rows at 200%, generated copy in `domain/` and `service/`, checked against `DesignDoctrine`/`DesignDoctrineTest`/`DoctrineParityTest`. 15 raised, 11 kept (10 revised), 4 dropped.

Most of this debt sits in shared primitives, not in screens. `bounceClick` gives no button role. `EditorialFigure` clamps its value and prints a delta with no unit. `BodyHeatmap` and the pushed screens' serif titles give TalkBack nothing. Two recipes teach 200% breaks, and the accepted goldens now defend them. A fix in the primitive closes several per-screen findings in sibling reports at once, so each entry below says which ones it covers. Colour has one gap of its own: on the default Red, the error flag uses the accent's hue. The voice findings were already raised by other reports in this pass and are dropped as duplicates.

## Findings

### AXV-03 · Primary capsules announce no role, and the buttons paired with them ripple instead of bouncing
accessibility · impact medium · effort S · Every `ForgePrimaryCapsule` / `ForgeOutlineCapsule` / `SegmentPill`; Coach Apply + Skip; modal commit + cancel pairs

- **Now:**
  - `bounceClick` calls `clickable` with no `role` and no `onClickLabel` ([BounceClick.kt:46-53](../../../forge-android/app/src/main/java/com/forge/app/ui/common/BounceClick.kt#L46-L53)). It has 73 call sites, among them `ForgePrimaryCapsule` and `ForgeOutlineCapsule` ([Capsules.kt:52](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Capsules.kt#L52), [:84](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Capsules.kt#L84)), `SegmentPill` ([SegmentPill.kt:40](../../../forge-android/app/src/main/java/com/forge/app/ui/common/SegmentPill.kt#L40)) and `EditorialHeader`'s action ([Editorial.kt:51](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Editorial.kt#L51)). TalkBack reads the label and "double-tap to activate", never "button".
  - `clickableLabeled` sets `Role.Button` and a label but keeps the default indication ([ForgeModifiers.kt:16-20](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeModifiers.kt#L16-L20)), so its 67 call sites ripple.
  - The two collide in the button pairs. Coach's Apply is a `ForgePrimaryCapsule`: it bounces and has no role. Skip, beside it, is `CoachAction` → `clickableLabeled`: it ripples and reads "Skip this change, button" ([CoachCall.kt:127-133](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachCall.kt#L127-L133), [CoachUi.kt:265-277](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachUi.kt#L265-L277)). `ForgeSecondaryCapsule` goes through `ForgeChromeButton` → `clickableLabeled` ([ForgeGroups.kt:685-698](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L685-L698)). So every primary + secondary pair has a commit that bounces and a cancel that ripples: the Modal recipe ([ModalRecipe.kt:96-97](../../../forge-android/app/src/debug/java/com/forge/app/ui/recipes/ModalRecipe.kt#L96-L97)), the check-in sheet ([CheckinSheet.kt:201-206](../../../forge-android/app/src/main/java/com/forge/app/ui/checkin/CheckinSheet.kt#L201-L206)), the cardio log sheet ([CardioLogSheetSections.kt:175-185](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/components/CardioLogSheetSections.kt#L175-L185)) and the custom-activity dialog ([CustomActivityDialog.kt:109-118](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/components/CustomActivityDialog.kt#L109-L118)).
  - The gate misses both problems. `tappablesAnnounceThemselves` names `bounceClick` as compliant ([DesignDoctrineTest.kt:190-195](../../../forge-android/app/src/test/java/com/forge/app/ui/DesignDoctrineTest.kt#L190-L195)), and `pressIsBounceNotRipple` only matches the literal word "ripple" ([DesignDoctrine.kt:393](../../../forge-android/app/src/test/java/com/forge/app/ui/DesignDoctrine.kt#L393)).
- **Why it matters:** SETTLED fixed this exact pair on Home on 2026-08-22: "a control that does not answer the press reads as the disabled one". It is now back on the Coach call and on every modal's commit pair. TalkBack users hear the Monday ritual's Apply with no button role.
- **Proposal:**
  - Give `bounceClick` `role: Role? = Role.Button` and `onClickLabel: String? = null`, and give `bounceCombinedClick` the same `role`. Pass both straight to `clickable` / `combinedClickable`. `SegmentPill` passes `Role.Tab`, since it already sets `selected`. This is a semantics change only.
  - Move the paired buttons onto the bounce: `ForgeChromeButton` and `CoachAction` call `bounceClick(onClickLabel = label)` instead of `clickableLabeled`. That brings every `ForgeSecondaryCapsule`, the top-bar chrome and Coach's Skip/Undo along with them.
  - Reword `tappablesAnnounceThemselves`' message so it says `bounceClick` carries a role.
  - Revised: the finder wanted `clickableLabeled` to delegate to `bounceClick` everywhere. That is cut (see dropped table), because it would change the press on the live screen.
- **Doctrine:** §9: press is a bounce, with ripple restored under TalkBack, and `bounceClick` keeps that fallback. §14: tappables announce themselves. SETTLED Home 2026-08-22, "the half-bounce button pair". No layout or colour changes. CCH-17 in [06-coach-goals.md](06-coach-goals.md) adds per-call wording to Apply/Skip. This finding covers the role and the press, and CCH-17's label can then travel through the new `onClickLabel`.

### AXV-01 · Figure rows ellipsize instead of wrapping, and the one shared figure is laid out five ways
ui-polish · impact medium · effort S · Coach week figures; Cardio hero, weeks, week detail and session sheet; Recap; Stats hero

- **Now:**
  - `EditorialFigure` draws its value at `headlineMedium` (28sp serif) with `maxLines = 1` and an ellipsis ([Editorial.kt:92-98](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Editorial.kt#L92-L98)). That line is frozen debt ([design-allowlist.txt:174](../../../forge-android/app/src/test/resources/design-allowlist.txt#L174)).
  - Callers lay the row out in five ways:
    - Stats: a `FlowRow` with figures at their natural width ([StatsOverview.kt:76-97](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/StatsOverview.kt#L76-L97)).
    - Recap, plus three Cardio screens: equal `weight(1f)` columns. That covers Recap ([RecapScreen.kt:202-224](../../../forge-android/app/src/main/java/com/forge/app/ui/recap/RecapScreen.kt#L202-L224)), Cardio weeks ([CardioWeeksScreen.kt:167-178](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/CardioWeeksScreen.kt#L167-L178)), the week detail ([CardioWeekDetail.kt:140-151](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/components/CardioWeekDetail.kt#L140-L151)) and the session sheet ([CardioSessionDetailSheet.kt:151-185](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/components/CardioSessionDetailSheet.kt#L151-L185)).
    - The Cardio hero: the same equal weights, plus a reserved `Spacer(weight(1f))` when there is no streak, under a comment that says the figures wrap ([CardioComponents.kt:106-138](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/CardioComponents.kt#L106-L138)).
    - Coach: a 1.3× font clamp plus 1 / 1.5 / 0.7 weights ([CoachAccount.kt:160-187](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachAccount.kt#L160-L187)). §14 reserves the clamp for the serif hero.
  - A weighted Row cannot wrap. On Recap, four columns on a 360dp phone get about 66dp each, while `formatVolumeCompact` returns "48.2k lb", roughly 100dp at 28sp. So the month's volume ellipsizes at 100% font, before any scaling. This is estimated from type metrics; Recap has no golden.
- **Why it matters:** Coach's week row and the Cardio hero are the readings on two hub tabs. At 200%, Cardio's figures turn into ellipses and Coach's stop scaling. Recap's headline volume is cut off at default size. Nobody can see that today, because Recap has no way in ([DayListScreen.kt:100-102](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/DayListScreen.kt#L100-L102)), but it lands with HOM-01 in [05-home-nav.md](05-home-nav.md).
- **Proposal:**
  - Promote Stats' working pattern into `ui/common/Editorial.kt` as `EditorialFigureRow(figures)`: a `FlowRow` with `spacedBy(20.dp)` horizontally and `12.dp` vertically, each figure at its natural width.
  - Route every caller above through it. Delete Cardio's reserved Spacers and Coach's clamp and weights.
  - Once no caller weights a figure, drop `maxLines = 1` from the value and its allowlist line.
  - Recap can pass `withUnit = false` and put the unit in the label ("LB LIFTED"), the shape SETTLED records for Home's `WeekReadout`.
  - Add the component to §8's kit line in the same edit. `DoctrineParityTest` checks the inventory, and DESIGN.md is at its 420-line cap, so the name goes into the existing line.
  - Revised: CAR-06 in [11-cardio.md](11-cardio.md) proposes the same `FlowRow` but cardio-local, explicitly leaving `EditorialFigure` alone. Five feature packages repeat this row, so §2⑥ says promote, and CAR-06 should use this component. HOM-11 in [05-home-nav.md](05-home-nav.md) decides which Recap figures survive; this is the row they sit in.
- **Doctrine:** §14: "Figure rows wrap rather than clip", and only the serif hero clamps. §2⑥: the third use is promoted. §2①: still 2–4 figures, so no Figure wall. FAILURES *Silent scale break*. Home has no figure row, so SETTLED's Home volume removals are untouched.

### AXV-13 · On the default Red accent, a failed change and a working one are the same red mark
accessibility · impact medium · effort S · Coach ledger spine, Coach lift sparklines, recovery-load meter

- **Now:**
  - §8 flags exceptions by colour: accent for a win or an active item, error for a failure. The default accent is Red `#E23D3D` and error is `#BF4040` ([Color.kt:48](../../../forge-android/app/src/main/java/com/forge/app/ui/theme/Color.kt#L48), [:64](../../../forge-android/app/src/main/java/com/forge/app/ui/theme/Color.kt#L64)). They share a hue and sit about 1.2:1 apart in luminance.
  - On the ledger, OPEN and APPLIED nodes take accent and FAILED takes error, and all three are filled discs. APPLIED and FAILED are both 3.5dp ([CoachUi.kt:224-230](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachUi.kt#L224-L230)). A change that failed or was reverted ([CoachAccount.kt:426](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachAccount.kt#L426)) draws the same red dot as one that worked.
  - A stalling lift's sparkline "goes red" ([CoachStand.kt:173-177](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachStand.kt#L173-L177)), next to rising lifts that are drawn in red accent.
  - The recovery meter's fill switches from accent to error at the deload line ([CoachCharts.kt:149](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachCharts.kt#L149)), which on the default theme is red to red.
  - The words still carry the meaning ("didn't stick", "stalling"), so nothing is unreadable, but the marks no longer stand out. The lighter error SETTLED open decision 2 considers, `#D96565`, is in the same hue family.
- **Why it matters:** The ledger is the record of whether the coach got it right (PRODUCT must-survive #3). On any other accent, colour lets you scan the spine for what failed. Default users can't.
- **Proposal:**
  - Ledger: draw the FAILED node as a 1.5dp error ring with a short diagonal stroke through it. That separates it from APPLIED's disc and from DECLINED's plain outline ring.
  - Lift sparklines: give `CoachSparkline` a hollow end-dot option, and use it for a stalling lift. Its filled end-dot is drawn at [CoachCharts.kt:107-108](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachCharts.kt#L107-L108).
  - Recovery meter: draw the part past the deload line as its own segment, after a 2dp gap at the tick.
  - Add a line to SETTLED open decision 2: a lighter error must stay hue-distinct from the Red default.
  - Revised: the finder left the diagonal optional. It is required here, because a bare red ring differs from DECLINED's grey ring only by hue.
- **Doctrine:** §8: flag only exceptions. FAILURES *Invisible ghost*: "shape and hue, never luminance alone", and *Grey dot column*. The SETTLED Coach-ledger removals stand: no section is added and the spine stays data. No accent- or error-coloured text is added.

### AXV-04 · Pushed screens have no name for TalkBack, and their serif titles are not headings
accessibility · impact medium · effort S · The 26 `ForgeTopBar` screens (Goals, Recap, Weeks, Measurements, Notifications, session detail and the rest)

- **Now:**
  - §4.6 keeps the screen name out of the top bar, so `ForgeTopBar` exposes only its back button. Its `title` parameter is used once, for a selection count ([ForgeGroups.kt:751-781](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L751-L781), [MirrorTestScreen.kt:591](../../../forge-android/app/src/main/java/com/forge/app/ui/profile/MirrorTestScreen.kt#L591)).
  - Nothing names the screen in semantics. `paneTitle` appears once in the app, on Coach's advanced-tracking overlay ([CoachAdvancedPrompt.kt:95](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachAdvancedPrompt.kt#L95)).
  - The serif titles that name these screens visually are plain `Text` on Goals ([GoalsScreen.kt:135](../../../forge-android/app/src/main/java/com/forge/app/ui/goals/GoalsScreen.kt#L135)), Recap ([RecapScreen.kt:78](../../../forge-android/app/src/main/java/com/forge/app/ui/recap/RecapScreen.kt#L78)), Weeks ([CardioWeeksScreen.kt:165](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/CardioWeeksScreen.kt#L165)), Measurements ([BodyMeasurementsScreen.kt:134](../../../forge-android/app/src/main/java/com/forge/app/ui/profile/BodyMeasurementsScreen.kt#L134)) and Notifications ([NotificationsScreen.kt:134](../../../forge-android/app/src/main/java/com/forge/app/ui/notifications/NotificationsScreen.kt#L134)). Only History's is a `heading()` ([SessionHistoryScreen.kt:86](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/history/SessionHistoryScreen.kt#L86)).
- **Why it matters:** A TalkBack user who opens one of these screens hears no name, focus lands on "Back, button", and heading navigation has no anchor at the top of the page.
- **Proposal:**
  - Add `screenName: String? = null` to `ForgeTopBar`. When it is set, put `Modifier.semantics { paneTitle = screenName }` on the bar's row, so the name is announced on arrival and never drawn. Fill it in at the 26 call sites.
  - Promote the serif page title to `ui/common` as `PageTitle(text, style)`, which adds `heading()`. History, Goals, Recap, Weeks, Measurements and Notifications make six uses (§2⑥). Add it to §8's kit line.
  - Revised: the hub pager part is dropped (see table).
- **Doctrine:** §4.6: the top bar never shows the screen's name. That still holds, because the name lives only in semantics. §14 semantics. No visible change.

### AXV-08 · Two recipes teach a 200% break, and their goldens pin it
accessibility · impact medium · effort S · Detail and Modal recipes; the commit pairs copied from them (check-in, cardio log, custom activity)

- **Now:**
  - The Detail recipe's `StatRow` gives the mono label `weight(1f)` and leaves the value and meta unweighted ([DetailRecipe.kt:161-185](../../../forge-android/app/src/debug/java/com/forge/app/ui/recipes/DetailRecipe.kt#L161-L185)). At 200% the value and meta take their width first, and "TOP SET" stacks as "TO / P / SE / T" in [detail-200.png](../../../forge-android/app/src/test/screenshots/detail-200.png) and [detail-long-200.png](../../../forge-android/app/src/test/screenshots/detail-long-200.png).
  - The Modal recipe gives `ForgePrimaryCapsule` and `ForgeSecondaryCapsule` `weight(1f)` each ([ModalRecipe.kt:92-98](../../../forge-android/app/src/debug/java/com/forge/app/ui/recipes/ModalRecipe.kt#L92-L98)). At 200%, "Open session" wraps to two lines while "Close" stays on one, so the pair stands at two heights ([modal-200.png](../../../forge-android/app/src/test/screenshots/modal-200.png)). The wrapped label is also left-aligned: the primary's label has no `textAlign` ([Capsules.kt:56-60](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Capsules.kt#L56-L60)), while the secondary's does ([ForgeGroups.kt:721](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L721)).
  - Real screens already copy the pair: the check-in sheet ([CheckinSheet.kt:201-206](../../../forge-android/app/src/main/java/com/forge/app/ui/checkin/CheckinSheet.kt#L201-L206)), the cardio log sheet, where "Save changes" or "Save rest day" sits beside Cancel ([CardioLogSheetSections.kt:175-185](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/components/CardioLogSheetSections.kt#L175-L185)), and the custom-activity dialog ([CustomActivityDialog.kt:109-118](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/components/CustomActivityDialog.kt#L109-L118)).
- **Why it matters:** §0 says every new screen starts from its recipe, so a recipe's 200% bug ships with each new detail page or modal. Because these goldens were accepted, CI now treats the break as correct.
- **Proposal:**
  - `StatRow`: the label sizes to its own text, the value takes `weight(1f)` and the meta trails it. Above 1.3×, the label moves to its own line, using the idiom in [CoachStand.kt:286](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachStand.kt#L286).
  - Button pairs: use `Row(Modifier.height(IntrinsicSize.Min))` with `fillMaxHeight()` on both capsules, and set `textAlign = TextAlign.Center` on `ForgePrimaryCapsule`'s and `ForgeOutlineCapsule`'s labels. Apply the same Row to the three real pairs.
  - Look at the changed goldens (detail-200, detail-long-200, modal-200) before re-recording them.
  - Revised: the finder's new `HubScreenshotTest` is cut, because real-screen goldens are already tracked (see table).
- **Doctrine:** §14: "a changed golden is a question, not a chore". DECISIONS 2026-07-24, third audit pass: goldens that agree with themselves. §8: the button levels are unchanged. This touches the recipes plus three button rows.

### AXV-07 · Fixed widths and heights around text break Stats rows and browser tiles at 200%
accessibility · impact medium · effort S · Stats Volume lens sets-per-muscle rows, Stats RPE histogram, Exercise Browser tiles, Cardio entry rows

- **Now:**
  - The sets-per-muscle rows fix the muscle name at `width(82.dp)` and the "12/16" count at `width(48.dp)` with 8dp of start padding ([StatsVolume.kt:75](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/StatsVolume.kt#L75), [:106](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/StatsVolume.kt#L106)). At 200%, `bodySmall` is 24sp, so "Hamstrings", "Shoulders" and two-digit counts break mid-word. This is the letter-stacking visible in detail-200.png.
  - The RPE histogram is a `Row` fixed at `height(72.dp)`, holding bars up to 52dp plus a 3dp gap and a `labelSmall` label ([StatsInterest.kt:42-58](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/StatsInterest.kt#L42-L58)). At 200% the label no longer fits under the tallest bar and is cut.
  - Exercise Browser tiles set `minLines = 2, maxLines = 2` with an ellipsis on the exercise name, in a fixed 2-column grid ([ExerciseBrowserScreen.kt:556-563](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/freestyle/ExerciseBrowserScreen.kt#L556-L563), [:232](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/freestyle/ExerciseBrowserScreen.kt#L232)). 38 of the 116 library names run 18 characters or more ("Captain's Chair Knee Raise", "DB Bulgarian Split Squat"), and custom names can be longer. At 200% a tile line holds about eight characters. The doctrine gate only matches `maxLines = 1` ([DesignDoctrine.kt:382](../../../forge-android/app/src/test/java/com/forge/app/ui/DesignDoctrine.kt#L382)).
  - Cardio entry rows put the day label in `width(56.dp)` ([CardioEntryRow.kt:117-124](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/components/CardioEntryRow.kt#L117-L124)). Older rows read "MON · SEP 22", which wraps even at 100%.
- **Why it matters:** Each of these is the thing a 200% user opened the screen to read: which muscle got how many sets, the name of the exercise they are picking, the day a run happened.
- **Proposal:**
  - Muscle rows: `widthIn(min = 82.dp)` and `widthIn(min = 48.dp)` instead of fixed widths. Above 1.3×, stack the name over its bar, like `LiftTrendRow`'s `stacked` ([CoachStand.kt:286](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachStand.kt#L286)).
  - RPE histogram: `heightIn(min = 72.dp)`, with a fixed 52dp bar area and the labels in their own row underneath.
  - Browser: drop `maxLines` but keep `minLines = 2` so the grid stays even, and use `GridCells.Fixed(1)` above 1.3×. The check-in sheet already changes its grid's column count by font scale ([CheckinSheet.kt:143](../../../forge-android/app/src/main/java/com/forge/app/ui/checkin/CheckinSheet.kt#L143)).
  - Cardio rows: `widthIn(min = 56.dp)`.
  - Revised: the gallery part and the regex widening are cut (see table).
- **Doctrine:** §14: containers size to their content, exercise titles are never clamped, and every touched screen is checked at 200%. DECISIONS 2026-07-24 found fixed heights mostly safe. These are fixed widths, plus one fixed height around text, which that pass did not classify. PRODUCT: 200% with no lost content. Stats' 16dp gutter (a SETTLED known defect) is not touched.

### AXV-02 · Coach's ↑7 is a percent with no %, and TalkBack hears a figure's delta before its label
consistency · impact low · effort S · Coach THIS WEEK figures; every `EditorialFigure` with a delta

- **Now:**
  - `EditorialFigure` takes `delta: Int?` and prints "↑N" with no unit ([Editorial.kt:99-107](../../../forge-android/app/src/main/java/com/forge/app/ui/common/Editorial.kt#L99-L107)).
  - Coach passes `volumeDeltaPct` ([CoachAccount.kt:176](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachAccount.kt#L176), [WeeklyReview.kt:40-43](../../../forge-android/app/src/main/java/com/forge/app/domain/coach/WeeklyReview.kt#L40-L43)), so "42.2k lb ↑7" means 7 percent. One tab over, Stats passes raw volume in display units, plus counts ([StatsOverview.kt:57-58](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/StatsOverview.kt#L57-L58)). The Overview recipe teaches the unitless shape ([OverviewRecipe.kt:121](../../../forge-android/app/src/debug/java/com/forge/app/ui/recipes/OverviewRecipe.kt#L121)).
  - The figure column has no merged semantics, so TalkBack makes three stops: the value, the arrow plus number, then the label.
- **Why it matters:** A lifter can't tell whether ↑7 is 7 kg, 7 sessions or 7%. A screen-reader user hears the delta before learning which figure it belongs to.
- **Proposal:**
  - Revised to build on STH-11 in [04-stats-history.md](04-stats-history.md), which adds an optional preformatted `deltaText` and fixes the Stats half ("↑1.2k"). With that in place, Coach passes "7%".
  - In `EditorialFigure`, merge the column into one node with a label-first description: "Volume 42.2k lb, up 7%" (`semantics(mergeDescendants = true) { contentDescription = … }`). The arrow is spoken as "up" or "down", the way session detail's `SessionFigure` already does it ([SessionDetailComponents.kt:266-269](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/session/SessionDetailComponents.kt#L266-L269)).
  - Give the recipe's delta its unit.
- **Doctrine:** §11: ↑ in accent and ↓ in muted, unchanged. §14 semantics. FAILURES *Two units, one section*, here across sibling tabs. No figure or mark is added.

### AXV-05 · The muscle map gives TalkBack nothing, and the finish summary has no text for it at all
accessibility · impact low · effort S · Finish summary (WHAT YOU WORKED), session detail header, Stats hero

- **Now:** `BodyHeatmap` draws two Canvas figures, each muscle tinted by its set count, and sets no semantics ([BodyHeatmap.kt:76-96](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/components/BodyHeatmap.kt#L76-L96)). Each of the three call sites handles that differently:
  - Session detail wraps it in "Muscles worked, tap to enlarge" ([SessionDetailComponents.kt:146](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/session/SessionDetailComponents.kt#L146)).
  - Stats captions it "LAST 7 DAYS" ([StatsOverview.kt:107-119](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/StatsOverview.kt#L107-L119)).
  - The finish summary draws it under WHAT YOU WORKED, followed by exercise rows ([SessionSummarySheet.kt:165-176](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/components/SessionSummarySheet.kt#L165-L176)). That sheet has no muscle-level text anywhere.
- **Why it matters:** After every workout, a TalkBack user gets the exercise list but not which muscles took the work.
- **Proposal:**
  - Do it once inside `BodyHeatmap`: merge both figures into one node whose description reads `setsByMuscle`. The busiest three come first ("Chest 12 sets, back 9, quads 6"), then "and N more" when there are more. At zero it reads "No sets on any muscle". The legend Canvas stays decorative.
  - Add an optional `readingPrefix` so Stats can say "Last 7 days".
  - Revised: STH-17 in [04-stats-history.md](04-stats-history.md) already adds a description at the session-detail call site. With this change, that call site passes "Muscles worked" as the prefix, and the finish summary gets its reading at no extra cost. The 2026-09-12 audit (S5) ruled that the Stats map has a text alternative on the Volume lens. That still holds, so impact is lowered to low.
- **Doctrine:** §14: every Canvas mark carries a description that reads its value. §12: the zero state's words match the faint silhouette it draws. No visual change, and the finish sheet is not the live screen.

### AXV-06 · TalkBack reads the Coach change line and trend words as symbol names
accessibility · impact low · effort S · Coach call change line, Coach lift trend words, `→` text links

- **Now:**
  - The Coach call's serif change line, "3 → 4 sets", is a plain `Text` with no description ([CoachCall.kt:54](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachCall.kt#L54), [:106-111](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachCall.kt#L106-L111)).
  - A watched lift's description is built as "$name trend, ↑ 4%" ([CoachStand.kt:342](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachStand.kt#L342), [:317](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachStand.kt#L317)).
  - Text links add " →" to the text that gets read: `CardioLinkCapsule` ([CardioChrome.kt:65](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/CardioChrome.kt#L65)), the Home and Profile card link ([SurfaceKit.kt:609](../../../forge-android/app/src/main/java/com/forge/app/ui/experiment/SurfaceKit.kt#L609)), and `CoachAction` labels such as "Apply all 3 →" ([CoachAccount.kt:108](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachAccount.kt#L108)).
  - Depending on the speech engine and TalkBack's punctuation setting, the arrows are either spoken as symbol names ("right arrow", "upwards arrow") or dropped. Dropped, "3 → 4 sets" becomes "3 4 sets". Not device-verified.
  - Two places already solve this locally: `SessionFigure`'s "up from last session" and △ LAST's "Beats last time" ([FreestyleLogParts.kt:442](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/freestyle/FreestyleLogParts.kt#L442)).
- **Why it matters:** The change line is the Monday decision. A TalkBack user should hear "3 to 4 sets".
- **Proposal:**
  - Add one `String.spokenGlyphs()` helper to `ui/common/Format.kt`. It rewrites "a → b" as "a to b", drops a trailing " →" (the button role already says it acts), and reads "↑ n" / "↓ n" as "up n" / "down n".
  - Use it as the `contentDescription` on the change line, in the lift-trend description, and in the three link and action composables named above.
  - Revised: cut to these sites, with no rewrites for × or · (see table). CCH-17 in [06-coach-goals.md](06-coach-goals.md) already says "to" for the arrow inside the Apply/Skip labels. This finding covers the line itself and the shared link text.
- **Doctrine:** §11: the glyphs stay exactly as drawn. §14 semantics. Semantics only.

### AXV-11 · Profile's "Closest:" trophy line builds broken sentences
copy-voice · impact low · effort S · Profile trophy case; Trophies progress hints

- **Now:**
  - Profile renders "Closest: " + `progressRemaining` + " away from " + the trophy name ([TrophyCaseSection.kt:107](../../../forge-android/app/src/main/java/com/forge/app/ui/profile/TrophyCaseSection.kt#L107), [ProfileRepository.kt:193-198](../../../forge-android/app/src/main/java/com/forge/app/data/repo/ProfileRepository.kt#L193-L198)).
  - Several of those remainders already end in "to go" ([TrophyEvaluator.kt:167](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L167), [:173](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L173)). The result is "Closest: 12 km to go away from Century Club" and "Closest: 184000 lb to go away from Million Pound Club", with the tonnage not k-abbreviated.
  - Counts never take the singular: "1 PRs", "1 workouts" ([:147](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L147), [:151](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L151)). Goals use a paren plural, "1 more goal(s)", which is frozen debt ([:166](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L166)). Cardio sessions read "3 more away from …" with no noun ([:172](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L172)).
  - Distance is hard-coded to km here and in `progressHint` ([:89](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L89)), whatever the distance setting.
  - The Variety Pack hint says "Train all 4 days in one week" ([:84](../../../forge-android/app/src/main/java/com/forge/app/domain/trophy/TrophyEvaluator.kt#L84)), but the rule checks every day of the current plan, anywhere from 3 to 7 ([TrophyRepository.kt:279-296](../../../forge-android/app/src/main/java/com/forge/app/data/repo/TrophyRepository.kt#L279-L296)).
- **Why it matters:** The line has visibly broken grammar, the wrong unit for anyone using miles, and the wrong instruction for anyone not on a 4-day split.
- **Proposal:**
  - Have `TrophyEvaluator` return a structured remainder (amount, singular and plural noun, unit kind) and compose the line in the UI: "12 km to Century Club" (miles when that is the setting), "184k lb to Million Pound Club" via `formatVolumeCompact`, "1 more PR to …", "3 more cardio sessions to …".
  - Build the Variety Pack hint from `Program.dayKeys.size`.
  - The 2026-09-26 audit's dead `closestTrophyNudge` (05, P3) is a separate field and is not touched.
- **Doctrine:** §11 *Translate the machine* (paren plurals) and k-abbreviation at 10,000 and above. PRODUCT: never hard-code units. SETTLED wait-lists new gamification surfaces. This fixes the copy of an existing line and adds nothing.

### AXV-15 · In RTL the Coach ledger spine stays on the left, and the Stats calendar arrows don't mirror
accessibility · impact low · effort S · Coach ledger, Stats consistency heatmap pager

- **Now:**
  - `ledgerSpine` draws its rule and nodes at `cx = SPINE_X` (10dp) from the left edge ([CoachUi.kt:215](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachUi.kt#L215), [:47](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachUi.kt#L47)), while every entry pads 24dp on both sides and mirrors. In RTL, the spine therefore runs beside the ends of lines, and each node no longer sits at the start of its entry. The same package already mirrors a drawn fill with `layoutDirection` ([CoachCharts.kt:231](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachCharts.kt#L231)).
  - The Stats calendar pager uses `Icons.Filled.ChevronLeft/Right` ([CalendarHeatmap.kt:135](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/components/CalendarHeatmap.kt#L135), [:139](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/stats/components/CalendarHeatmap.kt#L139)), which don't mirror. Cardio's pager uses the auto-mirrored pair ([CardioWeeksScreen.kt:248](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/CardioWeeksScreen.kt#L248), [:256](../../../forge-android/app/src/main/java/com/forge/app/ui/cardio/CardioWeeksScreen.kt#L256)).
  - The `rtl` rule only catches `Alignment` and `TextAlign` left/right ([DesignDoctrine.kt:390](../../../forge-android/app/src/test/java/com/forge/app/ui/DesignDoctrine.kt#L390)).
- **Why it matters:** RTL users see the Coach timeline on the wrong side of its entries, and get an "Earlier weeks" arrow that points toward the later side.
- **Proposal:**
  - In `ledgerSpine`: `val cx = if (layoutDirection == LayoutDirection.Rtl) size.width - SPINE_X.toPx() else SPINE_X.toPx()`.
  - Switch Stats' pager to `Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right`.
  - Add `Icons\.(Filled|Default|Rounded|Outlined)\.(Chevron|KeyboardArrow)(Left|Right)\b` to `RTL_UNSAFE`. Today it also hits Profile's two chevrons, which PRO-12 in [07-profile.md](07-profile.md) switches, so land the rule with PRO-12 or allowlist those two lines.
  - Revised: the Profile stepper part is cut as a duplicate of PRO-12.
- **Doctrine:** §14: layouts stay RTL-correct. No change in LTR. The spine stays a data line (§1), just placed correctly.

## Considered and dropped

| ID | Idea | Why dropped |
|---|---|---|
| AXV-09 | Rewrite SessionOpinion, the reminders and the come-back nudge as number-led lines; widen `HYPE` | Duplicate. FWK-03 in [15-first-week.md](15-first-week.md) rewrites every SessionOpinion branch with its numbers and adds "another gear" and "momentum is building" to `HYPE`. RCH-10 in [14-reachability.md](14-reachability.md) rewrites the reminder and come-back bodies and adds an emoji check. RIT-13 in [17-ritual-browse.md](17-ritual-browse.md) rewrites the brief push. |
| AXV-10 | The coach names itself ("He can read…", "the coach makes its first call", "Your coach has an update") | "He" is FWK-10 in [15-first-week.md](15-first-week.md), and the push title is RIT-13. The other lines are the app describing its Coach tab, not the coach speaking. §11's "never names itself" governs the coach's own generated lines, and FWK-10 keeps "the coach" as the app's word. |
| AXV-12 | Home's session sheet writes "4 × 185 lb" (sets × weight) | Unreachable. Gym rows push session detail ([OverviewScreen.kt:713](../../../forge-android/app/src/main/java/com/forge/app/ui/overview/OverviewScreen.kt#L713)), and `SummarySheet` opens only for cardio, so its exercise rows never render. Already the 2026-09-26 audit's P3 "Dead Overview state pipeline" ([06](../2026-09-26/06-common-nav-theme-overview-goals.md)), and HOM-06 in [05-home-nav.md](05-home-nav.md) deletes the sheet. |
| AXV-14 | Muted text at 0.6 passes the alpha gate; add a sub-floor rule | Every rendered text site is already in TRL-09 in [02-train-logging.md](02-train-logging.md) (RIR, rest reset/done, pinned cue) or FWK-10 (finish-summary labels, which also names the gate miss). A `muted.copy(alpha = 0.6` regex would also flag placeholders whose "placeholder" word sits on the line above ([ForgeGroups.kt:565](../../../forge-android/app/src/main/java/com/forge/app/ui/common/ForgeGroups.kt#L565), [NoteField.kt:158](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/components/NoteField.kt#L158), [MirrorTestScreen.kt:653](../../../forge-android/app/src/main/java/com/forge/app/ui/profile/MirrorTestScreen.kt#L653)) and two chart threshold lines ([CoachCharts.kt:270](../../../forge-android/app/src/main/java/com/forge/app/ui/coach/CoachCharts.kt#L270)), so it is not a one-line gate. |
| AXV-03 (part) | `clickableLabeled` delegates to `bounceClick` app-wide | That would change the press on the live session's steppers ([SetInputRow.kt](../../../forge-android/app/src/main/java/com/forge/app/ui/gym/train/components/SetInputRow.kt), 7 call sites), and SETTLED lists the live screen as untouchable. §9's "migrate when touched" covers the rest. |
| AXV-04 (part) | A pane title on each hub pager page | The bottom bar is `selectable` with `Role.Tab` ([ForgeBottomBar.kt:82-86](../../../forge-android/app/src/main/java/com/forge/app/ui/nav/ForgeBottomBar.kt#L82-L86)), so switching tabs already announces the tab. A pane title would announce it twice. |
| AXV-07 (part) | Gallery count and grid dates dropped at large font | Duplicate of BOD-11 in [08-body-photos.md](08-body-photos.md). |
| AXV-07 (part) | Widen the max-lines regex to `maxLines` 1–3 + ellipsis | A regex can't tell a mono label, which MAP lets clamp at two lines ([MAP.md:654](../../../.claude/design/MAP.md#L654)), from an exercise name. It would cry wolf. |
| AXV-08 (part) | A `HubScreenshotTest` at 100%, 200% and mono | Already tracked: AUDIT.md "Deferred: real-screen first-run goldens" ([AUDIT.md:92](../../../.claude/design/AUDIT.md#L92)), plus CAR-06 in [11-cardio.md](11-cardio.md) and PRO-13 in [07-profile.md](07-profile.md) for the Cardio and Profile goldens. Those should include each screen at 200%. |
| AXV-06 (part) | Spoken rewrites for × and · | "70 kg times 8" and a pause at "·" already read correctly. |
| AXV-15 (part) | Mirror Profile's ACTIVITY month stepper | Duplicate of PRO-12 in [07-profile.md](07-profile.md). |
