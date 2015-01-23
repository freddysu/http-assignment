#!/bin/bash

javac WebServer.java -d classes
javac WebServerTest.java -d classes
java -cp ./classes group13.WebServer &
java -cp ./classes group13.WebServerTest -p 8080
