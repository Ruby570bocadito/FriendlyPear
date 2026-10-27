#!/bin/bash
# vrec.sh start|stop <serial> <name>: Android's own screen recorder on an emulator (video only, up to 3 min).
# start runs it in the background; stop ends it cleanly (SIGINT finishes the MP4) and pulls $OUT/<name>.mp4 (default dist/demo/raw, ignored by git).
export MSYS_NO_PATHCONV=1
ADB="${ADB:-adb}"
OUT="${OUT:-dist/demo/raw}"
mkdir -p "$OUT"
case "$1" in
  start)
    $ADB -s "$2" shell rm -f "/data/local/tmp/$3.mp4"
    ($ADB -s "$2" shell screenrecord --bit-rate 8000000 --time-limit 180 "/data/local/tmp/$3.mp4" > /dev/null 2>&1 &)
    sleep 1
    echo "recording $3 on $2"
    ;;
  stop)
    $ADB -s "$2" shell pkill -2 screenrecord
    sleep 3
    $ADB -s "$2" pull "/data/local/tmp/$3.mp4" "$OUT/$3.mp4" > /dev/null && echo "saved $OUT/$3.mp4 ($(du -k "$OUT/$3.mp4" | cut -f1) KB)"
    ;;
esac
