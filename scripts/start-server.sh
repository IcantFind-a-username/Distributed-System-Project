#!/bin/bash
# Start the Bank Server
# Usage: ./start-server.sh [port] [requestLoss%] [replyLoss%]

PORT=${1:-8888}
REQ_LOSS=${2:-0}
REP_LOSS=${3:-0}

cd "$(dirname "$0")/.."
mvn -q compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="$PORT $REQ_LOSS $REP_LOSS"
