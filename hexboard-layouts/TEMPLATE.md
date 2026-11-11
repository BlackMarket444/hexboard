# Writing your own hex layout in the in-app editor

A layout made in Developer Settings > Custom layouts is drawn with hexagons when its `name:` starts
with "Hex". Every row must be offset by half a key from the rows next to it, or the hexagons overlap.

Starting point (10 keys, then 9, then 10, then a 9-cell bottom row):

```yaml
name: Hex My Layout
numberRowMode: AlwaysDisabled
rowHeightMode: FillHeight
bottomRowHeightMode: Flexible
bottomRowWidthMode: Identical
supportsSplit: false
minimumFunctionalKeyWidth: 0.1
minimumBottomRowFunctionalKeyWidth: 0.1
overrideWidths: { Custom1: 0.1, Custom2: 0.4 }
rows:
  - letters: "q w e r t y u i o p"
  - letters: "a s d f g h j k l"
  - letters: "$shift z x c v b n m & $delete"
  - bottom:
      - { type: symbols, attributes: { width: Custom1 } }
      - { type: contextual, attributes: { width: Custom1 }, fallbackKey: { type: base, spec: ",", attributes: { width: Custom1 } } }
      - { type: action, attributes: { width: Custom1 }, fallbackKey: { type: gap, attributes: { width: Custom1 } } }
      - { type: space, attributes: { width: Custom2 } }
      - { type: base, spec: ".", attributes: { width: Custom1 } }
      - { type: enter, attributes: { width: Custom1 } }
```

Rules that keep the stagger intact:
- Row 1 has 10 keys, row 2 has 9, row 3 has 10 counting shift and delete (so 8 letters between them).
- Change which letters go where freely; keep those counts.
