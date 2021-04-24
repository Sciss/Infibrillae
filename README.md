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
You must run a web server, such as

    python -m SimpleHTTPServer

(then open its default page, like [127.0.0.1:8000](http://127.0.0.1:8000))

## notes

I use this to fix COOP/COEP things for Firefox (not sure it's correct, but seems to be working):

```
Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"

Header set Cross-Origin-Opener-Policy: same-origin
Header set Cross-Origin-Embedder-Policy: require-corp
```
