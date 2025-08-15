#!/bin/bash

# Create bin directory if it doesn't exist
mkdir -p bin

# Compile Java source files
javac -cp lib/jade.jar -d bin src/**/*.java