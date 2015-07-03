/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bxdownloads;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.Observable;

/**
 *
 * @author hp
 */
public class Download extends Observable implements Runnable {

    public static final int MAX_BUFFER = 1024;
    public static final String[] ESTADOS = new String[]{"Descargando", "Pausada", "Completa", "Cancelada", "Error", "En Espera"};
    public static final Color[] ESTADOS_COLOR = new Color[]{Color.blue, Color.magenta, new Color(17, 90, 10), new Color(170, 85, 0), Color.red, Color.black};
    public static final int DESCARGANDO = 0;
    public static final int PAUSADA = 1;
    public static final int COMPLETA = 2;
    public static final int CANCELADA = 3;
    public static final int ERROR = 4;
    public static final int ESPERA = 5;
    private URL url;
    private long tamaño;
    private long descargado;
    private int estado;
    private String path;
    private StringBuilder log;
    private String fileSt;
    private boolean selected;

    public Download(URL url, boolean iniciar) {
        this(url, System.getProperty("user.dir"), 0, 0, ESPERA, iniciar);
    }

    public Download(URL url, String path, boolean iniciar) {
        this(url, path, 0, 0, ESPERA, iniciar);
    }

    public Download(URL url, String path, long tamaño, long descargado, int estado, boolean iniciar) {
        this.url = url;
        this.tamaño = tamaño;
        this.descargado = descargado;
        this.estado = estado;
        this.path = path;
        this.log = new StringBuilder("Log: " + url).append("\\n");
        log.append(new Date()).append(": ").append("Creada la descarga.").append("\\n");
        selected = false;
        if (iniciar) {
            iniciarDescarga();
        }
    }

    public void iniciarDescarga() {
        estado = DESCARGANDO;
        descargar();
    }

    public String getUrl() {
        return url.toString();
    }

    public int getEstado() {
        return estado;
    }

    public long getTamaño() {
        return tamaño;
    }

    public String getPath() {
        return path;
    }

    public long getDescargado() {
        return descargado;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public float getProgreso() {
        return ((float) descargado / tamaño) * 100;
    }

    public void pausar() {
        estado = PAUSADA;
        estadoCambiado();
    }

    public void reanudar() {
        estado = DESCARGANDO;
        estadoCambiado();
        descargar();
    }

    public void reiniciar() {
        cancelar();
        try {
            Files.deleteIfExists(Paths.get(fileSt, ""));
        } catch (IOException e) {
            error("Error reiniciando la descarga no se puede borrar el archivo:" + fileSt + "\n" + e.getMessage());
        }

        tamaño = 0;
        descargado = 0;
        estado = DESCARGANDO;
        estadoCambiado();
        descargar();
    }

    public void cancelar() {
        estado = CANCELADA;
        estadoCambiado();
    }

    public String getLog() {
        return log.toString();
    }

    private void error(String error) {
        estado = ERROR;
        System.err.println(error);
        log.append(new Date().toString()).append(": ").append(error).append("\\n");
        estadoCambiado();
//        new DownloadFile(url.toString()).download(getNombreArchivo());
    }

    private void descargar() {
        Thread hilo = new Thread(this);
        hilo.start();
    }

    public void setDescargado(long descargado) {
        this.descargado = descargado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    private String getNombreArchivo(URL url) {
        String nombre = url.getFile();
        nombre = nombre.substring(nombre.lastIndexOf("/") + 1);
        int ind = nombre.indexOf('?');
        if (ind > 3) {
            nombre = nombre.substring(0, ind);
        }
        return nombre;
    }

    public String getNombreArchivo() {
        return getNombreArchivo(url);
    }

    @Override
    public void run() {
        RandomAccessFile file = null;
        InputStream iStream = null;

        try {

            Long ln = System.currentTimeMillis();
//            System.out.println(url.getProtocol());
            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestProperty("Range", "bytes=" + descargado + "-");
            conexion.connect();
//            System.out.println(conexion.getContentEncoding());
//            System.out.println(conexion.getContentType());
//            System.out.println(conexion.getRequestMethod());
//            System.out.println(conexion.getResponseMessage());
//            System.out.println(conexion.getPermission().toString());

            if (conexion.getResponseCode() / 100 != 2) {
                error("Codigo de respuesta distinto de 200");
            }

            long contentLength = conexion.getContentLengthLong();
            if (contentLength < 1) {
                error("Tamaño de contenido invalido");
            }

            if (tamaño >= 0) {
                tamaño = contentLength;
                estadoCambiado();
            }
            Path get = Paths.get(path, getNombreArchivo(url));

            file = new RandomAccessFile(get.toString(), "rw");
            this.fileSt = file.toString();
            file.seek(descargado);

            iStream = conexion.getInputStream();

            while (estado == DESCARGANDO && descargado < tamaño) {
                byte[] buffer;
//                System.out.println("tam:"+descargado+"/"+tamaño);
                if (tamaño - descargado > MAX_BUFFER) {
                    buffer = new byte[MAX_BUFFER];
                } else {
                    buffer = new byte[(int) (tamaño - descargado)];
                }

                int leido = iStream.read(buffer);
                if (leido == -1) {
                    break;
                }

                file.write(buffer, 0, leido);
                descargado += leido;
                estadoCambiado();
            }

            if (estado == DESCARGANDO) {
                estado = COMPLETA;
                estadoCambiado();
            }
            System.out.println("TR:" + (System.currentTimeMillis() - ln));
        } catch (IOException e) {
            error("Ex retrieve:" + e.getMessage());
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    error("Ex Finally:" + e.getMessage());
                }
            }
            if (iStream != null) {
                try {
                    iStream.close();
                } catch (IOException e) {
                    error("Ex Close:" + e.getMessage());
                }
            }
        }

    }

    private void estadoCambiado() {
        setChanged();
        notifyObservers();
    }

    public long getTamañoDesc() {
        long contentLength = -1;
        try {
            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
            conexion.connect();
            if (conexion.getResponseCode() / 100 != 2) {
                error("Codigo de respuesta distinto de 200: " + conexion.getResponseCode());
            }

            contentLength = conexion.getContentLengthLong();
            if (contentLength < 1) {
                error("Tamaño de contenido invalido");
            }
        } catch (IOException e) {
            error("Ex getTamaño:" + e.getMessage());
        }
        return contentLength;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (!(obj instanceof Download)) {
            return false;
        } else {
            Download d = (Download) obj;
            return url.equals(d.url);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.url);
        hash = 89 * hash + Objects.hashCode(this.path);
        return hash;
    }
}
