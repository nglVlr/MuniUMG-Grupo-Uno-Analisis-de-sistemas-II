package gt.muni.quejas.util;

import java.util.Set;

public final class TipoCaso {

    public static final String QUEJA = "Queja";
    public static final String RECLAMO = "Reclamo";
    public static final String DENUNCIA = "Denuncia";
    public static final String SUGERENCIA = "Sugerencia";

    private static final Set<String> VALIDOS = Set.of(QUEJA, RECLAMO, DENUNCIA, SUGERENCIA);

    private TipoCaso() {
    }

    public static boolean esValido(String tipoCaso) {
        return tipoCaso != null && VALIDOS.contains(tipoCaso);
    }
}
