# Tandu

A tiny app for picking an activity to do with the kids. Comes with a curated list of activities and provides support for
both online and offline play.

## Develop

```sh
npm install
npm run dev
```

## Pictures

The content pictures are emoji that our own two fonts draw: a vector colour
font (Noto Color Emoji) and a mono font (Noto Emoji) for black-and-white
print. The fonts in `fonts/` contain only the emoji that the sources use.

After you add a new emoji to the sources, build the fonts again (this needs
[`uv`](https://docs.astral.sh/uv/)):

```sh
npm run fonts
```

`npm run build` fails if the fonts are out of date.

## Build

```sh
npm run build
```
