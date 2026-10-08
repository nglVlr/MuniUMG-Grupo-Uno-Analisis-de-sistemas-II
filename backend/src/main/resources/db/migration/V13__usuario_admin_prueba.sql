-- Usuario Admin de prueba, para probar el CU-10 Mantenimiento de Usuarios y el
-- CU-11 Administración de Catálogos. Credenciales en CREDENCIALES_PRUEBA.md

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190130', 'Administrador de Prueba', 'admin.prueba@miumg.edu.gt',
       '$2a$10$5tFk0s7cnzDKZisX0wRkDuqTb8eDUGjjXRki7COk95lIj2lZe.jWa',
       (SELECT id FROM rol WHERE rol = 'Admin'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'admin.prueba@miumg.edu.gt');
