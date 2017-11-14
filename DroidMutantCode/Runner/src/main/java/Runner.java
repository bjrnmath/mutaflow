/**This source code is part of the MutaFlow project. It manages the execution of the instrumenter.
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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;

public class Runner {
	
	private static String pathToAAPT = "<Path-to-Android>/Android/sdk/build-tools/23.0.1";
	private static String base = ".";
	
	public static void main(String[] args) throws IOException, InterruptedException {
		File instrout = new File(base+"/instrout/");
		File times = new File(base+"/ResultsTimes.txt");
		if (!times.exists()) {
			times.createNewFile();
		}
		PrintWriter pw = new PrintWriter(times);
		for (File folder: instrout.listFiles()) {
			if (!folder.isDirectory()) {
				continue;
			}
			File out = null;
			File err = null;
			for (File apkfolder : folder.listFiles()) {
				try {
					long millis = System.currentTimeMillis();
					if (apkfolder.toString().contains(".DS_Store")) {
						continue;
					}

					if (!apkfolder.isDirectory()) {
						continue;
					}
					
					System.out.println(apkfolder);
					for (File f : apkfolder.listFiles()) {
						try {
							if (f.getName().contains(".apk")) {
								System.out.println(f.getName());
								out = new File(base+"/ExecutionLogs/"+f.getParentFile().getName()+"_"+f.getName().replaceAll("\\((.*?)\\)", "")+"_out.txt");
								if (!out.exists()) {
									System.out.println(out);
									System.out.println(out.getName().length());
									out.createNewFile();
								}
								System.out.println(out);
								err = new File(base+"/ExecutionLogs/"+f.getParentFile().getName()+"_"+f.getName().replaceAll("\\((.*?)\\)", "")+"_err.txt");
								if (!err.exists()) {
									err.createNewFile();
								}
								//ProcessBuilder pb = new ProcessBuilder("sh","PrivacyAPK_appium_short.sh",foldername,f.getParentFile().getName(),f.getName().split(".apk")[0]);
								String script = "Execute_monkey.sh";
								String apkPackage = extractPackageName(f);
								ProcessBuilder pb = new ProcessBuilder("sh",script, apkPackage,"instrout" + "/"+ f.getParentFile().getParentFile().getName() + "/" +f.getParentFile().getName(), f.getName(), f.getParentFile().getName(), f.getName());
								System.out.println(pb.command());
								pb.directory(new File(base+"/"));
								pb.redirectError(err);
								pb.redirectOutput(out);
								Process proc = pb.start();
								proc.waitFor();
								
								pw.println(apkfolder.getName()+"_"+f.getName());
								pw.println(System.currentTimeMillis()-millis);
								System.out.println(System.currentTimeMillis()-millis);
								pw.println();
								pw.println();
								pw.flush();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		pw.flush();
		pw.close();

		File source = new File(base+"/AppOut");
		File target = new File(base+"/AppOutSave/"+folder.getName());
		target.mkdir();
		Files.move(source.toPath(),	target.toPath(), REPLACE_EXISTING);


		new File(base+"/AppOut").mkdirs();
		}
	}
	
	private static String extractPackageName(File pathToAPK) {
		Process aapt = null;
		String output = null;
		InputStream adbout = null;
		try {
			aapt = Runtime.getRuntime().exec(Runner.pathToAAPT +"/aapt dump badging "+pathToAPK);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			adbout = aapt.getInputStream();
			output= IOUtils.toString(adbout);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		output = output.split("'")[1];
//		System.out.println(output);
		return output;
	}
	

}
