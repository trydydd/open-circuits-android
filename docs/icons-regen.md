Icon regen — ic_launcher_foreground.png
=======================================

Source: docs/logo.svg (Open Circuits mark)

The foreground PNGs live in:
  mipmap-mdpi/ic_launcher_foreground.png    (108 x 108 px)
  mipmap-hdpi/ic_launcher_foreground.png    (162 x 162 px)
  mipmap-xhdpi/ic_launcher_foreground.png   (216 x 216 px)
  mipmap-xxhdpi/ic_launcher_foreground.png  (324 x 324 px)
  mipmap-xxxhdpi/ic_launcher_foreground.png (432 x 432 px)

Preferred command (requires rsvg-convert from librsvg):

  for d in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
    density="${d%%:*}"
    size="${d##*:}"
    rsvg-convert -w "$size" -h "$size" docs/logo.svg \
      -o "app/src/main/res/mipmap-${density}/ic_launcher_foreground.png"
  done

Fallback command (requires ImageMagick >= 7):

  for d in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
    density="${d%%:*}"
    size="${d##*:}"
    magick docs/logo.svg -resize "${size}x${size}" \
      "app/src/main/res/mipmap-${density}/ic_launcher_foreground.png"
  done

Notes:
- The adaptive icon canvas is 108 dp x 108 dp; the safe zone for content
  is the central 72 dp x 72 dp. The logo mark sits within the safe zone.
- The vector variant (drawable/ic_launcher_foreground.xml) is used by the
  adaptive icon (mipmap-anydpi-v26/ic_launcher.xml) and the splash screen
  theme. Keep the vector and PNGs visually identical.
- Background colour is @color/brand_cream (#F6F3EB) defined in
  drawable/ic_launcher_background.xml.
