#!/bin/sh

#This source code is part of the MutaFlow project. It signs and aligns applications according to the Android documentation.
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


#Configuration
ZIPALIGNHOME=<Path-to-Android>/Android/sdk/build-tools/23.0.1 #path to zipalign for aligning
PATH_TO_DEBUG_KEY="<Path-to-debug-key>.android" #path to the debug key for signing
#end configuration


#sign and align part
#for NAME in SootOutput/*/*.apk
for NAME in instrout/*/*/*.apk
do
	mv $NAME $NAME"_unsigned.apk"
	PATH_TO_UNSIGNED_APK=$NAME"_unsigned.apk"
	PATH_TO_SIGNED_APK=$NAME"_signed.apk"
	PATH_TO_RESULT_APK=$NAME
	echo "Signing..."
	jarsigner -sigalg MD5withRSA -digestalg SHA1 -keystore $PATH_TO_DEBUG_KEY/debug.keystore -storepass android -keypass android $PATH_TO_UNSIGNED_APK -signedjar $PATH_TO_SIGNED_APK androiddebugkey
	echo "Aligning..."
	$ZIPALIGNHOME/zipalign -v -f 4 $PATH_TO_SIGNED_APK $PATH_TO_RESULT_APK
	rm -rf $PATH_TO_UNSIGNED_APK
	rm -rf $PATH_TO_SIGNED_APK
	echo "Done"
done
