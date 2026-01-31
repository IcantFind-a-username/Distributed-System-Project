#!/bin/bash
# Run Protocol Round-Trip Tests

cd "$(dirname "$0")/.."
mvn -q compile exec:java -Dexec.mainClass="edu.ntu.ds.protocol.ProtocolTest"
