/**This source code is part of the MutaFlow project. This code gets injected in the program-under-test and creates the log.
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

package call;

import java.util.Arrays;
import java.util.Random;

import android.util.Log;

public class LogCaller {

	public static void main(String[] args) {
		System.out.println("The Main does nothing");
		return;
	}

	private String toLog = "";
	
	public static LogCaller createCaller() {
		return new LogCaller();
	}
	
	/**
	 * Writes APK name to log
	 */
	public void writeToLogAPKName(Object O) {
		if (O != null) {
			toLog +="<DroidMutantTag>"+ O.toString();
		} else {
			toLog +="<DroidMutantTag>"+ "No APKName given, object was null.";
		}
	}

	/**
	 * Writes Methodname to log
	 * 
	 * @param O
	 *            : Methodname
	 */
	public void writeToLogmethodname(Object O) {
		if (O != null) {
			toLog +="<DroidMutantTag>"+ O.toString();
		} else {
			toLog +="<DroidMutantTag>"+ "No methodname given, object was null.";
		}
	}

	/**
	 * Writes Object to log
	 * 
	 * @param O
	 *            : Object
	 */
	public void writeToLogObject(Object O) {
		if (O != null) {
			toLog +="<DroidMutantTag>"+ O.toString();
		}
	}

	/**
	 * Writes Integer to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLogint(int basicType) {

		toLog +="<DroidMutantTag>"+ Integer.valueOf(basicType).toString();

	}

	/**
	 * Writes Double to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLogdouble(double basicType) {

		toLog +="<DroidMutantTag>"+ Double.valueOf(basicType).toString();

	}

	/**
	 * Writes Float to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLogfloat(float basicType) {

		toLog +="<DroidMutantTag>"+ Float.valueOf(basicType).toString();

	}

	/**
	 * Writes Char to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLogchar(char basicType) {

		toLog +="<DroidMutantTag>"+ Character.valueOf(basicType).toString();

	}

	/**
	 * Writes Boolean to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLogboolean(boolean basicType) {

		toLog +="<DroidMutantTag>"+ Boolean.valueOf(basicType).toString();

	}

	/**
	 * Writes Byte to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLogbyte(byte basicType) {

		toLog +="<DroidMutantTag>"+ Byte.valueOf(basicType).toString();

	}

	/**
	 * Writes Short to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLogshort(short basicType) {

		toLog +="<DroidMutantTag>"+ Short.valueOf(basicType).toString();

	}

	/**
	 * Writes Long to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLoglong(long basicType) {
		toLog +="<DroidMutantTag>"+ Long.valueOf(basicType).toString();

	}
	
	/**
	 * Writes finally to log
	 * 
	 * @param basicType
	 *            : value
	 */
	public void writeToLog() {
		Log.i("MyOwnTag", this.toLog);

	}
	
	// ----------------------writer for Arrays

		/**
		 * Writes value of Array into log
		 * 
		 * @param O
		 *            : Array
		 */
		public void writeToLogArray(Object O) {
			if (O == null) {
				this.toLog += "<DroidMutantTag>null";
				return;
			}
			switch (O.getClass().getCanonicalName()) {
			case ("int[]"): {
				writeToLogconcreteArray((int[]) O);
				break;
			}
			case ("double[]"): {
				writeToLogconcreteArray((double[]) O);
				break;
			}
			case ("float[]"): {
				writeToLogconcreteArray((float[]) O);
				break;
			}
			case ("char[]"): {
				writeToLogconcreteArray((char[]) O);
				break;
			}
			case ("boolean[]"): {
				writeToLogconcreteArray((boolean[]) O);
				break;
			}
			case ("byte[]"): {
				writeToLogconcreteArray((byte[]) O);
				break;
			}
			case ("short[]"): {
				writeToLogconcreteArray((short[]) O);
				break;
			}
			case ("long[]"): {
				writeToLogconcreteArray((long[]) O);
				break;
			}
			default: {
				writeToLogconcreteArray((Object[]) O);
			}
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Object Array
		 */
		private void writeToLogconcreteArray(Object[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.deepToString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Integer Array
		 */
		private void writeToLogconcreteArray(int[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Double Array
		 */
		private void writeToLogconcreteArray(double[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Float Array
		 */
		private void writeToLogconcreteArray(float[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Char Array
		 */
		private void writeToLogconcreteArray(char[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Boolean Array
		 */
		private void writeToLogconcreteArray(boolean[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Byte Array
		 */
		private void writeToLogconcreteArray(byte[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Short Array
		 */
		private void writeToLogconcreteArray(short[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

		/**
		 * Takes care of concrete Array type when writing to log
		 * 
		 * @param O
		 *            : Long Array
		 */
		private void writeToLogconcreteArray(long[] O) {
			if (O != null) {
				toLog +="<DroidMutantTag>"+ Arrays.toString(O);
			}
		}

	// ------------------mutationcalls-----------------------------------------------------------------
	/**
	 * Mutates value
	 * 
	 * @param i
	 *            : Integer to mutate
	 * @return Mutated value
	 */
	public static int mutateInt(int i) {
		Random rnd = new Random();
		return rnd.nextInt();
	}

	/**
	 * Mutates value
	 * 
	 * @param d
	 *            : Double to mutate
	 * @return Mutated value
	 */
	public static double mutateDouble(double d) {
		Random rnd = new Random();
		return rnd.nextDouble();
	}

	/**
	 * Mutates value
	 * 
	 * @param f
	 *            : Float to mutate
	 * @return Mutated value
	 */
	public static float mutateFloat(float f) {
		Random rnd = new Random();
		return rnd.nextFloat();
	}

	/**
	 * Mutates value
	 * 
	 * @param c
	 *            : Char to mutate
	 * @return Mutated value
	 */
	public static char mutateChar(char c) {
		String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rnd = new Random();
		if (Character.isDigit(c)) {
			alphabet = "0123456789";
		} else {
			alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		}
		int randomNumber = rnd.nextInt(alphabet.length());
		if (alphabet.charAt(randomNumber) == c) { // check if the character is
													// the same, if yes change
													// to the next or the
													// previous character
			if (randomNumber + 1 == alphabet.length()) {
				randomNumber--;
			} else {
				randomNumber++;
			}
		}
		return alphabet.charAt(randomNumber);
	}

	/**
	 * Mutates value
	 * 
	 * @param b
	 *            : Boolean to mutate
	 * @return Mutated value
	 */
	public static boolean mutateBoolean(boolean b) {
		Random rnd = new Random();
		return rnd.nextBoolean();
	}

	/**
	 * Mutates value
	 * 
	 * @param by
	 *            : Byte to mutate
	 * @return Mutated value
	 */
	public static byte mutateByte(byte by) {
		Random rnd = new Random();
		byte[] b = { 0, 1, 2, 3, 4, 5, 6, 7 };
		rnd.nextBytes(b);
		return b[0];
	}

	/**
	 * Mutates value
	 * 
	 * @param s
	 *            : Short to mutate
	 * @return Mutated value
	 */
	public static short mutateShort(short s) {
		Random rnd = new Random();
		return (short) rnd.nextInt(Short.MAX_VALUE + 1);
	}

	/**
	 * Mutates value
	 * 
	 * @param l
	 *            : Long to mutate
	 * @return Mutated value
	 */
	public static long mutateLong(long l) {
		Random rnd = new Random();
		return rnd.nextLong();
	}

	/**
	 * Mutates value
	 * 
	 * @param str
	 *            : String to mutate
	 * @return Mutated value
	 */
	public static String mutateString(String str) { // mutate char in the middle
		if ((str == null)) {
			return "null";
		}
		if (str.equals("")) {
			return LogCaller.mutateString("0"); //return a random number as string
		}
		return str.substring(0, str.length() / 2)
				+ LogCaller.mutateChar(str.charAt(str.length() / 2))
				+ str.substring(str.length() / 2 + 1, str.length());
	}
}
