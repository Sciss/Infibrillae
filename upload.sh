#!/bin/bash
rsync -rltDuv lib *.css *.html *.mllt.bin www.sciss.de@ssh.strato.de:exp/infibrillae/
