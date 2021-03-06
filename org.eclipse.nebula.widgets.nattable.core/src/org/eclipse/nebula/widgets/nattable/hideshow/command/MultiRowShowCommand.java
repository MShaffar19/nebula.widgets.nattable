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
package org.eclipse.nebula.widgets.nattable.hideshow.command;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.nebula.widgets.nattable.command.AbstractContextFreeCommand;
import org.eclipse.nebula.widgets.nattable.util.ArrayUtil;

/**
 * Command for showing hidden rows again via index.
 */
public class MultiRowShowCommand extends AbstractContextFreeCommand {

    /**
     * The indexes of the rows that should be showed again.
     */
    private final int[] rowIndexes;

    /**
     *
     * @param rowIndexes
     *            The indexes of the rows that should be showed again.
     */
    public MultiRowShowCommand(Collection<Integer> rowIndexes) {
        this.rowIndexes = rowIndexes.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     *
     * @param rowIndexes
     *            The indexes of the rows that should be showed again.
     * @since 2.0
     */
    public MultiRowShowCommand(int... rowIndexes) {
        this.rowIndexes = rowIndexes;
    }

    /**
     *
     * @return The indexes of the rows that should be showed again.
     */
    public Collection<Integer> getRowIndexes() {
        return ArrayUtil.asIntegerList(this.rowIndexes);
    }

    /**
     *
     * @return The indexes of the rows that should be showed again.
     *
     * @since 2.0
     */
    public int[] getRowIndexesArray() {
        return this.rowIndexes;
    }

    @Override
    public MultiRowShowCommand cloneCommand() {
        return new MultiRowShowCommand(Arrays.copyOf(this.rowIndexes, this.rowIndexes.length));
    }
}
