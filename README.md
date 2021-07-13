# in|fibrillae

[![Build Status](https://github.com/Sciss/Infibrillae/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/Infibrillae/actions?query=workflow%3A%22Scala+CI%22)

## statement

A sound poem for the web browser.
Work in progress.

See online: [www.sciss.de/exp/infibrillae/](https://www.sciss.de/exp/infibrillae/).

Research process: [www.researchcatalogue.net/view/711664/1111185](https://www.researchcatalogue.net/view/711664/1111185)

Software based on:
- [SoundProcesses](https://github.com/Sciss/SoundProcesses) -- AGPL v3+ license
- [scsynth.wasm](https://github.com/Sciss/supercollider/tree/wasm) -- GPL v2+ license
- a translation of `java.awt.geom` classes from [OpenJDK](http://openjdk.java.net/) - GPL v2 with "classpath exception" license

The SoundProcesses/Mellite workspace file `workspace.mllt.bin` is released under
Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International (CC BY-NC-ND 4.0) license.
The font is Voltaire-Regular by Yvonne Schüttler, released under Open Font License.

This project is (C)opyright 2020–2021 by Hanns Holger Rutz & Nayarí Castillo. All rights reserved.
To contact us, send an e-mail to `contact at sciss.de`.

## building from source

Because the binary workspace `assets/workspace.mllt.bin` is a rather large blob that is not suited for
a git repository, the source `workspace.mllt` is included, and it must be exported as a binary workspace
using Mellite 3.4.0-SNAPSHOT or newer (currently in development, you can build Mellite from source).
Open `workspace.mllt` and choose the menu item _File_ > _Export Binary Workspace_.

Then compile the launcher with `sbt -J-Xmx2G fullOptJS`.

After successful compilation, the [index.html](index.html) can be used to run the application.
You must run a web server. It must correctly set the headers with `Cross-Origin-Opener-Policy: same-origin`
and `Cross-Origin-Embedder-Policy: require-corp` in order to work with Firefox. Thanks to
[this blog](https://github.com/cggallant/blog_post_code/tree/master/2020%20-%20July%20-%20Extending%20Python%E2%80%99s%20Simple%20HTTP%20Server),
a script is already included that works with Python 2;

    python wasm-server.py

(then open its default page, [127.0.0.1:8080](http://127.0.0.1:8080))

## physical window installation

Build with `sbt rootJVM/assembly`. See `run-window.sh` for the start script.

## Navigation

Moving the mouse over the canvas traverses the virtual spaces. The words of the poems appear and disappear
independently but follow the movement around (with friction). Keeping the mouse button pressed allows one to
move it outside the canvas without causing motion of the virtual space.
When bridging words appear, they are visible
by reddish colour. Bridging can be triggered explicitly by pressing the `Enter` key (canvas must have focus by
clicking with the mouse0. The view port of the  virtual space determines the spatial mix of the sound. Eight
invisible sensor trigger areas are designated (sound implementation pending).

## Desktop version

As there are currently some issues with the browser version, and during development, the desktop version
can be run. E.g. in IntelliJ run the `Infibrillae` object in the `jvm` project. This opens a window with
the visual presentation of the piece, and currently assumes that the workspace is actually run separately
in Mellite, connecting to it via OSC. In the future, we may have the option to launch the workspace directly
in the project, as is the case for the browser version.

## notes

I use this to fix COOP/COEP things for Firefox (not sure it's correct, but seems to be working):

```
Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"

Header set Cross-Origin-Opener-Policy: same-origin
Header set Cross-Origin-Embedder-Policy: require-corp
```

Chromium has very bad and choppy sound performance on Linux when running Pulse through Jack Audio. Avoid that.
