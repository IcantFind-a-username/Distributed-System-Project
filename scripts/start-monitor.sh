#!/bin/bash
# Start the Monitor Client
# Usage: ./start-monitor.sh [host] [port] [clientId] [ttl] [duration]

HOST=${1:-localhost}
PORT=${2:-8888}
CLIENT_ID=${3:-9999}
TTL=${4:-300}
DURATION=${5:-300}

cd "$(dirname "$0")/.."
mvn -q compile exec:java -Dexec.mainClass="edu.ntu.ds.client.MonitorClient" -Dexec.args="$HOST $PORT $CLIENT_ID $TTL $DURATION"
