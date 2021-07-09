#!/bin/bash
cd "$(dirname "$0")"
echo "in|fibrillae (window)"

sleep 4
xset s off
xset -dpms

qjackctl &

sleep 10

java -jar jvm/infibrillae.jar &

mellite-launcher --headless --boot-audio --auto-run start /home/pi/Documents/projects/infibrillae/workspace.mllt

