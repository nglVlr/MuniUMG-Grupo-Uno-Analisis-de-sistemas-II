-- Un solo Supervisor y un solo Jefe de Departamento activos por departamento, que es
-- como trabaja la Municipalidad. Sus bandejas son compartidas por departamento
-- (CU-06, CU-07): los casos no son de un usuario en particular, así que mientras haya
-- dos usuarios activos del mismo rol el CU-09 FA12 nunca se mostraría, porque el otro
-- usuario siempre puede seguir atendiendo la bandeja.
--
-- Queda un Supervisor y un Jefe por departamento:
--   Tecnologia           Supervisor de Prueba  / Jefatura de Prueba
--   Asuntos Municipales  Diego Paz             / Karla Xitumul
--   Recursos Humanos     Priscila Tzoc         / Edwin Say
--   Servicios Publicos   Jose Morales          / Luis Choc
--   Tesoreria            Gabriela Us           / Rolando Ixchop
--
-- Nada se borra: los usuarios que V7 creó como Técnicos vuelven a ese rol y los
-- sobrantes de V1 y V17 quedan Inactivos. Las credenciales siguen documentadas en
-- CREDENCIALES_PRUEBA.md

-- Servicios Publicos no tenía Jefe de Departamento y Tesoreria no tenía Supervisor,
-- así que ningún caso En Supervisión de Tesoreria ni Pendiente de Firma de Servicios
-- Publicos aparecía en una bandeja.
UPDATE usuario
SET id_rol = (SELECT id FROM rol WHERE rol = 'Jefe'),
    id_estado = (SELECT id FROM estado_usuarios WHERE estado = 'Activo')
WHERE correo = 'lchoc1@miumg.edu.gt';

UPDATE usuario
SET id_rol = (SELECT id FROM rol WHERE rol = 'Supervisor'),
    id_estado = (SELECT id FROM estado_usuarios WHERE estado = 'Activo')
WHERE correo = 'gus1@miumg.edu.gt';

-- Los Supervisores y Jefes que atienden cada departamento quedan Activos
UPDATE usuario
SET id_estado = (SELECT id FROM estado_usuarios WHERE estado = 'Activo')
WHERE correo IN ('supervisor.prueba@miumg.edu.gt', 'jefatura.prueba@miumg.edu.gt',
                 'diego.paz@miumg.edu.gt', 'kxitumul1@miumg.edu.gt',
                 'priscila.tzoc@miumg.edu.gt', 'edwin.say@miumg.edu.gt',
                 'jmorales1@miumg.edu.gt', 'rolando.ixchop@miumg.edu.gt');

-- V7 los creó como Técnicos de su departamento y así están documentados; el rol se les
-- había cambiado a mano desde el mantenimiento de usuarios
UPDATE usuario
SET id_rol = (SELECT id FROM rol WHERE rol = 'Tecnico'),
    id_estado = (SELECT id FROM estado_usuarios WHERE estado = 'Activo')
WHERE correo IN ('mcastillo1@miumg.edu.gt', 'aramirez1@miumg.edu.gt',
                 'cestrada1@miumg.edu.gt', 'bsical1@miumg.edu.gt',
                 'dpaz1@miumg.edu.gt', 'mroquel1@miumg.edu.gt',
                 'fboc1@miumg.edu.gt');

-- Los demás Supervisores y Jefes dejan de atender la bandeja de su departamento
UPDATE usuario
SET id_estado = (SELECT id FROM estado_usuarios WHERE estado = 'Inactivo')
WHERE correo IN ('melissa.roquel@miumg.edu.gt', 'fernando.boc@miumg.edu.gt',
                 'cristina.cux@miumg.edu.gt',
                 'supervisor2.prueba@miumg.edu.gt', 'jefatura2.prueba@miumg.edu.gt');
