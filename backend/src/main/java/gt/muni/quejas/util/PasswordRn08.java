package gt.muni.quejas.util;

import java.util.regex.Pattern;

// RN08: la contraseña del empleado es alfanumérica, de mínimo 6 caracteres.
// Es la misma regla que valida el ingreso al sistema (CU-01), para que una
// contraseña aceptada al crearla o cambiarla siempre sirva para iniciar sesión.
public final class PasswordRn08 {

    public static final int MINIMO = 6;

    private static final Pattern VALIDA = Pattern.compile("^[A-Za-z0-9]{" + MINIMO + ",}$");

    private PasswordRn08() {
    }

    public static boolean esValida(String password) {
        return password != null && VALIDA.matcher(password).matches();
    }
}
