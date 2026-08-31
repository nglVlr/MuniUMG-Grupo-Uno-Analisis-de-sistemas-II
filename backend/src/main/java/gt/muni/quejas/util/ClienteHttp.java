package gt.muni.quejas.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClienteHttp {

    private ClienteHttp() {
    }

    public static String ipOrigen(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            ip = "0.0.0.0";
        }
        return ip.length() > 50 ? ip.substring(0, 50) : ip;
    }
}
