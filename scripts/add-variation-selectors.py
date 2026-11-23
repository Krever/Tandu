"""Make a monochrome emoji font accept the emoji variation selector.

Usage: add-variation-selectors.py <font.ttf> <text-file>

Many emoji in the sources are a base character plus U+FE0F ("show as
emoji"). The Noto mono font has the base characters, but it does not
declare the sequences with U+FE0F. A browser then ignores the font for
those sequences and shows the colour emoji of the device.

This script adds a cmap subtable (format 14) that declares each such
sequence. Each sequence points to the glyph of its base character.
"""

import sys

from fontTools.ttLib import TTFont
from fontTools.ttLib.tables._c_m_a_p import CmapSubtable

VS16 = 0xFE0F

font_path, text_path = sys.argv[1], sys.argv[2]
with open(text_path, encoding="utf-8") as f:
    text = f.read()

font = TTFont(font_path)
known = font.getBestCmap()
bases = sorted(
    {ord(a) for a, b in zip(text, text[1:]) if ord(b) == VS16 and ord(a) in known}
)

subtable = CmapSubtable.newSubtable(14)
subtable.platformID = 0
subtable.platEncID = 5
subtable.language = 0xFF
subtable.cmap = {}
# `None` as the glyph name means "use the glyph of the base character".
subtable.uvsDict = {VS16: [(base, None) for base in bases]}

cmap = font["cmap"]
cmap.tables = [t for t in cmap.tables if t.format != 14] + [subtable]
font.save(font_path)
print(f"Declared {len(bases)} sequences with U+FE0F.")
