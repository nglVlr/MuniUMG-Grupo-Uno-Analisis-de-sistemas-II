package gt.muni.quejas.dto;

import java.util.List;

// CU-15: página de resultados de la consulta de bitácora
public record DtoPaginaBitacora(
        List<DtoBitacora> registros,
        int pagina,
        int totalPaginas,
        long totalRegistros
) {
}
