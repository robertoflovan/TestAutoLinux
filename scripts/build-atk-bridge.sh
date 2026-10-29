#!/usr/bin/env bash
# CI-only build. Never run on the customer's POS machine.
set -euo pipefail
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_vendor="$task_root/patches/suse-0.33.2"
task_build="$task_root/target/atk-build"
task_delivery="$task_build/delivery"
[[ ! -e "$task_build" ]] || { echo 'Build directory already exists; use a fresh checkout.' >&2; exit 1; }
mkdir -p "$task_delivery" "$task_build/original"
cd "$task_vendor"
echo 'b8f685caed7c424babac8b158f51355c772d1e2a0b8a1ce8ced4980edae223e2  java-atk-wrapper-0.33.2.tar.xz' | sha256sum -c -
tar -xJf java-atk-wrapper-0.33.2.tar.xz -C "$task_build"
cd "$task_build/java-atk-wrapper-0.33.2"
for task_patch in jaw-avoid-version.patch jaw-java_required.patch jaw-quotes.patch jaw-gdk.patch jaw-javah.patch; do
    patch --batch --fuzz=0 -p1 < "$task_vendor/$task_patch"
done
cp "$task_vendor/autogen.sh" .
cp "$task_vendor/manifest.txt" wrapper/
# Force regeneration of the template, just as in the openSUSE recipe.
if [[ -f wrapper/org/GNOME/Accessibility/AtkWrapper.java ]]; then
    mv wrapper/org/GNOME/Accessibility/AtkWrapper.java AtkWrapper.java.distribution-original
fi
NOCONFIGURE=1 bash autogen.sh
./configure --with-jdk-auto-detect=no JDK_SRC="$JAVA_HOME" JAVA_HOME="$JAVA_HOME" JAVACFLAGS='-source 1.7 -target 1.7' CFLAGS='-O2 -g -fcommon' --prefix="$task_build/unused-prefix"
make -j2
cp jni/src/.libs/libatk-wrapper.so "$task_build/original/"
cp wrapper/java-atk-wrapper.jar "$task_build/original/"
patch --batch --fuzz=0 -p1 < "$task_root/patches/java-atk-wrapper-0.33.2-listener-key.patch"
make -j2
cp jni/src/.libs/libatk-wrapper.so wrapper/java-atk-wrapper.jar COPYING.LESSER AUTHORS "$task_delivery/"
# Deliver exact modified sources and recipe for the LGPL library.
tar -cJf "$task_delivery/modified-sources.tar.xz" --exclude='.libs' --exclude='*.o' --exclude='*.lo' --exclude='*.class' --exclude='*.jar' --exclude='*.so' .
cp "$task_root/scripts/build-atk-bridge.sh" "$task_delivery/"
cp "$task_root/patches/java-atk-wrapper-0.33.2-listener-key.patch" "$task_delivery/"
{
    echo 'EXPERIMENTAL: build on Ubuntu 22.04 x86_64; not yet validated on customer SLES / Oracle 8u191.'
    echo 'Sources: openSUSE Leap 15.6 java-atk-wrapper rev d5ff2350023b5086f6d3e47644482452, plus listener-key fix.'
    git -C "$task_root" rev-parse HEAD
    java -version 2>&1
    gcc --version | head -n1
    pkg-config --modversion atk atk-bridge-2.0 glib-2.0 gdk-2.0
    readelf -d "$task_delivery/libatk-wrapper.so" | grep NEEDED
    readelf --version-info "$task_delivery/libatk-wrapper.so" | grep -oE 'GLIBC_[0-9.]+' | sort -Vu
} > "$task_delivery/BUILD-INFO.txt"
cd "$task_delivery"
sha256sum libatk-wrapper.so java-atk-wrapper.jar modified-sources.tar.xz > SHA256SUMS
