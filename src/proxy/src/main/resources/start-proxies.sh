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

echo "Starting proxies..."

# Start tmux session
tmux new-session -d -s proxies

tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./proxy-dev.jar --address tcp://localhost:6000" C-m

#sleep 3

tmux split-window -h
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./proxy-dev.jar --address tcp://localhost:6001" C-m

tmux split-window -v
tmux send-keys "java --enable-native-access=ALL-UNNAMED -jar ./proxy-dev.jar --address tcp://localhost:6002" C-m

tmux select-layout even-horizontal

# Attach tmux session
tmux attach -t proxies
