package cr.ac.una.tareaprogramacion3.util;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import java.util.regex.Pattern;

public final class ValidationUtil {
    private ValidationUtil(){}
    
    private static final String ERROR_STYLE = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 4;";
    private static final String OK_STYLE = "";
    
    // ========== LÍMITES DE BASE DE DATOS ==========
    public static class FieldLimits {
        // ADMINISTRADORES
        public static final int ADMIN_NOMBRE = 80;
        public static final int ADMIN_APELLIDOS = 120;
        public static final int ADMIN_CEDULA = 20;
        public static final int ADMIN_CORREO = 120;
        public static final int ADMIN_USUARIO = 40;
        public static final int ADMIN_PASSWORD = 255;
        
        // PROYECTOS
        public static final int PROYECTO_NOMBRE = 200;
        public static final int PROYECTO_PATROCINADOR_NOMBRE = 120;
        public static final int PROYECTO_PATROCINADOR_CORREO = 120;
        public static final int PROYECTO_LIDER_USUARIO_NOMBRE = 120;
        public static final int PROYECTO_LIDER_USUARIO_CORREO = 120;
        public static final int PROYECTO_LIDER_TECNICO_NOMBRE = 120;
        public static final int PROYECTO_LIDER_TECNICO_CORREO = 120;
        
        // ACTIVIDADES
        public static final int ACTIVIDAD_DESCRIPCION = 500;
        public static final int ACTIVIDAD_ENCARGADO_NOMBRE = 120;
        public static final int ACTIVIDAD_ENCARGADO_CORREO = 120;
    }
    
    // ========== PATRONES DE VALIDACIÓN ==========
    private static final Pattern EMAIL_RX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    // Patrón para nombres: solo letras, espacios, acentos, ñ, apostrofes y guiones
    private static final Pattern NOMBRES_RX = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s'\\-]+$");
    
    private static void mark(Node n, boolean ok) {
        if (n == null) return;
        n.setStyle(ok ? OK_STYLE : ERROR_STYLE);
    }
    
    // ========== VALIDACIONES EXISTENTES ==========
    /** Campo requerido (TextField/PasswordField/TextArea) */
    public static boolean require(TextInputControl field, String name) {
        boolean ok = field != null && field.getText() != null && !field.getText().trim().isEmpty();
        mark(field, ok);
        if (!ok) AlertUtil.warn("Dato requerido", "Completa el campo: " + name);
        return ok;
    }
    
    /** Combo requerido */
    public static boolean require(ComboBox<?> combo, String name) {
        boolean ok = combo != null && combo.getValue() != null;
        mark(combo, ok);
        if (!ok) AlertUtil.warn("Dato requerido", "Selecciona un valor en: " + name);
        return ok;
    }
    
    /** Validación simple de correo */
    public static boolean email(TextField field) {
        String v = field == null ? null : field.getText();
        boolean ok = v != null && !v.isBlank() && EMAIL_RX.matcher(v.trim()).matches();
        mark(field, ok);
        if (!ok) AlertUtil.warn("Correo inválido", "Escribe un correo válido.");
        return ok;
    }
    
    // ========== NUEVAS VALIDACIONES ==========
    
    /**
     * VALIDACIÓN 1: Validar que un campo solo contenga nombres (sin números)
     * Permite: letras, espacios, acentos, ñ, apostrofes y guiones
     */
    public static boolean nameOnly(TextInputControl field, String fieldName) {
        String value = field == null ? null : field.getText();
        boolean ok = true;
        
        if (value != null && !value.trim().isEmpty()) {
            ok = NOMBRES_RX.matcher(value.trim()).matches();
        }
        
        mark(field, ok);
        if (!ok) {
            AlertUtil.warn("Formato inválido", 
                "El campo '" + fieldName + "' solo puede contener letras, espacios y caracteres válidos para nombres (sin números).");
        }
        return ok;
    }
    
    /**
     * VALIDACIÓN 2: Validar longitud máxima según límites de base de datos
     */
    public static boolean maxLength(TextInputControl field, String fieldName, int maxLength) {
        String value = field == null ? null : field.getText();
        boolean ok = true;
        
        if (value != null) {
            ok = value.length() <= maxLength;
        }
        
        mark(field, ok);
        if (!ok) {
            AlertUtil.warn("Texto muy largo", 
                "El campo '" + fieldName + "' no puede tener más de " + maxLength + " caracteres.\n" +
                "Actualmente tiene: " + (value != null ? value.length() : 0) + " caracteres.");
        }
        return ok;
    }
    
