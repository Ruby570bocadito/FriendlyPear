"""
Checks the release APK before it's handed out, and fails on the first thing wrong:

- the shared test token (friendlypear.deviceToken in ~/.gradle/gradle.properties) isn't inside: a release build only
  lets in phones paired in person (never prints the token);
- there is no log: R8 drops every Log.v/d/i/w, and with them what Peri does (a call, a shield, the family's state);
- the report copied for the police is marked sensitive, so Android keeps it out of its clipboard preview;
- the screens a scammer would want stay out of captures and screen sharing (SECURE_SCREENS).

    cd android && ./gradlew :app:assembleRelease && cd .. && python eval/release_check.py
"""
import os
import re
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "android", "app", "build", "outputs", "apk", "release")
# Signed with our key when ~/.gradle/gradle.properties says where it is; unsigned otherwise.
APK = next((os.path.join(OUT, n) for n in ("app-release.apk", "app-release-unsigned.apk") if os.path.exists(os.path.join(OUT, n))), None)
CONFIG = os.path.join(HERE, "..", "android", "app", "build", "generated", "source", "buildConfig", "release", "app", "friendlypear", "BuildConfig.java")
# Lines Peri logs in debug builds: none may be left in the release one.
LOGGED = [b"listener connected", b"shield earned kind=", b"call active=", b"pear published="]

if APK is None:
    sys.exit("no release APK: build it first")
apk = zipfile.ZipFile(APK)
dex = b"".join(apk.read(n) for n in apk.namelist() if n.endswith(".dex"))

props = open(os.path.expanduser("~/.gradle/gradle.properties"), encoding="utf-8").read()
found = re.search(r"^friendlypear\.deviceToken=(.+)$", props, re.M)
if found and found.group(1).strip().encode() in dex:
    sys.exit("FAIL: the release APK carries the shared test token")
left = [line.decode() for line in LOGGED if line in dex]
if left:
    sys.exit(f"FAIL: the release APK still logs: {left}")
if b"android.content.extra.IS_SENSITIVE" not in dex:
    sys.exit("FAIL: the report copied for the police isn't marked sensitive")
if "SECURE_SCREENS = true" not in open(CONFIG, encoding="utf-8").read():
    sys.exit("FAIL: the release build lets its sensitive screens be captured")
print(f"ok: {os.path.basename(APK)} has no shared token, no log, a sensitive report and screens out of captures")
