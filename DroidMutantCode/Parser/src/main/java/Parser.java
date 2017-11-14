/**This source code is part of the MutaFlow project. It parses the log files and creates a raw output of the flows it finds.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Parser {

	private static String appOut = "./AppOut";
	private static String pathSuSi = "./SuSi";
	// private static String outputFormat = "dot";
	private static HashMap<String, HashSet<String>> mvcMapOriginal = new HashMap<String, HashSet<String>>(); // first
																												// Set
																												// for
																												// original
																												// values
																												// without
																												// noise
	private static HashMap<String, HashSet<String>> mvcMapComp = new HashMap<String, HashSet<String>>(); // second
																											// set
																											// for
																											// comparing
																											// differences
																											// between
																											// two
																											// runs

	private static boolean addSink = false; // stores if the next value is added
											// to the sinks or source map
	
	private static String uID = null;
	
	private static File f = null;  //here the current file is saved
	
	private static int addCounter = 0; //shows how many method calls where added up to this point
	
	private static final int ADDCOUNTERTHRESHOLD = 30;
	
	private static String ignoreUID = ""; //it happened that logcat printed a specific line for the rest of the evaluation. Such a line can then be ignored by the parser.

	/**
	 * Reads all data, packs it into HashMaps and produces intersections and
	 * differences. Also creates the output files.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		for (int x = 0; x<args.length; x++) {
			if (args[x].equals("-ignore")) {
				ignoreUID = args[x+1];
			}
		}

		// init sinks
		FileReader fr = null;
		try {
			fr = new FileReader(Parser.pathSuSi + "/Sinks");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		BufferedReader bur = new BufferedReader(fr);
		String ssusi = null;
		HashSet<String> lsinks = new HashSet<String>();
		try {
			while ((ssusi = bur.readLine()) != null) {
				lsinks.add(splitSourceorSink(ssusi));
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// init sources
		try {
			fr = new FileReader(Parser.pathSuSi + "/Sources");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		bur = new BufferedReader(fr);
		ssusi = null;
		HashSet<String> lsources = new HashSet<String>();
		try {
			while ((ssusi = bur.readLine()) != null) {
				lsources.add(splitSourceorSink(ssusi));
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		File[] files = (new File(Parser.appOut)).listFiles();
		boolean original = true;
		// bring original and original reference to the beginning for noise
		// reduction
		for (int x = 0; x < files.length; x++) {
			if (files[x].getName().contains("0_Original") && !files[x].getName().contains("0_OriginalRef")) {
				File temp = files[0];
				files[0] = files[x];
				files[x] = temp;
				continue;
			}
			if (files[x].getName().contains("0_OriginalRef")) {
				File temp = files[1];
				files[1] = files[x];
				files[x] = temp;
			}
		}

		// make reference map (the map with the noise reduced original run
		for (int x = 0; x < 2; x++) {
			addCounter = 0;
			//reset the uID
			uID = null;
			
			f = files[x];
			
//			System.out.println(f);
			if (!f.toString().endsWith(".pap")) { // take only files my program
													// outputted
				continue;
			}
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(f));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			String s = null;
			try {
				while ((s = br.readLine()) != null) {
					if (!s.contains(":")) { // skip everything with no useful
											// content
						continue;
					}
					if (original) { // fill Maps
						Parser.addValueOrMethod(s.replaceFirst(".*MyOwnTag: ", ""),
								Parser.mvcMapOriginal);
					} else {
						Parser.addValueOrMethod(s.replaceFirst(".*MyOwnTag: ", ""),
								Parser.mvcMapComp);
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			original = false; // first file is always original run file, all
								// others not
		}

		// create debuginfo file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(Parser.appOut + "/debuginfo.txt", "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		writer.write("Original\n");
		writer.write("{\n");
		for (String tests : Parser.mvcMapOriginal.keySet()) {
			writer.write(tests + "<DroidMutantTag>");
			for (String str : Parser.mvcMapOriginal.get(tests)) {
				writer.write(str + "<DroidMutantTag>");
			}
			writer.write("\n");
		}
		writer.write("}");
		writer.write("Comp\n");
		writer.write("{\n");
		for (String tests : Parser.mvcMapComp.keySet()) {
			writer.write(tests + "<DroidMutantTag>");
			for (String str : Parser.mvcMapComp.get(tests)) {
				writer.write(str + "<DroidMutantTag>");
			}
			writer.write("\n");
		}
		writer.write("}");
		writer.close();
		// after this method in Map1 the noise reduced methods and values of the
		// original run lies in

		Parser.makeIntersectionMap1(Parser.mvcMapOriginal, Parser.mvcMapComp);

		// end noise reduced map, Parser.mvcMapOriginal contains now map for
		// further computation

		ArrayList<HashSet<String>> dependencies = new ArrayList<HashSet<String>>();
		for (int x = 2; x < files.length; x++) {
			addCounter = 0;
			//reset the uID
			uID = null;
			
			f = files[x];
			if (!f.toString().endsWith(".pap")) { // take only files my program
													// outputted
				continue;
			}
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(f));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			String s = null;
			HashMap<String, HashSet<String>> sinks = new HashMap<String, HashSet<String>>();
			HashMap<String, HashSet<String>> sources = new HashMap<String, HashSet<String>>();
			try {
				while ((s = br.readLine()) != null) {
					if (!s.contains(":")) { // skip everything with no useful
											// content
						continue;
					}
					Parser.addValueOrMethod(s.replaceFirst(".*MyOwnTag: ", ""),
							sources, sinks, lsources, lsinks);
					// write new method to distinguish between source and sink
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// look which sources and sinks did newly occur
			makeDiffMap1(sinks, Parser.mvcMapOriginal);
			makeDiffMap1(sources, Parser.mvcMapOriginal);
			dependencies.add(Parser.computeDependencies(sources, sinks));

			try {
				writer = new PrintWriter(Parser.appOut + "/"
						+ f.getName().substring(0, f.getName().length() - 4)
						+ ".dot", "UTF-8");
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			writer.write("digraph PrivacyAPK \n");
			writer.write("{\n");
			for (HashSet<String> hs : dependencies) {
				for (String hss : hs) {
					writer.write(hss + ";\n");
				}
			}
			writer.write("}");
			writer.close();
			dependencies.clear();

			// output also simple format, will be changed later so one can
			// decide the format
			
			try {
				writer = new PrintWriter(Parser.appOut + "/"
						+ f.getName().substring(0, f.getName().length() - 4)
						+ "simple.txt", "UTF-8");
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			writer.write("Sources: \n");
			for (String hs : sources.keySet()) {
				writer.write(hs + "\n");
			}
			writer.write("\n\n\n\n\n\n\n\n\n\nSinks: \n");
			for (String hs : sinks.keySet()) {
				writer.write(hs + "\n");
			}
			writer.close();

			try {
				writer = new PrintWriter(Parser.appOut + "/"
						+ f.getName().substring(0, f.getName().length() - 4)
						+ "simple_rich.txt", "UTF-8");
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			writer.write("Sources: \n");
			for (String hs : sources.keySet()) {
				writer.write("\n"+hs + "<DroidMutantTag>");
				for (String line: sources.get(hs)) {
					writer.write(line + "<DroidMutantTag>");
				}
				writer.write("\n");
			}
			writer.write("\n\n\n\n\n\n\n\n\n\nSinks: \n");
			for (String hs : sinks.keySet()) {
				writer.write("\n"+hs + "<DroidMutantTag>");
				for (String line: sinks.get(hs)) {
					writer.write(line + "<DroidMutantTag>");
				}
				writer.write("\n");
			}
			writer.close();
		}
	}

	/**
	 * Makes intersection between maps, intersection of methods and then
	 * intersection of values for remaining methods.
	 * 
	 * @param Map1
	 *            : The map which will contain the intersection in the end.
	 * @param Map2
	 */
	private static void makeIntersectionMap1(
			HashMap<String, HashSet<String>> Map1,
			HashMap<String, HashSet<String>> Map2) {
		Set<String> keySet1 = new HashSet(Map1.keySet()); // conversion is type
															// safe
		Set<String> keySet2 = new HashSet(Map2.keySet()); // conversion is type
															// safe
		keySet1.removeAll(keySet2); // at this point both sets lost the
									// intersecting objects
		for (String s : keySet1) { // removes all methods which were not called
									// in both sets
			Map1.remove(s);
		} // Map1 now only has intersection methods

		keySet1 = Map1.keySet(); // get new keyset
		for (String s : keySet1) { // make intersection of method values, only
									// values which occured in both parts are
									// used
			HashSet<String> values1 = Map1.get(s);
			HashSet<String> values2 = Map2.get(s);

			if ((values1 == null) || (values2 == null)) { // values should not
															// be both null
															// since only the
															// intersection is
															// considered
				throw new Error("Values are not both not null.");
			}
			values1.retainAll(values2); // intersection is performed
		}
	}

	/**
	 * Reduces Map1 and its elements to the ones which did not occur in Map2.
	 * 
	 * @param Map1
	 * @param Map2
	 */
	private static void makeDiffMap1(HashMap<String, HashSet<String>> Map1,
			HashMap<String, HashSet<String>> Map2) {
		Set<String> keySet1 = new HashSet(Map1.keySet()); // conversion is type
															// safe
		Set<String> keySet2 = new HashSet(Map2.keySet()); // conversion is type
															// safe
		for (String s : keySet1) {
			HashSet<String> values1 = Map1.get(s);
			HashSet<String> values2 = null;
			try {
				values2 = (HashSet<String>) Map2.get(s).clone(); // conversion
																	// is type
																	// safe
			} catch (NullPointerException e) {
				continue; // here a method was found which is new and did not
							// occur in map1
			}

			if ((values1 == null) || (values2 == null)) { // values should not
															// be both null
															// since only the
															// intersection is
															// considered
				throw new Error("Values are not both not null.");
			}
			values1.removeAll(values2);
			if (values1.isEmpty()) {
				Map1.remove(s);
			}
		}
	}
	
	/**
	 * Adds a value or method to the given map.
	 * 
	 * @param s
	 *            : Value or method to add.
	 * @param map
	 *            : Map in which s will be added.
	 */
	private static void addValueOrMethod(String s,HashMap<String, HashSet<String>> map) {
		addCounter++;
		String[] values = valueAddCheckCorrectness(s);
		if (values == null) {
			return;
		}
		
		if (!values[1].equals(uID) && (addCounter < ADDCOUNTERTHRESHOLD)) {
			System.out.println("File: "+f.getName());
			System.err.println("The UUID was not correct, should be "+uID+" but was: "+s);
			System.err.println("Deleted old values and restarted the mapping.");
			System.err.println("There were "+addCounter+" method calls added until the wrong UUID appeared.");
			//clear map if the UID was wrong, then the entries where mixed up to this point. A restart if a safe way to overcome this problem.
			addCounter = 0;
			uID = values[1];
			map.clear();
		}
		String method = values[2];
//		System.out.println(Arrays.deepToString(values));
		if (!map.containsKey(method)) {
			map.put(method, new HashSet<String>());
		}
		HashSet<String> addHere = map.get(method);
		for (int x = 3; x<values.length; x++) {
			addHere.add(values[x]);
		}
	}
	
	/**
	 * Adds a value or a method to the sinks or sources map.
	 * 
	 * @param s
	 *            : Value or method
	 * @param mapSources
	 *            : Current map of sources, a source method or value will be
	 *            added here.
	 * @param mapSinks
	 *            : Current map of sources, a source method or value will be
	 *            added here.
	 * @param setSources
	 *            : Set of all sources under observation.
	 * @param setSinks
	 *            : Set of all sinks under observation.
	 */
	private static void addValueOrMethod(String s,HashMap<String, HashSet<String>> mapSources,HashMap<String, HashSet<String>> mapSinks,HashSet<String> setSources, HashSet<String> setSinks) {
		addCounter++;
		String[] values = valueAddCheckCorrectness(s);
		if (values == null) {
			return;
		}
		
		if (!values[1].equals(uID) && (addCounter < ADDCOUNTERTHRESHOLD)) {
			System.out.println("File: "+f.getName());
			System.err.println("The UID was not correct, should be "+uID+" but was: "+s);
			System.err.println("Deleted old values and restarted the mapping.");
			System.err.println("There were "+addCounter+" method calls added until the wrong UUID appeared.");
			
			addCounter = 0;
			uID = values[1];
			mapSources.clear();
			mapSinks.clear();
		}
		
		String method = values[2];
		//take only the method name to check whether it is a source or a sink, after that the full method reference is taken (with surrounding class, method, and so on)
		addSink = setSinks.contains(method.split("\t")[0]);
		if (addSink) {
			if (!mapSinks.containsKey(method)) {
				mapSinks.put(method, new HashSet<String>());
			}
		} else {
			if (!mapSources.containsKey(method)) {
				mapSources.put(method, new HashSet<String>());
			}
		}
		
		for (int x = 3; x < values.length; x++) {
			if (addSink) {
				HashSet<String> addHere = mapSinks.get(method);
				if (addHere != null) { // if null then the method was not
										// recognized
										// already
					addHere.add(values[x]);
				}
			} else {
				HashSet<String> addHere = mapSources.get(method);
				if (addHere != null) { // if null then the method was not
										// recognized
										// already
					addHere.add(values[x]);
				}
			}
		}
	}

	/**
	 * Checks if the respective String really contains a correct line, i.e. if a DroidMutantTag is contained and something like a UID is contained (i.e. if the value at the position where the UID should be has the same regular expression as a Java UID). 
	 * If not it returns null. If yes the UID is set if it was not already done and the splitted String is returned.
	 * @param s
	 * @return
	 */
	private static String[] valueAddCheckCorrectness(String s) {
		if (!s.contains("<DroidMutantTag>")) {
			return null;
		}
		String[] values = s.split("<DroidMutantTag>");
		
		if (!values[1].matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
			return null;
		}
		
		if (values[1].equals(ignoreUID)) {
			return null;
		}
		
		if (uID == null) {
			uID = values[1]; 
		}
		return values;
	}

	/**
	 * Element1 key of Map1 and Element2 key of Map2, then the following String
	 * is added to result: "Element1 -> Element2"
	 * 
	 * @param Map1
	 * @param Map2
	 * @return As described
	 */
	private static HashSet<String> computeDependencies(
			HashMap<String, HashSet<String>> Map1,
			HashMap<String, HashSet<String>> Map2) {
		HashSet<String> result = new HashSet<String>();
		for (String so : Map1.keySet()) {
			for (String si : Map2.keySet()) {
				result.add(so + " -> " + si);
			}
		}
		return result;
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