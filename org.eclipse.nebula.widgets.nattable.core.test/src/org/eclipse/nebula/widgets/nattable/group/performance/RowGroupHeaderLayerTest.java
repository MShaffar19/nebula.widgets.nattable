/*******************************************************************************
 * Copyright (c) 2019 Dirk Fauth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.group.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.nebula.widgets.nattable.coordinate.PositionUtil;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.dataset.person.ExtendedPersonWithAddress;
import org.eclipse.nebula.widgets.nattable.dataset.person.PersonService;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.command.ClientAreaResizeCommand;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.command.CreateRowGroupCommand;
import org.eclipse.nebula.widgets.nattable.group.command.RemoveRowGroupCommand;
import org.eclipse.nebula.widgets.nattable.group.command.RowGroupExpandCollapseCommand;
import org.eclipse.nebula.widgets.nattable.group.command.UngroupRowCommand;
import org.eclipse.nebula.widgets.nattable.group.performance.GroupModel.Group;
import org.eclipse.nebula.widgets.nattable.group.performance.command.RowGroupReorderCommand;
import org.eclipse.nebula.widgets.nattable.group.performance.command.RowGroupReorderEndCommand;
import org.eclipse.nebula.widgets.nattable.group.performance.command.RowGroupReorderStartCommand;
import org.eclipse.nebula.widgets.nattable.group.performance.config.GroupHeaderConfigLabels;
import org.eclipse.nebula.widgets.nattable.hideshow.RowHideShowLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.command.MultiRowHideCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.RowHideCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ShowAllRowsCommand;
import org.eclipse.nebula.widgets.nattable.layer.AbstractDpiConverter;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.IDpiConverter;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.layer.command.ConfigureScalingCommand;
import org.eclipse.nebula.widgets.nattable.reorder.RowReorderLayer;
import org.eclipse.nebula.widgets.nattable.reorder.command.MultiRowReorderCommand;
import org.eclipse.nebula.widgets.nattable.reorder.command.ResetRowReorderCommand;
import org.eclipse.nebula.widgets.nattable.reorder.command.RowReorderCommand;
import org.eclipse.nebula.widgets.nattable.reorder.command.RowReorderEndCommand;
import org.eclipse.nebula.widgets.nattable.reorder.command.RowReorderStartCommand;
import org.eclipse.nebula.widgets.nattable.resize.command.ColumnResizeCommand;
import org.eclipse.nebula.widgets.nattable.resize.command.MultiColumnResizeCommand;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectRowsCommand;
import org.eclipse.nebula.widgets.nattable.util.IClientAreaProvider;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.nebula.widgets.nattable.viewport.command.ShowRowInViewportCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class RowGroupHeaderLayerTest {

    GroupModel groupModel;
    RowGroupHeaderLayer rowGroupHeaderLayer;
    RowGroupExpandCollapseLayer rowGroupExpandCollapseLayer;
    SelectionLayer selectionLayer;
    GridLayer gridLayer;

    @Before
    public void setup() {
        String[] propertyNames = {
                "firstName", "lastName", "gender", "married",
                "address.street", "address.housenumber", "address.postalCode", "address.city",
                "age", "birthday", "money",
                "description", "favouriteFood", "favouriteDrinks" };

        // mapping from property to label, needed for column header labels
        Map<String, String> propertyToLabelMap = new HashMap<>();
        propertyToLabelMap.put("firstName", "Firstname");
        propertyToLabelMap.put("lastName", "Lastname");
        propertyToLabelMap.put("gender", "Gender");
        propertyToLabelMap.put("married", "Married");
        propertyToLabelMap.put("address.street", "Street");
        propertyToLabelMap.put("address.housenumber", "Housenumber");
        propertyToLabelMap.put("address.postalCode", "Postalcode");
        propertyToLabelMap.put("address.city", "City");
        propertyToLabelMap.put("age", "Age");
        propertyToLabelMap.put("birthday", "Birthday");
        propertyToLabelMap.put("money", "Money");
        propertyToLabelMap.put("description", "Description");
        propertyToLabelMap.put("favouriteFood", "Food");
        propertyToLabelMap.put("favouriteDrinks", "Drinks");

        IColumnPropertyAccessor<ExtendedPersonWithAddress> columnPropertyAccessor =
                new ExtendedReflectiveColumnPropertyAccessor<>(propertyNames);

        IDataProvider bodyDataProvider =
                new ListDataProvider<>(
                        PersonService.getExtendedPersonsWithAddress(14),
                        columnPropertyAccessor);
        DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
        RowReorderLayer rowReorderLayer = new RowReorderLayer(bodyDataLayer);
        RowHideShowLayer rowHideShowLayer = new RowHideShowLayer(rowReorderLayer);

        this.rowGroupExpandCollapseLayer = new RowGroupExpandCollapseLayer(rowHideShowLayer);

        this.selectionLayer = new SelectionLayer(this.rowGroupExpandCollapseLayer);
        ViewportLayer viewportLayer = new ViewportLayer(this.selectionLayer);

        // build the column header layer
        IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(propertyNames, propertyToLabelMap);
        DataLayer columnHeaderDataLayer = new DefaultColumnHeaderDataLayer(columnHeaderDataProvider);
        ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, this.selectionLayer);

        // build the row header layer
        IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
        DataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
        ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, viewportLayer, this.selectionLayer);

        this.rowGroupHeaderLayer = new RowGroupHeaderLayer(rowHeaderLayer, this.selectionLayer);

        this.groupModel = this.rowGroupHeaderLayer.getGroupModel();

        // configure the column groups
        this.rowGroupHeaderLayer.addGroup("Person", 0, 4);
        this.rowGroupHeaderLayer.addGroup("Address", 4, 4);
        this.rowGroupHeaderLayer.addGroup("Facts", 8, 3);
        this.rowGroupHeaderLayer.addGroup("Personal", 11, 3);

        // build the corner layer
        IDataProvider cornerDataProvider = new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider);
        DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
        ILayer cornerLayer = new CornerLayer(cornerDataLayer, this.rowGroupHeaderLayer, columnHeaderLayer);

        // build the grid layer
        this.gridLayer = new GridLayer(viewportLayer, columnHeaderLayer, this.rowGroupHeaderLayer, cornerLayer);

        // configure the visible area, needed for tests in scrolled state
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                // 10 rows + column header should be visible
                return new Rectangle(0, 0, 1010, 220);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(1, this.rowGroupHeaderLayer.getLevelCount());
        verifyCleanState();
    }

    private void verifyCleanState() {
        // nothing hidden below the SelectionLayer
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldRenderRowGroups() {
        assertEquals(11, this.gridLayer.getRowCount());
        assertEquals(12, this.gridLayer.getColumnCount());

        // increase the client area to show all columns
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().y);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(220, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldReturnSameCellForDifferentRowPositions() {
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(cell, this.rowGroupHeaderLayer.getCellByPosition(0, 1));
        assertEquals(cell, this.rowGroupHeaderLayer.getCellByPosition(0, 2));
        assertEquals(cell, this.rowGroupHeaderLayer.getCellByPosition(0, 3));

        // the next cell is the start of the next column group
        assertFalse(cell.equals(this.rowGroupHeaderLayer.getCellByPosition(0, 4)));
    }

    @Test
    public void shouldRenderGroupInScrolledState() {
        assertEquals(0, this.gridLayer.getBodyLayer().getRowIndexByPosition(0));

        // scroll
        this.gridLayer.doCommand(new ShowRowInViewportCommand(11));

        assertEquals(2, this.gridLayer.getBodyLayer().getRowIndexByPosition(0));

        int visibleStartPosition = this.groupModel.getGroupByPosition(0).getVisibleStartPosition();
        assertEquals(0, visibleStartPosition);
        assertEquals(2, this.rowGroupHeaderLayer.getRowIndexByPosition(visibleStartPosition));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(-2, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(new Rectangle(0, -40, 20, 80), cell.getBounds());
    }

    @Test
    public void shouldCheckIfPartOfGroup() {
        // remove last row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // set second group as unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {

            // check part of a group
            if (row == 3) {
                assertFalse(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            } else {
                assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            }

            // check part of an unbreakable group
            if (row >= 4 && row < 8) {
                assertTrue(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
            } else {
                assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
            }
        }

    }

    @Test
    public void shouldRemoveLastRowFromGroup() {
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // remove last row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);
    }

    @Test
    public void shouldRemoveFirstRowFromGroup() {
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // remove first row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 0);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldRemoveMiddleRowFromGroup() {
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // remove middle row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 1);

        // the result is the same as removing the last row in a group, as it
        // is not possible to split a row group by removing a middle group
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);
    }

    @Test
    public void shouldAddRowToEndOfGroup() {
        // remove last row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        // add the row back again
        this.rowGroupHeaderLayer.addPositionsToGroup(0, 0, 3);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldAddRowAtStartOfGroup() {
        // remove last row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        // add the row as first row to the second group
        this.rowGroupHeaderLayer.addPositionsToGroup(0, 4, 3);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldHideRowInMiddleOfGroup() {
        if (this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 3))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(7, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(10, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideLastRowInGroup() {
        if (this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 4))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(7, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(10, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideFirstRowInGroup() {
        if (this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 5))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(7, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(10, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleMiddleRows() {
        if (this.gridLayer.doCommand(new MultiRowHideCommand(this.gridLayer, 3, 6, 10))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
            assertEquals(3, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(60, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
            assertEquals(6, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(120, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleFirstRows() {
        if (this.gridLayer.doCommand(new MultiRowHideCommand(this.gridLayer, 1, 5, 9))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
            assertEquals(3, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(60, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
            assertEquals(6, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(120, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(1, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(9, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleLastRows() {
        // trigger the command on the SelectionLayer as we hide a row that is
        // not visible which would be blocked by command handling through the
        // ViewportLayer
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 3, 7, 10))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
            assertEquals(3, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(60, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
            assertEquals(6, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(120, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleMixedRows() {
        // last/first/middle
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 3, 4, 9))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
            assertEquals(3, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(60, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
            assertEquals(6, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(120, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(2, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(8, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleRowsInOneGroup() {
        // first two in second group
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 4, 5))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
            assertEquals(6, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(120, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 9);
            assertEquals(9, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(180, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(6, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(2, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(6, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(9);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(9, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideAllRowsInOneGroup() {
        // second group
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 4, 5, 6, 7))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
            assertEquals(7, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(140, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(4, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(7, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());

            // this group is not visible by row position, so we retrieve it
            // by name
            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByName("Address");
            assertEquals(4, group2.getStartIndex());
            assertEquals(-1, group2.getVisibleStartIndex());
            assertEquals(-1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(0, group2.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideRowBetweenGroups() {
        // remove last row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        // hide column
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 3))) {
            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
            assertEquals(3, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(60, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(3, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show row again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
            assertEquals(3, cell.getOriginRowPosition());
            assertEquals(1, cell.getRowSpan());
            assertEquals(2, cell.getColumnSpan());
            assertEquals(4, cell.getDataValue());
            assertEquals(60, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(20, cell.getBounds().height);
            assertEquals(60, cell.getBounds().width);

            Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(2);
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(3, group.getVisibleSpan());

            assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3));

            group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleRowsInMiddleOfTableToReduceRowCountMoreThanEventEnd() {
        // this test is for handling modification of the
        // HideRowPositionEventRanges in case the row count after hide is
        // less than the end of the hide event range

        // this hides completely the Address and Facts group and the first item
        // of the Personal group
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 4, 5, 6, 7, 8, 9, 10, 11))) {
            assertEquals(6, this.selectionLayer.getRowCount());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(12, cell.getRowIndex());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(11, group4.getStartIndex());
            assertEquals(12, group4.getVisibleStartIndex());
            assertEquals(4, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(2, group4.getVisibleSpan());

            // these groups are not visible by column position, so we retrieve
            // it by name
            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByName("Address");
            assertEquals(4, group2.getStartIndex());
            assertEquals(-1, group2.getVisibleStartIndex());
            assertEquals(-1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(0, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByName("Facts");
            assertEquals(8, group3.getStartIndex());
            assertEquals(-1, group3.getVisibleStartIndex());
            assertEquals(-1, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(0, group3.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleRowsInMiddleOfTableTwiceToReduceRowCountMoreThanEventEnd() {
        // this test is for handling modification of the
        // HideRowPositionEventRanges in case the column count after hide is
        // less than the end of the hide event range

        // this hides completely the Address and Facts group and the first item
        // of the Personal group via two separate commands
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 4, 5, 6, 7))
                && this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 4, 5, 6, 7))) {
            assertEquals(6, this.selectionLayer.getRowCount());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(12, cell.getRowIndex());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(11, group4.getStartIndex());
            assertEquals(12, group4.getVisibleStartIndex());
            assertEquals(4, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(2, group4.getVisibleSpan());

            // these groups are not visible by column position, so we retrieve
            // it by name
            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByName("Address");
            assertEquals(4, group2.getStartIndex());
            assertEquals(-1, group2.getVisibleStartIndex());
            assertEquals(-1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(0, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByName("Facts");
            assertEquals(8, group3.getStartIndex());
            assertEquals(-1, group3.getVisibleStartIndex());
            assertEquals(-1, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(0, group3.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            verifyCleanState();
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldCollapseExpandGroup() {
        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // collapse group with no static indexes
        this.rowGroupHeaderLayer.collapseGroup(0);

        assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // expand group with no static indexes
        this.rowGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        group = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldExpandCollapseNonVisibleGroup() {
        // with the RowGroupExpandCollapseCommand it should be possible to
        // expand and collapse groups outside the viewport
        this.gridLayer.doCommand(new RowGroupExpandCollapseCommand(this.selectionLayer, 11));

        assertEquals(12, this.selectionLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        this.gridLayer.doCommand(new RowGroupExpandCollapseCommand(this.selectionLayer, 11));

        assertEquals(14, this.selectionLayer.getRowCount());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleSpan());
        assertFalse(group.isCollapsed());
    }

    @Test
    public void shouldCollapseGroupWithStaticRows() {
        this.rowGroupHeaderLayer.addStaticRowIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // collapse group with static indexes
        this.rowGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // expand group with static indexes
        this.rowGroupHeaderLayer.expandGroup(4);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldShowFirstVisibleRowOnCollapseWhenFirstRowIsHidden() {
        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // hide first row in group
        if (this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 1))) {
            assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

            // collapse group with no static indexes
            this.rowGroupHeaderLayer.collapseGroup(0);

            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(1, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(0, cell.getRowPosition());
            assertEquals(1, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());

            // expand group with no static indexes
            this.rowGroupHeaderLayer.expandGroup(0);

            assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

            group = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(1, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(3, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(0, cell.getRowPosition());
            assertEquals(1, cell.getRowIndex());
            assertEquals(3, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(0, cell.getRowPosition());
            assertEquals(0, cell.getRowIndex());
            assertEquals(4, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldNotShowHiddenRowsInCollapsedGroup() {
        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // hide column in group
        if (this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 3))) {
            assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

            // collapse group
            this.rowGroupHeaderLayer.collapseGroup(0);

            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(0, cell.getRowPosition());
            assertEquals(0, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all rows again
        // collapsed rows should stay hidden
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(0, cell.getRowPosition());
            assertEquals(0, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand again to check that the group state is not changed
        this.rowGroupHeaderLayer.expandGroup(0);

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldNotShowHiddenFirstRowInCollapsedGroup() {
        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // hide row in group
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 4))) {
            assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

            // collapse group
            this.rowGroupHeaderLayer.collapseGroup(4);

            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(5, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(5, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all rows again
        // collapsed rows should stay hidden
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(4, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand again to check that the group state is not changed
        this.rowGroupHeaderLayer.expandGroup(4);

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldNotShowHiddenLastRowInCollapsedGroup() {
        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // hide column in group
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 7))) {
            assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

            // collapse group
            this.rowGroupHeaderLayer.collapseGroup(4);

            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(4, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all rows again
        // collapsed rows should stay hidden
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(4, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand again to check that the group state is not changed
        this.rowGroupHeaderLayer.expandGroup(4);

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldNotShowHiddenRowsInMultipleGroups() {
        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // hide last row in first group and first row in second group
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 3, 4))) {
            assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

            // collapse group
            this.rowGroupHeaderLayer.collapseGroup(4);
            this.rowGroupHeaderLayer.collapseGroup(0);

            assertEquals(8, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginRowPosition());
            assertEquals(0, cell1.getRowPosition());
            assertEquals(0, cell1.getRowIndex());
            assertEquals(1, cell1.getRowSpan());
            assertEquals(1, cell1.getColumnSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(1);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
            assertEquals(1, cell2.getOriginRowPosition());
            assertEquals(1, cell2.getRowPosition());
            assertEquals(5, cell2.getRowIndex());
            assertEquals(1, cell2.getRowSpan());
            assertEquals(1, cell2.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all columns again
        // collapsed columns should stay hidden
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(8, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginRowPosition());
            assertEquals(0, cell1.getRowPosition());
            assertEquals(0, cell1.getRowIndex());
            assertEquals(1, cell1.getRowSpan());
            assertEquals(1, cell1.getColumnSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(1);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(1, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
            assertEquals(1, cell2.getOriginRowPosition());
            assertEquals(1, cell2.getRowPosition());
            assertEquals(4, cell2.getRowIndex());
            assertEquals(1, cell2.getRowSpan());
            assertEquals(1, cell2.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand again to check that the group state is not changed
        this.rowGroupHeaderLayer.expandGroup(1);
        this.rowGroupHeaderLayer.expandGroup(0);

        Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group1.isCollapsed());
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        ILayerCell cell1 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell1.getOriginRowPosition());
        assertEquals(0, cell1.getRowPosition());
        assertEquals(0, cell1.getRowIndex());
        assertEquals(4, cell1.getRowSpan());
        assertEquals(1, cell1.getColumnSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group2.isCollapsed());
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        ILayerCell cell2 = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell2.getOriginRowPosition());
        assertEquals(4, cell2.getRowPosition());
        assertEquals(4, cell2.getRowIndex());
        assertEquals(4, cell2.getRowSpan());
        assertEquals(1, cell2.getColumnSpan());
    }

    @Test
    public void shouldShowNonGroupRowIfAdjacentGroupsAreCollapsed() {
        // remove a row between two groups
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // hide that column
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 3))) {
            assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(3, group1.getVisibleSpan());

            ILayerCell cell1 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginRowPosition());
            assertEquals(0, cell1.getRowPosition());
            assertEquals(0, cell1.getRowIndex());
            assertEquals(3, cell1.getRowSpan());
            assertEquals(1, cell1.getColumnSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(3);
            assertFalse(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(3, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            ILayerCell cell2 = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
            assertEquals(3, cell2.getOriginRowPosition());
            assertEquals(3, cell2.getRowPosition());
            assertEquals(4, cell2.getRowIndex());
            assertEquals(4, cell2.getRowSpan());
            assertEquals(1, cell2.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // collapse both groups
        this.rowGroupHeaderLayer.collapseGroup(4);
        this.rowGroupHeaderLayer.collapseGroup(0);

        assertEquals(8, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group11 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(1, group11.getVisibleSpan());

        ILayerCell cell11 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginRowPosition());
        assertEquals(0, cell11.getRowPosition());
        assertEquals(0, cell11.getRowIndex());
        assertEquals(1, cell11.getRowSpan());
        assertEquals(1, cell11.getColumnSpan());

        Group group22 = this.rowGroupHeaderLayer.getGroupByPosition(1);
        assertTrue(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(4, group22.getVisibleStartIndex());
        assertEquals(1, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(1, group22.getVisibleSpan());

        ILayerCell cell22 = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell22.getOriginRowPosition());
        assertEquals(1, cell22.getRowPosition());
        assertEquals(4, cell22.getRowIndex());
        assertEquals(1, cell22.getRowSpan());
        assertEquals(1, cell22.getColumnSpan());

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(9, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginRowPosition());
            assertEquals(0, cell1.getRowPosition());
            assertEquals(0, cell1.getRowIndex());
            assertEquals(1, cell1.getRowSpan());
            assertEquals(1, cell1.getColumnSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(2, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
            assertEquals(2, cell2.getOriginRowPosition());
            assertEquals(2, cell2.getRowPosition());
            assertEquals(4, cell2.getRowIndex());
            assertEquals(1, cell2.getRowSpan());
            assertEquals(1, cell2.getColumnSpan());

            ILayerCell cell3 = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
            assertEquals(1, cell3.getOriginRowPosition());
            assertEquals(1, cell3.getRowPosition());
            assertEquals(3, cell3.getRowIndex());
            assertEquals(1, cell3.getRowSpan());
            assertEquals(2, cell3.getColumnSpan());
            assertEquals(4, cell3.getDataValue());
        } else {
            fail("Rows not shown again");
        }

        // expand both groups again
        this.rowGroupHeaderLayer.expandGroup(2);
        this.rowGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        group11 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(3, group11.getVisibleSpan());

        cell11 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginRowPosition());
        assertEquals(0, cell11.getRowPosition());
        assertEquals(0, cell11.getRowIndex());
        assertEquals(3, cell11.getRowSpan());
        assertEquals(1, cell11.getColumnSpan());

        group22 = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(4, group22.getVisibleStartIndex());
        assertEquals(4, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(4, group22.getVisibleSpan());

        cell22 = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell22.getOriginRowPosition());
        assertEquals(4, cell22.getRowPosition());
        assertEquals(4, cell22.getRowIndex());
        assertEquals(4, cell22.getRowSpan());
        assertEquals(1, cell22.getColumnSpan());

        ILayerCell cell33 = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell33.getOriginRowPosition());
        assertEquals(3, cell33.getRowPosition());
        assertEquals(3, cell33.getRowIndex());
        assertEquals(1, cell33.getRowSpan());
        assertEquals(2, cell33.getColumnSpan());
        assertEquals(4, cell33.getDataValue());
    }

    @Test
    public void shouldOnlyShowNonGroupRowIfAdjacentGroupsAreCollapsed() {
        // remove a row between two groups
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // hide the last row of the first group
        // hide the non grouped row
        // hide the first row of the second groupd
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 2, 3, 4))) {
            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(2, group1.getVisibleSpan());

            ILayerCell cell1 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginRowPosition());
            assertEquals(0, cell1.getRowPosition());
            assertEquals(0, cell1.getRowIndex());
            assertEquals(2, cell1.getRowSpan());
            assertEquals(1, cell1.getColumnSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
            assertFalse(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(5, group2.getVisibleStartIndex());
            assertEquals(2, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(3, group2.getVisibleSpan());

            ILayerCell cell2 = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
            assertEquals(2, cell2.getOriginRowPosition());
            assertEquals(2, cell2.getRowPosition());
            assertEquals(5, cell2.getRowIndex());
            assertEquals(3, cell2.getRowSpan());
            assertEquals(1, cell2.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // collapse both groups
        this.rowGroupHeaderLayer.collapseGroup(2);
        this.rowGroupHeaderLayer.collapseGroup(0);

        assertEquals(8, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group11 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(1, group11.getVisibleSpan());

        ILayerCell cell11 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginRowPosition());
        assertEquals(0, cell11.getRowPosition());
        assertEquals(0, cell11.getRowIndex());
        assertEquals(1, cell11.getRowSpan());
        assertEquals(1, cell11.getColumnSpan());

        Group group22 = this.rowGroupHeaderLayer.getGroupByPosition(1);
        assertTrue(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(5, group22.getVisibleStartIndex());
        assertEquals(1, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(1, group22.getVisibleSpan());

        ILayerCell cell22 = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell22.getOriginRowPosition());
        assertEquals(1, cell22.getRowPosition());
        assertEquals(5, cell22.getRowIndex());
        assertEquals(1, cell22.getRowSpan());
        assertEquals(1, cell22.getColumnSpan());

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(9, this.rowGroupExpandCollapseLayer.getRowCount());

            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group1.isCollapsed());
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(1, group1.getVisibleSpan());

            ILayerCell cell1 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell1.getOriginRowPosition());
            assertEquals(0, cell1.getRowPosition());
            assertEquals(0, cell1.getRowIndex());
            assertEquals(1, cell1.getRowSpan());
            assertEquals(1, cell1.getColumnSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
            assertTrue(group2.isCollapsed());
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(2, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell2 = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
            assertEquals(2, cell2.getOriginRowPosition());
            assertEquals(2, cell2.getRowPosition());
            assertEquals(4, cell2.getRowIndex());
            assertEquals(1, cell2.getRowSpan());
            assertEquals(1, cell2.getColumnSpan());

            ILayerCell cell3 = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
            assertEquals(1, cell3.getOriginRowPosition());
            assertEquals(1, cell3.getRowPosition());
            assertEquals(3, cell3.getRowIndex());
            assertEquals(1, cell3.getRowSpan());
            assertEquals(2, cell3.getColumnSpan());
            assertEquals(4, cell3.getDataValue());
        } else {
            fail("Rows not shown again");
        }

        // expand both groups again
        this.rowGroupHeaderLayer.expandGroup(2);
        this.rowGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        group11 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertFalse(group11.isCollapsed());
        assertEquals(0, group11.getStartIndex());
        assertEquals(0, group11.getVisibleStartIndex());
        assertEquals(0, group11.getVisibleStartPosition());
        assertEquals(3, group11.getOriginalSpan());
        assertEquals(3, group11.getVisibleSpan());

        cell11 = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell11.getOriginRowPosition());
        assertEquals(0, cell11.getRowPosition());
        assertEquals(0, cell11.getRowIndex());
        assertEquals(3, cell11.getRowSpan());
        assertEquals(1, cell11.getColumnSpan());

        group22 = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group22.isCollapsed());
        assertEquals(4, group22.getStartIndex());
        assertEquals(4, group22.getVisibleStartIndex());
        assertEquals(4, group22.getVisibleStartPosition());
        assertEquals(4, group22.getOriginalSpan());
        assertEquals(4, group22.getVisibleSpan());

        cell22 = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell22.getOriginRowPosition());
        assertEquals(4, cell22.getRowPosition());
        assertEquals(4, cell22.getRowIndex());
        assertEquals(4, cell22.getRowSpan());
        assertEquals(1, cell22.getColumnSpan());

        ILayerCell cell33 = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell33.getOriginRowPosition());
        assertEquals(3, cell33.getRowPosition());
        assertEquals(3, cell33.getRowIndex());
        assertEquals(1, cell33.getRowSpan());
        assertEquals(2, cell33.getColumnSpan());
        assertEquals(4, cell33.getDataValue());
    }

    @Test
    public void shouldHideStaticRowInCollapsedState() {
        // set last two rows in second group as static
        this.rowGroupHeaderLayer.addStaticRowIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // collapse group with static indexes
        this.rowGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // hide first static column
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 4))) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(7, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(7, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // expand group with static indexes
        this.rowGroupHeaderLayer.expandGroup(4);

        assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

        group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldShowHiddenFirstStaticRowInCollapsedState() {
        // set last two rows in second group as static
        this.rowGroupHeaderLayer.addStaticRowIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // collapse group with static indexes
        this.rowGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // hide first static row
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 4))) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(7, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(7, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all rows again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(6, cell.getRowIndex());
            assertEquals(2, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand group with static indexes
        this.rowGroupHeaderLayer.expandGroup(4);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldShowHiddenLastStaticRowInCollapsedState() {
        // set last two rows in second group as static
        this.rowGroupHeaderLayer.addStaticRowIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // collapse group with static indexes
        this.rowGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // hide last static column
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 5))) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(6, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(6, cell.getRowIndex());
            assertEquals(2, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand group with static indexes
        this.rowGroupHeaderLayer.expandGroup(4);

        verifyCleanState();
    }

    @Test
    public void shouldShowAllHiddenStaticRowsInCollapsedState() {
        // set last two rows in second group as static
        this.rowGroupHeaderLayer.addStaticRowIndexesToGroup(0, 4, 6, 7);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        // collapse group with static indexes
        this.rowGroupHeaderLayer.collapseGroup(4);

        assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // hide all static rows
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 4, 5))) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertFalse(group.isCollapsed());
            assertEquals("Facts", group.getName());
            assertEquals(8, group.getStartIndex());
            assertEquals(8, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(3, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(8, cell.getRowIndex());
            assertEquals(3, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Row not hidden");
        }

        // show all rows again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(4);
            assertTrue(group.isCollapsed());
            assertEquals(4, group.getStartIndex());
            assertEquals(6, group.getVisibleStartIndex());
            assertEquals(4, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowPosition());
            assertEquals(6, cell.getRowIndex());
            assertEquals(2, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand group with static indexes
        this.rowGroupHeaderLayer.expandGroup(4);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertFalse(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldHideShowFirstGroupInCollapsedState() {
        // collapse group without static indexes
        this.rowGroupHeaderLayer.collapseGroup(0);

        assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // hide visible column in group
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 0))) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertFalse(group.isCollapsed());
            assertEquals("Address", group.getName());
            assertEquals(4, group.getStartIndex());
            assertEquals(4, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(4, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(0, cell.getRowPosition());
            assertEquals(4, cell.getRowIndex());
            assertEquals(4, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());

            // check group by name
            group = this.rowGroupHeaderLayer.getGroupByName("Person");
            assertNotNull(group);
            assertEquals(0, group.getStartIndex());
            assertEquals(-1, group.getVisibleStartIndex());
            assertEquals(-1, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(0, group.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show all columns again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            group = this.rowGroupHeaderLayer.getGroupByPosition(0);
            assertTrue(group.isCollapsed());
            assertEquals(0, group.getStartIndex());
            assertEquals(0, group.getVisibleStartIndex());
            assertEquals(0, group.getVisibleStartPosition());
            assertEquals(4, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(0, cell.getRowPosition());
            assertEquals(0, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // expand group
        this.rowGroupHeaderLayer.expandGroup(0);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        verifyCleanState();
    }

    @Test
    public void shouldHideShowLastGroupInCollapsedState() {
        // collapse last group without static indexes
        this.rowGroupHeaderLayer.collapseGroup(11);

        assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(11);
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(11, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(11, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // hide visible row in group
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 11))) {
            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            group = this.rowGroupHeaderLayer.getGroupByPosition(11);
            assertNull(group);

            // check group by name
            group = this.rowGroupHeaderLayer.getGroupByName("Personal");
            assertNotNull(group);
            // it is the last column so we where not able to update
            assertEquals(11, group.getStartIndex());
            assertEquals(-1, group.getVisibleStartIndex());
            assertEquals(-1, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(0, group.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show all rows again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(12, this.rowGroupExpandCollapseLayer.getRowCount());

            group = this.rowGroupHeaderLayer.getGroupByPosition(11);
            assertTrue(group.isCollapsed());
            assertEquals(11, group.getStartIndex());
            assertEquals(11, group.getVisibleStartIndex());
            assertEquals(11, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(1, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
            assertEquals(11, cell.getOriginRowPosition());
            assertEquals(11, cell.getRowPosition());
            assertEquals(11, cell.getRowIndex());
            assertEquals(1, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // scroll to show the last row only a bit

        // expand group
        this.rowGroupHeaderLayer.expandGroup(11);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        verifyCleanState();
    }

    @Test
    public void shouldHideShowLastGroupInCollapsedStateWithStatics() {
        this.rowGroupHeaderLayer.addStaticRowIndexesToGroup(0, 11, 12, 13);

        // collapse last group with static indexes
        this.rowGroupHeaderLayer.collapseGroup(11);

        assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(11);
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(12, group.getVisibleStartIndex());
        assertEquals(11, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(11, cell.getRowPosition());
        assertEquals(12, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());

        // hide visible row in group
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 11, 12))) {
            assertEquals(11, this.rowGroupExpandCollapseLayer.getRowCount());

            group = this.rowGroupHeaderLayer.getGroupByPosition(11);
            assertNull(group);

            // check group by name
            group = this.rowGroupHeaderLayer.getGroupByName("Personal");
            assertNotNull(group);
            // it is the last column so we where not able to update
            assertEquals(11, group.getStartIndex());
            assertEquals(-1, group.getVisibleStartIndex());
            assertEquals(-1, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(0, group.getVisibleSpan());
        } else {
            fail("Row not hidden");
        }

        // show all rows again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            assertEquals(13, this.rowGroupExpandCollapseLayer.getRowCount());

            group = this.rowGroupHeaderLayer.getGroupByPosition(11);
            assertTrue(group.isCollapsed());
            assertEquals(11, group.getStartIndex());
            assertEquals(12, group.getVisibleStartIndex());
            assertEquals(11, group.getVisibleStartPosition());
            assertEquals(3, group.getOriginalSpan());
            assertEquals(2, group.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
            assertEquals(11, cell.getOriginRowPosition());
            assertEquals(11, cell.getRowPosition());
            assertEquals(12, cell.getRowIndex());
            assertEquals(2, cell.getRowSpan());
            assertEquals(1, cell.getColumnSpan());
        } else {
            fail("Rows not shown again");
        }

        // scroll to show the last row only a bit

        // expand group
        this.rowGroupHeaderLayer.expandGroup(11);

        assertEquals(14, this.rowGroupExpandCollapseLayer.getRowCount());

        verifyCleanState();
    }

    @Test
    public void shouldHideLastRowInLastGroup() {
        // special test case for hide operations at the end of a table. The
        // HideRowEvent is not transported up to the RowGroupHeaderLayer
        // because the conversion is not able to convert the position outside
        // the new structure and the start position will be -1

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 250);
            }

        });

        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 13))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
            assertEquals(11, cell.getOriginRowPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(220, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(2, cell.getRowSpan());
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            // modifed verifyCleanState as we changed the client area
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
            assertEquals(11, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(220, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(8, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(11, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleRowsAfterFirstHideAtEndOfTable() {
        // special test case for hide operations at the end of a table. The
        // HideRowEvent is not transported up to the RowGroupHeaderLayer
        // because the conversion is not able to convert the position outside
        // the new structure and the start position will be -1

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 250);
            }

        });

        // first hide the first row in the last group
        if (this.selectionLayer.doCommand(new RowHideCommand(this.selectionLayer, 11))) {
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
            assertEquals(11, cell.getOriginRowPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(220, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(2, cell.getRowSpan());
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);
        } else {
            fail("Row not hidden");
        }

        // now hide the last row of the previous group and the now first
        // row of the last group. this looks like a contiguous selection, but
        // internally there is a gap. the second range
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 10, 11))) {
            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(8);
            assertEquals(8, group1.getStartIndex());
            assertEquals(8, group1.getVisibleStartIndex());
            assertEquals(8, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(2, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(10);
            assertEquals(11, group2.getStartIndex());
            assertEquals(13, group2.getVisibleStartIndex());
            assertEquals(10, group2.getVisibleStartPosition());
            assertEquals(3, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 10);
            assertEquals(10, cell.getOriginRowPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(200, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(1, cell.getRowSpan());
            assertEquals(20, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            // modifed verifyCleanState as we changed the client area
            ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
            assertEquals(11, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(220, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(8, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
            assertEquals(11, group4.getStartIndex());
            assertEquals(11, group4.getVisibleStartIndex());
            assertEquals(11, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(3, group4.getVisibleSpan());
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldHideMultipleRowsAfterCollapseWithStaticsAtEndOfTable() {
        // special test case for hide operations at the end of a table. The
        // HideRowEvent is not transported up to the RowGroupHeaderLayer
        // because the conversion is not able to convert the position outside
        // the new structure and the start position will be -1

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 250);
            }

        });

        // set last two rows in the last group static
        this.rowGroupHeaderLayer.addStaticRowIndexesToGroup(0, 11, 12, 13);

        // first collapse the last group
        this.rowGroupHeaderLayer.collapseGroup(11);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(12, cell.getRowIndex());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(220, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(2, cell.getRowSpan());
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(11);
        assertTrue(group2.isCollapsed());
        assertEquals(11, group2.getStartIndex());
        assertEquals(12, group2.getVisibleStartIndex());
        assertEquals(11, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());

        // now hide the last row of the previous group and the now first
        // row of the last group. this looks like a contiguous selection, but
        // internally there is a gap. the second range
        if (this.selectionLayer.doCommand(new MultiRowHideCommand(this.selectionLayer, 10, 11))) {
            Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(8);
            assertEquals(8, group1.getStartIndex());
            assertEquals(8, group1.getVisibleStartIndex());
            assertEquals(8, group1.getVisibleStartPosition());
            assertEquals(3, group1.getOriginalSpan());
            assertEquals(2, group1.getVisibleSpan());

            group2 = this.rowGroupHeaderLayer.getGroupByPosition(10);
            assertEquals(11, group2.getStartIndex());
            assertEquals(13, group2.getVisibleStartIndex());
            assertEquals(10, group2.getVisibleStartPosition());
            assertEquals(3, group2.getOriginalSpan());
            assertEquals(1, group2.getVisibleSpan());

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 10);
            assertEquals(10, cell.getOriginRowPosition());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(200, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(1, cell.getRowSpan());
            assertEquals(20, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);
        } else {
            fail("Row not hidden");
        }

        // show again
        if (this.gridLayer.doCommand(new ShowAllRowsCommand())) {
            // modifed verifyCleanState as we changed the client area
            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
            assertEquals(0, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Person", cell.getDataValue());
            assertEquals(0, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
            assertEquals(4, cell.getOriginRowPosition());
            assertEquals(4, cell.getRowSpan());
            assertEquals("Address", cell.getDataValue());
            assertEquals(80, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(80, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
            assertEquals(8, cell.getOriginRowPosition());
            assertEquals(3, cell.getRowSpan());
            assertEquals("Facts", cell.getDataValue());
            assertEquals(160, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(60, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
            assertEquals(11, cell.getOriginRowPosition());
            assertEquals(2, cell.getRowSpan());
            assertEquals("Personal", cell.getDataValue());
            assertEquals(220, cell.getBounds().y);
            assertEquals(0, cell.getBounds().x);
            assertEquals(40, cell.getBounds().height);
            assertEquals(20, cell.getBounds().width);

            Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
            assertEquals(0, group1.getStartIndex());
            assertEquals(0, group1.getVisibleStartIndex());
            assertEquals(0, group1.getVisibleStartPosition());
            assertEquals(4, group1.getOriginalSpan());
            assertEquals(4, group1.getVisibleSpan());

            group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
            assertEquals(4, group2.getStartIndex());
            assertEquals(4, group2.getVisibleStartIndex());
            assertEquals(4, group2.getVisibleStartPosition());
            assertEquals(4, group2.getOriginalSpan());
            assertEquals(4, group2.getVisibleSpan());

            Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
            assertEquals(8, group3.getStartIndex());
            assertEquals(8, group3.getVisibleStartIndex());
            assertEquals(8, group3.getVisibleStartPosition());
            assertEquals(3, group3.getOriginalSpan());
            assertEquals(3, group3.getVisibleSpan());

            Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
            assertTrue(group4.isCollapsed());
            assertEquals(11, group4.getStartIndex());
            assertEquals(12, group4.getVisibleStartIndex());
            assertEquals(11, group4.getVisibleStartPosition());
            assertEquals(3, group4.getOriginalSpan());
            assertEquals(2, group4.getVisibleSpan());
        } else {
            fail("Rows not shown again");
        }
    }

    @Test
    public void shouldExpandOnRemoveGroupByPosition() {
        this.rowGroupHeaderLayer.collapseGroup(4);

        Collection<Integer> hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertEquals(3, hiddenRowIndexes.size());
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(7)));

        assertEquals(11, this.selectionLayer.getRowCount());

        this.rowGroupHeaderLayer.removeGroup(4);

        hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertTrue(hiddenRowIndexes.isEmpty());

        assertEquals(14, this.selectionLayer.getRowCount());
    }

    @Test
    public void shouldExpandOnRemoveGroupByName() {
        this.rowGroupHeaderLayer.collapseGroup(0);

        Collection<Integer> hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertEquals(3, hiddenRowIndexes.size());
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(1)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(2)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(3)));

        assertEquals(11, this.selectionLayer.getRowCount());

        this.rowGroupHeaderLayer.removeGroup("Person");

        hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertTrue(hiddenRowIndexes.isEmpty());

        assertEquals(14, this.selectionLayer.getRowCount());
    }

    @Test
    public void shouldExpandOnRemovePositionFromGroup() {
        this.rowGroupHeaderLayer.collapseGroup("Address");

        Collection<Integer> hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertEquals(3, hiddenRowIndexes.size());
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(7)));

        assertEquals(11, this.selectionLayer.getRowCount());

        // Note: we can only remove the visible position
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 4);

        hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertTrue(hiddenRowIndexes.isEmpty());

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(5);
        assertEquals("Address", group.getName());
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        assertEquals(14, this.selectionLayer.getRowCount());
    }

    @Test
    public void shouldExpandOnRemovePositionsFromMultipleGroups() {
        this.rowGroupHeaderLayer.collapseGroup("Person");
        this.rowGroupHeaderLayer.collapseGroup("Address");

        Collection<Integer> hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertEquals(6, hiddenRowIndexes.size());
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(1)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(2)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(3)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(7)));

        assertEquals(8, this.selectionLayer.getRowCount());

        // Note: we can only remove the visible position
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 0, 1);

        hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertTrue(hiddenRowIndexes.isEmpty());

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(0));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(1);
        assertEquals("Person", group.getName());
        assertEquals(1, group.getStartIndex());
        assertEquals(1, group.getVisibleStartIndex());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(5);
        assertEquals("Address", group.getName());
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        assertEquals(14, this.selectionLayer.getRowCount());
    }

    @Test
    public void shouldExpandOnAddPositionToGroup() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 7);

        this.rowGroupHeaderLayer.collapseGroup("Address");

        Collection<Integer> hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertEquals(2, hiddenRowIndexes.size());
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(6)));

        assertEquals(12, this.selectionLayer.getRowCount());

        this.rowGroupHeaderLayer.addPositionsToGroup("Address", 7);

        hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertTrue(hiddenRowIndexes.isEmpty());

        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        verifyCleanState();
    }

    @Test
    public void shouldExpandOnClearGroups() {

        // increase visible area to show all
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1500, 300);
            }

        });

        this.rowGroupHeaderLayer.collapseGroup(11);
        this.rowGroupHeaderLayer.collapseGroup(8);
        this.rowGroupHeaderLayer.collapseGroup("Address");
        this.rowGroupHeaderLayer.collapseGroup("Person");

        Collection<Integer> hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertEquals(10, hiddenRowIndexes.size());
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(1)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(2)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(3)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(5)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(6)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(7)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(9)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(10)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(12)));
        assertTrue(hiddenRowIndexes.contains(Integer.valueOf(13)));

        assertEquals(4, this.selectionLayer.getRowCount());

        this.rowGroupHeaderLayer.clearAllGroups();

        hiddenRowIndexes = this.rowGroupExpandCollapseLayer.getHiddenRowIndexes();
        assertTrue(hiddenRowIndexes.isEmpty());

        assertEquals(14, this.rowGroupHeaderLayer.getRowCount());

        assertTrue(this.rowGroupHeaderLayer.getGroupModel().getGroups().isEmpty());

        ILayerCell cell = null;
        for (int i = 0; i < 14; i++) {
            cell = this.rowGroupHeaderLayer.getCellByPosition(0, i);
            assertEquals(i, cell.getRowPosition());
            assertEquals(1, cell.getRowSpan());
            assertEquals(2, cell.getColumnSpan());
        }
    }

    @Test
    public void shouldCollapseExpandAll() {
        this.rowGroupHeaderLayer.collapseAllGroups();

        assertEquals(4, this.rowGroupHeaderLayer.getRowCount());

        // verify collapsed states
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertNotNull(group);
        assertEquals("Person", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(1);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(1, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertNotNull(group);
        assertEquals("Facts", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(2, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(3);
        assertNotNull(group);
        assertEquals("Personal", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        // expand all
        this.rowGroupHeaderLayer.expandAllGroups();
        verifyCleanState();
    }

    @Test
    public void shouldLoadStateWithExpandCollapseStates() {
        verifyCleanState();

        Properties properties = new Properties();
        this.gridLayer.saveState("clean", properties);

        // collapse
        this.rowGroupHeaderLayer.collapseGroup("Address");

        this.gridLayer.saveState("one", properties);

        // restore the clean state again
        this.gridLayer.loadState("clean", properties);

        verifyCleanState();

        // load single collapsed
        this.gridLayer.loadState("one", properties);

        assertEquals(11, this.selectionLayer.getRowCount());

        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        // collapse all
        this.rowGroupHeaderLayer.collapseAllGroups();

        this.gridLayer.saveState("all", properties);

        // load single collapsed
        this.gridLayer.loadState("one", properties);

        // verify only Address is collapsed and other groups are not
        // collapsed and in correct state
        assertEquals(11, this.selectionLayer.getRowCount());

        group = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertNotNull(group);
        assertEquals("Person", group.getName());
        assertFalse(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(5);
        assertNotNull(group);
        assertEquals("Facts", group.getName());
        assertFalse(group.isCollapsed());
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(8);
        assertNotNull(group);
        assertEquals("Personal", group.getName());
        assertFalse(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(8, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // now load all collapsed again
        this.gridLayer.loadState("all", properties);

        // verify all collapsed
        assertEquals(4, this.rowGroupHeaderLayer.getRowCount());

        // verify collapsed states
        group = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertNotNull(group);
        assertEquals("Person", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(1);
        assertNotNull(group);
        assertEquals("Address", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(1, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertNotNull(group);
        assertEquals("Facts", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(2, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupByPosition(3);
        assertNotNull(group);
        assertEquals("Personal", group.getName());
        assertTrue(group.isCollapsed());
        assertEquals(11, group.getStartIndex());
        assertEquals(11, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        // restore the clean state again
        this.gridLayer.loadState("clean", properties);

        verifyCleanState();
    }

    @Test
    public void shouldDragReorderWithinGroup() {
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 2));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 4));

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // no changes in the group
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderFirstRowWithinGroup() {
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 1));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 4));

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderToFirstRowWithinGroup() {
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 1));

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderUngroupLastRowInGroup() {
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 4));

        // group header cell has less column span
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldNotDragReorderUngroupMiddleRowInGroup() {
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 3));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 3));

        // group header cell has not changed
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // group has not changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderDownAddUngroupedToGroupAsFirstRow() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(3));

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same row to add to next group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 5));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(3, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderUpAddUngroupedToGroupAsLastRow() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same column to add to next group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 4));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 4));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderUngroupFirstRowInGroup() {
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group1.getStartIndex());
        assertEquals(5, group1.getVisibleStartIndex());
        assertEquals(5, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderBetweenGroupsUp() {
        // second row in second group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 6));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 4));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderFirstRowBetweenGroupsUp() {
        // first row in second group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 4));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderToFirstRowBetweenGroupsUp() {
        // first row in second group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 1));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderBetweenGroupsDown() {
        // last row in first group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 4));
        // to middle of second group
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderFirstRowBetweenGroupsDown() {
        // middle row in first group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 3));
        // to first position in second group
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderToFirstRowBetweenGroupsDown() {
        // first row in first group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 1));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group.getStartIndex());
        assertEquals(1, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderWithinGroup() {
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 2, 4));

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // no changes in the group
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderFirstRowWithinGroup() {
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 1, 4));

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderToFirstRowWithinGroup() {
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 1));

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupLastRowInGroup() {
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 4));

        // group header cell has less row span
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldNotReorderUngroupMiddleRowInGroup() {
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 3, 3));

        // group header cell has not changed
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // group has not changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderDownAddUngroupedToGroupAsFirstRow() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // no changes in the group header cell
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(3));

        // only the visible start index should have changed
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same row to add to next group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 5));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(3, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderUpAddUngroupedToGroupAsLastRow() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // drag reorder in same row to add to next group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 4));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible start index should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupFirstRowInGroup() {
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group1.getStartIndex());
        assertEquals(5, group1.getVisibleStartIndex());
        assertEquals(5, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
    }

    @Test
    public void shouldReorderBetweenGroupsUp() {
        // second row in second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 6, 4));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderFirstRowBetweenGroupsUp() {
        // first row in second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 4));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderToFirstRowBetweenGroupsUp() {
        // first column in second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 1));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderBetweenGroupsDown() {
        // last row in first group
        // to middle of second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderFirstRowBetweenGroupsDown() {
        // middle row in first group
        // to first position in second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 3, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderToFirstRowBetweenGroupsDown() {
        // first row in first group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 1, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // check group
        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group.getStartIndex());
        assertEquals(1, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupedAddRowToGroupDown() {
        // remove group 1
        this.rowGroupHeaderLayer.removeGroup(0);

        // reorder third row to second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 3, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(2, cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupedAddRowToGroupUp() {
        // remove group 2
        this.rowGroupHeaderLayer.removeGroup(4);

        // reorder fifth row to first group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 6, 3));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupRowFromGroupUp() {
        // remove group 1
        this.rowGroupHeaderLayer.removeGroup(0);

        // reorder sixth row in second group to second row
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 6, 3));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(2, cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(6, cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(3, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderUngroupRowGroupDown() {
        // remove group 2
        this.rowGroupHeaderLayer.removeGroup(4);

        // reorder third row out of group 1
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 3, 7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(3, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleUngroupedAddRowToGroupDown() {
        // remove group 1
        this.rowGroupHeaderLayer.removeGroup(0);

        // reorder first and third row to second group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(1, 3), 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(2, cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(6, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(120, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(2, group.getVisibleStartPosition());
        assertEquals(6, group.getOriginalSpan());
        assertEquals(6, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleUngroupedAddRowToGroupUp() {
        // remove group 2
        this.rowGroupHeaderLayer.removeGroup(4);

        // reorder fifth and seventh row to first group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(5, 7), 2));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(6, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(120, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(6, cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(6, group.getOriginalSpan());
        assertEquals(6, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleRowsFromOneGroupToOtherGroupDown() {
        // reorder first and third row to second group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(1, 3), 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(6, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(120, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(2, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(6, group2.getOriginalSpan());
        assertEquals(6, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleRowsFromOneGroupToOtherGroupUp() {
        // reorder fifth and seventh row to first group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(5, 7), 2));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(6, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(120, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(6, group1.getOriginalSpan());
        assertEquals(6, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(6, group2.getVisibleStartPosition());
        assertEquals(2, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleRowsInsideGroupDown() {
        // reorder first two columns in second group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(5, 6), 9));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(6, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleRowsInsideGroupUp() {
        // reorder first two rows in second group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(7, 8), 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(6, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleRowsInsideGroupToUngroupDown() {
        // reorder last two rows in second group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(7, 8), 9));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(7, cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(2, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6));
    }

    @Test
    public void shouldReorderMultipleRowsInsideGroupToUngroupUp() {
        // reorder first two rows in second group
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(5, 6), 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(6, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(6, group.getStartIndex());
        assertEquals(6, group.getVisibleStartIndex());
        assertEquals(6, group.getVisibleStartPosition());
        assertEquals(2, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5));
    }

    @Test
    public void shouldReorderMultipleUngroupedNotAddRowToGroupDownOnEdge() {
        // remove group 1
        this.rowGroupHeaderLayer.removeGroup(0);

        // reorder first and third row to second group
        // nothing should happen as multi row reorder is not possible via UI
        // drag and drop
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(2, 4), 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(3, cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(2, cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(2));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3));

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());
    }

    @Test
    public void shouldReorderMultipleUngroupedAddRowToGroupUpOnEdge() {
        // remove group 2
        this.rowGroupHeaderLayer.removeGroup(4);

        // reorder fourth and sixth row to first group
        // nothing should happen as multi row reorder is not possible via UI
        // drag and drop
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(5, 7), 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(7, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(6, cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5));
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderToUnbreakable() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a row from first group to second
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 2, 5));

        // nothing should have been changed
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderFromUnbreakable() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a row from second group to first
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 2));

        // nothing should have been changed
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderToUnbreakable() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a row from first group to second
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 2));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 6));

        // nothing should have been changed
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderFromUnbreakable() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a row from second group to first
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 6));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 2));

        // nothing should have been changed
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderMultipleToUnbreakable() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a row from first group to second
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(2, 3), 6));

        // nothing should have been changed
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnDragReorderMultipleFromUnbreakable() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // try to reorder a row from second group to first
        this.gridLayer.doCommand(new MultiRowReorderCommand(this.gridLayer, Arrays.asList(6, 7), 2));

        // nothing should have been changed
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderToUnbreakableEdgeDown() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // remove first group
        this.rowGroupHeaderLayer.removeGroup(0);

        // try to reorder row 4 to second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 5));

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(1));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(2));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3));

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderToUnbreakableEdgeUp() {
        // set first group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);

        // remove second group
        this.rowGroupHeaderLayer.removeGroup(4);

        // try to reorder row 4 to first group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 5));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7));

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldNotBreakUnbreakableGroupOnReorderBetweenGroupsDown() {
        // set second group unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        // remove first group
        this.rowGroupHeaderLayer.removeGroup(0);

        // try to reorder row 4 to second group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 9));

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(1));
        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(2));

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7));

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderUnbreakableGroupsBetweenGroupsUp() {
        // set all groups unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 3 between group 1 and 2
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 9, 5));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertEquals("Address", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldReorderUnbreakableGroupsToStartUp() {
        // set all groups unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 3 to start
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 9, 1));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(8, group1.getStartIndex());
        assertEquals(8, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
        assertEquals("Facts", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertEquals("Address", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldReorderUnbreakableGroupsToEndDown() {
        // increase the client area to show all columns
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 300);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        // set all groups unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 2 to end
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 15));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(4, group4.getStartIndex());
        assertEquals(4, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(4, group4.getVisibleSpan());
        assertEquals("Address", group4.getName());
    }

    @Test
    public void shouldReorderUnbreakableGroupsDown() {
        // increase the client area to show all columns
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        // set all groups unbreakable
        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(8, true);
        this.rowGroupHeaderLayer.setGroupUnbreakable(11, true);

        // try to reorder group 1 between 2 and 3
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 1, 9));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Facts", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldDragReorderEntireRowGroupToStart() {
        // reorder second group to first
        this.gridLayer.doCommand(new RowGroupReorderStartCommand(this.gridLayer, 0, 6));
        this.gridLayer.doCommand(new RowGroupReorderEndCommand(this.gridLayer, 0, 1));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderEntireRowGroupToStart() {
        // reorder second group to first
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 1));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderEntireRowGroupToLast() {
        // configure the visible area, needed for tests in scrolled state
        // increase the height so the first four rows and therefore the first
        // group is not completely out of the viewport when showing the last row
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                // 9 rows + column header should be visible
                return new Rectangle(0, 0, 1010, 200);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        // start reorder second group
        this.gridLayer.doCommand(new RowGroupReorderStartCommand(this.gridLayer, 0, 6));

        // scroll to show last row
        this.gridLayer.doCommand(new ShowRowInViewportCommand(13));

        // end reorder to last position
        this.gridLayer.doCommand(new RowGroupReorderEndCommand(this.gridLayer, 0, 10));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(-1, cell.getOriginRowPosition());
        assertEquals(9, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(-20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(12, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // the position is related to the positionLayer, which is the
        // SelectionLayer
        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
        assertEquals(4, group4.getStartIndex());
        assertEquals(4, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(4, group4.getVisibleSpan());
        assertEquals("Address", group4.getName());
    }

    @Test
    public void shouldReorderEntireRowGroupToLast() {
        // reorder second group to first
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 1));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldReorderEntireRowGroupBetweenOtherGroups() {
        // reorder third group between first and second first
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 9, 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(8, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
    }

    @Test
    public void shouldNotUngroupOnReorderEntireGroupToGroupStart() {
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 5));
        verifyCleanState();
    }

    @Test
    public void shouldNotUngroupOnReorderEntireGroupToGroupEnd() {
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 6, 9));
        verifyCleanState();
    }

    @Test
    public void shouldReorderGroupToNonVisibleArea() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder first group to be the second group
        this.rowGroupHeaderLayer.reorderRowGroup(0, 0, 8);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Facts", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldReorderGroupToNonVisibleAreaEnd() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder first group to be the last group
        this.rowGroupHeaderLayer.reorderRowGroup(0, 0, 14);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(0, group4.getStartIndex());
        assertEquals(0, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(4, group4.getVisibleSpan());
        assertEquals("Person", group4.getName());
    }

    @Test
    public void shouldReorderGroupFromNonVisibleArea() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder last group to be the first group
        this.rowGroupHeaderLayer.reorderRowGroup(0, 11, 0);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(11, group1.getStartIndex());
        assertEquals(11, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
        assertEquals("Personal", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertEquals("Address", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(8, group4.getStartIndex());
        assertEquals(8, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Facts", group4.getName());
    }

    @Test
    public void shouldReorderGroupOutsideVisibleArea() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder last group to be the third group
        this.rowGroupHeaderLayer.reorderRowGroup(0, 11, 8);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Address", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(8, group4.getStartIndex());
        assertEquals(8, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Facts", group4.getName());
    }

    @Test
    public void shouldReorderGroupToNonVisibleAreaWithCommand() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder first group to be the second group
        this.gridLayer.doCommand(
                new RowGroupReorderCommand(this.rowGroupHeaderLayer.getPositionLayer(), 0, 0, 8, false));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Facts", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldReorderGroupToNonVisibleAreaEndWithCommand() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder first group to be the last group
        this.gridLayer.doCommand(
                new RowGroupReorderCommand(this.rowGroupHeaderLayer.getPositionLayer(), 0, 0, 14, false));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(0, group4.getStartIndex());
        assertEquals(0, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(4, group4.getVisibleSpan());
        assertEquals("Person", group4.getName());
    }

    @Test
    public void shouldReorderGroupFromNonVisibleAreaWithCommand() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder last group to be the first group
        this.gridLayer.doCommand(
                new RowGroupReorderCommand(this.rowGroupHeaderLayer.getPositionLayer(), 0, 11, 0, false));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(11, group1.getStartIndex());
        assertEquals(11, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());
        assertEquals("Personal", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group3.getStartIndex());
        assertEquals(4, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertEquals("Address", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(8, group4.getStartIndex());
        assertEquals(8, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Facts", group4.getName());
    }

    @Test
    public void shouldReorderGroupOutsideVisibleAreaWithCommand() {
        // reduce the client area to only show the first and half of the second
        // group
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1100, 140);
            }

        });
        this.gridLayer.doCommand(
                new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        assertEquals(7, this.gridLayer.getRowCount());

        // reorder last group to be the third group
        this.gridLayer.doCommand(
                new RowGroupReorderCommand(this.rowGroupHeaderLayer.getPositionLayer(), 0, 11, 8, false));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertEquals("Address", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(8, group4.getStartIndex());
        assertEquals(8, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Facts", group4.getName());
    }

    // reordering with expand/collapse

    @Test
    public void shouldReorderLeftAddUngroupedToCollapsedGroupUp() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // collapse first group
        this.rowGroupHeaderLayer.collapseGroup(0);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(1, group1.getVisibleSpan());

        // reorder in same column to add to next group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 2, 2));

        // added column is not shown as part of collapsed group
        // group is collapsed and therefore the added column is hidden
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible span should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(1, group1.getVisibleSpan());

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(1);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(1, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(0);

        verifyCleanState();
    }

    @Test
    public void shouldReorderUpAddUngroupedToCollapsedGroupUpJump() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // collapse first group
        this.rowGroupHeaderLayer.collapseGroup(0);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(1, group1.getVisibleSpan());

        // reorder in same row to add to next group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 2, 1));

        // added row is not shown as part of collapsed group
        // group is collapsed and therefore the added row is hidden
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // the visible span and the start index should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(1, group1.getVisibleSpan());

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(1);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(1, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(0);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderUpAddUngroupedToCollapsedGroupDown() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());

        // reorder down to add to next group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 5));

        // added row is shown as part of collapsed group
        // group is collapsed and therefore the added row is shown as first
        // row
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the visible span should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(3, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(1, group1.getVisibleSpan());

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group1.getStartIndex());
        assertEquals(8, group1.getVisibleStartIndex());
        assertEquals(4, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(3);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(3, group2.getStartIndex());
        assertEquals(3, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(5, group2.getOriginalSpan());
        assertEquals(5, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderDownAddUngroupedToCollapsedGroupUp() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 4);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4));

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(5);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());

        // reorder down to add to next group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 6));

        // added row is shown as visible row in collapsed group
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the original span should have changed
        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        verifyCleanState();
    }

    @Test
    public void shouldReorderDownAddUngroupedToCollapsedGroupDown() {
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 4);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4));

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(5);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        // reorder down to add to next group at the end
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 7));

        // added row is not shown as visible row in collapsed group
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the original span should have changed
        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderInsideCollapsedGroupWithStatics() {
        Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        group1.addStaticIndexes(1, 2);

        // collapse
        this.rowGroupHeaderLayer.collapseGroup(this.groupModel, group1);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reorder first visible to last visible inside a group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 1, 3));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(2, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // expand
        this.rowGroupHeaderLayer.expandGroup(0);

        verifyCleanState();
    }

    @Test
    public void shouldReorderInsideCollapsedGroupWithStaticsFromBeginning() {
        Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        group1.addStaticIndexes(0, 1);

        // collapse
        this.rowGroupHeaderLayer.collapseGroup(this.groupModel, group1);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reorder first visible to last visible inside a group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 1, 3));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reorder again first visible to last visible inside a group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 1, 3));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // expand
        this.rowGroupHeaderLayer.expandGroup(0);

        // verify expanded state, slightly different than the clean state
        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(2, group1.getStartIndex());
        assertEquals(2, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldDragReorderInsideCollapsedGroupWithStaticsFromBeginning() {
        Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        group1.addStaticIndexes(0, 1);

        // collapse
        this.rowGroupHeaderLayer.collapseGroup(this.groupModel, group1);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reorder first visible to last visible inside a group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 1));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 3));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reorder again first visible to last visible inside a group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 1));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 3));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // expand
        this.rowGroupHeaderLayer.expandGroup(0);

        // verify expanded state, slightly different than the clean state
        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(2, group1.getStartIndex());
        assertEquals(2, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderLeftAddColumnToCollapsedGroupWithStatics() {
        Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        group1.addStaticIndexes(1, 2);

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // collapse first group
        this.rowGroupHeaderLayer.collapseGroup(0);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        // reorder in same row to add to next group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 3, 3));

        // added row is not shown as part of collapsed group
        // group is collapsed and therefore the added row is hidden
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the original span should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(2);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(2, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(0);

        verifyCleanState();
    }

    @Test
    public void shouldDragReorderUpAddRowToCollapsedGroupWithStatics() {
        Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        group1.addStaticIndexes(1, 2);

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        // collapse first group
        this.rowGroupHeaderLayer.collapseGroup(0);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        // reorder in same row to add to next group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 3));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 3));

        // added row is not shown as part of collapsed group
        // group is collapsed and therefore the added row is hidden
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the original span should have changed
        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(2);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(2, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(0);

        verifyCleanState();
    }

    @Test
    public void shouldReorderDownAddRowToCollapsedGroupWithStatics() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(5);
        group.addStaticIndexes(5, 6);

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 4);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4));

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(5);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        // reorder down to add to next group at the beginning
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 6));

        // added row is not shown as visible row in collapsed group
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the original span should have changed
        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(6, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        verifyCleanState();
    }

    @Test
    public void shouldReorderUpAddRowToCollapsedGroupWithStaticsInGroupUpperEdge() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(5);
        group.addStaticIndexes(5, 6);

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 7);

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(7, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(8, cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7));

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(8, cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertEquals(4, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        // reorder to top to add to previous group at the beginning
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 7, 5));

        // added row is not shown as visible row in collapsed group
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // only the original span should have changed
        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(6, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        verifyCleanState();
    }

    @Test
    public void shouldReorderDownAddRowToCollapsedGroupWithStaticsInGroupBottomEdge() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(5);
        group.addStaticIndexes(5, 6);

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(5);

        // reorder down to add to next group at the end
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 7));

        // added row is not shown as visible row in collapsed group
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        // fourth row at the end of Address group
        assertEquals(3, this.rowGroupHeaderLayer.getRowIndexByPosition(7));

        // nothing hidden below the SelectionLayer
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(5, group2.getOriginalSpan());
        assertEquals(5, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderDownAddRowToCollapsedGroupWithStaticsInMiddleOfGroup() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(5);
        group.addStaticIndexes(5, 6);

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(5);

        // reorder down to add to next group in the middle
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 6));

        // added row is not shown as visible row in collapsed group
        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(3);
        assertEquals(4, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(3, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(2, group.getVisibleSpan());

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(8, group.getStartIndex());
        assertEquals(8, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        // fourth row in middle of Address group
        assertEquals(3, this.rowGroupHeaderLayer.getRowIndexByPosition(5));

        // nothing hidden below the SelectionLayer
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(3, group1.getOriginalSpan());
        assertEquals(3, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(3, group2.getVisibleStartPosition());
        assertEquals(5, group2.getOriginalSpan());
        assertEquals(5, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderUpRemoveColumnFromCollapsedGroup() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        assertEquals(3, group.getMembers().size());
        assertTrue(group.getMembers().contains(5));
        assertTrue(group.getMembers().contains(6));
        assertTrue(group.getMembers().contains(7));
    }

    @Test
    public void shouldReorderFromCollapsedGroupUpToRemoveAndRightToAddRowAgain() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 6));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        assertEquals(4, group.getMembers().size());
        assertTrue(group.getMembers().contains(4));
        assertTrue(group.getMembers().contains(5));
        assertTrue(group.getMembers().contains(6));
        assertTrue(group.getMembers().contains(7));

        verifyCleanState();
    }

    @Test
    public void shouldDragReorderFromCollapsedGroupUpToRemoveAndDownToAddRowAgain() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(5, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 6));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group.getStartIndex());
        assertEquals(4, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        assertEquals(4, group.getMembers().size());
        assertTrue(group.getMembers().contains(4));
        assertTrue(group.getMembers().contains(5));
        assertTrue(group.getMembers().contains(6));
        assertTrue(group.getMembers().contains(7));

        verifyCleanState();
    }

    @Test
    public void shouldReorderDownRemoveRowFromCollapsedGroup() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        assertEquals(3, group.getMembers().size());
        assertTrue(group.getMembers().contains(5));
        assertTrue(group.getMembers().contains(6));
        assertTrue(group.getMembers().contains(7));
    }

    @Test
    public void shouldReorderFromCollapsedGroupDownToRemoveAndRightToAddRowAgain() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));

        Group group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 6, 6));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group.getStartIndex());
        assertEquals(5, group.getVisibleStartIndex());
        assertEquals(4, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(1, group.getVisibleSpan());
        assertTrue(group.isCollapsed());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        // verifyCleanState modified through reorder
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        assertEquals(4, this.rowGroupHeaderLayer.getRowIndexByPosition(7));

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(4, group.getMembers().size());
        assertTrue(group.getMembers().contains(4));
        assertTrue(group.getMembers().contains(5));
        assertTrue(group.getMembers().contains(6));
        assertTrue(group.getMembers().contains(7));
    }

    @Test
    public void shouldReorderUpRemoveFromCollapsedGroupAddToOtherGroup() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 3));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(0));
        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(6));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        // verifyCleanState modified through reorder
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());

        assertEquals(4, this.rowGroupHeaderLayer.getRowIndexByPosition(2));

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(5, group1.getMembers().size());
        assertTrue(group1.getMembers().contains(0));
        assertTrue(group1.getMembers().contains(1));
        assertTrue(group1.getMembers().contains(2));
        assertTrue(group1.getMembers().contains(3));
        assertTrue(group1.getMembers().contains(4));

        assertEquals(3, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(6));
        assertTrue(group2.getMembers().contains(7));
    }

    @Test
    public void shouldDragReorderUpRemoveFromCollapsedGroupAddToOtherGroup() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 3));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(5, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(0));
        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(6));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        // verifyCleanState modified through reorder
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(100, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());

        assertEquals(4, this.rowGroupHeaderLayer.getRowIndexByPosition(2));

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(5, group1.getMembers().size());
        assertTrue(group1.getMembers().contains(0));
        assertTrue(group1.getMembers().contains(1));
        assertTrue(group1.getMembers().contains(2));
        assertTrue(group1.getMembers().contains(3));
        assertTrue(group1.getMembers().contains(4));

        assertEquals(3, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(6));
        assertTrue(group2.getMembers().contains(7));
    }

    @Test
    public void shouldReorderDownRemoveFromCollapsedGroupAddToOtherGroup() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        // the out of the group triggers expand
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(7, cell.getRowPosition());
        assertEquals(8, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertFalse(group3.isCollapsed());

        // verifyCleanState modified through reorder
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(4, this.rowGroupHeaderLayer.getRowIndexByPosition(8));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(4, group3.getMembers().size());
        assertTrue(group3.getMembers().contains(8));
        assertTrue(group3.getMembers().contains(9));
        assertTrue(group3.getMembers().contains(10));
        assertTrue(group3.getMembers().contains(4));

        assertEquals(3, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(6));
        assertTrue(group2.getMembers().contains(7));
    }

    @Test
    public void shouldDragReorderDownRemoveFromCollapsedGroupAddToOtherGroup() {
        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(7, cell.getRowPosition());
        assertEquals(8, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());
        assertFalse(group3.isCollapsed());

        // verifyCleanState modified through reorder
        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(4, this.rowGroupHeaderLayer.getRowIndexByPosition(8));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(4, group3.getOriginalSpan());
        assertEquals(4, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(4, group3.getMembers().size());
        assertTrue(group3.getMembers().contains(8));
        assertTrue(group3.getMembers().contains(9));
        assertTrue(group3.getMembers().contains(10));
        assertTrue(group3.getMembers().contains(4));

        assertEquals(3, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(6));
        assertTrue(group2.getMembers().contains(7));
    }

    @Test
    public void shouldReorderUpRowFromCollapsedGroupWithStatics() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        group.addStaticIndexes(5, 6);

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        // since the visible static row is not the first row in the group,
        // the reorder will move the row to the first position in the group
        // and expand it, but it will still be part of the group and not be
        // ungrouped
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 5));

        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // the start index has changed
        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(4, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(4));
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(6));
        assertTrue(group2.getMembers().contains(7));
    }

    @Test
    public void shouldDragReorderUpRowFromCollapsedGroupWithStatics() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        group.addStaticIndexes(5, 6);

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        // since the visible static row is not the first row in the group,
        // the reorder will move the row to the first position in the group
        // and expand it, but it will still be part of the group and not be
        // ungrouped
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 5));

        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // the start index has changed
        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(4, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(4));
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(6));
        assertTrue(group2.getMembers().contains(7));
    }

    @Test
    public void shouldReorderDownRemoveRowFromCollapsedGroupWithStatics() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        group.addStaticIndexes(5, 6);

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        // reorder the most right static row in a collapsed group will
        // ungroup, works because of reordering to right reorders correctly in
        // the lower layers
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 6, 7));

        assertEquals(12, this.selectionLayer.getRowCount());

        assertFalse(this.rowGroupHeaderLayer.isPartOfAGroup(5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(7, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
        assertTrue(group2.isCollapsed());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5));

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(6, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(7, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(7, cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // the span has changed
        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7));

        group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(3, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(4));
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(7));

        assertEquals(1, group2.getStaticIndexes().size());
        assertTrue(group2.getStaticIndexes().contains(5));
    }

    @Test
    public void shouldDragReorderDownRemoveRowFromCollapsedGroupWithStatics() {
        Group group = this.rowGroupHeaderLayer.getGroupByPosition(4);
        group.addStaticIndexes(5, 6);

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        // reorder the most right static row in a collapsed group will
        // ungroup, works because of reordering to right reorders correctly in
        // the lower layers
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 6));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 7));

        assertEquals(12, this.selectionLayer.getRowCount());

        assertFalse(this.rowGroupHeaderLayer.isPartOfAGroup(5));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(7, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
        assertTrue(group2.isCollapsed());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5));

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(6);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(6, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        // expand again
        this.rowGroupHeaderLayer.expandGroup(4);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 7);
        assertEquals(7, cell.getOriginRowPosition());
        assertEquals(7, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(7, cell.getDataValue());
        assertEquals(140, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        // the span has changed
        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        assertNull(this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7));

        group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());

        assertEquals(3, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(4));
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(7));

        assertEquals(1, group2.getStaticIndexes().size());
        assertTrue(group2.getStaticIndexes().contains(5));
    }

    @Test
    public void shouldNotRemoveGroupOnReorderLastRowInGroup() {
        // if a group has only one row left, a reorder operation on the same
        // row should not remove the group to avoid that by accident a group
        // is destroyed

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 5, 6, 7);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 5));

        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));

        assertEquals(4, this.rowGroupHeaderLayer.getGroupModel().getGroups().size());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(1, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
    }

    @Test
    public void shouldNotRemoveGroupOnDragReorderLastRowInGroup() {
        // if a group has only one row left, a reorder operation on the same
        // row should not remove the group to avoid that by accident a group
        // is destroyed

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 5, 6, 7);

        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 5));

        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));

        assertEquals(4, this.rowGroupHeaderLayer.getGroupModel().getGroups().size());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(1, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
    }

    @Test
    public void shouldNotRemoveGroupOnReorderLastRowInGroupToNonGrouped() {
        // if a group has only one row left, a reorder operation to a
        // position between some non grouped rows should not remove the group
        // to avoid that by accident a group is destroyed, as it could also mean
        // the group itself was reordered

        this.rowGroupHeaderLayer.removeGroup(8);
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 5, 6, 7);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 10));

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));
        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(8));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(9));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(10));

        assertEquals(3, this.rowGroupHeaderLayer.getGroupModel().getGroups().size());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(8, group2.getVisibleStartPosition());
        assertEquals(1, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
    }

    @Test
    public void shouldRemoveGroupOnReorderLastRowInGroupToOtherGroup() {
        // if a group has only one row left, a reorder operation to another
        // group should remove the group as the reorder operation is clearly to
        // another position

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 5, 6, 7);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 2));

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));

        assertEquals(3, this.rowGroupHeaderLayer.getGroupModel().getGroups().size());

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
    }

    @Test
    public void shouldNotRemoveGroupOnReorderLastRowInGroupToUnbreakableGroup() {
        // reorder to an unbreakable group should never work

        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 5, 6, 7);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 2));

        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));

        assertEquals(4, this.rowGroupHeaderLayer.getGroupModel().getGroups().size());

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(1, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
    }

    @Test
    public void shouldNotRemoveGroupOnReorderRemoveLastFromCollapsed() {
        // if a group has only one row left, a reorder operation on the same
        // row should not remove the group to avoid that by accident a group
        // is destroyed

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 5, 6, 7);
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 5));

        assertNotNull(this.rowGroupHeaderLayer.getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));

        assertEquals(4, this.rowGroupHeaderLayer.getGroupModel().getGroups().size());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(1, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
    }

    @Test
    public void shouldRemoveGroupOnReorderLastFromCollapsedGroupToOtherGroup() {
        // if a group has only one row left, a reorder operation to another
        // group should remove the group as the reorder operation is clearly to
        // another position

        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 5, 6, 7);
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 2));

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));

        assertEquals(3, this.rowGroupHeaderLayer.getGroupModel().getGroups().size());

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(5, group1.getOriginalSpan());
        assertEquals(5, group1.getVisibleSpan());
    }

    @Test
    public void shouldNotExpandCollapsedGroupOnReorderLeft() {
        this.rowGroupHeaderLayer.collapseGroup(4);

        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 1));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(1, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(1);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(1, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldNotExpandCollapsedGroupOnDragReorderToStart() {
        this.rowGroupHeaderLayer.collapseGroup(4);

        // reorder second group to first
        this.gridLayer.doCommand(new RowGroupReorderStartCommand(this.gridLayer, 0, 5));
        this.gridLayer.doCommand(new RowGroupReorderEndCommand(this.gridLayer, 0, 1));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(1, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(1);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(1, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
    }

    @Test
    public void shouldNotExpandCollapsedGroupOnReorderDown() {
        // increase the client area to show all rows
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        this.rowGroupHeaderLayer.collapseGroup(0);

        // try to reorder group 1 between 2 and 3
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 1, 6));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(0, group2.getStartIndex());
        assertEquals(0, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
        assertEquals("Person", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(5, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Facts", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(8, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
        assertEquals("Personal", group4.getName());
    }

    @Test
    public void shouldNotExpandCollapsedGroupOnDragReorderToEnd() {
        // increase the client area to show all columns
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        this.rowGroupHeaderLayer.collapseGroup(0);

        // try to reorder group 1 to end
        this.gridLayer.doCommand(new RowGroupReorderStartCommand(this.gridLayer, 0, 1));
        this.gridLayer.doCommand(new RowGroupReorderEndCommand(this.gridLayer, 0, 12));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
        assertEquals(0, group4.getStartIndex());
        assertEquals(0, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(1, group4.getVisibleSpan());
        assertEquals("Person", group4.getName());
    }

    @Test
    public void shouldNotExpandCollapsedGroupOnReorderToEnd() {
        // increase the client area to show all rows
        this.gridLayer.setClientAreaProvider(new IClientAreaProvider() {

            @Override
            public Rectangle getClientArea() {
                return new Rectangle(0, 0, 1600, 250);
            }

        });
        this.gridLayer.doCommand(new ClientAreaResizeCommand(new Shell(Display.getDefault(), SWT.V_SCROLL | SWT.H_SCROLL)));

        this.rowGroupHeaderLayer.collapseGroup(0);

        // try to reorder group 1 to end
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 1, 12));

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(4, group1.getStartIndex());
        assertEquals(4, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Address", group1.getName());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(8, group2.getStartIndex());
        assertEquals(8, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertEquals("Facts", group2.getName());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(7);
        assertEquals(11, group3.getStartIndex());
        assertEquals(11, group3.getVisibleStartIndex());
        assertEquals(7, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertEquals("Personal", group3.getName());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(10);
        assertEquals(0, group4.getStartIndex());
        assertEquals(0, group4.getVisibleStartIndex());
        assertEquals(10, group4.getVisibleStartPosition());
        assertEquals(4, group4.getOriginalSpan());
        assertEquals(1, group4.getVisibleSpan());
        assertEquals("Person", group4.getName());
    }

    @Test
    public void shouldAvoidReorderGroupInOtherGroup() {
        // try to reorder group 2 into group 1
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 3));

        // nothing should have happened
        verifyCleanState();
    }

    @Test
    public void shouldAvoidReorderCollapsedGroupInOtherGroup() {
        this.rowGroupHeaderLayer.collapseGroup(4);

        // try to reorder group 2 into group 1
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 3));

        // nothing should have happened
        assertEquals(11, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
        assertTrue(group2.isCollapsed());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(5);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(5, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(8, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Test
    public void shouldReorderToLastWithHidden() {
        // remove last item from second group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 7);

        // hide new last row in second group
        this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 7));

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        // reorder removed row to second group again
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 7, 7));

        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        // show all again
        this.gridLayer.doCommand(new ShowAllRowsCommand());

        verifyCleanState();
    }

    @Test
    public void shouldReorderToFirstWithHidden() {
        // remove first item from second group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 4);

        // hide new first row in second group
        this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 6));

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(6, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        // reorder removed row to second group again
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 5, 6));

        assertEquals(5, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());

        // show all again
        this.gridLayer.doCommand(new ShowAllRowsCommand());

        assertEquals(14, this.selectionLayer.getRowCount());

        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        assertEquals(4, group2.getMembers().size());
        assertTrue(group2.getMembers().contains(4));
        assertTrue(group2.getMembers().contains(5));
        assertTrue(group2.getMembers().contains(6));
        assertTrue(group2.getMembers().contains(7));
    }

    @Test
    public void shouldReorderInsideGroupWithHiddenColumnsAtEnd() {

        // hide last two rows at group end
        this.gridLayer.doCommand(new MultiRowHideCommand(this.gridLayer, 3, 4));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group1 = this.rowGroupHeaderLayer.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reorder first visible to last visible inside a group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 1, 3));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reorder again first visible to last visible inside a group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 1, 3));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(2);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(2, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // show all again
        this.gridLayer.doCommand(new ShowAllRowsCommand());

        // verify expanded state, slightly different than the clean state
        for (int row = 0; row < this.rowGroupHeaderLayer.getRowCount(); row++) {
            assertTrue(this.rowGroupHeaderLayer.isPartOfAGroup(row));
            assertFalse(this.rowGroupHeaderLayer.isPartOfAnUnbreakableGroup(row));
        }

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 8);
        assertEquals(8, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(160, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        // this cell is not visible because of the client area
        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 11);
        assertEquals(11, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowSpan());
        assertEquals("Personal", cell.getDataValue());
        assertEquals(-1, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(0, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group1 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(0);
        assertEquals(2, group1.getStartIndex());
        assertEquals(2, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        group2 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        Group group3 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(8);
        assertEquals(8, group3.getStartIndex());
        assertEquals(8, group3.getVisibleStartIndex());
        assertEquals(8, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());

        Group group4 = this.rowGroupHeaderLayer.getGroupModel().getGroupByPosition(11);
        assertEquals(11, group4.getStartIndex());
        assertEquals(11, group4.getVisibleStartIndex());
        assertEquals(11, group4.getVisibleStartPosition());
        assertEquals(3, group4.getOriginalSpan());
        assertEquals(3, group4.getVisibleSpan());
    }

    @Ignore
    @Test
    public void shouldReorderToFirstWithHiddenAndCollapsed() {
        // hide first in third group
        this.gridLayer.doCommand(new RowHideCommand(this.gridLayer, 9));

        // collapse second group
        this.rowGroupHeaderLayer.collapseGroup(4);

        // reorder to remove last in second group
        this.gridLayer.doCommand(new RowReorderStartCommand(this.gridLayer, 5));
        this.gridLayer.doCommand(new RowReorderEndCommand(this.gridLayer, 6));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(9, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
        assertTrue(group2.isCollapsed());

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));

        Group group3 = this.rowGroupHeaderLayer.getGroupByPosition(6);
        assertEquals(8, group3.getStartIndex());
        assertEquals(9, group3.getVisibleStartIndex());
        assertEquals(6, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(2, group3.getVisibleSpan());
        assertFalse(group3.isCollapsed());

        // show all again
        this.gridLayer.doCommand(new ShowAllRowsCommand());

        assertEquals(12, this.selectionLayer.getRowCount());

        // FIXME Street should stay ungrouped left of the Facts group

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(7, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(9, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Facts", cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        group2 = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
        assertTrue(group2.isCollapsed());

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));

        group3 = this.rowGroupHeaderLayer.getGroupByPosition(6);
        assertEquals(8, group3.getStartIndex());
        assertEquals(9, group3.getVisibleStartIndex());
        assertEquals(6, group3.getVisibleStartPosition());
        assertEquals(3, group3.getOriginalSpan());
        assertEquals(3, group3.getVisibleSpan());
        assertFalse(group3.isCollapsed());
    }

    @Test
    public void shouldCreateRowGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();
        groupModel.clear();

        assertTrue(groupModel.isEmpty());

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 0, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, true, false));

        assertEquals(4, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new CreateRowGroupCommand("Person"));

        assertEquals(1, groupModel.size());

        Group group = groupModel.getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());
        assertFalse(group.isCollapsed());
    }

    @Test
    public void shouldCreateTwoRowGroupsWithSameName() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();
        groupModel.clear();

        assertTrue(groupModel.isEmpty());

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 0, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, true, false));

        assertEquals(4, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new CreateRowGroupCommand("Person"));

        // create a second group with the same name
        // this fails with the old column grouping
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 4, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 7, true, false));

        assertEquals(4, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new CreateRowGroupCommand("Person"));

        assertEquals(2, groupModel.size());

        Group group1 = groupModel.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Group group2 = groupModel.getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());
    }

    @Test
    public void shouldCreateGroupFromUncontiguous() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();
        groupModel.clear();

        assertTrue(groupModel.isEmpty());

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 0, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 2, false, true));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 4, false, true));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 6, false, true));

        assertEquals(4, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new CreateRowGroupCommand("Person"));

        assertEquals(1, groupModel.size());

        Group group = groupModel.getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(4, group.getOriginalSpan());
        assertEquals(4, group.getVisibleSpan());
        assertFalse(group.isCollapsed());

        Collection<Integer> members = group.getMembers();
        assertEquals(4, members.size());
        assertTrue(members.contains(0));
        assertTrue(members.contains(2));
        assertTrue(members.contains(4));
        assertTrue(members.contains(6));

        assertEquals(0, this.rowGroupHeaderLayer.getRowIndexByPosition(0));
        assertEquals(2, this.rowGroupHeaderLayer.getRowIndexByPosition(1));
        assertEquals(4, this.rowGroupHeaderLayer.getRowIndexByPosition(2));
        assertEquals(6, this.rowGroupHeaderLayer.getRowIndexByPosition(3));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldUngroupLastItemInGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, false, false));

        assertEquals(1, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        assertEquals(4, groupModel.size());

        Group group = groupModel.getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
        assertFalse(group.isCollapsed());

        Collection<Integer> members = group.getMembers();
        assertEquals(3, members.size());
        assertTrue(members.contains(0));
        assertTrue(members.contains(1));
        assertTrue(members.contains(2));

        assertNull(groupModel.getGroupByPosition(3));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldUngroupFirstItemInGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 4, false, false));

        assertEquals(1, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        assertEquals(4, groupModel.size());

        Group group1 = groupModel.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Collection<Integer> members = group1.getMembers();
        assertEquals(4, members.size());
        assertTrue(members.contains(0));
        assertTrue(members.contains(1));
        assertTrue(members.contains(2));
        assertTrue(members.contains(3));

        Group group2 = groupModel.getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(3, group2.getOriginalSpan());
        assertEquals(3, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        members = group2.getMembers();
        assertEquals(3, members.size());
        assertTrue(members.contains(5));
        assertTrue(members.contains(6));
        assertTrue(members.contains(7));

        assertNull(groupModel.getGroupByPosition(4));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldUngroupMiddleItemInGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 2, false, false));

        assertEquals(1, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        assertEquals(4, groupModel.size());

        Group group = groupModel.getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(3, group.getOriginalSpan());
        assertEquals(3, group.getVisibleSpan());
        assertFalse(group.isCollapsed());

        Collection<Integer> members = group.getMembers();
        assertEquals(3, members.size());
        assertTrue(members.contains(0));
        assertTrue(members.contains(1));
        assertTrue(members.contains(3));

        assertNull(groupModel.getGroupByPosition(3));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(3, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(60, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(3, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);
    }

    @Test
    public void shouldUngroupMultipleFirstItemsInGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 4, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 5, false, true));

        assertEquals(2, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        assertEquals(4, groupModel.size());

        Group group1 = groupModel.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Collection<Integer> members = group1.getMembers();
        assertEquals(4, members.size());
        assertTrue(members.contains(0));
        assertTrue(members.contains(1));
        assertTrue(members.contains(2));
        assertTrue(members.contains(3));

        assertNull(groupModel.getGroupByPosition(4));
        assertNull(groupModel.getGroupByPosition(5));

        Group group2 = groupModel.getGroupByPosition(6);
        assertEquals(6, group2.getStartIndex());
        assertEquals(6, group2.getVisibleStartIndex());
        assertEquals(6, group2.getVisibleStartPosition());
        assertEquals(2, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        members = group2.getMembers();
        assertEquals(2, members.size());
        assertTrue(members.contains(6));
        assertTrue(members.contains(7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(5, cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 5);
        assertEquals(5, cell.getOriginRowPosition());
        assertEquals(5, cell.getRowPosition());
        assertEquals(5, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(6, cell.getDataValue());
        assertEquals(100, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 6);
        assertEquals(6, cell.getOriginRowPosition());
        assertEquals(6, cell.getRowPosition());
        assertEquals(6, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(120, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldUngroupMultipleLastItemsInGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 2, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, false, true));

        assertEquals(2, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        assertEquals(4, groupModel.size());

        Group group1 = groupModel.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(2, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Collection<Integer> members = group1.getMembers();
        assertEquals(2, members.size());
        assertTrue(members.contains(0));
        assertTrue(members.contains(1));

        assertNull(groupModel.getGroupByPosition(2));
        assertNull(groupModel.getGroupByPosition(3));

        Group group2 = groupModel.getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        members = group2.getMembers();
        assertEquals(4, members.size());
        assertTrue(members.contains(4));
        assertTrue(members.contains(5));
        assertTrue(members.contains(6));
        assertTrue(members.contains(7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(3, cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldUngroupMultipleItemsInMiddleOfGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 1, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 2, false, true));

        assertEquals(2, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        assertEquals(4, groupModel.size());

        Group group1 = groupModel.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(2, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Collection<Integer> members = group1.getMembers();
        assertEquals(2, members.size());
        assertTrue(members.contains(0));
        assertTrue(members.contains(3));

        assertNull(groupModel.getGroupByPosition(2));
        assertNull(groupModel.getGroupByPosition(3));

        Group group2 = groupModel.getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        members = group2.getMembers();
        assertEquals(4, members.size());
        assertTrue(members.contains(4));
        assertTrue(members.contains(5));
        assertTrue(members.contains(6));
        assertTrue(members.contains(7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 2);
        assertEquals(2, cell.getOriginRowPosition());
        assertEquals(2, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(2, cell.getDataValue());
        assertEquals(40, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(2, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(3, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldUngroupMultipleItemsFirstLastOfGroup() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 0, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, false, true));

        assertEquals(2, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        assertEquals(4, groupModel.size());

        assertNull(groupModel.getGroupByPosition(0));

        Group group1 = groupModel.getGroupByPosition(1);
        assertEquals(1, group1.getStartIndex());
        assertEquals(1, group1.getVisibleStartIndex());
        assertEquals(1, group1.getVisibleStartPosition());
        assertEquals(2, group1.getOriginalSpan());
        assertEquals(2, group1.getVisibleSpan());
        assertFalse(group1.isCollapsed());

        Collection<Integer> members = group1.getMembers();
        assertEquals(2, members.size());
        assertTrue(members.contains(1));
        assertTrue(members.contains(2));

        assertNull(groupModel.getGroupByPosition(3));

        Group group2 = groupModel.getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());
        assertFalse(group2.isCollapsed());

        members = group2.getMembers();
        assertEquals(4, members.size());
        assertTrue(members.contains(4));
        assertTrue(members.contains(5));
        assertTrue(members.contains(6));
        assertTrue(members.contains(7));

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(0, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 1);
        assertEquals(1, cell.getOriginRowPosition());
        assertEquals(1, cell.getRowPosition());
        assertEquals(1, cell.getRowIndex());
        assertEquals(2, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Person", cell.getDataValue());
        assertEquals(20, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(40, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 3);
        assertEquals(3, cell.getOriginRowPosition());
        assertEquals(3, cell.getRowPosition());
        assertEquals(3, cell.getRowIndex());
        assertEquals(1, cell.getRowSpan());
        assertEquals(2, cell.getColumnSpan());
        assertEquals(4, cell.getDataValue());
        assertEquals(60, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(20, cell.getBounds().height);
        assertEquals(60, cell.getBounds().width);

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 4);
        assertEquals(4, cell.getOriginRowPosition());
        assertEquals(4, cell.getRowPosition());
        assertEquals(4, cell.getRowIndex());
        assertEquals(4, cell.getRowSpan());
        assertEquals(1, cell.getColumnSpan());
        assertEquals("Address", cell.getDataValue());
        assertEquals(80, cell.getBounds().y);
        assertEquals(0, cell.getBounds().x);
        assertEquals(80, cell.getBounds().height);
        assertEquals(20, cell.getBounds().width);
    }

    @Test
    public void shouldUpdateGroupOnCreate() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();
        groupModel.removeGroup(4);
        groupModel.removeGroup(8);
        groupModel.removeGroup(11);

        assertEquals(1, groupModel.size());

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 4, false, true));

        assertEquals(2, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new CreateRowGroupCommand("Person"));

        assertEquals(1, groupModel.size());

        Group group = groupModel.getGroupByPosition(0);
        assertEquals(0, group.getStartIndex());
        assertEquals(0, group.getVisibleStartIndex());
        assertEquals(0, group.getVisibleStartPosition());
        assertEquals(5, group.getOriginalSpan());
        assertEquals(5, group.getVisibleSpan());
        assertFalse(group.isCollapsed());
    }

    @Test
    public void shouldNotModifyUnbreakableGroupOnCreate() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();
        groupModel.removeGroup(4);
        groupModel.removeGroup(8);
        groupModel.removeGroup(11);

        assertEquals(1, groupModel.size());

        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 4, false, true));

        assertEquals(2, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new CreateRowGroupCommand("Test"));

        assertEquals(2, groupModel.size());

        Group group1 = groupModel.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());
        assertFalse(group1.isCollapsed());

        Group group2 = groupModel.getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(1, group2.getOriginalSpan());
        assertEquals(1, group2.getVisibleSpan());
        assertEquals("Test", group2.getName());
        assertFalse(group2.isCollapsed());
    }

    @Test
    public void shouldNotModifyUnbreakableGroupOnCreateAndCreateNewNonContiguous() {
        GroupModel groupModel = this.rowGroupHeaderLayer.getGroupModel();
        groupModel.removeGroup(4);
        groupModel.removeGroup(8);
        groupModel.removeGroup(11);

        assertEquals(1, groupModel.size());

        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 2, false, false));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 5, false, true));
        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 7, false, true));

        assertEquals(3, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new CreateRowGroupCommand("Test"));

        assertEquals(2, groupModel.size());

        Group group1 = groupModel.getGroupByPosition(0);
        assertEquals(0, group1.getStartIndex());
        assertEquals(0, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());
        assertEquals("Person", group1.getName());
        assertFalse(group1.isCollapsed());

        assertNull(groupModel.getGroupByPosition(4));

        Group group2 = groupModel.getGroupByPosition(5);
        assertEquals(5, group2.getStartIndex());
        assertEquals(5, group2.getVisibleStartIndex());
        assertEquals(5, group2.getVisibleStartPosition());
        assertEquals(2, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());
        assertEquals("Test", group2.getName());
        assertFalse(group2.isCollapsed());

        assertNull(groupModel.getGroupByPosition(8));

        assertEquals(7, this.rowGroupHeaderLayer.getRowIndexByPosition(6));
    }

    @Test
    public void shouldNotUngroupFromUnbreakableGroup() {
        this.rowGroupHeaderLayer.setGroupUnbreakable(0, true);

        this.selectionLayer.doCommand(new SelectRowsCommand(this.selectionLayer, 0, 3, false, false));

        assertEquals(1, PositionUtil.getPositions(this.selectionLayer.getSelectedRowPositions()).length);

        this.gridLayer.doCommand(new UngroupRowCommand());

        // revert unbreakable change so verifyCleanState is correct
        this.rowGroupHeaderLayer.setGroupUnbreakable(0, false);
        verifyCleanState();
    }

    @Test
    public void shouldRemoveRowGroup() {
        this.gridLayer.doCommand(new RemoveRowGroupCommand(4));

        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(4));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(5));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(6));
        assertNull(this.rowGroupHeaderLayer.getGroupByPosition(7));
        assertNull(this.rowGroupHeaderLayer.getGroupByName("Address"));

        assertEquals(3, this.rowGroupHeaderLayer.getGroupModel().size());
    }

    @Test
    public void shouldNotRemoveUnbreakableColumnGroup() {
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, true);

        this.gridLayer.doCommand(new RemoveRowGroupCommand(4));

        // revert unbreakable change so verifyCleanState is correct
        this.rowGroupHeaderLayer.setGroupUnbreakable(4, false);
        verifyCleanState();
    }

    // TODO shouldRenameColumnGroup
    // currently not testable because the DisplayColumnGroupRenameDialogCommand
    // opens the dialog and directly sets the column group name afterwards.
    // Needs to be fixed with NatTable 2.0 to only set the name and not opening
    // a dialog.

    @Test
    public void shouldReturnConfigLabels() {
        // check expanded column group
        LabelStack stack = this.rowGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(2, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.ROW_GROUP_HEADER));
        assertTrue(stack.hasLabel(GroupHeaderConfigLabels.GROUP_EXPANDED_CONFIG_TYPE));

        // check collapsed column group
        this.rowGroupHeaderLayer.collapseGroup(0);
        stack = this.rowGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(2, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.ROW_GROUP_HEADER));
        assertTrue(stack.hasLabel(GroupHeaderConfigLabels.GROUP_COLLAPSED_CONFIG_TYPE));

        // expand again as positions are visible and otherwise we cannot remove
        // a row from the group
        this.rowGroupHeaderLayer.expandGroup(0);

        // remove last row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // check ungrouped
        stack = this.rowGroupHeaderLayer.getConfigLabelsByPosition(0, 3);
        assertEquals(0, stack.getLabels().size());
    }

    @Test
    public void shouldReturnConfigLabelsWithAccumulator() {
        // set config label accumulator
        this.rowGroupHeaderLayer.setConfigLabelAccumulator(new IConfigLabelAccumulator() {

            @Override
            public void accumulateConfigLabels(LabelStack configLabels, int columnPosition, int rowPosition) {
                if (rowPosition == 0 || rowPosition == 3) {
                    configLabels.addLabel("custom");
                }
            }
        });

        // check expanded row group
        LabelStack stack = this.rowGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(3, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.ROW_GROUP_HEADER));
        assertTrue(stack.hasLabel("custom"));
        assertTrue(stack.hasLabel(GroupHeaderConfigLabels.GROUP_EXPANDED_CONFIG_TYPE));

        // check collapsed row group
        this.rowGroupHeaderLayer.collapseGroup(0);
        stack = this.rowGroupHeaderLayer.getConfigLabelsByPosition(0, 0);
        assertEquals(3, stack.getLabels().size());
        assertTrue(stack.hasLabel(GridRegion.ROW_GROUP_HEADER));
        assertTrue(stack.hasLabel("custom"));
        assertTrue(stack.hasLabel(GroupHeaderConfigLabels.GROUP_COLLAPSED_CONFIG_TYPE));

        // expand again as positions are visible and otherwise we cannot remove
        // a row from the group
        this.rowGroupHeaderLayer.expandGroup(0);

        // remove last row from first group
        this.rowGroupHeaderLayer.removePositionsFromGroup(0, 3);

        // check ungrouped
        stack = this.rowGroupHeaderLayer.getConfigLabelsByPosition(0, 3);
        assertEquals(0, stack.getLabels().size());
    }

    @Test
    public void shouldCalculateColumnWidthByPosition() {
        this.rowGroupHeaderLayer.clearAllGroups();
        this.rowGroupHeaderLayer.setColumnWidth(100);
        // Width of the header row column - see fixture
        assertEquals(140, this.rowGroupHeaderLayer.getWidth());
        assertEquals(2, this.rowGroupHeaderLayer.getColumnCount());
        assertEquals(100, this.rowGroupHeaderLayer.getColumnWidthByPosition(0));
        assertEquals(40, this.rowGroupHeaderLayer.getColumnWidthByPosition(1));
        // Test calculated width
        this.rowGroupHeaderLayer.setCalculateWidth(true);
        assertEquals(40, this.rowGroupHeaderLayer.getWidth());
        assertEquals(2, this.rowGroupHeaderLayer.getColumnCount());
        assertEquals(0, this.rowGroupHeaderLayer.getColumnWidthByPosition(0));
        assertEquals(40, this.rowGroupHeaderLayer.getColumnWidthByPosition(1));
    }

    @Test
    public void shouldCalculateColumnWidthOnGroupModelChanges() {
        this.rowGroupHeaderLayer.setCalculateWidth(true);

        assertEquals(60, this.rowGroupHeaderLayer.getWidth());
        assertEquals(2, this.rowGroupHeaderLayer.getColumnCount());

        ILayerCell cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());

        this.rowGroupHeaderLayer.clearAllGroups();

        assertEquals(40, this.rowGroupHeaderLayer.getWidth());
        assertEquals(2, this.rowGroupHeaderLayer.getColumnCount());
        assertEquals(0, this.rowGroupHeaderLayer.getColumnWidthByPosition(0));
        assertEquals(40, this.rowGroupHeaderLayer.getColumnWidthByPosition(1));

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());

        this.rowGroupHeaderLayer.setCalculateWidth(false);

        assertEquals(60, this.rowGroupHeaderLayer.getWidth());
        assertEquals(2, this.rowGroupHeaderLayer.getColumnCount());

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnPosition());
        assertEquals(2, cell.getColumnSpan());

        // add group again
        this.rowGroupHeaderLayer.addGroup("Person", 0, 4);

        assertEquals(60, this.rowGroupHeaderLayer.getWidth());
        assertEquals(2, this.rowGroupHeaderLayer.getColumnCount());

        cell = this.rowGroupHeaderLayer.getCellByPosition(0, 0);
        assertEquals("Person", cell.getDataValue());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(4, cell.getRowSpan());
        assertEquals(0, cell.getOriginColumnPosition());
        assertEquals(0, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());

        cell = this.rowGroupHeaderLayer.getCellByPosition(1, 0);
        assertEquals(1, cell.getDataValue());
        assertEquals(0, cell.getOriginRowPosition());
        assertEquals(0, cell.getRowPosition());
        assertEquals(1, cell.getRowSpan());
        assertEquals(1, cell.getOriginColumnPosition());
        assertEquals(1, cell.getColumnPosition());
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void shouldSetGroupHeaderColumnWidth() {
        this.rowGroupHeaderLayer.setColumnWidth(100);
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldResizeRowGroupHeaderColumn() {
        this.gridLayer.doCommand(new ColumnResizeCommand(this.gridLayer, 0, 100));
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldResizeRowHeaderRow() {
        this.gridLayer.doCommand(new ColumnResizeCommand(this.gridLayer, 1, 100));
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(1));
    }

    @Test
    public void shouldResizeRowGroupHeaderColumnWithoutDownScale() {
        IDpiConverter dpiConverter = new AbstractDpiConverter() {

            @Override
            protected void readDpiFromDisplay() {
                this.dpi = 120;
            }

        };
        this.gridLayer.doCommand(new ConfigureScalingCommand(dpiConverter, dpiConverter));

        // scaling enabled, therefore default height of 20 pixels is up scaled
        // to 25
        assertEquals(25, this.gridLayer.getColumnWidthByPosition(0));

        this.gridLayer.doCommand(new ColumnResizeCommand(this.gridLayer, 0, 100));
        assertEquals(125, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldResizeRowGroupHeaderColumnWithDownScale() {
        IDpiConverter dpiConverter = new AbstractDpiConverter() {

            @Override
            protected void readDpiFromDisplay() {
                this.dpi = 120;
            }

        };
        this.gridLayer.doCommand(new ConfigureScalingCommand(dpiConverter, dpiConverter));

        // scaling enabled, therefore default width of 20 pixels is up scaled
        // to 25
        assertEquals(25, this.gridLayer.getColumnWidthByPosition(0));

        this.gridLayer.doCommand(new ColumnResizeCommand(this.gridLayer, 0, 100, true));

        // down scaling in the command was enabled, therefore the value set is
        // the value that will be returned
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldMultiResizeRowGroupHeaderColumn() {
        this.gridLayer.doCommand(new MultiColumnResizeCommand(this.gridLayer, new int[] { 0 }, 100));
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldMultiResizeRowGroupHeaderAndRowHeader() {
        this.gridLayer.doCommand(new MultiColumnResizeCommand(this.gridLayer, new int[] { 0, 1 }, 100));
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(0));
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(1));
    }

    @Test
    public void shouldMultiResizeRowGroupHeaderColumnWithoutDownScale() {
        IDpiConverter dpiConverter = new AbstractDpiConverter() {

            @Override
            protected void readDpiFromDisplay() {
                this.dpi = 120;
            }

        };
        this.gridLayer.doCommand(new ConfigureScalingCommand(dpiConverter, dpiConverter));

        // scaling enabled, therefore default height of 20 pixels is up scaled
        // to 25
        assertEquals(25, this.gridLayer.getColumnWidthByPosition(0));

        this.gridLayer.doCommand(new MultiColumnResizeCommand(this.gridLayer, new int[] { 0 }, 100));
        assertEquals(125, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldMultiResizeRowGroupHeaderColumnWithDownScale() {
        IDpiConverter dpiConverter = new AbstractDpiConverter() {

            @Override
            protected void readDpiFromDisplay() {
                this.dpi = 120;
            }

        };
        this.gridLayer.doCommand(new ConfigureScalingCommand(dpiConverter, dpiConverter));

        // scaling enabled, therefore default width of 20 pixels is up scaled
        // to 25
        assertEquals(25, this.gridLayer.getColumnWidthByPosition(0));

        this.gridLayer.doCommand(new MultiColumnResizeCommand(this.gridLayer, new int[] { 0 }, 100, true));

        // down scaling in the command was enabled, therefore the value set is
        // the value that will be returned
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldNotResizeNotResizableRowGroupHeaderColumn() {
        this.rowGroupHeaderLayer.setColumnPositionResizable(0, false);
        this.gridLayer.doCommand(new ColumnResizeCommand(this.gridLayer, 0, 100));
        assertEquals(20, this.gridLayer.getColumnWidthByPosition(0));
    }

    @Test
    public void shouldNotResizeNotResizableRowGroupHeaderColumnMulti() {
        this.rowGroupHeaderLayer.setColumnPositionResizable(0, false);
        this.gridLayer.doCommand(new MultiColumnResizeCommand(this.gridLayer, new int[] { 0, 1 }, 100));
        assertEquals(20, this.gridLayer.getColumnWidthByPosition(0));
        assertEquals(100, this.gridLayer.getColumnWidthByPosition(1));
    }

    @Test
    public void shouldHandleResetOfRowReordering() {
        Group group1 = this.groupModel.getGroupByPosition(0);
        group1.addStaticIndexes(0, 1);

        Group group2 = this.groupModel.getGroupByPosition(4);
        group2.addStaticIndexes(5, 6);

        // reorder some rows to the first position of a group
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 4, 1));
        this.gridLayer.doCommand(new RowReorderCommand(this.gridLayer, 8, 5));

        assertEquals(3, group1.getStartIndex());
        assertEquals(3, group1.getVisibleStartIndex());
        assertEquals(0, group1.getVisibleStartPosition());
        assertEquals(4, group1.getOriginalSpan());
        assertEquals(4, group1.getVisibleSpan());

        assertEquals(7, group2.getStartIndex());
        assertEquals(7, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        // reset reordering
        this.gridLayer.doCommand(new ResetRowReorderCommand());

        group1.removeStaticIndexes(0, 1);
        group2.removeStaticIndexes(5, 6);

        verifyCleanState();
    }

    @Test
    public void shouldReorderGroupWithHiddenColumns() {
        // remove the first row group
        this.rowGroupHeaderLayer.removeGroup(0);

        // hide the last two rows in the second group
        this.gridLayer.doCommand(new MultiRowHideCommand(this.gridLayer, 7, 8));

        Group group2 = this.rowGroupHeaderLayer.getGroupByPosition(4);
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(4, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());

        // reorder the second group to position 0
        this.gridLayer.doCommand(new RowGroupReorderCommand(this.gridLayer, 0, 5, 1));

        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(0, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(2, group2.getVisibleSpan());

        assertEquals(4, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(0));
        assertEquals(5, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(1));
        assertEquals(0, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(2));

        // show all rows again
        this.gridLayer.doCommand(new ShowAllRowsCommand());

        // we expect that the column group is intact
        assertEquals(4, group2.getStartIndex());
        assertEquals(4, group2.getVisibleStartIndex());
        assertEquals(0, group2.getVisibleStartPosition());
        assertEquals(4, group2.getOriginalSpan());
        assertEquals(4, group2.getVisibleSpan());

        assertEquals(4, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(0));
        assertEquals(5, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(1));
        assertEquals(6, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(2));
        assertEquals(7, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(3));
        assertEquals(0, this.rowGroupHeaderLayer.getPositionLayer().getRowIndexByPosition(4));
    }

    // TODO testcases with compositions that have no scrolling
    // TODO testcases with hierarchical tree layer
    // TODO testcases with freeze composition
}