#!/bin/bash
cd "$(dirname "$0")"
echo "in|fibrillae (window)"

# xrandr
xrandr --output HDMI-1 --mode 640x480

sleep 4
# turn off screen blanking
xset s off
xset -dpms

echo "- start qjackctl"
qjackctl &

sleep 10
echo "- start mellite"
mellite-launcher --headless --boot-audio --auto-run start /home/pi/Documents/projects/infibrillae/workspace.mllt &

sleep 30
echo "- start visual"
java -jar jvm/infibrillae.jar --full-screen
