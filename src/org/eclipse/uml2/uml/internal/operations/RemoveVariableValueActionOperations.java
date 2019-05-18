/*
 * Copyright (c) 2005, 2014 IBM Corporation, CEA, and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - initial API and implementation
 *   Kenn Hussey (CEA) - 327039, 418466
 *
 */
package org.eclipse.uml2.uml.internal.operations;

import java.util.Map;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;

import org.eclipse.uml2.uml.RemoveVariableValueAction;

import org.eclipse.uml2.uml.util.UMLValidator;

/**
 * <!-- begin-user-doc -->
 * A static utility class that provides operations related to '<em><b>Remove Variable Value Action</b></em>' model objects.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following operations are supported:
 * <ul>
 *   <li>{@link org.eclipse.uml2.uml.RemoveVariableValueAction#validateRemoveAtAndValue(org.eclipse.emf.common.util.DiagnosticChain, java.util.Map) <em>Validate Remove At And Value</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class RemoveVariableValueActionOperations
		extends WriteVariableActionOperations {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RemoveVariableValueActionOperations() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * ReadVariableActions removing a value from ordered, non-unique Variables must have a single removeAt InputPin and no value InputPin, if isRemoveDuplicates is false. The removeAt InputPin must be of type Unlimited Natural with multiplicity 1..1. Otherwise, the Action has a value InputPin and no removeAt InputPin.
	 * if  variable.isOrdered and not variable.isUnique and not isRemoveDuplicates then 
	 *   value = null and
	 *   removeAt <> null and
	 *   removeAt.type = UnlimitedNatural and
	 *   removeAt.is(1,1)
	 * else
	 *   removeAt = null and value <> null
	 * endif
	 * @param removeVariableValueAction The receiving '<em><b>Remove Variable Value Action</b></em>' model object.
	 * @param diagnostics The chain of diagnostics to which problems are to be appended.
	 * @param context The cache of context-specific information.
	 * <!-- end-model-doc -->
	 * @generated
	 */
	public static boolean validateRemoveAtAndValue(
			RemoveVariableValueAction removeVariableValueAction,
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		// TODO: implement this method
		// -> specify the condition that violates the invariant
		// -> verify the details of the diagnostic, including severity and message
		// Ensure that you remove @generated or mark it @generated NOT
		if (false) {
			if (diagnostics != null) {
				diagnostics
					.add(new BasicDiagnostic(
						Diagnostic.ERROR,
						UMLValidator.DIAGNOSTIC_SOURCE,
						UMLValidator.REMOVE_VARIABLE_VALUE_ACTION__REMOVE_AT_AND_VALUE,
						org.eclipse.emf.ecore.plugin.EcorePlugin.INSTANCE
							.getString(
								"_UI_GenericInvariant_diagnostic", new Object[]{"validateRemoveAtAndValue", org.eclipse.emf.ecore.util.EObjectValidator.getObjectLabel(removeVariableValueAction, context)}), //$NON-NLS-1$ //$NON-NLS-2$
						new Object[]{removeVariableValueAction}));
			}
			return false;
		}
		return true;
	}

} // RemoveVariableValueActionOperations