#!/bin/bash
# Jules AI Setup Script for PDF Toolkit
# Run this first in every Jules session

set -e

echo "🔧 Setting up PDF Toolkit environment for Jules AI..."

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ Error: Not in project root. Please run from pdf_tools directory."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Check Java version
echo "☕ Checking Java..."
java -version 2>&1 | head -1

# Check Android CLI availability
if command -v android &> /dev/null; then
    echo "✅ Android CLI found"
    android --version
else
    echo "⚠️  Android CLI not found in PATH"
    echo "   Jules skill should provide this. Continuing with gradle only."
fi

# Initialize Android CLI if available
if command -v android &> /dev/null; then
    echo "🔧 Initializing Android CLI..."
    android init || true
fi

# Verify gradle wrapper works
echo "🔨 Testing gradle wrapper..."
./gradlew --version

echo ""
echo "✅ Setup complete! Ready for weekly optimization."
echo ""
echo "Next steps:"
echo "  1. Run: ./gradlew assembleFdroidDebug"
echo "  2. Check AI_RUN_LOG.md for recent work"
echo "  3. Start weekly optimization task"
