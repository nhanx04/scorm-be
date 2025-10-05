#!/bin/bash

echo "=========================================="
echo "   SCORM Package Generator - Quick Run"
echo "=========================================="
echo

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java không được tìm thấy!"
    echo
    echo "Vui lòng cài đặt Java 11 hoặc cao hơn:"
    echo "1. Sử dụng Homebrew: brew install openjdk@11"
    echo "2. Hoặc truy cập: https://adoptium.net/"
    echo "3. Khởi động lại Terminal sau khi cài đặt"
    echo
    read -p "Nhấn Enter để thoát..."
    exit 1
fi

echo "✅ Java đã được cài đặt"
echo

# Create directories
mkdir -p target/classes lib

# Check if we have dependencies
HAS_DEPS=0
if [ -f "lib/dom4j-2.1.4.jar" ]; then
    HAS_DEPS=1
fi

if [ $HAS_DEPS -eq 0 ]; then
    echo "📦 Đang tải dependencies..."
    echo
    
    # Download essential JARs using curl
    declare -A urls=(
        ["dom4j-2.1.4.jar"]="https://repo1.maven.org/maven2/org/dom4j/dom4j/2.1.4/dom4j-2.1.4.jar"
        ["jackson-databind-2.15.2.jar"]="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"
        ["jackson-core-2.15.2.jar"]="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar"
        ["jackson-annotations-2.15.2.jar"]="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar"
        ["commons-compress-1.24.0.jar"]="https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.24.0/commons-compress-1.24.0.jar"
        ["commons-io-2.11.0.jar"]="https://repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar"
        ["slf4j-api-2.0.9.jar"]="https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"
        ["slf4j-simple-2.0.9.jar"]="https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"
    )
    
    for file in "${!urls[@]}"; do
        filepath="lib/$file"
        if [ ! -f "$filepath" ]; then
            echo "Đang tải: $file"
            if curl -L -o "$filepath" "${urls[$file]}" --silent --show-error; then
                echo "✅ Hoàn thành: $file"
            else
                echo "❌ Lỗi: $file"
            fi
        fi
    done
    
    echo
    echo "✅ Tải dependencies hoàn thành!"
fi

echo "🔨 Đang compile..."

# Build classpath
CP="target/classes"
for jar in lib/*.jar; do
    if [ -f "$jar" ]; then
        CP="$CP:$jar"
    fi
done

# Compile
javac -cp "$CP" -d target/classes -encoding UTF-8 \
    src/main/java/com/scorm/generator/*.java \
    src/main/java/com/scorm/generator/model/*.java \
    src/main/java/com/scorm/generator/gui/*.java 2>compile_error.log

if [ $? -ne 0 ]; then
    echo "❌ Lỗi compile! Xem file compile_error.log"
    cat compile_error.log
    read -p "Nhấn Enter để thoát..."
    exit 1
fi

# Copy resources
if [ -d "src/main/resources" ]; then
    cp src/main/resources/* target/classes/ 2>/dev/null || true
fi

echo "✅ Compile thành công!"
echo
echo "🚀 Đang khởi động ứng dụng..."
echo

# Run
java -cp "$CP" com.scorm.generator.ScormGeneratorApp

echo
echo "👋 Cảm ơn bạn đã sử dụng SCORM Package Generator!"
read -p "Nhấn Enter để thoát..."
