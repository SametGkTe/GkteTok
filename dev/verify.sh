#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODULE="$ROOT/module-baseline"
CACHE="${HOME}/.cache/ttplus-verify"
BUILD="${CACHE}/build"
mkdir -p "$CACHE" "$BUILD/main" "$BUILD/test"

say() { printf "\n\033[1m%s\033[0m\n" "$*"; }

# -- 1) JDK 17 (AGP 8.12 and libxposed API 102 both need Java 17) --
JDK="${HOME}/.cache/jdk17"
if [ ! -x "$JDK/bin/javac" ]; then
    if command -v javac >/dev/null 2>&1 && javac -version 2>&1 | grep -q " 17\."; then
        JDK="$(cd "$(dirname "$(command -v javac)")/.." && pwd)"
    else
        say "Downloading JDK 17..."
        curl -sL -o "$CACHE/jdk17.tar.gz" \
            "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
        mkdir -p "$JDK"
        tar xzf "$CACHE/jdk17.tar.gz" -C "$JDK" --strip-components=1
        rm -f "$CACHE/jdk17.tar.gz"
    fi
fi
JAVAC="$JDK/bin/javac"; JAVA="$JDK/bin/java"
say "JDK: $("$JAVAC" -version 2>&1)"

# -- 2) android.jar (API 36) --
ANDROID_JAR="$CACHE/android-36/android.jar"
if [ ! -f "$ANDROID_JAR" ]; then
    say "Downloading android.jar (API 36)..."
    curl -sL -o "$CACHE/platform36.zip" "https://dl.google.com/android/repository/platform-36_r02.zip"
    unzip -o -q "$CACHE/platform36.zip" -d "$CACHE/platform" "android-36/android.jar"
    mkdir -p "$(dirname "$ANDROID_JAR")"
    mv "$CACHE/platform/android-36/android.jar" "$ANDROID_JAR"
    rm -rf "$CACHE/platform" "$CACHE/platform36.zip"
fi

# -- 3) libxposed API 102 (classes.jar from the .aar) + JUnit --
LX="$CACHE/libxposed-classes.jar"
if [ ! -f "$LX" ]; then
    say "Downloading libxposed API 102..."
    curl -sL -o "$CACHE/api.aar" "https://repo1.maven.org/maven2/io/github/libxposed/api/102.0.0/api-102.0.0.aar"
    unzip -o -q "$CACHE/api.aar" -d "$CACHE/api_aar" classes.jar
    cp "$CACHE/api_aar/classes.jar" "$LX"; rm -rf "$CACHE/api.aar" "$CACHE/api_aar"
fi
for j in junit-4.13.2 hamcrest-core-1.3; do
    if [ ! -f "$CACHE/$j.jar" ]; then
        art="${j%%-*}"
        case "$j" in
            junit*)    url="https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar" ;;
            hamcrest*) url="https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar" ;;
        esac
        curl -sL -o "$CACHE/$j.jar" "$url"
    fi
done

mkdir -p "$CACHE/stub/com/golda/patchertiktok"
cat > "$CACHE/stub/com/golda/patchertiktok/BuildConfig.java" << 'EOF'
package com.golda.patchertiktok;
public final class BuildConfig {
    public static final boolean DEBUG = true;
    public static final String APPLICATION_ID = "com.golda.patchertiktok";
    public static final String BUILD_TYPE = "debug";
    public static final int VERSION_CODE = 52;
    public static final String VERSION_NAME = "GkteTok-1.6";
}
EOF

CP="$ANDROID_JAR:$MODULE/app/libs/xposed-api-82.jar:$LX:$CACHE/junit-4.13.2.jar:$CACHE/hamcrest-core-1.3.jar"
MAIN="$MODULE/app/src/main/java"; TEST="$MODULE/app/src/test/java"
rm -rf "$BUILD"; mkdir -p "$BUILD/main" "$BUILD/test"

# -- 5) Compile --
say "1/3  Compiling main ($(find "$MAIN" -name '*.java' | wc -l) .java)..."
"$JAVAC" -encoding UTF-8 -nowarn -cp "$CP" -d "$BUILD/main" \
    $(find "$MAIN" -name '*.java') "$CACHE/stub/com/golda/patchertiktok/BuildConfig.java"
echo "     ✓ 0 errors"

say "2/3  Compiling tests ($(find "$TEST" -name '*.java' | wc -l) .java)..."
"$JAVAC" -encoding UTF-8 -nowarn -cp "$CP:$BUILD/main" -d "$BUILD/test" $(find "$TEST" -name '*.java')
echo "     ✓ 0 errors"

# -- 6) Tests --
say "3/3  Running unit tests..."
CLASSES=$(cd "$BUILD/test" && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g' | sort)
"$JAVA" -cp "$CP:$BUILD/main:$BUILD/test" org.junit.runner.JUnitCore $CLASSES

say "RESULT: module compiles, all tests pass ✓"
echo "     (Windows: gradlew.bat testDebugUnitTest assembleDebug)"