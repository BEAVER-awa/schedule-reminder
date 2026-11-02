#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOLS="${ANDROID_TOOLS:-$(dirname "$ROOT")/tools}"
BT=$TOOLS/android-14
AJAR=$TOOLS/android-35/android.jar
export LD_LIBRARY_PATH=$BT/lib64
cd "$ROOT"

rm -rf build && mkdir -p build/gen build/classes build/dex

echo "[1/6] aapt2 compile"
"$BT/aapt2" compile --dir res -o build/res.zip

echo "[2/6] aapt2 link"
"$BT/aapt2" link -o build/base.apk -I "$AJAR" --manifest AndroidManifest.xml \
  -R build/res.zip --java build/gen --min-sdk-version 24 --target-sdk-version 34 \
  -A "$ROOT/assets" --auto-add-overlay

echo "[3/6] javac"
find src -name "*.java" > build/sources.txt
find build/gen -name "*.java" >> build/sources.txt
javac --release 8 -encoding UTF-8 -cp "$AJAR" -d build/classes @build/sources.txt

echo "[4/6] d8"
find build/classes -name "*.class" > build/classes.txt
java -cp "$TOOLS/r8.jar" com.android.tools.r8.D8 --min-api 24 --lib "$AJAR" --output build/dex @build/classes.txt

echo "[5/6] package"
cp build/base.apk build/app.apk
(cd build && zip -q -j app.apk dex/classes*.dex)
"$BT/zipalign" -f -p 4 build/app.apk build/app-aligned.apk

echo "[6/6] sign"
# 签名口令不写死在脚本里：构建前 export KS_PASS=你的口令
KS_PASS="${KS_PASS:-}"
if [ -z "$KS_PASS" ]; then
  echo "缺少签名口令：请先执行 export KS_PASS=<你的口令>"
  exit 1
fi
KS=$ROOT/schedule.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias schedule -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$KS_PASS" -keypass "$KS_PASS" -dname "CN=Moshang Schedule, O=Schedule, C=CN" \
    -J-Djava.security.egd=file:/dev/./urandom
fi
# 在内存盘(tmpfs)完成签名，规避工作区盘偶发 I/O 抖动
rm -f /dev/shm/app.apk /dev/shm/app-signed.apk
cp build/app-aligned.apk /dev/shm/app.apk
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --out /dev/shm/app-signed.apk /dev/shm/app.apk
cp /dev/shm/app-signed.apk build/app-signed.apk
"$BT/apksigner" verify -v build/app-signed.apk
echo "DONE -> $ROOT/build/app-signed.apk"
ls -la build/app-signed.apk

