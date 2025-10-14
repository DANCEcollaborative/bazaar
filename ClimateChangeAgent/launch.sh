#!/bin/bash

# ClimateChangeAgent Launch Script
# This script replaces the Eclipse launch configuration

# Set the base directory
BASEDIR="$(cd "$(dirname "$0")" && pwd)"
PROJECTDIR="$(cd "$BASEDIR/.." && pwd)"

# Java main class
MAIN_CLASS="basilica2.myagent.operation.NewAgentRunner"

# Function to compile Java sources
compile_project() {
    echo "Compiling ClimateChangeAgent..."
    
    # Create bin directory if it doesn't exist
    mkdir -p "$BASEDIR/bin"
    
    # Build compile classpath
    COMPILE_CP=""
    
    # Add dependent project classes/sources
    for project in AccountableTalkAgent BaseAgent BasilicaCore SocialAgent SocketIOClient TuTalkSlim TutorAgent Genesis-Plugins Synonymizer LightSideMessageAnnotator; do
        if [ -d "$PROJECTDIR/$project/bin" ]; then
            COMPILE_CP="$COMPILE_CP:$PROJECTDIR/$project/bin"
        fi
        if [ -d "$PROJECTDIR/$project/src" ]; then
            COMPILE_CP="$COMPILE_CP:$PROJECTDIR/$project/src"
        fi
    done
    
    # Add all JAR dependencies
    # BaseAgent JARs
    COMPILE_CP="$COMPILE_CP:$PROJECTDIR/BaseAgent/lib/commons-lang3-3.2.1.jar"
    COMPILE_CP="$COMPILE_CP:$PROJECTDIR/BaseAgent/lib/Environments/Moodle/mysql-connector-java-5.1.26-bin.jar"
    
    # ConcertChat Libraries
    for jar in $PROJECTDIR/BaseAgent/lib/Environments/ConcertChat/Libraries/*.jar; do
        if [ -f "$jar" ]; then
            COMPILE_CP="$COMPILE_CP:$jar"
        fi
    done
    
    # BasilicaCore JARs
    COMPILE_CP="$COMPILE_CP:$PROJECTDIR/BasilicaCore/lib/OtherLibraries/Utilities.jar"
    COMPILE_CP="$COMPILE_CP:$PROJECTDIR/BasilicaCore/lib/OtherLibraries/JGraph/jgraph.jar"
    
    # Xerces JARs
    for jar in $PROJECTDIR/BasilicaCore/lib/OtherLibraries/Xerces/*.jar; do
        if [ -f "$jar" ]; then
            COMPILE_CP="$COMPILE_CP:$jar"
        fi
    done
    
    # Add JAR files from other projects
    for project in AccountableTalkAgent SocialAgent SocketIOClient TuTalkSlim TutorAgent Genesis-Plugins Synonymizer LightSideMessageAnnotator; do
        if [ -d "$PROJECTDIR/$project/lib" ]; then
            for jar in $PROJECTDIR/$project/lib/*.jar; do
                if [ -f "$jar" ]; then
                    COMPILE_CP="$COMPILE_CP:$jar"
                fi
            done
        fi
    done
    
    # Remove leading colon
    COMPILE_CP="${COMPILE_CP#:}"
    
    # Find all Java files and compile them
    echo "Finding and compiling Java source files..."
    find "$BASEDIR/src" -name "*.java" > /tmp/climatechangeagent_sources.txt
    
    if [ -s /tmp/climatechangeagent_sources.txt ]; then
        javac -d "$BASEDIR/bin" -cp "$COMPILE_CP" -sourcepath "$BASEDIR/src" @/tmp/climatechangeagent_sources.txt
        
        if [ $? -eq 0 ]; then
            echo "Compilation successful!"
            rm /tmp/climatechangeagent_sources.txt
            return 0
        else
            echo "Compilation failed!"
            rm /tmp/climatechangeagent_sources.txt
            return 1
        fi
    else
        echo "No Java source files found!"
        rm /tmp/climatechangeagent_sources.txt
        return 1
    fi
}

# Check if bin directory exists and has the main class
if [ ! -f "$BASEDIR/bin/basilica2/myagent/operation/NewAgentRunner.class" ]; then
    echo "Compiled classes not found. Compiling project..."
    compile_project
    if [ $? -ne 0 ]; then
        echo "Failed to compile the project. Exiting."
        exit 1
    fi
else
    echo "Using existing compiled classes."
fi

# Build the runtime classpath
CLASSPATH=""

# Add ClimateChangeAgent classes
CLASSPATH="$BASEDIR/bin"

# Add source directory (for resources)
CLASSPATH="$CLASSPATH:$BASEDIR/src"

# Add dependent project classes
CLASSPATH="$CLASSPATH:$PROJECTDIR/AccountableTalkAgent/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/BaseAgent/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/BasilicaCore/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/SocialAgent/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/SocketIOClient/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/TuTalkSlim/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/TutorAgent/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/Genesis-Plugins/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/Synonymizer/bin"
CLASSPATH="$CLASSPATH:$PROJECTDIR/LightSideMessageAnnotator/bin"

# Add dependent project sources (for resources)
CLASSPATH="$CLASSPATH:$PROJECTDIR/AccountableTalkAgent/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/BaseAgent/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/BasilicaCore/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/SocialAgent/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/SocketIOClient/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/TuTalkSlim/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/TutorAgent/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/Genesis-Plugins/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/Synonymizer/src"
CLASSPATH="$CLASSPATH:$PROJECTDIR/LightSideMessageAnnotator/src"

# Add JAR dependencies
# BaseAgent JARs
CLASSPATH="$CLASSPATH:$PROJECTDIR/BaseAgent/lib/commons-lang3-3.2.1.jar"
CLASSPATH="$CLASSPATH:$PROJECTDIR/BaseAgent/lib/Environments/Moodle/mysql-connector-java-5.1.26-bin.jar"

# ConcertChat Libraries
for jar in $PROJECTDIR/BaseAgent/lib/Environments/ConcertChat/Libraries/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# BasilicaCore JARs
CLASSPATH="$CLASSPATH:$PROJECTDIR/BasilicaCore/lib/OtherLibraries/Utilities.jar"
CLASSPATH="$CLASSPATH:$PROJECTDIR/BasilicaCore/lib/OtherLibraries/JGraph/jgraph.jar"

# Xerces JARs
for jar in $PROJECTDIR/BasilicaCore/lib/OtherLibraries/Xerces/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# Add any other JAR files from dependent projects
for project in AccountableTalkAgent SocialAgent SocketIOClient TuTalkSlim TutorAgent Genesis-Plugins Synonymizer LightSideMessageAnnotator; do
    if [ -d "$PROJECTDIR/$project/lib" ]; then
        for jar in $PROJECTDIR/$project/lib/*.jar; do
            if [ -f "$jar" ]; then
                CLASSPATH="$CLASSPATH:$jar"
            fi
        done
    fi
done

# Set working directory to runtime folder
cd "$BASEDIR/runtime"

# Copy system properties if exists
if [ -f "system.properties" ]; then
    echo "Using system.properties from runtime directory"
fi

# Default parameters
ROOM="ROOM"
CONDITION=""
X=0
Y=0

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --room)
            ROOM="$2"
            shift 2
            ;;
        --condition)
            CONDITION="$2"
            shift 2
            ;;
        --x)
            X="$2"
            shift 2
            ;;
        --y)
            Y="$2"
            shift 2
            ;;
        --launch)
            LAUNCH="true"
            shift
            ;;
        --compile)
            echo "Forcing recompilation..."
            compile_project
            exit $?
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --room <room_name>     Set room name (default: ROOM)"
            echo "  --condition <cond>     Set conditions"
            echo "  --x <value>            Set X position (default: 0)"
            echo "  --y <value>            Set Y position (default: 0)"
            echo "  --launch               Launch without UI"
            echo "  --compile              Force recompilation of source files"
            echo "  --help                 Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Build Java command
JAVA_CMD="java -cp \"$CLASSPATH\""

# Add system properties
JAVA_CMD="$JAVA_CMD -Dbasilica2.agents.room_name=$ROOM"

if [ ! -z "$CONDITION" ]; then
    JAVA_CMD="$JAVA_CMD -Dbasilica2.agents.condition=\"$CONDITION\""
fi

# Add main class
JAVA_CMD="$JAVA_CMD $MAIN_CLASS"

# Add program arguments
JAVA_CMD="$JAVA_CMD -room $ROOM"
JAVA_CMD="$JAVA_CMD -x $X -y $Y"

if [ ! -z "$CONDITION" ]; then
    JAVA_CMD="$JAVA_CMD -condition \"$CONDITION\""
fi

if [ "$LAUNCH" = "true" ]; then
    JAVA_CMD="$JAVA_CMD -launch"
fi

# Execute the command
echo "Starting ClimateChangeAgent..."
echo "Room: $ROOM"
if [ ! -z "$CONDITION" ]; then
    echo "Condition: $CONDITION"
fi
echo "Working directory: $(pwd)"
echo ""

eval $JAVA_CMD