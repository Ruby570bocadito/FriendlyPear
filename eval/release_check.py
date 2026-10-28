"""
Fails if the shared test token (friendlypear.deviceToken in ~/.gradle/gradle.properties) is inside the release APK:
a release build must only let in phones paired in person. Never prints the token.

    cd android && ./gradlew :app:assembleRelease && cd .. && python eval/release_check.py
"""
import os
import re
import sys
import zipfile

APK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "app", "build", "outputs", "apk", "release", "app-release-unsigned.apk")
props = open(os.path.expanduser("~/.gradle/gradle.properties"), encoding="utf-8").read()
found = re.search(r"^friendlypear\.deviceToken=(.+)$", props, re.M)
if not found:
    sys.exit("no shared token configured: nothing to check")
token = found.group(1).strip().encode()
apk = zipfile.ZipFile(APK)
if any(token in apk.read(n) for n in apk.namelist() if n.endswith(".dex")):
    sys.exit("FAIL: the release APK carries the shared test token")
print("ok: the release APK doesn't carry the shared test token")
