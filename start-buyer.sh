#!/bin/bash

if [[ "$#" != 3 ]]; then
    echo "Usage: $0 (main container host) (agent name) (book name)" >&2
    exit 1
fi

HOST=$1
AGENT=$2
BOOK=$3
AGENT_CLASS=jade.agents.bookTrading.BookBuyerAgent

mvn compile exec:java \
    -Dexec.mainClass=jade.Boot \
    -Dexec.args="-container -host $HOST -agents '$AGENT:$AGENT_CLASS($BOOK)'"
