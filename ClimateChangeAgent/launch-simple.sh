#!/bin/bash

# Simplified ClimateChangeAgent Launch Script
# This script uses pre-compiled classes or compiles on demand

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
PROJECTDIR="$(cd "$BASEDIR/.." && pwd)"

# Java main class
MAIN_CLASS="basilica2.myagent.operation.NewAgentRunner"

echo "Setting up ClimateChangeAgent..."

# Function to find all JAR files
find_all_jars() {
    local JARS=""
    # Find ALL jar files in the entire project structure
    for jar in $(find "$PROJECTDIR" -name "*.jar" 2>/dev/null); do
        JARS="$JARS:$jar"
    done
    echo "${JARS#:}"
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
        javac -d "$BASEDIR/bin" \
              -cp "$FULL_CP" \
              -sourcepath "$BASEDIR/src" \
              "$BASEDIR/src/basilica2/myagent/operation/NewAgentRunner.java" 2>/dev/null
              
        if [ $? -ne 0 ]; then
            echo "Warning: Compilation had errors, but attempting to proceed..."
            # Try to compile everything, ignoring errors
            find "$BASEDIR/src" -name "*.java" | xargs javac -d "$BASEDIR/bin" \
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

java -cp "$CLASSPATH" \
     -Dbasilica2.agents.room_name="$ROOM" \
     $MAIN_CLASS \
     -room "$ROOM" \
     $LAUNCH