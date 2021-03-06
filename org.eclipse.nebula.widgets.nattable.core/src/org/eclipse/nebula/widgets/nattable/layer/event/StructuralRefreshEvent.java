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
package org.eclipse.nebula.widgets.nattable.layer.event;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.swt.graphics.Rectangle;

/**
 * General event indicating that structures cached by the layers need
 * refreshing.
 * <p>
 * TIP: Consider throwing a more focused event (subclass) if you need to do
 * this.
 */
public class StructuralRefreshEvent implements IStructuralChangeEvent {

    private ILayer layer;

    public StructuralRefreshEvent(ILayer layer) {
        this.layer = layer;
    }

    protected StructuralRefreshEvent(StructuralRefreshEvent event) {
        this.layer = event.layer;
    }

    @Override
    public ILayer getLayer() {
        return this.layer;
    }

    @Override
    public boolean convertToLocal(ILayer localLayer) {
        this.layer = localLayer;

        return true;
    }

    @Override
    public Collection<Rectangle> getChangedPositionRectangles() {
        return Arrays.asList(new Rectangle(0, 0, this.layer.getColumnCount(), this.layer.getRowCount()));
    }

    @Override
    public boolean isHorizontalStructureChanged() {
        return true;
    }

    @Override
    public boolean isVerticalStructureChanged() {
        return true;
    }

    @Override
    public Collection<StructuralDiff> getColumnDiffs() {
        return null;
    }

    @Override
    public Collection<StructuralDiff> getRowDiffs() {
        return null;
    }

    @Override
    public ILayerEvent cloneEvent() {
        return this;
    }

}
