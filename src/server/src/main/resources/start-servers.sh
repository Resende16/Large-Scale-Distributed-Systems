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

echo "Starting 4 servers instances..."

# Start tmux session
tmux new-session -d -s servers

# Start servers

tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --address tcp://localhost:5558" C-m

tmux split-window -h
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --address tcp://localhost:5559" C-m

tmux split-window -v
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --address tcp://localhost:5510" C-m

tmux split-window -v
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./server-dev.jar --address tcp://localhost:5511" C-m

tmux select-layout even-horizontal

# Attach tmux session
tmux attach -t servers
