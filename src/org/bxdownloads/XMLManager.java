/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bxdownloads;

import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 * @author hp
 */
public class XMLManager {

    public static final String TAG_ROOT = "downloads";
    public static final String TAG_DESCARGA = "download";
    public static final String TAG_LINK = "link";
    public static final String TAG_ARCHIVO = "archivo";
    public static final String TAG_TAMANO = "tamano";
    public static final String TAG_DESCARGADO = "descargado";
    public static final String TAG_ESTADO = "estado";
    private static final String XML_VERSION = "1.0";
    private static final String XML_ENCODING = "ISO-8859-1";
    private static final String JAVA_ENCODING = "8859_1";

    public ArrayList<Download> cargarDownloadXML(Document xmlDoc) throws MalformedURLException {
        ArrayList<Download> lista = new ArrayList<>();
        Download download = null;
        String link = null, archivo = null;
        long tamano = 0, descargado = 0;
        int estado = 0;
        NodeList nodes = xmlDoc.getDocumentElement().getChildNodes();
        if (nodes != null && nodes.getLength() > 0) { // If there are some...
            Node elementNode = null;
            for (int i = 0; i < nodes.getLength(); ++i) {
                elementNode = nodes.item(i);
                switch (elementNode.getNodeName()) {
                    case TAG_DESCARGA:
                        if (elementNode.hasChildNodes()) {
                            NodeList nh = elementNode.getChildNodes();
                            for (int j = 0; j < nh.getLength(); j++) {
                                Node item = nh.item(j);
                                if (item.getNodeName().equals(TAG_LINK)) {
                                    link = item.getTextContent();
                                    System.out.println("link = " + link);
                                } else if (item.getNodeName().equals(TAG_ARCHIVO)) {
                                    archivo = item.getTextContent();
                                    System.out.println("archivo = " + archivo);
                                } else if (item.getNodeName().equals(TAG_TAMANO)) {
                                    try {
                                        tamano = Long.parseLong(item.getTextContent());
                                    } catch (NumberFormatException e) {
                                        tamano = -1;
                                    }
                                    System.out.println("tamano = " + tamano);
                                } else if (item.getNodeName().equals(TAG_DESCARGADO)) {
                                    try {
                                        descargado = Long.parseLong(item.getTextContent());
                                    } catch (NumberFormatException e) {
                                        descargado = 0;
                                    }
                                    System.out.println("descargado = " + descargado);
                                } else if (item.getNodeName().equals(TAG_ESTADO)) {
                                    try {
                                        estado = Integer.parseInt(item.getTextContent());
                                    } catch (NumberFormatException e) {
                                        estado = Download.ESPERA;
                                    }
                                    System.out.println("estado = " + estado);
                                }
                            }
                            download = new Download(new URL(link), archivo, tamano, descargado, estado, false);
                            lista.add(download);
                        }
                        break;
                }
            }
        }
        return lista;
    }

    public Document getDocument(String archivo) {
        Document doc = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
            doc = dbBuilder.parse(new File(archivo));
            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("exc: " + e.getMessage());
        }

        return doc;

    }

    public void guardarXML(Path archivo, ArrayList<Download> descargas) {

        try {
            OutputStream fout = new FileOutputStream(archivo.toFile());
            OutputStream bout = new BufferedOutputStream(fout);
            try (OutputStreamWriter out = new OutputStreamWriter(bout, JAVA_ENCODING)) {
                String texto = generaTextoXML(generarDocumentoXMLHorario(descargas));
                out.write(texto);
                out.flush();
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Error codificacion");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("Error Guardando el xml: " + e);
        }

    }

    private Document generarDocumentoXMLHorario(ArrayList<Download> desacargas) {
        Document documentoXML = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactoryImpl.newInstance();
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            documentoXML = docBuilder.newDocument();
        } catch (Exception e) {
            System.out.println("Error formatenado xml : " + e);
        }
        Element elementRoot = documentoXML.createElement(TAG_ROOT);
        documentoXML.appendChild(elementRoot);

        Element elemento, item;
        for (int i = 0; i < desacargas.size(); i++) {
            Download download = desacargas.get(i);
            elemento = documentoXML.createElement(TAG_DESCARGA);
            item = documentoXML.createElement(TAG_LINK);
            item.appendChild(documentoXML.createTextNode(download.getUrl()));
            elemento.appendChild(item);
            item = documentoXML.createElement(TAG_ARCHIVO);
            item.appendChild(documentoXML.createTextNode(download.getPath()));
            elemento.appendChild(item);
            item = documentoXML.createElement(TAG_TAMANO);
            item.appendChild(documentoXML.createTextNode("" + download.getTamaño()));
            elemento.appendChild(item);
            item = documentoXML.createElement(TAG_DESCARGADO);
            item.appendChild(documentoXML.createTextNode("" + download.getDescargado()));
            elemento.appendChild(item);
            item = documentoXML.createElement(TAG_ESTADO);
            item.appendChild(documentoXML.createTextNode("" + download.getEstado()));
            elemento.appendChild(item);
            elementRoot.appendChild(elemento);
        }
        return documentoXML;

    }

    private String generaTextoXML(Document documentoXML) {
        StringWriter strWriter = null;
        XMLSerializer seliarizadorXML = null;
        OutputFormat formatoSalida = null;
        try {
            seliarizadorXML = new XMLSerializer();
            strWriter = new StringWriter();
            formatoSalida = new OutputFormat();
            formatoSalida.setEncoding(XML_ENCODING);
            formatoSalida.setVersion(XML_VERSION);
            formatoSalida.setIndenting(true);
            formatoSalida.setIndent(4);
            seliarizadorXML.setOutputCharStream(strWriter);
            seliarizadorXML.setOutputFormat(formatoSalida);
            seliarizadorXML.serialize(documentoXML);
            strWriter.close();
        } catch (IOException ioEx) {
            System.out.println("Error  generando texto xml: " + ioEx);
        }
        return strWriter.toString();
    }
}
