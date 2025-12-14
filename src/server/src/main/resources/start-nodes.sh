#!/bin/bash

# Check if tmux is installed
if ! command -v tmux &> /dev/null; then
    echo "--------------------------------------------"
    echo "[WARNING] tmux is not installed. Please install it to run this automation!"
    echo "On MacOS using Homebrew >> brew install tmux"
    echo "On Ubuntu >> sudo apt update + sudo apt install tmux"
    echo "--------------------------------------------"
    exit 1
fi

echo "Starting 4 nodes..."

# Static node ids
UUID1="00000000-0000-0000-0000-000000000000"
UUID2="ffffffff-ffff-ffff-ffff-ffffffffffff"
UUID3="12345678-1234-5678-1234-567812345678"
UUID4="deadbeef-cafe-babe-face-feedc0ffee00"

# Start tmux session
tmux new-session -d -s nodes

# Start nodes

tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --id $UUID1 --address tcp://localhost:5555" C-m

# Wait for bootstrapping
sleep 2

tmux split-window -h
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --id $UUID2 --address tcp://localhost:5556" C-m

tmux split-window -v
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --id $UUID3 --address tcp://localhost:5557" C-m

tmux split-window -v
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --id $UUID4 --address tcp://localhost:5558" C-m

tmux select-layout even-horizontal

# Attach tmux session
tmux attach -t nodes
