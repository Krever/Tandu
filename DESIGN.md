# Tandu — Design

A tiny app for picking an activity to do with the kids. Tap a button, get a
suggestion, optionally play it in-app.

Status: prototype. Family use first, public release possibly later.

## Goals

- Suggest an activity for the kids on demand.
- Each activity has its own first-class implementation — no forced
  generalization.
- Cheap to prototype, cheap to extend with one more activity.
- Mobile-first; installable on a phone (PWA).

## Non-goals (for now)

- Accounts, auth, multi-user state.
- Age-based filtering of suggestions (future addition).
- Content management UI.
- Cross-device sync.
- Recommender / personalization.
- iOS-specific polish. Target is Android. If it happens to work on iOS,
  great; we don't design around its quirks.

## Languages

Polish and English are first-class from day one. Every user-visible string —
UI chrome, activity descriptions, hangman word bank, 20-questions prompts,
custom-activity form labels — must be available in both.

- Language is user-selectable; default from the device locale, fallback to
  English.
- Selection is persisted across sessions.

## Users

- **Parent** (primary): opens the app, gets a suggestion, optionally taps
  through to play.
- **Kid**: may operate the app during a game (tic-tac-toe board, memory
  cards). UI must be touch-friendly and forgiving.

## Core user flow

1. Open app on phone.
2. Tap *Suggest activity*.
3. App picks one at random.
4. Activity screen shows:
   - Name, short description, what you need (paper, board, just hands…).
   - One of: **Play in app**, **Print board**, **Open external site**, or
     just instructions to play offline.
5. Optional: *Suggest another* to reroll. Reroll should not immediately
   repeat the previous suggestion unless there is nothing else to pick.

## Activities — first batch

Each activity has a **support level** that determines what the activity
screen offers:

| Activity          | Support level                                |
|-------------------|----------------------------------------------|
| Tic-tac-toe       | In-app, pass-and-play                        |
| Solitaire         | In-app                                       |
| Memory            | In-app, pass-and-play                        |
| Hangman           | In-app (built-in word bank)                  |
| Checkers          | In-app, pass-and-play                        |
| Battleships       | In-app *or* printable board                  |
| Chess             | Idea only + link to lichess/chesskid         |
| Word association  | Prompt (seed word from bank, optional timer) |
| 20 questions      | In-app guided (timer, question count)        |
| Story building    | Prompt (seed sentence/word, turn indicator)  |
| Last letter       | Prompt (random starting letter)              |
| Would you rather  | Prompt (random pair of options from bank)    |

Support levels:

- **idea-only** — title, description, "go play".
- **printable** — render a board/sheet to print or screenshot.
- **external** — deep link to another site/app.
- **prompt** — app supplies starting content (a word, a category, a
  seed sentence) and optionally a timer or turn cue; the game itself is
  played verbally. Good for hands-busy contexts like the car.
- **in-app** — fully playable inside Tandu.

A single activity can combine these (battleships = printable + in-app).


## Tags / contexts

Activities differ in what they require from the player — a flat surface,
a screen, two hands, the ability to look at the phone. We want to be
able to ask the app "what can we play *right now*, in *this* situation?"
without scrolling past everything that doesn't fit.

The concrete schema is not settled (single tag, multi-tag, structured
"requires" fields, etc.) — what matters for design is that each activity
carries enough metadata to be filtered by context. Likely dimensions:

- **Setting** — at a table, on the couch, in the car, outdoors, waiting
  in a queue.
- **Attention on phone** — required (memory, tic-tac-toe), helpful
  (20 questions, hangman), none (word association, story building).
- **Materials** — none, paper + pen, printed board, dice (covered by
  the in-app tool), cards.
- **Player count** — solo, two, two-plus.
- **Rough age band** — to be added later (see non-goals).

This unlocks features like a **"Car games"** entry point (filter:
setting=car, attention=none) that only suggests verbal/prompt-style
activities, and lets *Suggest activity* be scoped by context instead of
picking blindly from the whole catalog.

### Other car-friendly activities worth adding

Not committed to building, but the obvious candidates once tags exist:

- **Riddles** — app shows a riddle, players guess; tap to reveal.

## Tools

Standalone utilities, reachable from the home screen, that aren't activities
in themselves but help play almost any offline game.

- **Virtual dice** — tap to roll. Configurable number of dice (1–5) and
  sides (d4, d6, d8, d10, d12, d20; default d6). Big result display,
  satisfying animation, history of the last few rolls. Useful for any board
  game where the physical dice are lost.
- **Score tracker** — ad-hoc scoreboard for any game. Add 2–6 players with
  names, +/- buttons per player, running totals, undo, reset. Optional
  per-round log. Must survive the phone locking mid-game.
- **Custom activities** — parent can add their own activity entries via a
  simple form: title, short description, optional "what you need" text.
  Surfaced in *Suggest activity* alongside built-ins. Support level is
  fixed to **idea-only** (no custom gameplay). Edit / delete from a
  "My activities" screen.

## Design principles

- **No premature abstraction over activities.** Each activity is its own
  thing with its own UI and rules. The shared layer is intentionally thin.
- **Frontend-only until proven otherwise.** No server, no accounts.
- **Public-later, not public-never.** Avoid choices that lock out a future
  backend, but don't build for it today.
- **Touch-first.** All interactive activities must work on a small kid's
  finger on a phone screen.
- **Visual uniformity.** Activities share a single design system: a small
  set of design tokens (colors, spacing, typography, motion) and a shared
  component library (buttons, cards, screen shell, result displays).
  Activity-specific styling is limited to board/grid geometry. New visual
  primitives are added to the shared library, not invented per activity.

## Tech stack

Settled, not up for debate at this stage:

- **Scala 3** compiled to **Scala.js**.
- **Laminar** for UI, **Waypoint** for routing.
- **Vite** for dev server / bundling, **vite-plugin-pwa** for manifest +
  service worker.
- **scalatest** for logic, **Selenium** for e2e.
- If a backend ever appears, it will be Scala sharing models via a
  `shared/` cross-project.
