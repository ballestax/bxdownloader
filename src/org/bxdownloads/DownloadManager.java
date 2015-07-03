/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bxdownloads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import org.bx.MyAction;
import org.dzur.rec.GetIcon;

/**
 *
 * @author hp
 */
public class DownloadManager extends JPanel implements Observer, Runnable {
    // Add download text field.

    private JTextField addTextField;
    // Download table's data model.
    private TablaDownloadModel tableModel;
    // Table listing downloads.
    private JTable tabla;
    // These are the buttons for managing the selected download.
    private JButton pauseButton, resumeButton;
    private JButton cancelButton, clearButton;
    private JButton cargarLista;
    private JButton btAutomatico;
    // Currently selected download.
    private Download selectedDownload;
    // Flag for whether or not table selection is being cleared.
    private boolean clearing;
    // Constructor for Download Manager.
    private final JButton iniciarButton;
    private XMLManager xml = new XMLManager();
    private Thread hilo;
    private final boolean corriendo;
    private Path path;
    public static final Color COLOR_SEL = new Color(225, 255, 170);
    public static final Color COLOR_BACK = new Color(225, 211, 255);
    public static final String AC_TODOS = "ac_todos";
    public static final String AC_NINGUNO = "ac_ninguno";
    public static final String AC_INVERTIR = "ac_invertir";

    private final JPopupMenu popup;
    private MyAction acIniciar, acPausar, acReanudar, acCancelar, acReiniciar, acBorrar, acInfo, acCopiarLink, acAutomatico;
    private static final String CARPETA_DESCARGAS = "downloads";
    private Path directorioDescarga;
    private String carpetaDescarga;
    public static final int MAX_DESCARGAS = 2;
    public static final int MAX_REINTENTO_ERROR = 3;
    private int descargasActivas = 0;
    private JCheckBox cbIniciar;
    private JCheckBox cbGoogleLinks;
    private boolean descargando;
    private Thread hiloAutomatico;
    private boolean automaticoEncendido;

    public DownloadManager() {
        boolean creada = configCarpetaDescargas();
        if (creada == false) {
            JOptionPane.showMessageDialog(null, "No se pudo crear la carpeta de descargas");
        }

        initActions();

        JPanel addPanel = new JPanel();
        addTextField = new JTextField(30);
        addPanel.add(addTextField);
        JButton addButton = new JButton("Add Download");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionAdd();
            }
        });
        addPanel.add(addButton);

        cbIniciar = new JCheckBox("Iniciar descarga");
        addPanel.add(cbIniciar);

        cargarLista = new JButton("Cargar Lista");
        cargarLista.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cargarListadoExterno();
            }
        });
        addPanel.add(cargarLista);

        automaticoEncendido = false;
        btAutomatico = new JButton(acAutomatico);
        addPanel.add(btAutomatico);

        cbGoogleLinks = new JCheckBox("Google Links");
        addPanel.add(cbGoogleLinks);

// Set up Downloads table.
        tableModel = new TablaDownloadModel();
        tabla = new JTable(tableModel);

        ListaSeleccion listaSeleccion = new ListaSeleccion();
        tabla.getTableHeader().addMouseListener(listaSeleccion);
        tabla.getColumnModel().getColumn(0).setHeaderRenderer(listaSeleccion);
        tabla.getTableHeader().setReorderingAllowed(false);

        tabla.getColumnModel().getColumn(0).setCellEditor(tabla.getDefaultEditor(Boolean.class));

        tabla.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

                tableSelectionChanged();
            }
        });
// Allow only one row at a time to be selected.
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
// Set up ProgressBar as renderer for progress column.
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true); // show progress text
        tabla.setDefaultRenderer(JProgressBar.class, renderer);
// Set table's row height large enough to fit JProgressBar.
        tabla.setRowHeight(
                (int) renderer.getPreferredSize().getHeight());
