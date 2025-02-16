[![Release](https://jitpack.io/v/umjammer/vavi-sound-pmd.svg)](https://jitpack.io/#umjammer/vavi-sound-pmd)
[![Java CI](https://github.com/umjammer/vavi-sound-pmd/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-pmd/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-pmd/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-sound-pmd/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-sound-pmd

<img alt="duke sings pmd" src="https://github.com/user-attachments/assets/c72c44f7-ae8b-4501-be07-fcf247ca9dd3" width="100"/>

Java version of PMD.

this is a fork of [PMDDotNET](https://github.com/kuma4649/PMDDotNET)

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-sound-pmd)

## Usage

## References

* https://github.com/gzaffin/pmdmini

## TODO


---

# [Original](https://github.com/kuma4649/PMDDotNET)

## Overview

.NET version of PMD.

## Functions, Features

PMD compiler and driver functions can be used.

## Required environment

- PC with Windows 7 or later OS installed
- Text editor
- Motivation and guts

## Before use

Run removeZoneIdent.bat included in the archive to remove the zone identifier.
(A zone identifier is security information that is added to a file to prevent it from working when you run a program that you have downloaded unintentionally. It is added even if you download it intentionally, so it may cause problems.)

## Quick start

### Compile

Drop an mml file into the included compile.bat to compile.

### Play

Drop an m file into the included play.bat to play.
Edit the above bat file to specify options, etc. (Of course, you can also specify it directly from the command line or use an environment variable.)

## Copyright / Disclaimer

PMDDotNET is licensed under GPLv3.
The copyright is held by the author.
This software is not guaranteed, and the author is not responsible for any damages caused by the use of this software.

The source code of the following software has been modified for C# and used.
Or code/dll is used.
These sources/binaries are copyrighted by their respective authors.
For the license, please refer to each document.

- PMD/MC -> ? -> Code reference, porting, modification
- PMDwin -> ? -> Code reference, partially used
- PPZ8 -> ? -> Code reference
- musicDriverInterface -> MIT -> Used with dll dynamic linking

## Special Thanks

This tool is indebted to the following people. The following software and web pages have also been referenced and used.
PMDDotNET would not have existed without the application sources created by KAJA, C60, and sio29 (Ukky).
We are especially grateful to them for allowing us to reference and use their source code, and for the debugging and advice they gave us during the creation process.
Thank you so much. We look forward to working with you in the future lol.
Additionally, the volume balance was overseen by Sonson. Thank you for that as well.

- KAJA
- TAN-Y
- UME-3
- C60
- sio29(Ukky)
- mucom
- boukichi
- M.S
- kurouma
- sonson

- PMD / MC
- FMPMD
- PMDwin
- PPZ8
- Visual Studio Community 2019
- Sakura Editor
- NAUDIO
- Lots of great music data and its programmers
