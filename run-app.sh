#!/bin/bash

echo "========================================"
echo "SCORM Package Generator"
echo "========================================"
echo

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 11 or higher"
    echo "On macOS, you can install Java using:"
    echo "  brew install openjdk@11"
    echo "  or download from: https://adoptium.net/"
    read -p "Press any key to exit..."
    exit 1
fi

echo "Java found!"
echo

# Create directories if they don't exist
mkdir -p target/classes

# Check if lib directory has JAR files
if ! ls lib/*.jar 1> /dev/null 2>&1; then
    echo "ERROR: Dependencies not found in lib/ directory"
    echo "Please run the download script first: ./download-deps.sh"
    echo
    read -p "Press any key to exit..."
    exit 1
fi

echo "Building classpath..."

# Build classpath with all JAR files
CLASSPATH="target/classes"
for jar in lib/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

echo "Classpath: $CLASSPATH"
echo

echo "Compiling Java sources..."

# Compile all Java files with proper classpath
javac -cp "$CLASSPATH" -d target/classes -encoding UTF-8 \
    src/main/java/com/scorm/generator/model/*.java \
    src/main/java/com/scorm/generator/*.java \
    src/main/java/com/scorm/generator/gui/*.java

if [ $? -ne 0 ]; then
    echo
    echo "ERROR: Compilation failed"
    read -p "Press any key to exit..."
    exit 1
fi

echo "Compilation successful!"
echo

# Copy resources if they exist
if [ -d "src/main/resources" ]; then
    echo "Copying resources..."
    cp -r src/main/resources/* target/classes/ 2>/dev/null || true
fi

echo "Starting SCORM Package Generator..."
echo

# Run the application
java -cp "$CLASSPATH" com.scorm.generator.ScormGeneratorApp

if [ $? -ne 0 ]; then
    echo
    echo "ERROR: Application failed to start"
    read -p "Press any key to exit..."
    exit 1
fi

echo
echo "Application closed successfully."
read -p "Press any key to exit..."
