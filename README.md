# DroidTooth File Transfer

Listen for Bluetooth connection from nearby Android Server and respond to received protocols

## Overview

This repository contains the PC server end of the of the 3-way file transfer that makes up DroidTooth File Transfer, which acted as our senior seminar project at [Adelphi University](https://www.adelphi.edu/). Developed with [Java](https://www.java.com/en/) using [Eclipse](https://www.eclipse.org/)

There are three components which make up the entirety of DroidTooth File Transfer

### Abstract

*Suppose that you have to present your final project in an hour, but you left a file on your home desktop, which has terrible internet connectivity. However, you realize that a spare Android device is sitting at home and that it is Bluetooth enabled. Can you use your current smart phone to connect to your device at home, and grab your file in a 3-way file transfer?*

We developed a 3-way file transfer system that allows users to use an Android device or any web browser to connect to an Android device at home and then use it to access files from a nearby computer via Bluetooth. Once the Android device at home receives the requested file, it uses cloud storage to send the requested file back to the user

### Documentation

* [Data Sheet](media/data-sheet.pdf)

* [Decision Log](media/decision-log.pdf)

* [Requirements](media/requirements.pdf)

* [Resources](media/resources.pdf)

* [Technical Documentation](media/technical-documentation.pdf)

## PC Bluetooth Server

PC server that runs in the background on the computer at home, listening for a Bluetooth connection from a nearby Android server

## Android Bluetooth Server

Android server that runs in the background on an Android device at home, receives protocols from the Cloud server, and sends those protocols to the PC server running on a nearby computer

## Cloud Server

* App accessible from anywhere that is used to send protocols to the Android server running on an Android device at home

* Due to time restrictions, the cloud portion of the 3-way file transfer had to be dropped for the final product. Instead, the demo showcased how the Android server is used to connect via Bluetooth to the nearby PC server, choose a file on the computer, and receive the selected file over the Bluetooth connection

## Owners

* [Michael Agarenzo](https://linkedin.com/in/magarenzo)

* [Jai Punjwani](https://github.com/jaipunjwani)

This was originally created as the final project in CSC 480 (Senior Seminar in Information Systems) as part of the culmination of the Computer Science program at Adelphi University
