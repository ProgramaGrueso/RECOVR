# Dónde va esto y qué hacer

1. Descomprime este ZIP.
2. Copia toda la carpeta `backend` (tal cual, con ese nombre) dentro de:
   `C:\RECOVR\RECOVR\`

   Debe quedar así:
   ```
   C:\RECOVR\RECOVR\
   ├── backend\        <- esta carpeta nueva
   ├── src\            <- del Angular, ya existente
   ├── angular.json
   └── ...
   ```

3. Antes de abrir el proyecto, edita `backend/src/main/resources/application.properties`
   y reemplaza:
   - `NOMBRE_DE_TU_BASE_DE_DATOS` por el nombre que quieras (ej. `recovr_db`)
   - `TU_USUARIO_DE_BD` por tu usuario de MySQL (normalmente `root` en local)
   - `TU_PASSWORD_DE_BD` por tu contraseña de MySQL

   No necesitas crear la base de datos a mano: dejé `createDatabaseIfNotExist=true`
   en la URL, así que MySQL la crea sola la primera vez que corras el proyecto
   (siempre que el usuario tenga permisos).

4. Abre la carpeta `backend` en VS Code (Archivo > Abrir carpeta > selecciona `backend`,
   no la carpeta RECOVR completa).

5. Instala en VS Code, si no las tienes, estas extensiones:
   - "Extension Pack for Java"
   - "Spring Boot Extension Pack"

6. Este ZIP no incluye el Maven Wrapper (mvnw), así que necesitas Maven instalado en tu PC:
   - Verifica si ya lo tienes: `mvn -v` en PowerShell.
   - Si no sale nada, descárgalo de https://maven.apache.org/download.cgi (el .zip "Binary"),
     descomprímelo en, por ejemplo, `C:\Program Files\Apache\maven`, y agrega la carpeta
     `...\maven\bin` a la variable de entorno PATH de Windows. Cierra y abre PowerShell de nuevo.
   - Alternativa más simple: con la extensión "Spring Boot Extension Pack" instalada en VS Code,
     puedes correr el proyecto con click derecho sobre `BackendApplication.java` → "Run Java",
     sin necesitar `mvn` en la terminal para nada.

7. Para correr por terminal (si instalaste Maven), dentro de la carpeta `backend`:
   ```
   mvn spring-boot:run
   ```

8. Cuando levante, prueba en el navegador o Postman:
   `http://localhost:8080/api/clientes` (debería devolver `[]`, una lista vacía).

## Qué contiene esta carpeta

- `entity/` → Cliente, Empleado, Servicio, Sala, Reserva, Pago, EstadoReserva
- `repository/` → uno por entidad, con las consultas JPQL en `ReservaRepository`
- `service/` → CRUD completo de cada entidad + `confirmarYPagar` (transacción de ejemplo)
- `controller/` → endpoints REST bajo `/api/<recurso>`

## Para subirlo a tu rama de Git

Una vez que ya copiaste la carpeta `backend` dentro de `C:\RECOVR\RECOVR` y confirmaste
que corre, desde PowerShell (parado en `C:\RECOVR\RECOVR`, en tu rama `feature/04-database-api`):

```
git add backend
git commit -m "feat(database): estructura inicial de Spring Boot + entidades + CRUD + JPQL"
git push origin feature/04-database-api
```
