-- Tecnologia habia quedado sin Supervisor activo, con casos esperando en la bandeja de
-- supervision (En Supervision y Pendiente de Aprobacion de Rechazo) que no aparecian en
-- ninguna pantalla. La V11 creo a Supervisor de Prueba en Tecnologia, pero su departamento
-- se habia cambiado a mano a Asuntos Municipales, que ademas quedo con dos Supervisores
-- activos cuando la V18 dejo uno por departamento.
--
-- Se le devuelve su departamento original y su estado Activo. Con esto:
--   Tecnologia           Supervisor de Prueba  (antes ninguno)
--   Asuntos Municipales  Diego Paz             (antes dos)
--
-- Supervisor de Prueba 2 se deja Inactivo a proposito: es el usuario que el Administrador
-- activa en el CU-09 FA12, cuando el sistema le impide inactivar al unico Supervisor de un
-- departamento que todavia tiene casos pendientes en su bandeja.
UPDATE usuario
SET id_departamento = (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
    id_estado = (SELECT id FROM estado_usuarios WHERE estado = 'Activo')
WHERE correo = 'supervisor.prueba@miumg.edu.gt';
