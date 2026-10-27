#!/bin/bash
# moments.sh <name>, from the repo root (newnumber, call, code, picture, family, after, fallen): records one of the demo's moments on the emulators, for real (Peri, the shield, the calls).
# Voice orders go in through the debug broadcast, since the emulator has no microphone.
export MSYS_NO_PATHCONV=1
ADB="${ADB:-adb}"
M="$ADB -s emulator-5554"
A="$ADB -s emulator-5556"
S="$(cd "$(dirname "$0")" && pwd)"
REC="bash $S/vrec.sh"
log() { echo "$(date +%T) $*"; }
hear() { $M shell "am broadcast -a app.friendlypear.debug.HEAR --es text '$1'" > /dev/null; log "heard: $1"; }
hangup() {
  for d in "$M" "$A"; do
    for i in 1 2 3; do
      [ "$($d shell dumpsys telephony.registry | grep -m1 mCallState | tr -dc '0-9')" = "0" ] && break
      $d shell input keyevent KEYCODE_ENDCALL
      sleep 1
    done
  done
}
fresh() {
  hangup
  $M shell input keyevent KEYCODE_WAKEUP
  $M shell am force-stop app.friendlypear
  $M shell am start -n app.friendlypear/.ui.peri.PeriActivity > /dev/null
  sleep 7
}
ring() {
  $M emu gsm call "$1" > /dev/null
  for i in $(seq 1 20); do
    [ "$($M shell dumpsys telephony.registry | grep -m1 mCallState | tr -dc '0-9')" = "1" ] && break
    sleep 0.5
  done
}

case "$1" in
  newnumber)
    fresh
    $REC start emulator-5554 m1_newnumber
    sleep 3
    $M emu sms send 447700900123 "Hi Mum, it's Ana. My phone broke and this is my new number. Can you send me 380 today for a bill? I'll pay you back tomorrow x" > /dev/null
    log "sms"
    sleep 16
    hear "yes"
    sleep 9
    hangup
    sleep 2
    $REC stop emulator-5554 m1_newnumber
    ;;
  call)
    fresh
    $REC start emulator-5554 m2_call
    sleep 2
    ring 911000111
    log "ringing"
    sleep 7
    $M emu gsm accept 911000111 > /dev/null
    sleep 6
    $M emu gsm cancel 911000111 > /dev/null
    log "hung up"
    sleep 7
    hear "the bank"
    sleep 10
    hear "they wanted the code they texted me"
    sleep 12
    $REC stop emulator-5554 m2_call
    ;;
  code)
    fresh
    $REC start emulator-5554 m3_code
    sleep 2
    ring 911000222
    sleep 4
    $M emu gsm accept 911000222 > /dev/null
    log "on the call"
    sleep 5
    $M emu sms send 22395 "Your bank verification code is 482913. Never share it with anyone." > /dev/null
    log "code arrived during the call"
    sleep 14
    $M emu gsm cancel 911000222 > /dev/null
    sleep 4
    $REC stop emulator-5554 m3_code
    ;;
  picture)
    fresh
    $REC start emulator-5554 m4_picture
    sleep 2
    $M shell input tap 540 2022
    log "look at a picture"
    sleep 4
    $M shell input tap 179 1450
    sleep 2
    $M shell input tap 906 2196
    log "picked the parcel notice"
    sleep 30
    $REC stop emulator-5554 m4_picture
    ;;
  family)
    fresh
    $A shell input keyevent KEYCODE_WAKEUP
    $A shell cmd statusbar collapse
    $A shell am force-stop app.friendlypear
    $A shell am start -n app.friendlypear/.ui.peri.PeriActivity > /dev/null
    sleep 5
    $REC start emulator-5554 m6_family_mum
    $REC start emulator-5556 m6_family_ana
    sleep 2
    ring 911000333
    sleep 5
    $M emu gsm accept 911000333 > /dev/null
    sleep 6
    $M emu gsm cancel 911000333 > /dev/null
    log "stranger's call over"
    sleep 7
    hear "the bank"
    sleep 11
    hear "tell Marisa that the code is 7350"
    sleep 10
    # The emulators don't pass texts between them: Ana's phone receives the exact text Mum's phone sent.
    TEXT=$($M shell content query --uri content://sms/sent --projection body | head -1 | sed 's/^Row: 0 body=//' | tr -d '
')
    log "sent: ${TEXT:0:70}"
    $A emu sms send 5554 "$TEXT" > /dev/null
    sleep 9
    $A shell input tap 540 1352
    log "Ana taps call Mum now"
    sleep 15
    $REC stop emulator-5554 m6_family_mum
    $REC stop emulator-5556 m6_family_ana
    hangup
    ;;
  after)
    fresh
    $REC start emulator-5554 m7_after
    sleep 2
    $M emu sms send 447700900456 "Royal Mail: your parcel is on hold due to an unpaid shipping fee of 1.45 GBP. Pay today at https://royalmail-redeliver.top or it will be returned." > /dev/null
    log "parcel scam"
    sleep 14
    $M shell input tap 330 1554
    log "see the trap"
    sleep 4
    for i in 1 2 3; do $M shell input swipe 540 1800 540 700 900; sleep 3; done
    $M shell input keyevent BACK
    sleep 2
    $M shell input tap 810 1554
    log "report it"
    sleep 4
    $M shell input tap 540 880
    log "write it"
    sleep 22
    $M shell input swipe 540 1800 540 900 1200
    sleep 4
    $M shell input swipe 540 1800 540 900 1200
    sleep 4
    $REC stop emulator-5554 m7_after
    ;;
  fallen)
    fresh
    $REC start emulator-5554 m5_fallen
    sleep 3
    hear "I have fallen"
    sleep 16
    hangup
    sleep 2
    $REC stop emulator-5554 m5_fallen
    ;;
esac
log "done $1"
