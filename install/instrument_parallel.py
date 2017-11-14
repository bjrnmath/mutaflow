#!/bin/sh#!/usr/bin/env python3

#This source code is part of the MutaFlow project. It starts the instrumentation and parallelizes it.
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

import os
import sys
import multiprocessing
from threading import Thread
import time

ANDROIDJAR="SootClassPath/android.jar"
NUMBEROFTHREADS=3
USABLERAM=10*1024
APKFOLDER="apk"

def main(numThreads, maxRAM):
	counter = 0
	for (dirpath,dirname,filenames) in os.walk(APKFOLDER):
		for file in filenames:
			with open("times.txt","a") as times:
				counter += 1
				print("Running APK " + str(counter) + " with name " + str(file) +"\n")
				times.write(str(file)+"\n")
				ms = time.time()*1000.0
				runOnAPK(file, dirpath, numThreads, maxRAM)
				os.rename("SootOutput","instrout/"+str(file)[:-4])
				os.makedirs("SootOutput")
				timeTaken = (time.time() * 1000.0) - ms
				times.write(str(timeTaken) + "\n\n")
				print(str(timeTaken) + "\n")
			
					
def runOnAPK(file, dirpath, numThreads, maxRAM):
	if (APKFOLDER) in str(file):
		print(str(dirpath) + "/" + str(file))
		
		path = "logs/"+str(file)[:-4];
		try:
			os.makedirs(path)
		except:
			print()
		runString = "java -Xmx"+str(maxRAM)+"M -jar Instrumenter.jar -base ./ -apk " + str(dirpath) + "/" + str(file) + " -ajar " + ANDROIDJAR + " 2>&1 >> "+path+"/Original.txt"
		print(runString)
		os.system(runString)
		
		#now split sources and run in parallel
		#generate 2D-array with fixed size
		runCalls = [[] for i in range(numThreads)] #well... thats how you actually create a 2D list in python with fixed top level size..
		print("Runcalls: " + str(len(runCalls)))
		counter = 0;
		#each thread gets one processor more
		numProc = multiprocessing.cpu_count() // numThreads +1;
		dividedRAM = maxRAM // numThreads; #each thread only gets a portion of the available ram s.t. out tool finally has the same maximum amount of used ram
		with open("SootOutput/sourceInformation.txt","r") as result:
			for line in result.readlines():
				line = line[:-1] #remove linebreak at the end
				elements = line.split("\t")
				logName = elements[0] + "_" + elements[3] + "_" + elements[4] + "_" + elements[5] + "_" + elements[7] #-1 to get rid of line break
				call = runCalls[counter % numThreads]
				call.append("java -Xmx"+str(dividedRAM)+"M -jar Instrumenter.jar -base ./ -apk " + str(dirpath) + "/" + str(file) + " -ajar " + ANDROIDJAR + " -proc "+str(numProc)+" -source \""+line+"\" 2>&1 >> "+path+"/"+logName+".txt")
				counter += 1
		
		#start threads with the given bash commands
		threads = []
		counter = 0
		for runCall in runCalls:
			counter += 1
			run = Thread(target = threaded_run, args = (runCall,counter, ))
			threads.append(run)
			run.start()
		
		for thread in threads:
			thread.join()
				
def threaded_run(commands, ident):
	for cmd in commands:
		print("Thread "+str(ident)+": " + cmd)
		os.system(cmd)
	

if __name__ == "__main__":
	main(NUMBEROFTHREADS,USABLERAM)
