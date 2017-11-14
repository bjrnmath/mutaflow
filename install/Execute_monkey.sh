#!/bin/sh

#This source code is part of the MutaFlow project. It executes one file with the monkey input generator of Android.
#Copyright (C) 2017  Björn Mathis

#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by
#the Free Software Foundation, either version 3 of the License, or
#(at your option) any later version.

#This program is distributed in the hope that it will be useful,
#but WITHOUT ANY WARRANTY; without even the implied warranty of
#MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#GNU General Public License for more details.

#You should have received a copy of the GNU General Public License
#along with this program.  If not, see <https://www.gnu.org/licenses/>.


PACKAGENAME="$1"
FOLDER="$2"
APPNAME="$3"
PAPNAME="$4"

#configuration
NOOFACTIONS=200
SEED=1000
#end configuration

#now uninstall and install to perform
adb uninstall $PACKAGENAME


(while(true) 
do
(
echo "power capacity 100"
sleep 1
echo "power capacity 10"
sleep 1
echo "exit"

) |
telnet localhost 5554 #usually devices start on this number for telnet, if this is different on your computer please change
done) &
backgroundworker=$!

file="$FOLDER"/"$APPNAME"
kill -KILL $pauseandresume
(while(true) 
do
(
sleep 2
adb shell input keyevent 3
sleep 1
adb shell monkey -p $PACKAGENAME 1
sleep 2
)
done) &
pauseandresume=$!

adb uninstall $PACKAGENAME

adb install -g "$file"
adb shell pm grant $PACKAGENAME android.permission.READ_SMS
parentname="$(basename "$(dirname "$file")")"
echo "$parentname"

adb logcat -s MyOwnTag > "AppOut/"$PAPNAME".pap" &
last_pid=$!
#now start monkey
adb shell monkey -p $PACKAGENAME -s $SEED -v $NOOFACTIONS --kill-process-after-error --throttle 200 --pct-appswitch 2 

adb logcat -c

kill -KILL $last_pid

kill -KILL $pauseandresume
kill -KILL $backgroundworker

adb uninstall $PACKAGENAME
