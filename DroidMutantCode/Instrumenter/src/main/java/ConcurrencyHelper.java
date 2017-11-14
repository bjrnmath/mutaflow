/**This source code is part of the MutaFlow project. It is a helper class to enable mutli-threaded instrumentation of the program-under-test.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

public class ConcurrencyHelper implements Runnable {
	private static HashSet<String> lsinks;
	private static HashSet<String> lsources;
	private static HashSet<String> mutations;
	private static SootClass[] classes;
	private static Lock acitveBodyLock = new ReentrantLock();
	private static SourceOrSink currentSource;

	private int id;
	private int numberOfThreads;

	/**
	 * Setup for concurrency helper to manage all helpers when executing.
	 * 
	 * @param lsinks
	 *            : Set of sinks to take care of
	 * @param lsources
	 *            : Set of sources to take care of
	 * @param mutations
	 *            : Set of mutations that will be done
	 * @param classes
	 *            : Array
	 * @param currentSource 
	 */
	public static void setup(HashSet<String> lsinks, HashSet<String> lsources,
			HashSet<String> mutations, SootClass[] classes, SourceOrSink currentSource) {
		ConcurrencyHelper.lsinks = lsinks;
		ConcurrencyHelper.lsources = lsources;
		ConcurrencyHelper.mutations = mutations;
		ConcurrencyHelper.classes = classes;
		ConcurrencyHelper.currentSource = currentSource;
	}

	public ConcurrencyHelper(int id, int numberOfThreads) {
		this.id = id;
		this.numberOfThreads = numberOfThreads;
	}

	/**
	 * Run Method for worker thread, manages the classes to instrument for the
	 * specific worker.
	 */
	public void run() {
		int counter = id;

		while (counter < classes.length) {
			if (classes[counter].getName().equals("call.LogCaller")) {
				counter += this.numberOfThreads;
				continue;
			}
			long positionInClass = 0;
			List<SootMethod> methods = classes[counter].getMethods();
			LinkedList<SootMethod> storedMethods = new LinkedList<SootMethod>();
			for (SootMethod m : methods) {
				storedMethods.add(m);
			}
			for (SootMethod m : storedMethods) {
				long positionInMethod = 0;
				Body body = null;
				try {
					ConcurrencyHelper.acitveBodyLock.lock();
					body = m.retrieveActiveBody();
					ConcurrencyHelper.acitveBodyLock.unlock();
				} catch (Exception e) {
					try {
						ConcurrencyHelper.acitveBodyLock.unlock();
					} catch (Exception concException) {
						continue;
					}
					continue;
				}
				Iterator<Unit> i = body.getUnits().snapshotIterator();
				while (i.hasNext()) {
					positionInClass++;
					positionInMethod++;
					Unit u = i.next();
					checkUnit(u, lsinks, lsources, body, classes[counter], positionInClass, positionInMethod);
				}
			}
			counter += this.numberOfThreads;
		}

	}

	/**
	 * This method is called on a Soot unit and manages the mutation and
	 * log-calling.
	 * 
	 * @param u
	 *            : Soot unit
	 * @param lsinks
	 *            : Set of sinks to take care of
	 * @param lsources
	 *            : Set of sources to take care of
	 * @param body
	 *            : The current body in which the unit lies in.
	 * @param positionInMethod 
	 * @param positionInClass 
	 * @param containingClass 
	 */
	private static void checkUnit(Unit u, HashSet<String> lsinks,
			HashSet<String> lsources, Body body, SootClass containingClass, long positionInClass, long positionInMethod) {
		if (u instanceof Stmt) {
			Stmt s = (Stmt) u;
			if (s.containsInvokeExpr()) {
				InvokeExpr iinv = (InvokeExpr) s.getInvokeExpr();
				if (mutations.contains(iinv.getMethod().getSignature().toString())) {
					System.out.println("Mutated: " + iinv.getMethod().getSignature());
					SourceOrSink src = new SourceOrSink(iinv.getMethod().toString(), iinv.getMethod().getSubSignature().toString(), s.getJavaSourceStartLineNumber(), positionInClass, positionInMethod, body.getMethod().getSubSignature(), containingClass.getName());
					//only mutate one specific source per file
					if(src.equals(ConcurrencyHelper.currentSource)) {
						addMutationCall(iinv, body, u, src);
						return;
					}
				}
				if (lsinks.contains(iinv.getMethod().getSignature().toString())) {
					SourceOrSink sink = new SourceOrSink(iinv.getMethod().toString(), iinv.getMethod().getSubSignature().toString(), s.getJavaSourceStartLineNumber(), positionInClass, positionInMethod, body.getMethod().getSubSignature(), containingClass.getName());
					addSinkCall(iinv, body, u, sink);
					return;
				}

				if (lsources.contains(iinv.getMethod().getSignature().toString())) {
					
					SourceOrSink src = new SourceOrSink(iinv.getMethod().toString(), iinv.getMethod().getSubSignature().toString(), s.getJavaSourceStartLineNumber(), positionInClass, positionInMethod, body.getMethod().getSubSignature(), containingClass.getName());
					System.out.println(src);
					//report to the main the available sources
					try {
						PrivacyAPK.availableSourcesLock.lock();
						PrivacyAPK.availableSources.add(src);
						PrivacyAPK.availableSourcesLock.unlock();
					} catch (Exception e) {
						try {
							PrivacyAPK.availableSourcesLock.unlock();
						} catch (Exception concException) {
						}
					}

					//this will only be called for the original file, otherwise lsources is always empty
					
					addSourceCall(iinv, body, u, src);
					return;
				}
			}
		}
	}

	/**
	 * This Method makes a LogCall after the Statement with the name of the
	 * Method as well as the values of the parameters of the method.
	 * 
	 * @param iinv
	 *            Invoke expression the unit contains
	 * @param body
	 *            Body where the unit lies in
	 * @param u
	 *            Current Unit
	 * @param sink 
	 */
	private static void addSinkCall(InvokeExpr iinv, Body body, Unit u, SourceOrSink sink) {
		//create new Object of the logcaller instance
		Local l = Jimple.v().newLocal("droidMutantLogCaller",RefType.v("call.LogCaller"));
		body.getLocals().add(l);
		AssignStmt assign = Jimple.v().newAssignStmt(l,Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(PrivacyAPK.CREATELOGCALLER).makeRef()));
		body.getUnits().insertBefore(assign,u);
		
		// write method name
		Value str = StringConstant.v(sink.toString());
		Value uid = StringConstant.v(PrivacyAPK.uID);
		body.getUnits().insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.LOGWRITEAPKNAME).makeRef(),uid)), u);
		body.getUnits().insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.LOGWRITEMETHODNAME).makeRef(),str)), u);
		
		for (Value v : iinv.getArgs()) {
			SootMethod sm = null;
			switch (v.getType().toString()) {
			case "int": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEINT);
				break;
			}
			case "double": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEDOUBLE);
				break;
			}
			case "float": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEFLOAT);
				break;
			}
			case "char": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITECHAR);
				break;
			}
			case "boolean": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEBOOLEAN);
				break;
			}
			case "byte": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEBYTE);
				break;
			}
			case "short": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITESHORT);
				break;
			}
			case "long": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITELONG);
				break;
			}
			default: {
				if (v.getType().toString().contains("[]")) {
					sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEARRAY);
					break;
				}
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEOBJECT);
				break;
			}
			}
			body.getUnits().insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, sm.makeRef(),v)), u);
		}
		body.getUnits().insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.WRITETOLOG).makeRef())), u);
	}

	/**
	 * This Method adds a LogCall in front of the given Statement with the name
	 * as well as the return value of the statement.
	 * 
	 * @param iinv
	 *            Invoke expression the unit contains
	 * @param body
	 *            Body where the unit lies in
	 * @param u
	 *            Current Unit
	 * @param src 
	 */
	private static void addSourceCall(InvokeExpr iinv, Body body, Unit u, SourceOrSink src) {
		//create new Object of the logcaller instance
		Local l = Jimple.v().newLocal("droidMutantLogCaller",RefType.v("call.LogCaller"));
		body.getLocals().add(l);
		AssignStmt assign = Jimple.v().newAssignStmt(l,Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(PrivacyAPK.CREATELOGCALLER).makeRef()));
		body.getUnits().insertBefore(assign,u);
		
		body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.WRITETOLOG).makeRef())), u);
		for (ValueBox vb : u.getDefBoxes()) {
			Value v = vb.getValue();
			SootMethod sm = null;
			switch (v.getType().toString()) {
			case "int": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEINT);
				break;
			}
			case "double": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEDOUBLE);
				break;
			}
			case "float": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEFLOAT);
				break;
			}
			case "char": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITECHAR);
				break;
			}
			case "boolean": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEBOOLEAN);
				break;
			}
			case "byte": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEBYTE);
				break;
			}
			case "short": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITESHORT);
				break;
			}
			case "long": {
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITELONG);
				break;
			}
			case "void": {
				return;
			}
			default: {
				if (v.getType().toString().contains("[]")) {
					sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEARRAY);
					break;
				}
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEOBJECT);
				break;
			}
			}
			body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, sm.makeRef(),v)), u);

		}
		// write method name and UID
		Value str = StringConstant.v(src.toString());
		body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.LOGWRITEMETHODNAME).makeRef(),str)), u);
		Value uid = StringConstant.v(PrivacyAPK.uID);
		body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.LOGWRITEAPKNAME).makeRef(),uid)), u);
					
	}

	/**
	 * Adds a call which mutates the given unit outcome.
	 * 
	 * @param iinv
	 *            Invoke expression the unit contains
	 * @param body
	 *            Body where the unit lies in
	 * @param u
	 *            Current Unit
	 * @param src 
	 */
	private static void addMutationCall(InvokeExpr iinv, Body body, Unit u, SourceOrSink src) {
		//create new Object of the logcaller instance
		Local l = Jimple.v().newLocal("droidMutantLogCaller",RefType.v("call.LogCaller"));
		body.getLocals().add(l);
		AssignStmt assign = Jimple.v().newAssignStmt(l,Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(PrivacyAPK.CREATELOGCALLER).makeRef()));
		body.getUnits().insertBefore(assign,u);
				
		body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.WRITETOLOG).makeRef())), u);
		for (ValueBox vb : u.getDefBoxes()) {
			Value v = vb.getValue();
			SootMethod sm = null; // method for printing the value
			SootMethod mut = null; // method for mutating the value
			switch (v.getType().toString()) {
			case "int": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATEINT);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEINT);
				break;
			}
			case "double": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATEDOUBLE);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEDOUBLE);
				break;
			}
			case "float": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATEFLOAT);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEFLOAT);
				break;
			}
			case "char": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATECHAR);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITECHAR);
				break;
			}
			case "boolean": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATEBOOLEAN);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEBOOLEAN);
				break;
			}
			case "byte": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATEBYTE);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEBYTE);
				break;
			}
			case "short": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATESHORT);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITESHORT);
				break;
			}
			case "long": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATELONG);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITELONG);
				break;
			}
			case "java.lang.String": {
				mut = Scene.v().getMethod(PrivacyAPK.MUTATESTRING);
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEOBJECT);
				break;
			}
			case "void": {
				return;
			}
			default: {
				if (v.getType().toString().contains("[]")) {
					sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEARRAY);
					break;
				}
				sm = Scene.v().getMethod(PrivacyAPK.LOGWRITEOBJECT);
				break;
			}
			}
			body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, sm.makeRef(),v)), u);
			if (mut != null) {
				body.getUnits()
						.insertAfter(
								Jimple.v().newAssignStmt(
										v,
										Jimple.v().newStaticInvokeExpr(
												mut.makeRef(), v)), u);
			}
		}
		// write method name and UID
		Value str = StringConstant.v(src.toString());
		body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.LOGWRITEMETHODNAME).makeRef(),str)), u);
		Value uid = StringConstant.v(PrivacyAPK.uID);
		body.getUnits().insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, Scene.v().getMethod(PrivacyAPK.LOGWRITEAPKNAME).makeRef(),uid)), u);
	}

}
