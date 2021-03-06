/*******************************************************************************
 * Copyright (c) 2012, 2020 Original authors and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Original authors and others - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.data.convert;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * Converts the display value to a double and vice versa.
 */
public class DefaultDoubleDisplayConverter extends DecimalNumericDisplayConverter {

    /**
     * Creates a converter that uses {@link NumberFormat}.
     */
    public DefaultDoubleDisplayConverter() {
        super(true);
    }

    /**
     * Creates a converter and allows to specify whether {@link NumberFormat}
     * should be used or not.
     *
     * @param useNumberFormat
     *            <code>true</code> if a {@link NumberFormat} should be used,
     *            <code>false</code> if not.
     *
     * @since 1.4
     */
    public DefaultDoubleDisplayConverter(boolean useNumberFormat) {
        super(useNumberFormat);
    }

    @Override
    protected Object convertToNumericValue(String value) {
        if (this.nf != null) {
            try {
                return this.nf.parse(value).doubleValue();
            } catch (ParseException e) {
                throw new NumberFormatException(e.getLocalizedMessage());
            }
        }
        return Double.valueOf(value);
    }

}
