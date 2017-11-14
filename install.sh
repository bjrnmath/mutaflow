#!/bin/sh
mkdir DroidMutantCode/android-jars

read -p "Please place an \"android.jar\" file in the DroidMutantCode/android-jars folder, then continue ..."

cd DroidMutantCode

gradle build

cd ..

mkdir install/apk
mkdir install/instrout
mkdir install/ExecutionLogs
mkdir install/AppOut
mkdir install/AppOutSave
mkdir install/SootOutput
mkdir install/logs
cp DroidMutantCode/android-jars/android.jar install/SootClassPath/android.jar