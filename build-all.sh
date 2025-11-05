#!/bin/bash

# Build script for all Bazaar projects
# This script compiles all projects in the correct dependency order

BASEDIR="$(cd "$(dirname "$0")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building all Bazaar projects...${NC}"

# Helper to list all jar files from a directory tree
gather_jars() {
    local dir=$1
    if [ -d "$dir" ]; then
        find "$dir" -type f -name "*.jar" -print
    fi
}

# Function to compile a project
compile_project() {
    local PROJECT_NAME=$1
    local PROJECT_DIR="$BASEDIR/$PROJECT_NAME"
    
    if [ ! -d "$PROJECT_DIR" ]; then
        echo -e "${YELLOW}Skipping $PROJECT_NAME (directory not found)${NC}"
        return 0
    fi
    
    if [ ! -d "$PROJECT_DIR/src" ]; then
        echo -e "${YELLOW}Skipping $PROJECT_NAME (no src directory)${NC}"
        return 0
    fi
    
    echo -e "${GREEN}Building $PROJECT_NAME...${NC}"
    
    # Rebuild bin directory to avoid stale classes from other environments
    rm -rf "$PROJECT_DIR/bin"
    mkdir -p "$PROJECT_DIR/bin"
    
    # Build classpath for this project
    local -a CP_ENTRIES=()
    
    # Basilica Core jars and dependencies
    while IFS= read -r jar; do
        CP_ENTRIES+=("$jar")
    done < <(gather_jars "$BASEDIR/BasilicaCore/lib")
    
    # BaseAgent jars (includes environment-specific libraries)
    while IFS= read -r jar; do
        CP_ENTRIES+=("$jar")
    done < <(gather_jars "$BASEDIR/BaseAgent/lib")
    
    # LightSide resources (needed by Genesis plugins and others)
    while IFS= read -r jar; do
        CP_ENTRIES+=("$jar")
    done < <(gather_jars "$BASEDIR/LightSide/lib")
    while IFS= read -r jar; do
        CP_ENTRIES+=("$jar")
    done < <(gather_jars "$BASEDIR/LightSide/wekafiles/packages")
    if [ -d "$BASEDIR/LightSide/bin" ]; then
        CP_ENTRIES+=("$BASEDIR/LightSide/bin")
    fi
    
    # Project-specific JARs
    while IFS= read -r jar; do
        CP_ENTRIES+=("$jar")
    done < <(gather_jars "$PROJECT_DIR/lib")
    
    # Add bin/src directories of already compiled projects
    for dep in BasilicaCore BaseAgent AccountableTalkAgent SocialAgent SocketIOClient SocketIOClientLegacy TuTalkSlim TutorAgent Genesis-Plugins Synonymizer LightSideMessageAnnotator; do
        if [ -d "$BASEDIR/$dep/bin" ]; then
            CP_ENTRIES+=("$BASEDIR/$dep/bin")
        fi
        if [ -d "$BASEDIR/$dep/src" ]; then
            CP_ENTRIES+=("$BASEDIR/$dep/src")
        fi
    done
    
    # Build colon-delimited classpath string
    local COMPILE_CP=""
    if [ ${#CP_ENTRIES[@]} -gt 0 ]; then
        COMPILE_CP=$(IFS=:; echo "${CP_ENTRIES[*]}")
    fi
    
    # Find all Java files
    find "$PROJECT_DIR/src" -name "*.java" > /tmp/${PROJECT_NAME}_sources.txt
    
    if [ ! -s /tmp/${PROJECT_NAME}_sources.txt ]; then
        echo -e "${YELLOW}No Java files found in $PROJECT_NAME${NC}"
        rm /tmp/${PROJECT_NAME}_sources.txt
        return 0
    fi
    
    # Compile with error suppression for warnings
    javac -d "$PROJECT_DIR/bin" \
          -cp "$COMPILE_CP" \
          -sourcepath "$PROJECT_DIR/src" \
          -encoding UTF-8 \
          -nowarn \
          -XDignore.symbol.file \
          @/tmp/${PROJECT_NAME}_sources.txt 2>&1 | grep -v "warning" | grep -v "Note:"
    
    if [ ${PIPESTATUS[0]} -eq 0 ]; then
        echo -e "${GREEN}✓ $PROJECT_NAME compiled successfully${NC}"
        rm /tmp/${PROJECT_NAME}_sources.txt
        return 0
    else
        echo -e "${RED}✗ $PROJECT_NAME compilation failed${NC}"
        rm /tmp/${PROJECT_NAME}_sources.txt
        return 1
    fi
}

# Build projects in dependency order
# Core projects first
compile_project "BasilicaCore"
compile_project "BaseAgent"

# Socket and communication projects
compile_project "Synonymizer"
compile_project "SocketIOClient"
compile_project "SocketIOClientLegacy"

# Agent-related projects
compile_project "SocialAgent"
compile_project "AccountableTalkAgent"
compile_project "LabAssistantAgent"
compile_project "TuTalkSlim"
compile_project "TutorAgent"

# Plugin and utility projects
compile_project "Genesis-Plugins"
compile_project "LightSideMessageAnnotator"

# Finally, compile the main agents
compile_project "ClimateChangeAgent"

echo ""
echo -e "${GREEN}Build complete!${NC}"
echo ""
echo "To run ClimateChangeAgent, use:"
echo "  cd ClimateChangeAgent"
echo "  ./launch.sh --room TestRoom --launch"
