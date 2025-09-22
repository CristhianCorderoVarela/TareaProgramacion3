package cr.ac.una.tareaprogramacion3.service;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class ExcelExportService {

    private static final String EXCEL_EXTENSION = ".xlsx";
    private static final String EXCEL_DESCRIPTION = "Archivos Excel";

   
    public static boolean guardarExcelEn(String rutaCompleta, byte[] excelBytes) {
        try {
            Path path = Paths.get(rutaCompleta);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
           
            Files.write(path, excelBytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false; 
        }
    }

    /** ¿El archivo existe? */
    public static boolean archivoExiste(String rutaCompleta) {
        return Files.exists(Paths.get(rutaCompleta));
    }

    /** Devuelve true si el archivo existe  */
    public static boolean estaBloqueado(String rutaCompleta) {
        Path path = Paths.get(rutaCompleta);
        if (!Files.exists(path)) return false;
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.WRITE)) {
            try {
                FileLock lock = ch.tryLock();
                if (lock != null) {
                    try { lock.release(); } catch (Exception ignore) {}
                    return false; 
                }
                return true; 
            } catch (Exception lockEx) {
                
                return true;
            }
        } catch (Exception openEx) {
            
            return true;
        }
    }

   
    public static String proponerNombreCopia(String rutaOriginal) {
        Path p = Paths.get(rutaOriginal);
        String fileName = p.getFileName().toString();

        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        if (ext.isEmpty()) ext = EXCEL_EXTENSION;

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String candidato = base + " (copia " + stamp + ")" + ext;

        return p.getParent() == null ? candidato : p.getParent().resolve(candidato).toString();
    }

    
    public static String limpiarNombreArchivo(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "cronograma_proyecto" + EXCEL_EXTENSION;
        }
        String n = nombre.replaceAll("[^a-zA-Z0-9\\-_.\\s]", "_").trim();
        if (!n.toLowerCase().endsWith(EXCEL_EXTENSION)) {
            
            n = n.replaceAll("(?i)\\.xlsx$", "");
            n += EXCEL_EXTENSION;
        }
        return n;
    }

   
    public static String obtenerDirectorioDescargas() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + "Downloads";
    }

    
    public static CompletableFuture<Boolean> guardarExcel(Window parentWindow, byte[] excelBytes, String nombreSugerido) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Guardar Cronograma Excel");
                fileChooser.setInitialFileName(limpiarNombreArchivo(nombreSugerido));
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(EXCEL_DESCRIPTION, "*" + EXCEL_EXTENSION));

                final File[] elegido = new File[1];
                javafx.application.Platform.runLater(() -> {
                    elegido[0] = fileChooser.showSaveDialog(parentWindow);
                    synchronized (elegido) { elegido.notify(); }
                });

                synchronized (elegido) { elegido.wait(); }

                if (elegido[0] == null) return false;

                try (FileOutputStream fos = new FileOutputStream(elegido[0])) {
                    fos.write(excelBytes);
                    fos.flush();
                }
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

   
    public static Task<Boolean> crearTareaDescargaExcel(Window parentWindow, byte[] excelBytes, String nombreArchivo) {
        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateMessage("Preparando archivo Excel...");
                updateProgress(0, 100);
                Thread.sleep(300);
                updateProgress(40, 100);
                updateMessage("Mostrando diálogo...");
                boolean ok = guardarExcel(parentWindow, excelBytes, nombreArchivo).get();
                updateProgress(100, 100);
                updateMessage(ok ? "Archivo guardado" : "Operación cancelada");
                return ok;
            }
        };
    }
}
