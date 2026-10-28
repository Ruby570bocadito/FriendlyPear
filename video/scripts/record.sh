#!/bin/bash
# record.sh <moment> [poster.png], from the repo root: records one real moment of the app on Mamá's virtual phone for
# the ad, with Android's own screen recorder (video only), into video/public/clips/ (ignored by git).
#   newnumber          a "Hi Mum, new number" text arrives; Peri warns and, on "yes", calls the real Ana
#   qrtrap <poster>    a poster with a trap QR code is shared with "Look at this picture" (one Muse Spark call)
# Voice orders go in through the debug broadcast, since the emulator has no microphone.
# Smooth clips need a quiet computer: pause the other virtual phones first (adb -s <serial> emu avd stop, and
# avd start after). With four running, a third of the frames came out late and the app looked laggy.
export MSYS_NO_PATHCONV=1
ADB="${ADB:-adb}"
MUM="$ADB -s ${MUM_SERIAL:-emulator-5556}"
ANA="$ADB -s ${ANA_SERIAL:-emulator-5554}"
OUT="video/public/clips"
# Enough for the phone in the video, and lighter for the emulator to record than its full 1080x2400.
SIZE="${SIZE:-864x1920}"
mkdir -p "$OUT"

start() {
  $MUM shell rm -f "/data/local/tmp/$1.mp4"
  ($MUM shell screenrecord --size $SIZE --bit-rate 8000000 --time-limit 90 "/data/local/tmp/$1.mp4" > /dev/null 2>&1 &)
  sleep 1
}
stop() {
  $MUM shell pkill -2 screenrecord
  sleep 3
  $MUM pull "/data/local/tmp/$1.mp4" "$OUT/$1.mp4" > /dev/null && echo "saved $OUT/$1.mp4"
}
hangup() {
  for d in "$MUM" "$ANA"; do
    for i in 1 2 3; do
      # Ringing (1) or on a call (2); a paused phone never answers, so it's left alone after a few seconds.
      state="$(timeout 5 $d shell dumpsys telephony.registry | grep -m1 mCallState | tr -dc '0-9')"
      [ "$state" = "1" ] || [ "$state" = "2" ] || break
      timeout 5 $d shell input keyevent KEYCODE_ENDCALL
      sleep 1
    done
  done
}

$MUM shell input keyevent KEYCODE_WAKEUP
case "$1" in
  newnumber)
    $MUM shell am start -n app.friendlypear/.ui.peri.PeriActivity > /dev/null
    sleep 5
    start newnumber
    sleep 2
    $MUM emu sms send 447700900123 "Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today for a bill? I'll pay you back tomorrow x" > /dev/null
    sleep 16
    $MUM shell "am broadcast -a app.friendlypear.debug.HEAR --es text 'yes'" > /dev/null
    sleep 9
    hangup
    sleep 2
    stop newnumber
    ;;
  qrtrap)
    $MUM push "$2" /data/local/tmp/poster.png > /dev/null
    $MUM shell "cat /data/local/tmp/poster.png | run-as app.friendlypear sh -c 'cat > files/poster.png'"
    $MUM shell am start -n app.friendlypear/.ui.peri.PeriActivity > /dev/null
    sleep 3
    start qrtrap
    sleep 2
    $MUM shell "am start -a android.intent.action.SEND -t image/png --eu android.intent.extra.STREAM file:///data/data/app.friendlypear/files/poster.png -n app.friendlypear/.ui.peri.PictureActivity" > /dev/null
    sleep 22
    stop qrtrap
    $MUM shell input keyevent KEYCODE_BACK
    ;;
  *)
    echo "usage: record.sh newnumber | qrtrap <poster.png>"
    exit 1
    ;;
esac
