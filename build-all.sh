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
    
    # Create bin directory
    mkdir -p "$PROJECT_DIR/bin"
    
    # Build classpath for this project
    local COMPILE_CP=""
    
    # Add BasilicaCore JARs first (includes JGraph and Xerces)
    if [ -d "$BASEDIR/BasilicaCore/lib" ]; then
        # Add JGraph
        if [ -f "$BASEDIR/BasilicaCore/lib/OtherLibraries/JGraph/jgraph.jar" ]; then
            COMPILE_CP="$COMPILE_CP:$BASEDIR/BasilicaCore/lib/OtherLibraries/JGraph/jgraph.jar"
        fi
        
        # Add Xerces
        for jar in $BASEDIR/BasilicaCore/lib/OtherLibraries/Xerces/*.jar; do
            if [ -f "$jar" ]; then
                COMPILE_CP="$COMPILE_CP:$jar"
            fi
        done
        
        # Add Utilities
        if [ -f "$BASEDIR/BasilicaCore/lib/OtherLibraries/Utilities.jar" ]; then
            COMPILE_CP="$COMPILE_CP:$BASEDIR/BasilicaCore/lib/OtherLibraries/Utilities.jar"
        fi
        
        # Add any other JARs in BasilicaCore/lib
        for jar in $BASEDIR/BasilicaCore/lib/*.jar; do
            if [ -f "$jar" ]; then
                COMPILE_CP="$COMPILE_CP:$jar"
            fi
        done
    fi
    
    # Add all JAR files from BaseAgent lib
    if [ -d "$BASEDIR/BaseAgent/lib" ]; then
        # Add main lib JARs
        for jar in $BASEDIR/BaseAgent/lib/*.jar; do
            if [ -f "$jar" ]; then
                COMPILE_CP="$COMPILE_CP:$jar"
            fi
        done
        
        # Add ConcertChat Libraries
        for jar in $BASEDIR/BaseAgent/lib/Environments/ConcertChat/Libraries/*.jar; do
            if [ -f "$jar" ]; then
                COMPILE_CP="$COMPILE_CP:$jar"
            fi
        done
        
        # Add Moodle libraries
        for jar in $BASEDIR/BaseAgent/lib/Environments/Moodle/*.jar; do
            if [ -f "$jar" ]; then
                COMPILE_CP="$COMPILE_CP:$jar"
            fi
        done
    fi
    
    # Add project-specific JARs
    if [ -d "$PROJECT_DIR/lib" ]; then
        for jar in $PROJECT_DIR/lib/*.jar $PROJECT_DIR/lib/**/*.jar; do
            if [ -f "$jar" ]; then
                COMPILE_CP="$COMPILE_CP:$jar"
            fi
        done
    fi
    
    # Add bin directories of already compiled projects
    for dep in BasilicaCore BaseAgent AccountableTalkAgent SocialAgent SocketIOClient SocketIOClientLegacy TuTalkSlim TutorAgent Genesis-Plugins Synonymizer LightSideMessageAnnotator; do
        if [ -d "$BASEDIR/$dep/bin" ]; then
            COMPILE_CP="$COMPILE_CP:$BASEDIR/$dep/bin"
        fi
        if [ -d "$BASEDIR/$dep/src" ]; then
            COMPILE_CP="$COMPILE_CP:$BASEDIR/$dep/src"
        fi
    done
    
    # Remove leading colon
    COMPILE_CP="${COMPILE_CP#:}"
    
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
compile_project "SocketIOClient"
compile_project "SocketIOClientLegacy"

# Agent-related projects
compile_project "AccountableTalkAgent"
compile_project "SocialAgent"
compile_project "TutorAgent"
compile_project "TuTalkSlim"

# Plugin and utility projects
compile_project "Genesis-Plugins"
compile_project "Synonymizer"
compile_project "LightSideMessageAnnotator"

# Finally, compile the main agents
compile_project "ClimateChangeAgent"

echo ""
echo -e "${GREEN}Build complete!${NC}"
echo ""
echo "To run ClimateChangeAgent, use:"
echo "  cd ClimateChangeAgent"
echo "  ./launch.sh --room TestRoom --launch"