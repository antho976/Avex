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
| `activity.create` | `MainActivity.onCreate` begins |
| `activity.storage` | the Activity has seen storage become ready |
| `prefs.read` | the settings needed before any content have been read |
| `gate.known` | the app knows whether it is showing Home or onboarding |
| `home.composed` | Home has composed for the first time |
| `content.frame` | Home's first frame is laid out; the splash may go |
| `home.state`, `home.directive`, `home.coach`, `home.slots`, `home.body` | each of Home's sources has answered |
| `home.complete` | all of them have; this is also when `Fully drawn` is reported |
| `launch.settled` | the splash hand-off has finished |

## What a person sees

The marks stop at "the frame is ready". What reaches the eye is later: the splash fades, and
anything that animates in is still moving. To time that, record the screen and read the frames:

```sh
adb shell screenrecord --time-limit 6 /sdcard/launch.mp4 &   # then launch the app
```

`screenrecord` writes a frame only when the screen changes, so the first changed frame is the tap
landing and the last one is the picture coming to rest.
