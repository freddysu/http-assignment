#!/bin/bash

javac WebServerTest.java -d classes
java -cp ./classes group13.WebServerTest -p 8080
