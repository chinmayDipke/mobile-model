# Shan — I pulled everything. Here is what changed on my side.

Reply to your `CHINMAY-READ-NOW.md`. Short on purpose.

---

## Your work is now the base. I threw mine away where it overlapped.

We had not actually diverged — you were 8 commits ahead of me and I was 0 ahead
of you. So this was a fast-forward, not a merge. Nothing of yours was touched.

**Read and understood:**

- `handleMessageAsync`, never `handleMessage`. Got it. The QR code will use it.
- Don't touch `GemmaDetector.kt`. Not touching it.
- `StubDetector` stays as the fallback. Agreed — it is why we can demo at all if
  the model dies on stage.
- `DetectionService` for anything not in a foreground activity.

**Your three fixes were the right calls.** The threading one especially — a real
SMS would have ANR'd in front of a judge and we would never have known why.

`HybridDetector` is the best idea either of us has had this weekend. "Rules
catch what we have seen, the model catches what we haven't, and the model can
only escalate a message that actually asks you to act" is the line I will say in
the pitch.

---

## Three things I added on top

### 1. Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)

The repo had no wrapper, so it built only on a machine with the right Gradle
already installed. Mine did not have it, and I lost an hour to it.

Pinned to **Gradle 8.13**, which is what AGP 8.13.0 needs. **Gradle 9.x breaks
the build** — it removed an internal API that AGP 8.13 calls.

You do not have to change how you build. If `gradle assembleDebug` works for
you, keep using it. But `.\gradlew.bat assembleDebug` now works on a fresh
clone, which it did not before.

One trap that will bite you if you build in Android Studio Quail: **it ships
JBR 25, and Gradle 8.13 rejects JDK 25.** The error is just the text `25.0.3`,
which tells you nothing. Point `JAVA_HOME` at JDK 21.

### 2. Engine load time on screen

Your background `initDetector` call was right — I only added a timer around it.
The status box now prints `Engine ready in <n> ms` under the engine name.

Small, but it is the answer to "is the model really on the phone?" A number
beats a claim, same reasoning as your `EXTRA_ENGINE` point.

### 3. `Locale("en", "IN")` → `Locale.forLanguageTag("en-IN")` in the TTS

That constructor is deprecated. Same behaviour, no warning.

---

## `SETUP.md` was lying and I rewrote it

It still said "make a new Android Studio project and copy the files into
`app/src/main`". That was true before your `sourceSets` change and is actively
wrong now. Anyone following it would have built a second, stale copy of the app.

It is now the single build truth: wrapper, the JDK 21 trap, the Office Kit
install path, the model path, and a table of who owns which file.

I also marked `Team-Handover.md` as **historical** at the top of that table. Its
setup steps predate the Gradle project. The event plan and timeline in it are
still good — just not the setup half.

---

## Still mine to do, unchanged from your list

1. **Camera → QR warning.** The 15% gap. Will use `handleMessageAsync`, will
   reuse `BlockActivity` with `pattern = "qr_send_only"`, and I will add the
   reason string to your `REASONS` map rather than generating it.
2. **Show `EXTRA_ENGINE` and "No network used" on the block screen.**
3. Voice is already done — `BlockActivity` speaks `verdict.reason`. Adding a
   mute toggle for a loud judging room.

## One open question for you

`HybridDetector`'s header says **55/55**. `TestSetRunner` on my build should
print `40/40` and `15/15`. Same thing, said two ways — but if a judge sees both
numbers they will ask. **Which one do we say on stage?** I would say
"40 of 40 scams caught, 15 of 15 genuine messages passed", because the second
half is the part that actually impresses.

Also still open from your list: **slide 10 claims the test set is real messages
from our phones, and it is synthetic.** I will reword the slide — cheaper than
inventing data, and it is a claim a judge could catch.
