#!/bin/bash
# Run ALO vs AMO Semantics Demo
# Prerequisites: Server must be running with reply loss enabled

HOST=${1:-localhost}
PORT=${2:-8888}

cd "$(dirname "$0")/.."

echo "Make sure the server is running with reply loss!"
echo "Example: ./scripts/start-server.sh 8888 0 30"
echo ""
echo "Press Enter to continue..."
read

mvn -q compile exec:java -Dexec.mainClass="edu.ntu.ds.demo.SemanticsDemo" -Dexec.args="$HOST $PORT"
