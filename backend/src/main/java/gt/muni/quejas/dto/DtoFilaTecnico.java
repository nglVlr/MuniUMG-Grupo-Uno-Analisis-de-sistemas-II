package gt.muni.quejas.dto;

// CU-19: fila de la tabla de Técnicos. semaforo vale verde, amarillo o rojo
// según el tiempo promedio de resolución.
public record DtoFilaTecnico(
        String tecnico,
        String departamento,
        long enAtencion,
        long resueltos,
        double tiempoPromedio,
        String semaforo
) {
}