    // ========== VALIDACIONES COMBINADAS ==========
    
    /**
     * Validar campo de nombre con longitud y formato
     */
    public static boolean validateName(TextInputControl field, String fieldName, int maxLength) {
        return require(field, fieldName) 
            && nameOnly(field, fieldName)
            && maxLength(field, fieldName, maxLength);
    }
    
    /**
     * Validar campo de correo con longitud
     */
    public static boolean validateEmail(TextField field, String fieldName, int maxLength) {
        return require(field, fieldName)
            && maxLength(field, fieldName, maxLength)
            && email(field);
    }
    
    /**
     * Validar campo de texto general con longitud
     */
    public static boolean validateText(TextInputControl field, String fieldName, int maxLength) {
        return require(field, fieldName)
            && maxLength(field, fieldName, maxLength);
    }
    
    // ========== VALIDACIONES ESPECÍFICAS POR ENTIDAD ==========
    
    // ADMINISTRADORES
    public static boolean validateAdminName(TextInputControl field) {
        return validateName(field, "Nombre", FieldLimits.ADMIN_NOMBRE);
    }
    
    public static boolean validateAdminLastName(TextInputControl field) {
        return validateName(field, "Apellidos", FieldLimits.ADMIN_APELLIDOS);
    }
    
    public static boolean validateAdminCedula(TextInputControl field) {
        return validateText(field, "Cédula", FieldLimits.ADMIN_CEDULA);
    }
    
    public static boolean validateAdminEmail(TextField field) {
        return validateEmail(field, "Correo", FieldLimits.ADMIN_CORREO);
    }
    
    public static boolean validateAdminUser(TextInputControl field) {
        return validateText(field, "Usuario", FieldLimits.ADMIN_USUARIO);
    }
    
    public static boolean validateAdminPassword(TextInputControl field) {
        return validateText(field, "Contraseña", FieldLimits.ADMIN_PASSWORD);
    }
    
    // PROYECTOS
    public static boolean validateProjectName(TextInputControl field) {
        return validateText(field, "Nombre del Proyecto", FieldLimits.PROYECTO_NOMBRE);
    }
    
    public static boolean validateSponsorName(TextInputControl field) {
        return validateName(field, "Nombre del Patrocinador", FieldLimits.PROYECTO_PATROCINADOR_NOMBRE);
    }
    
    public static boolean validateSponsorEmail(TextField field) {
        return validateEmail(field, "Correo del Patrocinador", FieldLimits.PROYECTO_PATROCINADOR_CORREO);
    }
    
    public static boolean validateUserLeaderName(TextInputControl field) {
        return validateName(field, "Nombre del Líder Usuario", FieldLimits.PROYECTO_LIDER_USUARIO_NOMBRE);
    }
    
    public static boolean validateUserLeaderEmail(TextField field) {
        return validateEmail(field, "Correo del Líder Usuario", FieldLimits.PROYECTO_LIDER_USUARIO_CORREO);
    }
    
    public static boolean validateTechLeaderName(TextInputControl field) {
        return validateName(field, "Nombre del Líder Técnico", FieldLimits.PROYECTO_LIDER_TECNICO_NOMBRE);
    }
    
    public static boolean validateTechLeaderEmail(TextField field) {
        return validateEmail(field, "Correo del Líder Técnico", FieldLimits.PROYECTO_LIDER_TECNICO_CORREO);
    }
    
    // ACTIVIDADES
    public static boolean validateActivityDescription(TextInputControl field) {
        return validateText(field, "Descripción de la Actividad", FieldLimits.ACTIVIDAD_DESCRIPCION);
    }
    
    public static boolean validateActivityResponsibleName(TextInputControl field) {
        return validateName(field, "Nombre del Encargado", FieldLimits.ACTIVIDAD_ENCARGADO_NOMBRE);
    }
    
    public static boolean validateActivityResponsibleEmail(TextField field) {
        return validateEmail(field, "Correo del Encargado", FieldLimits.ACTIVIDAD_ENCARGADO_CORREO);
    }
}