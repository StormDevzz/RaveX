#!/bin/bash
# Прогон обоих чекеров эквивалентности (нужен собранный build)
J=/home/deb32/opt/jdk-21.0.13+9/bin
CP=build/classes/java/main:build/resources/main:build/check
$J/javac -cp build/classes/java/main -d build/check check/RustPortCheck.java check/CalcPortCheck.java || exit 1
echo "== SEARCH ==" && $J/java -cp $CP RustPortCheck
echo "== CALC ==" && $J/java -cp $CP CalcPortCheck
