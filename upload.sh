#!/bin/bash
rsync -rltDuv lib assets *.html www.sciss.de@ssh.strato.de:exp/infibrillae/
