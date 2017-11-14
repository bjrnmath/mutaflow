/**This source code is part of the MutaFlow project. It is the main class of the instrumenter.
Copyright (C) 2017  Björn Mathis

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
**/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class PrivacyAPK {

	public static String pathToBase = "."; // the base is in general where the
											// jar file lies in, if not it can
											// be changed here

	public static String androidJAR = pathToBase
			+ "/android-platforms-master/android-21/android.jar";
	public static String classpath = pathToBase
			+ "/SootClassPath/LogCaller.jar";
	
	//defines the number of processors to use
	private static int proc = Runtime.getRuntime().availableProcessors() + 2;
	
	//defnies the source to mutate. The string is by default the "original" source, so not mutation but only tracking of values.
	private static String source = "0\tOriginal\tOriginal\t0\t0\t0\tnull\tnull";

	public static String apk = null;
	public static String SINKS = null;
	public static String SOURCES = null;
	public static String ANDROIDCLASSES = null;

	public static LinkedList<SourceOrSink> availableSources = new LinkedList<SourceOrSink>(); // used
																			// to
																			// make
																			// sure
																			// that
																			// in
																			// the
																			// end
																			// only
																			// apks
																			// are
																			// generated
																			// which
																			// really
																			// contain
																			// the
																			// source
	public static ReentrantLock availableSourcesLock = new ReentrantLock();
	public static String uID = null;

	//create LogCaller Object
	public final static String CREATELOGCALLER = "<call.LogCaller: call.LogCaller createCaller()>";
	
	// different logwriter methods
	public final static String LOGWRITEOBJECT = "<call.LogCaller: void writeToLogObject(java.lang.Object)>";
	public final static String LOGWRITEINT = "<call.LogCaller: void writeToLogint(int)>";
	public final static String LOGWRITEDOUBLE = "<call.LogCaller: void writeToLogdouble(double)>";
	public final static String LOGWRITEFLOAT = "<call.LogCaller: void writeToLogfloat(float)>";
	public final static String LOGWRITECHAR = "<call.LogCaller: void writeToLogchar(char)>";
	public final static String LOGWRITEBOOLEAN = "<call.LogCaller: void writeToLogboolean(boolean)>";
	public final static String LOGWRITEBYTE = "<call.LogCaller: void writeToLogbyte(byte)>";
	public final static String LOGWRITESHORT = "<call.LogCaller: void writeToLogshort(short)>";
	public final static String LOGWRITELONG = "<call.LogCaller: void writeToLoglong(long)>";
	public final static String LOGWRITEMETHODNAME = "<call.LogCaller: void writeToLogmethodname(java.lang.Object)>";
	public final static String LOGWRITEAPKNAME = "<call.LogCaller: void writeToLogAPKName(java.lang.Object)>";
	public final static String LOGWRITEARRAY = "<call.LogCaller: void writeToLogArray(java.lang.Object)>";
	public final static String WRITETOLOG = "<call.LogCaller: void writeToLog()>";

	// calls for methods which mutate source values
	public final static String MUTATEINT = "<call.LogCaller: int mutateInt(int)>";
	public final static String MUTATEDOUBLE = "<call.LogCaller: double mutateDouble(double)>";
	public final static String MUTATEFLOAT = "<call.LogCaller: float mutateFloat(float)>";
	public final static String MUTATECHAR = "<call.LogCaller: char mutateChar(char)>";
	public final static String MUTATEBOOLEAN = "<call.LogCaller: boolean mutateBoolean(boolean)>";
	public final static String MUTATEBYTE = "<call.LogCaller: byte mutateByte(byte)>";
	public final static String MUTATESHORT = "<call.LogCaller: short mutatesShort(short)>";
	public final static String MUTATELONG = "<call.LogCaller: long mutateLong(long)>";
	public final static String MUTATESTRING = "<call.LogCaller: java.lang.String mutateString(java.lang.String)>";

	/**
	 * Provides all needed information to soot.
	 */
	public static void initialiseSoot() {

		// prefer Android APK files// -src-prec apk
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_force_overwrite(true);

		// output as APK, too//-f J
		Options.v().set_output_format(Options.output_format_dex);

		Options.v().set_force_android_jar(androidJAR);
		Options.v().set_android_jars(androidJAR);
		Options.v().set_allow_phantom_refs(true);
		List<String> procDir = new ArrayList<String>();
		procDir.add(apk);
		Options.v().set_process_dir(procDir);
		Options.v().set_soot_classpath(classpath);

		Scene.v().addBasicClass("android.util.Log", SootClass.SIGNATURES);
		Scene.v().addBasicClass("call.LogCaller", SootClass.BODIES);

		Scene.v().loadNecessaryClasses();

		Scene.v().getSootClass("call.LogCaller").setApplicationClass();
		
	}

	/**
	 * Collects information, sets everything up and starts the instrumentation.
	 * 
	 * @param args
	 *            : Command line arguments
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// parse here command line input
		// -apk for path to apk, -ajar path to android jar
		int argcounter = 0;
		try {
			while (argcounter < args.length) {
				switch (args[argcounter]) {
				case ("-apk"): {
					PrivacyAPK.apk = args[argcounter + 1];
					break;
				}
				case ("-ajar"): {
					PrivacyAPK.androidJAR = args[argcounter + 1];
					break;
				}
				case ("-base"): {
					PrivacyAPK.pathToBase = args[argcounter + 1];
					break;
				}
				case ("-numProc"): {
					PrivacyAPK.proc = Integer.parseInt(args[argcounter + 1]);
					break;
				}
				case ("-source"): {
					PrivacyAPK.source = args[argcounter + 1];
					break;
				}
				}
				argcounter++;
			}
			if ((PrivacyAPK.apk == null)) {
				throw new ArrayIndexOutOfBoundsException(); // will be caught
															// and correctly
															// outputted by
															// outer try-catch
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException(
					"At least one path is not given.");
		}

		PrivacyAPK.classpath = pathToBase + "SootClassPath/LogCaller.jar";
		PrivacyAPK.SINKS = pathToBase + "SuSi/Sinks";
		PrivacyAPK.SOURCES = pathToBase + "SuSi/Sources";
		PrivacyAPK.ANDROIDCLASSES = pathToBase + "SuSi/androidClasses"; // those
																		// classes
																		// will
																		// not
																		// be
																		// taken
																		// into
																		// account
																		// when
																		// instrumenting

		// init sinks
		FileReader f = null;
		try {
			f = new FileReader(SINKS);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		BufferedReader br = new BufferedReader(f);
		String s = null;
		HashSet<String> lsinks = new HashSet<String>();
		try {
			while ((s = br.readLine()) != null) {
				String str = PrivacyAPK.splitSourceorSink(s);
				if (str == null) {
					continue;
				}
				lsinks.add(str);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// init sources
		try {
			f = new FileReader(SOURCES);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		br = new BufferedReader(f);
		s = null;
		HashSet<String> lsources = new HashSet<String>();
		try {
			while ((s = br.readLine()) != null) {
				String str = PrivacyAPK.splitSourceorSink(s);
				if (str == null) {
					continue;
				}
				lsources.add(str);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// init android classes which will later not be taken into account
		try {
			f = new FileReader(ANDROIDCLASSES);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		br = new BufferedReader(f);
		s = null;
		HashSet<String> landroidClasses = new HashSet<String>();
		try {
			while ((s = br.readLine()) != null) {
				landroidClasses.add(s);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// init soot and then run the instrumentation for the base and every
		// mutation
		runInstrumentation(lsinks, lsources, landroidClasses);
	}

	/**
	 * Method to run the full instrumentation.
	 * 
	 * @param lsinks
	 *            : List of sinks to take care of.
	 * @param lsources
	 *            : List of sources to take care of.
	 * @param landroidClasses
	 *            : List of classes to exclude from the instrumentation.
	 * @throws IOException 
	 */
	private static void runInstrumentation(HashSet<String> lsinks,
			HashSet<String> lsources, HashSet<String> landroidClasses) throws IOException {

		
		HashSet<String> mut = null;
		
		String[] sourceInfo = PrivacyAPK.source.split("\t");
		int javaLine = Integer.parseInt(sourceInfo[3]);
		int classLine = Integer.parseInt(sourceInfo[4]);
		int methodLine = Integer.parseInt(sourceInfo[5]);
		SourceOrSink source = new SourceOrSink(sourceInfo[1], sourceInfo[2], javaLine, classLine, methodLine, sourceInfo[6], sourceInfo[7]);
		
		

		// for every mutation run soot
		System.out.println(PrivacyAPK.availableSources);
		mut = new HashSet<String>();
		// check if this source has to be taken into account (so if it
		// appears in the apk) -> information comes from the first run when
		// creating the reference file (original)
		mut.add(source.getName());
		
		soot.G.reset();
		soot.G.v().reset(); // method access has to be done this way
		initialiseSoot();
		// soot has to be initialized again after each run

		String[] nargs = { "-d", pathToBase + "SootOutput/" + sourceInfo[0] + "_" + source.fileName(),"-validate" };
		uID = UUID.randomUUID().toString();
		// number of threads can be downscaled to 1 -> like non concurrent
		// version of app
		int numberOfThreads = proc;

		// setup classes to take care of when instrumenting (i.e. all not
		// android api classes)
		LinkedList<SootClass> classtmp = new LinkedList<SootClass>();
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (!(landroidClasses.contains(sc.toString()))) {
				classtmp.add(sc);
			}
		}
		SootClass[] classes = new SootClass[classtmp.size()]; // use array
																// for
																// faster
																// access in
																// concurrency
		int counter = -1;
		for (SootClass sc : classtmp) {
			counter++;
			classes[counter] = sc;
		}
		
		//only track sources for the original file, after that only log the value of the mutated source
		if (!source.getName().equals("Original")) {
			lsources.clear();
		}
		ConcurrencyHelper.setup(lsinks, lsources, mut, classes, source);
		ConcurrencyHelper[] ch = new ConcurrencyHelper[numberOfThreads];
		Thread[] threads = new Thread[numberOfThreads];
		for (int x = 0; x < numberOfThreads; x++) {
			ch[x] = new ConcurrencyHelper(x, numberOfThreads);
			threads[x] = new Thread(ch[x]);
			threads[x].start();
		}

		for (int x = 0; x < numberOfThreads; x++) {
			try {
				threads[x].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		soot.Main.main(nargs);
		
		//create OriginalRef APK by copying the Original Folder
		//therefore later we do not need to differ between the executions
		if (source.getName().equals("Original")) {
			File original = new File(pathToBase + "SootOutput/0_Original");
			File originalRef = new File(pathToBase + "SootOutput/0_OriginalRef");
			if (original.exists()) {
				FileUtils.copyDirectory(original, originalRef);
			}
			
			//write out found sources
			PrintWriter sourceInformation = new PrintWriter(new File(pathToBase + "SootOutput/sourceInformation.txt"));
			int sourceCounter = -1;  //generates a unique ID to avoid possible file name clashes
			for (SourceOrSink src : PrivacyAPK.availableSources) {
				sourceCounter++;
				sourceInformation.write(sourceCounter + "\t" + src.toString() + "\n");
			}
			sourceInformation.close();
		}
	}

	/**
	 * Caller for deletion of files in folder.
	 * 
	 * @param dir
	 *            : Folder in which the files will be deleted.
	 */
	private static void calldel(File dir) {
		String[] entries = dir.list();
		for (int x = 0; x < entries.length; x++) {
			File aktFile = new File(dir.getPath(), entries[x]);
			del(aktFile);
		}
	}

	/**
	 * Deletion of files in folder.
	 * 
	 * @param dir
	 */
	public static void del(File dir) {
		if (dir.isDirectory()) {
			String[] entries = dir.list();
			for (int x = 0; x < entries.length; x++) {
				File aktFile = new File(dir.getPath(), entries[x]);
				del(aktFile);
			}
			dir.delete();
		} else {
			dir.delete();
		}
	}

	/**
	 * Extracts a source or sink from a line in SuSi-Format.
	 * 
	 * @param s
	 *            : Line which contains a source or sink.
	 * @return: The name of the source or sink.
	 */
	public static String splitSourceorSink(String s) {
		if (!s.contains(">")) {
			return null;
		}
		String[] temp = s.split(">");
		String result = "";
		for (int x = 0; x < temp.length - 2; x++) {
			result += temp[x] + ">";
		}
		return result;
	}

}
