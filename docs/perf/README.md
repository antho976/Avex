# Performance readings

Measured numbers for Avex, one file per reading: `BASELINE-<date>.md` for where things stand,
`FIXES-<date>.md` for what a change moved. Raw runs sit beside the reading that used them
(`raw/`), so the next reading can be compared against the same data rather than a summary of it.

`docs/PERFORMANCE_REVIEW_2026-09-28.md` is the earlier review. It was done by reading code and
says so; nothing in it was timed. The readings here are timed on a device.

## The build that is measured

`perf` is a build type of `:app`: the release (R8, the shipped baseline profile) installed as
`com.quietsoftware.avex.perf`, beside the real app. A measurement run never replaces a working
install or touches its data. It is `profileable`, and it carries a receiver that fills an
onboarded install with eight weeks of sample training.

```sh
cd forge-android
ANDROID_HOME=$HOME/Android/Sdk ANDROID_SERIAL=<serial> ./gradlew :app:installPerf
# open "Avex Perf" once and finish onboarding, then give it a history to load:
adb shell am broadcast -a com.forge.app.perf.SEED \
    -n com.quietsoftware.avex.perf/com.forge.app.perf.SeedSampleDataReceiver
```

Never time a debug build: it is several times slower and slow in different places.

## Cold start

```sh
forge-android/tools/perf/coldstart.py -n 10 --raw docs/perf/raw/<name>.json
```

Ten cold starts, launched the way the launcher does. It reports the system's two numbers,
`Displayed` (first frame) and `Fully drawn` (Home showing its real numbers), and the app's own
`AvexStartup` marks in between (`core/perf/StartupTimeline.kt`), as median, p95 and worst.

What the marks mean, in the order a launch reaches them:

| Mark | Reached when |
|---|---|
| `app.create` | `Application.onCreate` begins (process and class loading are behind it) |
| `app.injected` | Hilt has built what the Application injects |
| `storage.ready` | boot-time restore and reset checks are done; storage may open |
| `prefs.loaded` | the settings file is in memory (the Application starts this read) |
| `activity.create` | `MainActivity.onCreate` begins |
| `prefs.preloaded` | the settings were already in memory, so the first screen is composed inside `onCreate`; absent when the Activity had to wait for them |
| `activity.storage` | the Activity has seen storage become ready |
| `db.open` | Room has opened the database |
| `prefs.read` | the settings needed before any content are in hand |
| `program.loaded` | the training program is loaded from the database |
| `nav.composed`, `hub.composed`, `home.composed` | the nav host, the hub pager and Home have each composed for the first time |
| `gate.known` | the first content composition has been applied |
| `home.body`, `home.coach`, `home.directive`, `home.state`, `home.slots` | each of Home's sources has answered |
| `home.complete` | all of them have, and the spec line; the splash is released on this |
| `content.frame` | Home is loaded and laid out; the splash may go |
| `launch.settled` | the splash hand-off has finished |

`system.displayed` is the first frame the system could show (the splash is held over it), and
`system.fullyDrawn` is Home reporting itself loaded. Both are counted from the launch intent, about
85 ms before the process exists on the S21 Ultra; the marks are counted from the process start.

A launch that never reaches `home.composed` was interrupted: a call arriving, the screen locking,
someone using the phone. The harness refuses to start while the phone is asleep, locked or in a
call, and such a run should be thrown away rather than averaged in.

## What a person sees

The marks stop at "the frame is ready". What reaches the eye is later: the splash fades, and
anything that animates in is still moving. To time that, record the screen and read the frames:

```sh
adb shell screenrecord --time-limit 6 /sdcard/launch.mp4 &   # then launch the app
```

`screenrecord` writes a frame only when the screen changes, so the first changed frame is the tap
landing and the last frame that changes the picture visibly is it coming to rest. Faint changes
long after that (a scrollbar fading, a badge) are not the launch.
