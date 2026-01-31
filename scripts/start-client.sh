#!/bin/bash
# Start the Interactive Client
# Usage: ./start-client.sh [host] [port] [clientId] [semantics]

HOST=${1:-localhost}
PORT=${2:-8888}
CLIENT_ID=${3:-}
SEMANTICS=${4:-AMO}

cd "$(dirname "$0")/.."

if [ -z "$CLIENT_ID" ]; then
    mvn -q compile exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="$HOST $PORT"
else
    mvn -q compile exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="$HOST $PORT $CLIENT_ID $SEMANTICS"
fi
