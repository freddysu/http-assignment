#!/bin/bash

javac WebServer.java -d classes
java -cp ./classes group13.WebServer &
