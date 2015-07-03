/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bxdownloads;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JProgressBar;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author hp
 */
public class TablaDownloadModel extends AbstractTableModel implements Observer {
    // These are the names for the table's columns.

    private static final String[] columnNames = {"Sel","URL", "Size", "Downloaded", "Progress", "Status"};
    // These are the classes for each column's values.
    private static final Class[] columnClasses = {Boolean.class, String.class, String.class, String.class, JProgressBar.class, String.class};
    // The table's list of downloads.
    private ArrayList<Download> downloadList = new ArrayList();
    private ArrayList<String> links = new ArrayList<>();

    // Add a new download to the table.
    public void addDownload(Download download) {
        // Register to be notified when the download changes.
        download.addObserver(this);
        downloadList.add(download);
        links.add(download.getUrl());
        // Fire table row insertion notification to table.
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }

    // Get a download for the specified row.
    public Download getDownload(int row) {
        return (Download) downloadList.get(row);
    }

    public ArrayList<Download> getDownloadList() {
        return downloadList;
    }
    
    public ArrayList<String> getLinksList() {
        return links;
    }
    
    // Remove a download from the list.
    public void clearDownload(int row) {
        downloadList.remove(row);
        links.remove(row);
        // Fire table row deletion notification to table.
        fireTableRowsDeleted(row, row);
    }

    // Get table's column count.
    public int getColumnCount() {
        return columnNames.length;
    }

    // Get a column's name.
    public String getColumnName(int col) {
        return columnNames[col];
    }

    // Get a column's class.
    public Class getColumnClass(int col) {
        return columnClasses[col];
    }

    // Get table's row count.
    public int getRowCount() {
        return downloadList.size();
    }

    // Get value for a specific row and column combination.
    public Object getValueAt(int row, int col) {
        Download download = (Download) downloadList.get(row);
        switch (col) {
            case 0:
                return download.isSelected();
            case 1: // URL
                return download.getUrl();
            case 2: // Size
                long size = download.getTamaño();
                return (size == -1) ? "" : Long.toString(size)+ " B (" + org.bx.Utiles.formatearBytes(size)+")";
            case 3: // Size
                long desc = download.getDescargado();
                return (desc <=0) ? "" : Long.toString(desc) + " B (" + org.bx.Utiles.formatearBytes(desc)+")";
            case 4: // Progress
                return new Float(download.getProgreso());
            case 5: // Status
                return Download.ESTADOS[download.getEstado()];
        }
        return "";
    }

    /* Update is called when a Download notifies its
     observers of any changes */
    public void update(Observable o, Object arg) {
        int index = downloadList.indexOf(o);
        // Fire table row update notification to table.
        fireTableRowsUpdated(index, index);
    }
}