// Set up downloads panel.
        tabla.getColumnModel().getColumn(0).setCellRenderer(new TablaCellRenderer(true));
        tabla.getColumnModel().getColumn(1).setCellRenderer(new TablaCellRenderer(true));
        tabla.getColumnModel().getColumn(2).setCellRenderer(new TablaCellRenderer(true));
        tabla.getColumnModel().getColumn(4).setCellRenderer(new TablaCellRenderer(true));

        popup = new JPopupMenu();
        popup.add(acIniciar);
        popup.add(acPausar);
        popup.add(acReanudar);
        popup.add(acReiniciar);
        popup.add(acCancelar);
        popup.add(acBorrar);
        popup.add(acCopiarLink);
        popup.add(acInfo);

        tabla.addMouseListener(new PopupListener(true));

        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("Descargas"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(tabla), BorderLayout.CENTER);
// Set up buttons panel.
        JPanel buttonsPanel = new JPanel();

        iniciarButton = new JButton(acIniciar);
        iniciarButton.setEnabled(false);
        buttonsPanel.add(iniciarButton);

        pauseButton = new JButton(acPausar);
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);

        resumeButton = new JButton(acReanudar);
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);

        cancelButton = new JButton(acCancelar);
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);

        clearButton = new JButton(acBorrar);
        clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);
