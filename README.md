MutaFlow Code
================

This repository contains the sources of MutaFlow as well as additional files to run MutaFlow. Initially, the project was named DroidMutant and there still exist some naming artifacts in the repository.

MutaFlow is a lightweight mutation-based analysis that systematically mutates dynamic values returned by sensitive sources to assess whether the mutation changes the values passed to sensitive sinks. If so, a flow between source and sink is found. In contrast to existing techniques, mutation-based flow analysis does not attempt to identify the specific path of the flow and is thus resilient to obfuscation.

The paper describing MutaFlow in more detail can be found here: [https://dl.acm.org/citation.cfm?id=3155598&CFID=1005478616&CFTOKEN=50031448](https://dl.acm.org/citation.cfm?id=3155598&CFID=1005478616&CFTOKEN=50031448)

Prerequesities
---------------

* Java
* Python
* Gradle
* Android ADB (make sure adb can be called from within scripts)
* Android AAPT
* android.jar (can for example be obtained from the Android installation folder, i.e. "Android/sdk/platforms/android-*/android.jar")

The tool was tested under MacOS but should also work with Linux. 

Installation
---------------

In the file _"DroidMutantCode/Runner/src/main/java/Runner.java"_ the variable _"pathToAAPT"_ needs to be set to your Android build tools.

Run the _"install.sh"_ script and follow the instructions.

If the build fails after _"downloading soot"_ is shown, soot was likely not be able to download.

If the soot jar is not downloaded, find a _"sootclasses-trunk-jar-with-dependencies.jar"_ online and replace the link to the jar in the buildscript _"DroidMutantCode/Instrumenter/build.gradle"_.

Usage
----------------

The install folder can be used anywhere on the system. Before each usage, check that the SootOutput and AppOut folders are empty. The _"cleanup.sh"_ script cleans the install folder if necessary.

<h3 id="firststeps"> 0. First Steps </h3>

In _"SignAndAlign.sh"_ you need to set _"ZIPALIGNHOME"_ to the _"zipalign"_ of Android and _"PATH\_TO\_DEBUG\_KEY"_ to your Android debugkey.

After installing and configuring all paths, connect a device or start the emulator. Put one or more APK-Files in the _"apk"_ folder. Then run the _"analyze\_application.sh"_ script. This instruments the apks, stores them in the _"instrout"_ folder, then signs and aligns them, executes them on the devices, stores the output in the _"AppOutSave"_ folder, then starts the _"startParser"_ script. This script calculates the raw flows and stores them in the _"AppOutSave"_ folder. Subsequently the _"FlowExtractor.jar"_ is started which generates the _"FoundFlows.csv"_, a csv file which contains the found flows.

The next chapters describe the steps to take in more detail to make adaptions of steps easier.

<h3 id="instrumentation"> 1. Instrumentation </h3>

First the application needs to be instrumented, this is done by starting the python script _"instrument\_parallel.py"_. The _"SootOutput"_, _"instrout"_ and _"logs"_ folders need to be empty when starting the script.

At the top of the script you can set :

* the path to the _"android.jar"_ Soot will be forced to use
* the number of parallel instrumentations 
* the maximal amount of RAM that can be used
* the folder where the apks lie in

If MutaFlow runs out of RAM while instrumenting, either reduce the number of parallel instrumentations or increase the amount of RAM.

The sources and sinks are defined in the respective files in the _"SuSi"_ folder. Additional method calls can be added by just extending the respective files. A list of possible API calls can be found [here](https://github.com/secure-software-engineering/SuSi). 

The instrumented APKs are stored in the instrout folder, the logs of the instrumentations are stored in the logs folder.

<h3 id="signalign"> 2. Signing and Aligning </h3>

After instrumenting, several APKs lie in the _"instrout"_ folder. In a next step the generated APKs need to be signed and aligned. The _"SignAndAlign.sh"_ file contains an example on how to do this. _"ZIPALIGNHOME"_ and _"PATH\_TO\_DEBUG\_KEY"_ need to be set to the respective location on your system.

<h3 id="execution"> 3. Execution </h3>

Now the applications can be executed on a device or emulator, the logcat output needs to be stored. This is done with the following command:

* adb logcat -s MyOwnTag > AppOut/0\_Original.pap &
* adb logcat -s MyOwnTag > AppOut/0\_OriginalRef.pap &
* adb logcat -s MyOwnTag > AppOut/\*X\*\_\*MutatedAPKName\*.pap &

This command only stores the output of the instrumentation. For the two unmutated executions two files need to be stored, called "0\_Original.pap" and "0\_OriginalRef.pap".

The file _"Execute\_monkey.sh"_ gives an example on how to do this. For monkey the packagename is needed to run everything, so we additionally added java sources and a _"Runner.jar"_ which handles the package extraction and the order of execution. The _"Runner.jar"_ needs to be set up for your system by setting the _"pathToAAPT"_ to the android build tools you wish to use. 

The runner takes the APKs from the _"instrout"_ folder and starts the _"Execute\_monkey.sh"_ script.

If the telnet connection to the device fails, adapt line 44 in the _"Execute\_monkey.sh"_ script.

<h3 id="extraction"> 4. Flow extraction </h3>

The logs need to be stored in the AppOut folder. The _"Parser.jar"_ uses the logs stored in this folder to extract the flows. In a second step the _"FlowExtractor.jar"_ creates a csv file containing all found flows.

<h3 id="cleanup"> 5. Cleanup </h3>

Before starting a new analysis, run the _"cleanup.sh"_ script which deletes all collected information from the previous analysis.

**Be aware that all content is deleted with this script. If you want to keep any information (including found flows), you have to save it in a different folder before starting the script**