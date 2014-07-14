/** This file is part of OpenIAB **
 *
 * Copyright (C) 2013-2014 Nokia Corporation and/or its subsidiary(-ies). All rights reserved. *
 *
 * This software, including documentation, is protected by copyright controlled
 * by Nokia Corporation. All rights are reserved. Copying, including reproducing,
 * storing, adapting or translating, any or all of this material requires the prior
 * written consent of Nokia Corporation. This material also contains confidential
 * information which may not be disclosed to others * without the prior written
 * consent of Nokia.
 *
 */

package org.onepf.oms.appstore.nokiaUtils;

import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;

public class NokiaResult extends IabResult {

	public static final int RESULT_NO_SIM = 9;

	public NokiaResult(final int response, final String message) {

		super(
			response == RESULT_NO_SIM ? IabHelper.BILLING_RESPONSE_RESULT_ERROR : response,
			response == RESULT_NO_SIM ? "No sim. " + message : message);
	}
}