// Add panels to display.
        setLayout(new BorderLayout());
        add(addPanel, BorderLayout.NORTH);
        add(downloadsPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        corriendo = true;
        hilo = new Thread(this);
        hilo.start();

        setupAutomatico();
    }

    private String getCarpetaDescarga() {
        return directorioDescarga.toString();
    }

    // Add a new download.
    private void actionAdd() {
        if (!addTextField.getText().trim().isEmpty()) {
            URL verifiedUrl = verifyUrl(addTextField.getText());
            if (cbGoogleLinks.isSelected()) {
                try {
                    verifiedUrl = new URL(extractGoogleLink(verifiedUrl.toString()));
                } catch (MalformedURLException e) {
                    System.err.println("Error with url:" + verifiedUrl.toString() + "\n" + e.getMessage());
                }
            }
            if (verifiedUrl != null) {
                Download dl = new Download(verifiedUrl, getCarpetaDescarga(), cbIniciar.isSelected());
                try {
                    verificarArchivo(dl);
                } catch (IOException ex) {
                    System.err.println("Error verificando el archivo:" + dl.getNombreArchivo());
                }
                tableModel.addDownload(dl);
                addTextField.setText(""); // reset add text field
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invalid Download URL", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
// Verify download URL.

    private URL verifyUrl(String url) {
// Only allow HTTP URLs.
        if (!url.toLowerCase().startsWith("http://")) {
            return null;
        }
// Verify format of URL.

        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }
// Make sure URL specifies a file.
        if (verifiedUrl.getFile().length() < 2) {
            return null;
        }
        return verifiedUrl;
    }
// Called when table row selection changes.

    private void tableSelectionChanged() {
        /* Unregister from receiving notifications
         from the last selected download. */
        if (selectedDownload != null) {
            selectedDownload.deleteObserver(DownloadManager.this);
        }
        /* If not in the middle of clearing a download,
         set the selected download and register to
         receive notifications from it. */
        if (!clearing) {
            selectedDownload = tableModel.getDownload(tabla.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateActions();
        }
    }

    public static String extractGoogleLink(String url) {
        String link = url;
        link = link.replace("%3A%2F%2F", "<");
        link = link.replace("&ei=", ">");
        int begin = link.indexOf("<");
        int end = link.indexOf(">");
        if (link.startsWith("https")) {
            link = "https://" + link.substring(begin + 1, end);
        } else {
            link = "http://" + link.substring(begin + 1, end);
        }
        link = link.replaceAll("%2F", "/");
        return link;
    }

    private void initActions() {

        acIniciar = new MyAction("Iniciar", "Iniciar la descarga", 'i') {
            public void actionPerformed(ActionEvent e) {
                actionIniciar();
            }
        };

        acPausar = new MyAction("Pausar", "Pausar la descarga", 'p') {
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        };
        acReanudar = new MyAction("Reanudar", "Reanudar la descarga", 'r') {
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        };

        acReiniciar = new MyAction("Reiniciar", "Reiniciar la descarga", 'b') {
            public void actionPerformed(ActionEvent e) {
                actionReinit();
            }
        };

        acCancelar = new MyAction("Cancelar", "Cancelar la descarga", 'x') {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        };

        acBorrar = new MyAction("Borrar", "Borra la descarga", 'b') {
            public void actionPerformed(ActionEvent e) {
                actionClear();
            }
        };

        acInfo = new MyAction("Info", "Estado de la descarga", 'l') {
            public void actionPerformed(ActionEvent e) {
                actionInfo();
            }
        };

        acCopiarLink = new MyAction("Copiar Link", "Copiar link de descarga", 'c') {
            public void actionPerformed(ActionEvent e) {
                actionCopiarLink();
            }
        };

        acAutomatico = new MyAction("Auto off", "Inicia las descargas automaticamente", 'a') {
            public void actionPerformed(ActionEvent e) {
                if (!automaticoEncendido) {
                    hiloAutomatico.start();
                    btAutomatico.setText("Auto On");
                    automaticoEncendido = true;
                } else {
                    descargando = false;
                    btAutomatico.setText("Auto Off");
                    automaticoEncendido = false;
                }

            }
        };

    }

    private void actionInfo() {
        String msg = ("<html><font color=blue>Archivo: </font>" + selectedDownload.getNombreArchivo()
                + "<br><font color=blue>Path: </font>" + selectedDownload.getPath()
                + "<br><font color=blue>Link: </font>" + selectedDownload.getUrl()
                + "<br><font color=blue>Tamaño: </font>" + org.bx.Utiles.formatearBytes(selectedDownload.getTamaño())
                + "<br><font color=blue>Descargado: </font>" + org.bx.Utiles.formatearBytes(selectedDownload.getDescargado())
                + "<br><font color=blue>Progreso: </font>" + selectedDownload.getProgreso() + " %"
                + "<br><font color=red>Log: </font>" + selectedDownload.getLog()
                + "</html>");
        msg = msg.replace("\\n", "<br>");
        JLabel labMsg = new JLabel(msg);
        JOptionPane.showMessageDialog(tabla, labMsg, "Descarga", JOptionPane.INFORMATION_MESSAGE);

    }

    private void actionCopiarLink() {
        selectedDownload.getUrl();
    }

// Pause the selected download.
    private void actionPause() {
        selectedDownload.pausar();
        descargasActivas--;
        updateActions();
    }
// Resume the selected download.

    private void actionResume() {
        selectedDownload.reanudar();
        descargasActivas++;
        updateActions();
    }

    private void actionReinit() {
        selectedDownload.reiniciar();
        updateActions();
    }
// Cancel the selected download.

    private void actionCancel() {
        selectedDownload.cancelar();
        descargasActivas--;
        updateActions();
    }
// Clear the selected download.

    private void actionClear() {
        clearing = true;
        tableModel.clearDownload(tabla.getSelectedRow());
        clearing = false;
        selectedDownload = null;
        updateActions();
    }

    private void actionIniciar() {
        selectedDownload.iniciarDescarga();
        descargasActivas++;
        updateActions();
    }

    /* Update each button's state based off of the
     currently selected download's status. */
    private void updateActions() {
        if (selectedDownload != null) {
            int status = selectedDownload.getEstado();
            switch (status) {
                case Download.ESPERA:
                    System.out.println(status + " en espera");
                    acIniciar.setEnabled(true);
                    acPausar.setEnabled(false);
                    acReanudar.setEnabled(false);
                    acCancelar.setEnabled(false);
                    acBorrar.setEnabled(true);
                    break;
                case Download.DESCARGANDO:
                    acIniciar.setEnabled(false);
                    acPausar.setEnabled(true);
                    acReanudar.setEnabled(false);
                    acCancelar.setEnabled(true);
                    acBorrar.setEnabled(false);
                    break;
                case Download.PAUSADA:
                    acIniciar.setEnabled(false);
                    acPausar.setEnabled(false);
                    acReanudar.setEnabled(true);
                    acCancelar.setEnabled(true);
                    acBorrar.setEnabled(false);
                    break;
                case Download.ERROR:
                    acIniciar.setEnabled(true);
                    acPausar.setEnabled(true);
                    acReanudar.setEnabled(true);
                    acCancelar.setEnabled(false);
                    acBorrar.setEnabled(true);
                    break;
                default: // COMPLETE or CANCELLED
                    acIniciar.setEnabled(false);
                    acPausar.setEnabled(true);
                    acReanudar.setEnabled(true);
                    acCancelar.setEnabled(false);
                    acBorrar.setEnabled(true);
            }
        } else {

// No download is selected in table.
            acIniciar.setEnabled(false);
            acPausar.setEnabled(false);
            acReanudar.setEnabled(false);
            acCancelar.setEnabled(false);
            acBorrar.setEnabled(false);
        }
    }

    /* Update is called when a Download notifies its
     observers of any changes. */
    public void update(Observable o, Object arg) {
        // Update buttons if the selected download has changed.
        if (selectedDownload != null && selectedDownload.equals(o)) {
            updateActions();
        }
        if (o instanceof Download) {
            Download d = (Download) o;
            if (d.getEstado() == Download.COMPLETA || d.getEstado() == Download.PAUSADA || d.getEstado() == Download.ERROR) {
                descargasActivas--;
                if (!hiloAutomatico.isAlive()) {
//                    hiloAutomatico.start();
                }
            }

        }
    }

    public void cargarLista(ArrayList<Download> lista) {
        int i;
        for (i = 0; i < lista.size(); i++) {
            Download download = lista.get(i);
            if (verifyUrl(download.getUrl()) != null) {
                try {
                    verificarArchivo(download);
                } catch (IOException ex) {
                    System.err.println("Error verificando el archivo:" + download.getNombreArchivo());
                }

                tableModel.addDownload(download);
            }
        }
        if (i > 0) {
            xml.guardarXML(path, tableModel.getDownloadList());
        }
    }

    private void verificarArchivo(Download d) throws IOException {
        Path path = Paths.get(getCarpetaDescarga(), d.getNombreArchivo());
//        System.out.println("Comprobando:" + path.toString());
        if (d.getEstado() == Download.COMPLETA) {
            return;
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
//            System.out.println("Archivo existe");
            long desc = Files.size(path);
            long tam = d.getTamañoDesc();
            System.out.println(desc + "/" + tam);
            if (desc < tam) {

                d.setDescargado(desc);
                d.setEstado(Download.ESPERA);
            } else if (desc == tam) {
                d.setDescargado(desc);
                d.setEstado(Download.COMPLETA);

            }
        } else {
//            System.out.println("archivo no existe");
        }
    }

    private void setupAutomatico() {
        hiloAutomatico = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Download> downloadList = tableModel.getDownloadList();
                System.out.println("List:" + downloadList.size());
                descargando = true;
                while (descargando) {
                    for (int i = 0; i < downloadList.size(); i++) {
                        Download download = downloadList.get(i);
                        if (download.getEstado() == Download.ESPERA && descargasActivas < MAX_DESCARGAS) {
                            download.iniciarDescarga();
//                            descargasActivas++;
                            System.out.println("descargasActivas = " + descargasActivas);
                            descargando = true;
                        } else {
                            descargando = false;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void run() {
        path = Paths.get(System.getProperty("user.dir"), "downloads.xml");
        while (corriendo) {
            try {
                Thread.sleep(30000);
                xml.guardarXML(path, tableModel.getDownloadList());
            } catch (InterruptedException ex) {
                Logger.getLogger(DownloadManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void guardarDatos() {
        xml.guardarXML(path, tableModel.getDownloadList());
    }

    private boolean configCarpetaDescargas() {
        boolean creado = false;
        Path path = Paths.get(System.getProperty("user.dir"), CARPETA_DESCARGAS);
        directorioDescarga = path;
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectory(path);
                path = path.toAbsolutePath();
                System.out.println("\n" + path + " directorio creado.");
                return true;
            } catch (NoSuchFileException e) {
                creado = false;
                System.err.println("\nDirectory creation failed:\n" + e);
            } catch (FileAlreadyExistsException e) {
                creado = false;
                System.err.println("\nDirectory creation failed:\n" + e);
            } catch (IOException e) {
                creado = false;
                System.err.println("\nDirectory creation failed:\n" + e);
            }
        } else {
            creado = true;
        }
        return creado;
    }

    private void cargarListadoExterno() throws HeadlessException {
        JFileChooser fc = new JFileChooser();
        int opc = fc.showOpenDialog(DownloadManager.this);
        if (opc == JFileChooser.APPROVE_OPTION) {
            File fil = fc.getSelectedFile();
            try {
                int c = 0;
                ArrayList<String> links = org.bx.Utiles.splitArchivoEnLineas(fil, true);
                ArrayList<String> urls = new ArrayList<>();
                if (links != null && !links.isEmpty()) {
                    for (int i = 0; i < links.size(); i++) {
                        String string = links.get(i);
                        URL vUrl = verifyUrl(string);
                        if (vUrl != null) {
                            urls.add(string);
                            c++;
                        }
                    }
                    if (c > 0) {
                        String s = "Se encontraron " + c + " links. Desea agregarlos (se omiten repetidos)?.";
                        int res = JOptionPane.showConfirmDialog(DownloadManager.this, s, "Info", JOptionPane.YES_NO_OPTION);
                        boolean skip = true;
                        int cSkip = 0;
                        if (res == JOptionPane.YES_OPTION) {
                            if (skip) {
                                ArrayList<String> linksList = tableModel.getLinksList();
                                for (int i = 0; i < urls.size(); i++) {
                                    if (!linksList.contains(urls.get(i))) {
                                        tableModel.addDownload(new Download(new URL(urls.get(i)), getCarpetaDescarga(), false));
                                    } else {
                                        cSkip++;
                                    }
                                }
                            }
                            if (cSkip > 0) {
                                JOptionPane.showMessageDialog(DownloadManager.this, "Se omitieron " + cSkip + " links existentes.", "Info", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                }

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(pauseButton, "Error al intertar cargar la lista de links: " + fil.getPath() + "\n" + ex.getMessage());
            }
        }
    }

    public class ListaSeleccion extends JPanel implements TableCellRenderer, MouseListener, ActionListener {

        JLabel label;
        JButton boton;
        JPopupMenu pop;
        Box box;

        public ListaSeleccion() {
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            iniciarComponentes();
            setLayout(new BorderLayout());
            add(box, BorderLayout.CENTER);
        }

        private void iniciarComponentes() {
            label = new JLabel("Sel");
//            label.setOpaque(true);            
            label.setFont(UIManager.getFont("TableHeader.font"));

            boton = new JButton();
            boton.setContentAreaFilled(false);
            boton.setBorderPainted(false);
//            boton.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, COLOR_SEL));
            boton.setMinimumSize(new Dimension(15, 15));
            boton.setPreferredSize(new Dimension(15, 15));
            boton.setIcon(org.dzur.rec.GetIcon.getIcono(GetIcon.ABAJO, 9));

            box = Box.createHorizontalBox();
            box.add(Box.createHorizontalGlue());
            box.add(label);
            box.add(boton);
            box.add(Box.createHorizontalGlue());

            JMenuItem item1 = new JMenuItem("Todos");
            item1.setActionCommand(DownloadManager.AC_TODOS);
            item1.addActionListener(this);
            JMenuItem item2 = new JMenuItem("Ninguno");
            item2.setActionCommand(DownloadManager.AC_NINGUNO);
            item2.addActionListener(this);
            JMenuItem item3 = new JMenuItem("Invertir");
            item3.setActionCommand(DownloadManager.AC_INVERTIR);
            item3.addActionListener(this);
            pop = new JPopupMenu();
            pop.add(item1);
            pop.add(item2);
            pop.add(item3);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Rectangle rect = boton.getBounds();
            rect.add(label.getBounds());
            if (rect.contains(e.getPoint())) {
                pop.show(tabla.getTableHeader(), e.getX(), boton.getY() + boton.getHeight());
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand()) {
                case AC_TODOS:
                    for (int i = 0; i < tabla.getRowCount(); i++) {
                        tabla.getModel().setValueAt(Boolean.TRUE, i, 0);
                    }
                    break;
                case AC_NINGUNO:
                    for (int i = 0; i < tabla.getRowCount(); i++) {
                        tabla.getModel().setValueAt(Boolean.FALSE, i, 0);
                    }
                    break;
                case AC_INVERTIR:
                    for (int i = 0; i < tabla.getRowCount(); i++) {
                        Boolean value = (Boolean) tabla.getModel().getValueAt(i, 0);
                        tabla.getModel().setValueAt(!value, i, 0);
                    }
                    break;
            }
        }
    }

    public class TablaCellRenderer extends JLabel implements TableCellRenderer {

        boolean isBordered = true;
        private int[] estados;

        public TablaCellRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//            if (column == table.getColumnCount() - 1) {
            int ind = org.bx.Utiles.indexOf(Download.ESTADOS, table.getValueAt(row, table.getColumnCount() - 1));
            if (column == 0) {
                JCheckBox component = (JCheckBox) table.getDefaultRenderer(Boolean.class).
                        getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (table.getValueAt(row, 0) == Boolean.TRUE) {
                    component.setBackground(COLOR_SEL);
                    component.setForeground(Color.white);
                    if (isSelected) {
                        if (hasFocus) {
                            component.setBorder(BorderFactory.createLineBorder(Color.red));
                        } else {
                            component.setBorder(BorderFactory.createLineBorder(Color.ORANGE));
                        }
                    } else {
                        component.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.ORANGE));
                    }
                } else {
                    if (isSelected) {
                        component.setForeground(tabla.getSelectionForeground());
                        component.setBackground(tabla.getSelectionBackground());
                        if (hasFocus) {
                            component.setBorder(BorderFactory.createLineBorder(Color.darkGray));
                        } else {
                            component.setBorder(BorderFactory.createLineBorder(Color.lightGray));
                        }
                    } else {
                        component.setBackground(tabla.getBackground());
                        component.setForeground(tabla.getForeground());
                        component.setBorder(UIManager.getBorder("Table.cellBorder"));
                    }
                }
                return component;

            } else {
                if (table.getValueAt(row, 0) == Boolean.TRUE) {
                    setBackground(COLOR_SEL);
                    setForeground(Download.ESTADOS_COLOR[row]);
                    if (hasFocus) {
                        setBorder(BorderFactory.createLineBorder(Color.red));
                    } else {
                        setBorder(BorderFactory.createLineBorder(Color.ORANGE));
                    }

                } else {
                    if (isSelected) {
                        setForeground(Download.ESTADOS_COLOR[ind]);
                        setBackground(COLOR_SEL);
                        if (hasFocus) {
                            setBorder(BorderFactory.createLineBorder(Color.darkGray));
                        } else {
                            setBorder(BorderFactory.createLineBorder(Color.lightGray));
                        }
                    } else {
                        setForeground(Download.ESTADOS_COLOR[ind]);
                        setBackground(COLOR_BACK);
                        setBorder(UIManager.getBorder("Table.cellBorder"));
                    }
                    if (value != null) {
                        setText(value.toString());
                    }
                }
                return this;
            }
        }
    }

    public class PopupListener implements MouseListener {

        boolean seleccionar;

        public PopupListener(boolean seleccionar) {
            this.seleccionar = seleccionar;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            isPopupTrigger(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            isPopupTrigger(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isPopupTrigger(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            isPopupTrigger(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            isPopupTrigger(e);
        }

        public void isPopupTrigger(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (seleccionar) {
                    int r = tabla.rowAtPoint(e.getPoint());
                    tabla.getSelectionModel().addSelectionInterval(r, r);
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
