/**This source code is part of the MutaFlow project. It is a wrapper for information of a source or sink.
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

public class SourceOrSink {
	
	private String name;
	private long javaLine;
	private long classLine;
	private long methodLine;
	private String containingMethod;
	private String containingClass;
	private String methodName;

	public SourceOrSink(String name, String MethodName, long javaLine, long classLine, long methodLine, String containingMethod, String containingClass) {
		this.name = name;
		this.javaLine = javaLine;
		this.classLine = classLine;
		this.methodLine = methodLine;
		this.containingMethod = containingMethod;
		this.containingClass = containingClass;
		this.methodName = MethodName;
	}
	
	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getJavaLine() {
		return javaLine;
	}

	public void setJavaLine(long javaLine) {
		this.javaLine = javaLine;
	}

	public long getClassLine() {
		return classLine;
	}

	public void setClassLine(long classLine) {
		this.classLine = classLine;
	}

	public long getMethodLine() {
		return methodLine;
	}

	public void setMethodLine(long methodLine) {
		this.methodLine = methodLine;
	}

	public String getContainingMethod() {
		return containingMethod;
	}

	public void setContainingMethod(String containingMethod) {
		this.containingMethod = containingMethod;
	}

	public String getContainingClass() {
		return containingClass;
	}

	public void setContainingClass(String containingClass) {
		this.containingClass = containingClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (classLine ^ (classLine >>> 32));
		result = prime * result
				+ ((containingClass == null) ? 0 : containingClass.hashCode());
		result = prime
				* result
				+ ((containingMethod == null) ? 0 : containingMethod.hashCode());
		result = prime * result + (int) (javaLine ^ (javaLine >>> 32));
		result = prime * result + (int) (methodLine ^ (methodLine >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceOrSink other = (SourceOrSink) obj;
		if (classLine != other.classLine)
			return false;
		if (containingClass == null) {
			if (other.containingClass != null)
				return false;
		} else if (!containingClass.equals(other.containingClass))
			return false;
		if (containingMethod == null) {
			if (other.containingMethod != null)
				return false;
		} else if (!containingMethod.equals(other.containingMethod))
			return false;
		if (javaLine != other.javaLine)
			return false;
		if (methodLine != other.methodLine)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		String out = name;
		out += "\t";
		out += methodName;
		out += "\t";
		out += javaLine;
		out += "\t";
		out += classLine;
		out += "\t";
		out += methodLine;
		out += "\t";
		out += containingMethod;
		out += "\t";
		out += containingClass;
		return out;
	}
	
	public String fileName() {
		//to make it applicable to the existing scripts
		if (methodName.equals("Original")) {
			return "Original";
		}
		if (methodName.equals("OriginalRef")) {
			return "OriginalRef";
		}
		String out = methodName.split(" ")[1].split("\\(")[0];
		out += "_";
		out += classLine;
		out += "_";
		out += containingClass;
		out = out.replace(" ", "-");
		if (out.length() > 210) {  //unix maximal 255 character for file names. Leave some buffer for later analysis which may add characters to the file name.
			out = out.substring(0, 210);
		}
		return out;
	}
	
	

}
