/**This source code is part of the MutaFlow project. It performs the flow extraction after the parser parsed all logs.
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
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;


public class FlowExtractor {

	private static String BASEFOLDER;
	private static String current = null;
	private static String currentAPK = null;
	
	private static int counter = 0;
	private static int refoundCounter = 0;
	
	private static final boolean notCondensed = true;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			throw new IllegalStateException("You need to define the base folder as first argument.");
		}
		BASEFOLDER = args[0];
		HashMap<String, HashSet<String>> sources = new HashMap<String,HashSet<String>>();
		HashMap<String, HashSet<String>> sinks = new HashMap<String,HashSet<String>>();
		HashMap<String,HashMap<String,Integer>> flowocc = new HashMap<String,HashMap<String,Integer>>(); //shows how often a flow occured over all files
		HashMap<String,Integer> sinkCounter = new HashMap<String,Integer>(); //counts how often a sink occurred over all files in one APK
		HashMap<String,HashMap<String,String>> flowPosition = new HashMap<String,HashMap<String,String>>();  //reports the position of the flow, so where it occurred
		HashSet<String> noSourceButSink = new HashSet<String>();  //shows whether a sink occurred also for a file where no sources occurred
		HashSet<String> allSinksOfAPK = new HashSet<String>();  //contains in the end all sinks that occured for one APK
		
		//total over all apk's
		HashMap<String,Integer> sinkCounterAll = new HashMap<String,Integer>(); //counts how often a sink occurred over all apk's
		HashMap<String,Integer> sinkCounterNoSourceAll = new HashMap<String,Integer>();  //counts how often a sink occurred without a source over all apk's
		
		//read crash reports
		HashMap<String,HashMap<String,Boolean>> crashReports = new HashMap<String,HashMap<String,Boolean>>();  //if true then a crash occurred in the file

		File resultFolder = new File(BASEFOLDER+"/AppOutSave");

		PrintWriter oneFoundWriter = null;
		try {
			oneFoundWriter = new PrintWriter(BASEFOLDER + "/FoundFlows.csv", "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}


		oneFoundWriter.write("Source\tAPI-Methodname\tLine in Code\tLine in Class (Jimple)\tLine in Method (Jimple)\tSurrounding Method Name\tSurrounding Class Name\tSink\tAPI-Methodname\tLine in Code\tLine in Class (Jimple)\tLine in Method (Jimple)\tSurrounding Method Name\tSurrounding Class Name\tOccured x times in all files\tnumber of files\tsink occurred without mutation\tnumber of flies where no mutation occured\n");
		
		int fileCounter = 0;
		int noMutationCounter = 0;
		for (File f : resultFolder.listFiles()) {
			HashMap<String, HashSet<String>> refoundMap = new HashMap<String, HashSet<String>>();
			if (!f.isDirectory()) {
				continue;
			}
			noMutationCounter = 0;
			System.out.println(f.getName()+"\n");
			
			FlowExtractor.currentAPK = f.getName().replace("_", "");
			if (notCondensed) {
				oneFoundWriter.write("\n\n"+f.getName()+"\n");
			}
			fileCounter = 0;
			for (File apkFolder : f.listFiles()) {
				if (!apkFolder.getName().contains("simple_rich.txt")) {
					continue;
				}
				fileCounter++;
				BufferedReader br = new BufferedReader(new FileReader(apkFolder));
				
				String line = null;
				boolean sink = false;
				while((line = br.readLine()) != null) {
					if (line.startsWith("Sink")) {
						sink = true;
						continue;
					}
					
					if (line.equals("") || (line.startsWith("Source"))) {
						continue;
					}
					
					if (sink) {
						addLineToMap(line,sinks);
						addMethodToSet(line, allSinksOfAPK);
					} else {
						addLineToMap(line,sources);
					}
				}
				
//				System.out.println(sources);
//				System.out.println(sinks);
//				System.exit(0);
				
				if (sources.isEmpty()) {
					noMutationCounter++;
				}

				sourceValueRefound(sinks,sources, refoundMap);
				current = apkFolder.getName();
				occurenceAdder(sources, sinks, noSourceButSink, sinkCounter);
				occurenceAdderMutation(sinks, sources, flowocc, flowPosition, apkFolder.getName());
				occurenceAdderSink(sinks, sources, sinkCounterAll, sinkCounterNoSourceAll);
				sinks.clear();
				sources.clear();
				br.close();
			}
			
			//writes the number of found flows in the "SusiAnalysisMutationFound" file
			occurrenceAnalyzer(flowocc, sinkCounter,noSourceButSink, fileCounter, noMutationCounter, oneFoundWriter, flowPosition, crashReports, refoundMap);
			
			flowocc.clear();
			flowPosition.clear();
			sinkCounter.clear();
			noSourceButSink.clear();
			allSinksOfAPK.clear();
		}

		oneFoundWriter.flush();
		oneFoundWriter.close();
		System.out.println(counter);
		System.out.println("Number of flows with a refound value in the sink: "+refoundCounter);
	}


	/**
	 * Reports Flows where the sink did not occur for all mutated files. It also reports how often a sink occurred and whether it occurred for files where no source was mutated.
	 * @param flowocc
	 * @param sinkCounter
	 * @param occurredForNoSource
	 * @param fileCounter
	 * @param writer
	 * @param flowPosition 
	 * @param crashReports 
	 */
	private static void occurrenceAnalyzer(HashMap<String, HashMap<String, Integer>> flowocc, HashMap<String, Integer> sinkCounter, HashSet<String> occurredForNoSource,float fileCounter, int noMutationCounter, 
			PrintWriter writer, HashMap<String, HashMap<String, String>> flowPosition, HashMap<String, HashMap<String, Boolean>> crashReports, HashMap<String, HashSet<String>> refoundFlows) {
		if (notCondensed) {
			writer.write("\nDroidMutant found the following flows. Such without a number indicate flows, where a value was again found in the sink: \n");
		}
		for (Entry<String, HashSet<String>> ent : refoundFlows.entrySet()) {
			for (String sink : ent.getValue()) {
				writer.write(ent.getKey()+"\t"+sink+"\n");
				refoundCounter++;
			}
		}
		
		for (Entry<String,HashMap<String, Integer>> sourceEntry : flowocc.entrySet()) {
			for (Entry<String,Integer> sinkEntry : sourceEntry.getValue().entrySet()) {
				if (!sinkCounter.get(sinkEntry.getKey()).equals(fileCounter)) {  //if sink did not occur for all mutated files
					float sinkCount = sinkCounter.get(sinkEntry.getKey());
					if (!occurredForNoSource.contains(sinkEntry.getKey())) {
						if ((!refoundFlows.containsKey(sourceEntry.getKey())) || (!refoundFlows.get(sourceEntry.getKey()).contains(sinkEntry.getKey()))) {
							writer.write(sourceEntry.getKey()+"\t"+sinkEntry.getKey()+"\t"+sinkCount+"\t"+fileCounter + "\t" + (occurredForNoSource.contains(sinkEntry.getKey()) ? 0 : 1)+"\t"+noMutationCounter+ "\n");
						}
						HashMap<String, Boolean> currentAPKMap = crashReports.get(FlowExtractor.currentAPK);

						//crashes with NPE if Converter was not run firstly
						boolean crashed = false;

						
						System.out.println(flowPosition.get(sourceEntry.getKey()).get(sinkEntry.getKey()) +" " + sourceEntry.getKey() + " " + sinkEntry.getKey() + " " + crashed );
					}
				}
			}
		}
	}
	
	/**
	 * Adds how often a sink occurred over all files and also checks whether a sink occurred for a file where no mutations happened.
	 * @param sources
	 * @param sinks
	 * @param occurredForNoSource
	 * @param sinkCounter
	 */
	private static void occurenceAdder(HashMap<String, HashSet<String>> sources, HashMap<String, HashSet<String>> sinks, HashSet<String> occurredForNoSource, HashMap<String, Integer> sinkCounter) {
		for (String sink : sinks.keySet()) {
			if (sinkCounter.containsKey(sink)) {
				sinkCounter.put(sink, sinkCounter.get(sink)+1);
			} else {
				sinkCounter.put(sink, 1);
			}
			
			if (sources.isEmpty()) {
				occurredForNoSource.add(sink);
			}
		}
	}


	/**
	 * Reports how often a flow occurred (flowocc) and where a flow occurred (flowposition). Flows that occurred more than once will not be reported, so the flowposition is later on unambigous.
	 * @param sinks
	 * @param sources
	 * @param flowocc
	 * @param flowPosition
	 * @param currentFile
	 */
	private static void occurenceAdderMutation(HashMap<String, HashSet<String>> sinks, HashMap<String, HashSet<String>> sources, HashMap<String, HashMap<String, Integer>> flowocc, HashMap<String, HashMap<String, String>> flowPosition, String currentFile) {
		for (String source : sources.keySet()) {
			for (String sink : sinks.keySet()) {
				if (flowocc.containsKey(source)) {
					if (flowocc.get(source).containsKey(sink)) {
						Integer i = flowocc.get(source).get(sink);
						flowocc.get(source).remove(sink);
						flowocc.get(source).put(sink,i++);
					} else {
						flowocc.get(source).put(sink, 0);
						flowPosition.get(source).put(sink, currentFile);
					}
				} else {
					HashMap<String,Integer> map = new HashMap<String,Integer>();
					map.put(sink, 0);
					flowocc.put(source, map);
					
					HashMap<String,String> posMap = new HashMap<String,String>();
					posMap.put(sink, currentFile);
					flowPosition.put(source, posMap);
				}
			}
		}
	}
	
	/**
	 * Reports how often a sink occurred and how often it occurred without a source
	 * @param sinks
	 * @param sources
	 */
	private static void occurenceAdderSink(HashMap<String, HashSet<String>> sinks, HashMap<String, HashSet<String>> sources, HashMap<String, Integer> sinksOccurred, HashMap<String, Integer> sinksOccurredNoSource) {
		for (String sink : sinks.keySet()) {
//			System.out.println(sink);
			if (sinksOccurred.containsKey(sink)) {
				sinksOccurred.put(sink, sinksOccurred.get(sink)+1);
			} else {
				sinksOccurred.put(sink, 1);
				sinksOccurredNoSource.put(sink,0);
			}
			
			if (sources.isEmpty()) {
				sinksOccurredNoSource.put(sink, sinksOccurredNoSource.get(sink)+1);
			}
		}
	}

	/**
	 * Checks whether a value from a source can be refound in a sink. I yes the respective data is written out.
	 * @param sinks
	 * @param sources
	 */
	private static void sourceValueRefound(HashMap<String, HashSet<String>> sinks, HashMap<String, HashSet<String>> sources, HashMap<String,HashSet<String>> flowsForRefound) {
		for (Entry<String, HashSet<String>> sourceEntry : sources.entrySet()) {
			for (String sourceString : sourceEntry.getValue()) {
				for (Entry<String, HashSet<String>> sinkEntry : sinks.entrySet()) {
					for(String sinkString : sinkEntry.getValue()) {
						if (sinkString.contains(sourceString)) {
							if ((sourceString.length() < 5) || (sourceString.equals("false")) || (sourceString.equals("true"))) { //false, true and values with less than 5 characters are not specific enough, there is a high chance that they are refound
								continue;
							}
							HashSet<String> sinksSet = flowsForRefound.containsKey(sourceEntry.getKey()) ? flowsForRefound.get(sourceEntry.getKey()) : new HashSet<String>();
							if(!flowsForRefound.containsKey(sourceEntry.getKey())) {
								flowsForRefound.put(sourceEntry.getKey(), sinksSet);
							}
							sinksSet.add(sinkEntry.getKey());
							counter++;
						}
					}
				}
			}
		}
	}

	/**
	 * Adds sources or sinks to the given hashmap and also adds the belonging values to the respective map. This is done by remembering the current method.
	 * @param line
	 * @param hashMap
	 */
	private static void addLineToMap(String line, HashMap<String, HashSet<String>> hashMap) {
		String[] values = line.split("<DroidMutantTag>");
		hashMap.put(values[0], new HashSet<String>());
		current = values[0];
		for (String value : values) {
			hashMap.get(current).add(value);
		}
	}
	
	/**
	 * Extracts the method from the line and adds it to the Set.
	 * @param line
	 */
	private static void addMethodToSet(String line, HashSet<String> hashSet) {
		String[] values = line.split("<DroidMutantTag>");
		hashSet.add(values[0]);
	}

}
