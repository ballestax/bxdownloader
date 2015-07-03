/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bxdownloads;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.w3c.dom.Document;

/**
 *
 * @author hp
 */
public class BxDownloads {

    private static DownloadManager dm = null;
    private static final String ARCHIVO_DESCARGA = "downloads.xml";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws MalformedURLException {
        // TODO code application logic here

//        DownloadManager dm = null;
        try {
            XMLManager xml = new XMLManager();
            dm = new DownloadManager();
            System.out.println(System.getProperty("user.dir"));
            Path get = Paths.get(System.getProperty("user.dir"), ARCHIVO_DESCARGA);
            if (Files.exists(get, LinkOption.NOFOLLOW_LINKS)) {
                System.out.println(get+" existe");
                Document doc = xml.getDocument(get.toString());
                ArrayList<Download> cargarDownloadXML = xml.cargarDownloadXML(doc);                
                dm.cargarLista(cargarDownloadXML);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }


        JFrame frame = new JFrame("Ventana");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(500, 400);
        frame.add(dm != null ? dm : new DownloadManager());
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dm.guardarDatos();
                System.exit(0);
            }
        });

    }
}
