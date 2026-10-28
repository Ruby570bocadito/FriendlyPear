#!/usr/bin/env bash
# share.sh <serial> <prefix> <seconds> <poster>...: shares each trap poster (eval/data/qr, made by posters.py) with
# "Look at this picture" on a virtual phone, screenshots Peri's answer into eval/data/qr/shots and closes the screen.
# With the phone online each poster is one Muse Spark call; offline (svc wifi disable; svc data disable) none, and
# the QR verdict must stand on its own. Run from the repo root; debug builds only (run-as).
export MSYS_NO_PATHCONV=1
ADB="${ADB:-adb}"
S=$1; P=$2; W=$3; shift 3
DIR="eval/data/qr"
mkdir -p "$DIR/shots"
for f in "$@"; do
  $ADB -s $S push "$DIR/$f.png" "/data/local/tmp/$f.png" > /dev/null
  $ADB -s $S shell "cat /data/local/tmp/$f.png | run-as app.friendlypear sh -c 'mkdir -p files/qrtest && cat > files/qrtest/$f.png'"
  $ADB -s $S logcat -c
  $ADB -s $S shell "am start -W -a android.intent.action.SEND -t image/png --eu android.intent.extra.STREAM file:///data/data/app.friendlypear/files/qrtest/$f.png -n app.friendlypear/.ui.peri.PictureActivity" > /dev/null
  sleep "$W"
  $ADB -s $S exec-out screencap -p > "$DIR/shots/${P}_$f.png"
  echo "$f: $($ADB -s $S logcat -d -s FriendlyPear:I | grep -o 'picture verdict.*' | tail -1)"
  $ADB -s $S shell input keyevent KEYCODE_BACK
  sleep 1
done
