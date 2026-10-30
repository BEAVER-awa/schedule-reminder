#!/usr/bin/env bash
# 行程提醒 —— 混淆构建（R8：shrink + obfuscate）
# 与 build.sh 唯一区别：[4/6] 用 R8 代替 D8，产出混淆后的 dex + mapping.txt
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOLS="${ANDROID_TOOLS:-$(dirname "$ROOT")/tools}"
BT=$TOOLS/android-14
AJAR=$TOOLS/android-35/android.jar
export LD_LIBRARY_PATH=$BT/lib64
cd "$ROOT"

rm -rf build-obf && mkdir -p build-obf/gen build-obf/classes build-obf/dex

echo "[1/6] aapt2 compile"
"$BT/aapt2" compile --dir res -o build-obf/res.zip

echo "[2/6] aapt2 link"
"$BT/aapt2" link -o build-obf/base.apk -I "$AJAR" --manifest AndroidManifest.xml \
  -R build-obf/res.zip --java build-obf/gen --min-sdk-version 24 --target-sdk-version 34 \
  -A "$ROOT/assets" --auto-add-overlay --version-code 100 --version-name 9.29-vivo-obf

echo "[3/6] javac"
find src -name "*.java" > build-obf/sources.txt
find build-obf/gen -name "*.java" >> build-obf/sources.txt
javac --release 8 -encoding UTF-8 -cp "$AJAR" -d build-obf/classes @build-obf/sources.txt

echo "[4/6] R8 (shrink + obfuscate)"
find build-obf/classes -name "*.class" > build-obf/classes.txt
java -cp "$TOOLS/r8.jar" com.android.tools.r8.R8 \
  --release --min-api 24 --lib "$AJAR" \
  --pg-conf proguard-rules.pro \
  --pg-map-output build-obf/mapping.txt \
  --output build-obf/dex @build-obf/classes.txt

echo "[5/6] package"
cp build-obf/base.apk build-obf/app.apk
(cd build-obf && zip -q -j app.apk dex/classes*.dex)
"$BT/zipalign" -f -p 4 build-obf/app.apk build-obf/app-aligned.apk

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
rm -f /dev/shm/app-obf.apk /dev/shm/app-obf-signed.apk
cp build-obf/app-aligned.apk /dev/shm/app-obf.apk
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --out /dev/shm/app-obf-signed.apk /dev/shm/app-obf.apk
cp /dev/shm/app-obf-signed.apk build-obf/app-signed.apk
"$BT/apksigner" verify -v build-obf/app-signed.apk
echo "DONE -> $ROOT/build-obf/app-signed.apk  (mapping: build-obf/mapping.txt)"
ls -la build-obf/app-signed.apk

