#!/bin/bash

# Simplified ClimateChangeAgent Launch Script
# This script uses pre-compiled classes or compiles on demand

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
PROJECTDIR="$(cd "$BASEDIR/.." && pwd)"

# Prefer Java 8 runtime; fall back to system java if necessary
JAVA_CMD="${JAVA_CMD:-java}"
JAVAC_CMD="${JAVAC_CMD:-javac}"

detect_java_version() {
    "$1" -version 2>&1 | awk -F\" '/version/ {print $2}' | head -n1
}

select_java8() {
    local version
    if command -v "$JAVA_CMD" >/dev/null 2>&1; then
        version=$(detect_java_version "$JAVA_CMD")
    fi

    if [[ $version != 1.8.* && $version != 8.* ]]; then
        if command -v /usr/libexec/java_home >/dev/null 2>&1; then
            local java8_home
            java8_home=$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)
            if [ -n "$java8_home" ]; then
                JAVA_CMD="$java8_home/bin/java"
                JAVAC_CMD="$java8_home/bin/javac"
                version=$(detect_java_version "$JAVA_CMD")
            fi
        fi
    fi

    if [[ $version != 1.8.* && $version != 8.* ]]; then
        echo "ClimateChangeAgent requires Java 8, but detected java version '$version'." >&2
        echo "Set JAVA_HOME to a JDK 8 installation, e.g.:" >&2
        echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v 1.8)" >&2
        echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >&2
        exit 1
    fi
}

select_java8

# Java main class
MAIN_CLASS="basilica2.myagent.operation.NewAgentRunner"

echo "Setting up ClimateChangeAgent..."

# Function to find all JAR files
find_all_jars() {
    local jars=()
    while IFS= read -r -d '' jar; do
        jars+=("$jar")
    done < <(find "$PROJECTDIR" -name "*.jar" -print0 2>/dev/null)

    if [ ${#jars[@]} -eq 0 ]; then
        echo ""
        return
    fi

    local output="${jars[0]}"
    for jar in "${jars[@]:1}"; do
        output="$output:$jar"
    done
    echo "$output"
}

# Function to compile if needed
ensure_compiled() {
    if [ ! -f "$BASEDIR/bin/basilica2/myagent/operation/NewAgentRunner.class" ]; then
        echo "Compiling ClimateChangeAgent..."
        
        mkdir -p "$BASEDIR/bin"
        
        # Get all JARs
        local ALL_JARS=$(find_all_jars)
        
        # Get all source/bin directories for classpath
        local SOURCE_CP=""
        for project in BasilicaCore BaseAgent AccountableTalkAgent SocialAgent SocketIOClient SocketIOClientLegacy TuTalkSlim TutorAgent Genesis-Plugins Synonymizer LightSideMessageAnnotator; do
            if [ -d "$PROJECTDIR/$project/bin" ]; then
                SOURCE_CP="$SOURCE_CP:$PROJECTDIR/$project/bin"
            fi
            if [ -d "$PROJECTDIR/$project/src" ]; then
                SOURCE_CP="$SOURCE_CP:$PROJECTDIR/$project/src"  
            fi
        done
        
        # Combine all classpaths
        local FULL_CP="$ALL_JARS$SOURCE_CP"
        
        # Try to compile just the main runner class and its direct dependencies
        echo "Attempting to compile main class..."
        "$JAVAC_CMD" -d "$BASEDIR/bin" \
              -cp "$FULL_CP" \
              -sourcepath "$BASEDIR/src" \
              "$BASEDIR/src/basilica2/myagent/operation/NewAgentRunner.java" 2>/dev/null
              
        if [ $? -ne 0 ]; then
            echo "Warning: Compilation had errors, but attempting to proceed..."
            # Try to compile everything, ignoring errors
            find "$BASEDIR/src" -name "*.java" -print0 | xargs -0 "$JAVAC_CMD" -d "$BASEDIR/bin" \
                  -cp "$FULL_CP" \
                  -sourcepath "$BASEDIR/src" \
                  -nowarn 2>/dev/null || true
        fi
    fi
}

# Ensure we have compiled classes
ensure_compiled

# Build runtime classpath
echo "Building classpath..."
CLASSPATH="$BASEDIR/bin:$BASEDIR/src"

# Add all project bin and src directories
for project in BasilicaCore BaseAgent AccountableTalkAgent SocialAgent SocketIOClient SocketIOClientLegacy TuTalkSlim TutorAgent Genesis-Plugins Synonymizer LightSideMessageAnnotator; do
    [ -d "$PROJECTDIR/$project/bin" ] && CLASSPATH="$CLASSPATH:$PROJECTDIR/$project/bin"
    [ -d "$PROJECTDIR/$project/src" ] && CLASSPATH="$CLASSPATH:$PROJECTDIR/$project/src"
done

# Add ALL JAR files
ALL_JARS=$(find_all_jars)
CLASSPATH="$CLASSPATH:$ALL_JARS"

# Change to runtime directory
cd "$BASEDIR/runtime"

# Parse arguments
ROOM="ROOM"
LAUNCH=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --room) ROOM="$2"; shift 2 ;;
        --launch) LAUNCH="-launch"; shift ;;
        --help)
            echo "Usage: $0 [--room ROOM_NAME] [--launch]"
            echo "  --room ROOM_NAME  Set the room name (default: ROOM)"
            echo "  --launch          Launch without UI"
            exit 0
            ;;
        *) shift ;;
    esac
done

# Run the application
echo "Starting ClimateChangeAgent..."
echo "Room: $ROOM"
echo "Working directory: $(pwd)"
echo ""

"$JAVA_CMD" -cp "$CLASSPATH" \
     -Dbasilica2.agents.room_name="$ROOM" \
     $MAIN_CLASS \
     -room "$ROOM" \
     $LAUNCH
