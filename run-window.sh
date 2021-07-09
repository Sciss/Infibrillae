#!/bin/bash
cd "$(dirname "$0")"
echo "in|fibrillae (window)"

sleep 4
xset s off
xset -dpms

java -jar jvm/infibrillae.jar --osc-port 0 --double-size

