# InstrucciónFácil Backend

API REST para la escuela de manejo. El proyecto usa Java 21, Spring Boot 4, Maven, Spring Security, Spring JDBC, Flyway y OpenAPI. Los paquetes `controller`, `service`, `repository`, `entity` y `dto` siguen la organización de PharmaBackend.

## Arquitectura MVC

- `controller` recibe las peticiones HTTP, valida los DTO de entrada y delega en `service`.
- `service` aplica las reglas de negocio y convierte los modelos internos en DTO de respuesta.
- `repository` ejecuta el SQL con JDBC y devuelve modelos internos de `entity`, donde también están los enums de roles y estados.
- `dto/Solicitudes.java` y `dto/Respuestas.java` definen el contrato de la API REST que consume la vista Angular.

## Inicio local con Docker y Oracle

En Windows, abre Docker Desktop en modo de contenedores Linux. Desde PowerShell, ubícate en la carpeta `InstruccionFacil-Backend`. Copia `.env.example` a `.env` y rellena las dos contraseñas en ese archivo local.

```powershell
Copy-Item .env.example .env
notepad .env
```

`ORACLE_APP_PASSWORD` es la contraseña de Oracle. `APP_ADMIN_PASSWORD` es la contraseña inicial de la aplicación. En Docker local requiere al menos 8 caracteres, una letra y un número. El perfil Oracle fuera de Docker requiere al menos 10 caracteres.

Para iniciar ambos contenedores, ejecuta:

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

La primera ejecución descarga Oracle Database Free y compila el backend en Docker. Cuando Oracle está listo, el backend arranca con el perfil `docker`, que activa el perfil `oracle`. La imagen de Oracle crea el usuario `INSTRUCCIONFACIL` la primera vez que se inicializa el volumen con la contraseña de `ORACLE_APP_PASSWORD`. Flyway crea las tablas al arrancar la API. Puedes cerrar la vista de registros con **Ctrl+C** y los contenedores seguirán en ejecución.

Para comprobar la API, abre [health](http://localhost:8080/api/v1/health) o [Swagger](http://localhost:8080/swagger-ui/index.html). El administrador inicial de la API es `admin@instruccionfacil.test` con la contraseña que pusiste en `APP_ADMIN_PASSWORD`. Puedes cambiar los valores iniciales con `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` antes de crear la cuenta por primera vez. Si omites `APP_JWT_SECRET`, se genera una clave nueva al reiniciar el backend.

Para restablecer la contraseña de un administrador existente, coloca su correo en `APP_ADMIN_EMAIL`, la nueva contraseña en `APP_ADMIN_PASSWORD` y activa `APP_ADMIN_RESET_PASSWORD=true` en `.env`. Luego ejecuta `docker compose up -d --build --force-recreate backend`. Prueba el inicio de sesión y vuelve a poner `APP_ADMIN_RESET_PASSWORD=false`. Aplica ese cambio con `docker compose up -d backend`.

### Conexión desde SQL Developer

| Campo | Valor |
| --- | --- |
| Usuario | `INSTRUCCIONFACIL` |
| Contraseña | Valor local de `ORACLE_APP_PASSWORD` |
| Tipo | Básico |
| Rol | Default |
| Host | `127.0.0.1` |
| Puerto | `1521` |
| Service name | `FREEPDB1` |

Compose publica ambos puertos en `127.0.0.1`. El backend se conecta desde su contenedor a `oracle:1521/FREEPDB1`.

Si reutilizas un volumen existente, usa la contraseña con la que se creó el usuario. Cambiar el valor en `.env` no modifica por sí solo la cuenta en Oracle.

Puedes comprobar las tablas con `SELECT table_name FROM user_tables ORDER BY table_name;` desde la conexión de `INSTRUCCIONFACIL`. Usa `docker compose down` para detener el proyecto y conservar los datos en el volumen `oracle-data`.

## Inicio local en Windows

Abre PowerShell dentro de la carpeta `InstruccionFacil-Backend`. Para ejecutar directamente el JAR, compílalo con un **JDK 21** y acceso a Maven Central.

```powershell
java -version
.\mvnw.cmd clean package
$env:APP_ADMIN_EMAIL = 'admin@instruccionfacil.test'
$env:APP_ADMIN_PASSWORD = Read-Host 'Contraseña inicial (10 caracteres, letras y números)' -MaskInput
$env:APP_PAGO_YAPE = 'Número de demostración'
java -jar target/instruccionfacil-backend-0.1.0.jar
```

`-MaskInput` requiere PowerShell 7. En Windows PowerShell 5.1 puedes usar `$env:APP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Contraseña inicial' -AsSecureString)).Password`.

- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Comprobación de servicio y base: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)

El perfil `local` usa H2 en modo Oracle y guarda los datos en `data/` dentro del directorio desde el que se inicia la aplicación. Flyway aplica la migración automáticamente. Las variables de administrador crean la cuenta la primera vez y los arranques siguientes respetan su contraseña existente. Si omites `APP_JWT_SECRET` en local, el servidor genera una clave nueva al iniciar y las sesiones anteriores caducan con el reinicio.

Para compilar el código necesitas un **JDK 21** y acceso a Maven Central. Ejecuta `.\mvnw.cmd clean package` y el JAR nuevo quedará en `target/`. En Linux o macOS el wrapper es `./mvnw`.

## Primer recorrido en Swagger

1. En `POST /api/v1/auth/login`, usa `{"usuario":"admin@instruccionfacil.test","password":"TU_CONTRASEÑA"}`. Copia `accessToken` y pégalo en **Authorize**. Swagger añade el prefijo `Bearer`.
2. Crea una cuenta de instructor en `POST /api/v1/admin/personal` con `rol: INSTRUCTOR`, `nombre`, `email`, `telefono`, `dni`, `nacimiento`, `direccion` y `password`.
3. Crea un tipo de clase en `POST /api/v1/admin/tipos-clase` con `{"nombre":"Práctica de manejo","minutos":60,"tarifa":80.00,"activo":true}`. Crea un vehículo en `POST /api/v1/admin/vehiculos` con `{"placa":"ABC123","descripcion":"Vehículo de prácticas","transmision":"MECANICA","activo":true}`.
4. Inicia sesión con la cuenta del instructor y crea su disponibilidad en `POST /api/v1/instructor/horarios`, con `inicio` y `fin` en formato ISO, por ejemplo `2027-01-15T09:00:00` y `2027-01-15T13:00:00`. Usa una fecha futura al momento de probar.
5. Registra un alumno en `POST /api/v1/auth/registro`, con `nombre`, `email`, `telefono`, `dni`, `nacimiento`, `direccion` y `password`. Inicia sesión como alumno y sustituye el token en **Authorize**.
6. Consulta `GET /api/v1/disponibilidad` con `instructorId`, `tipoClaseId`, `fecha`, `transmision` y `vehiculoPropio`. Usa uno de los horarios devueltos en `POST /api/v1/citas`.

```json
{
  "instructorId": 2,
  "tipoClaseId": 3,
  "inicio": "2027-01-15T09:00:00",
  "transmision": "MECANICA",
  "vehiculoPropio": false
}
```

7. En `POST /api/v1/citas/{id}/pagos`, envía un formulario multipart con `medio` (`YAPE`, `PLIN` o `TRANSFERENCIA`), `operacion` y `archivo` (JPG, PNG o PDF de hasta 5 MB). Luego cambia a un token de administrador o recepción. Comprueba los datos con `GET /api/v1/admin/pagos/pendientes` y el archivo con `GET /api/v1/pagos/{id}/comprobante`. Para pago en efectivo, envía solo `medio=EFECTIVO` y confirma la recepción del dinero en caja.
8. Usa `PATCH /api/v1/admin/pagos/{id}/revision` con `{"aprobar":true}`. El identificador de esta llamada es el **ID del pago**. Comprueba en `GET /api/v1/citas/{id}` que la **cita** quedó `CONFIRMADA`. El alumno y el administrador pueden descargar el recibo interno en `GET /api/v1/pagos/{id}/recibo`.

## API y reglas

| Recurso | Operaciones |
| --- | --- |
| Autenticación y perfil | Registro, acceso con correo o teléfono, bloqueo temporal por intentos fallidos, perfil e historial de citas. |
| Administración | Altas de instructor y recepción, tipos de clase y tarifas, vehículos, consulta de horarios, bloqueos de flota y auditoría. |
| Agenda | Disponibilidad, reserva propia o manual por recepción, reprogramación, cancelación y transiciones de ejecución. |
| Pagos | Medios configurables, carga privada, revisión manual, historial y detección de operaciones duplicadas por medio. |

- Los horarios se envían y reciben como fecha y hora local de **America/Lima**, sin sufijo de zona. Los inicios disponibles van en múltiplos de 15 minutos. Una cita usa la duración y la tarifa vigentes en el momento de reservar.
- Con `vehiculoPropio: true` se requieren `placaPropia`, `marcaPropia` y `modeloPropio`. Con `false`, el servidor asigna una unidad libre de la transmisión solicitada. Recepción puede buscar al alumno por correo o teléfono en `GET /api/v1/admin/clientes/busqueda?contacto=...` e indicar `alumnoId` y `canal` (`PRESENCIAL`, `WHATSAPP`, `FACEBOOK`, `LLAMADA` o `WEB`) al agendar.
- La disponibilidad se valida de nuevo dentro de la transacción de reserva. Un bloqueo de base de datos serializa los cambios que pueden competir por instructor, alumno o vehículo.
- Para pagos digitales, el plazo de aprobación es de 30 minutos por defecto y nunca supera el inicio de la clase. Si el alumno elige efectivo, caja puede confirmar el pago hasta el fin previsto de la clase. El vencimiento libera inmediatamente el horario para nuevas reservas y una tarea periódica actualiza el estado registrado. Recepción debe revisar los comprobantes antes del vencimiento.
- La API exige token Bearer salvo en registro, login, salud y documentación. El cierre de sesión se realiza quitando el token en el cliente. Su vigencia es de una hora.
- El origen CORS por defecto es `http://localhost:4200`. Para integrar Angular desde otro origen, establece `APP_CORS_ORIGINS` con la lista separada por comas.

El administrador puede guardar su número de Yape y adjuntar el QR oficial descargado de Yape en **Gestión de la escuela → Cobros**. Los alumnos pueden escanear ese QR desde su cita. El recibo PDF de la escuela es una constancia interna de pago.

## Activar Oracle en AWS

1. Compila el JAR con `.\mvnw.cmd clean package`. En SQL Developer y dentro de la **PDB** que vayas a usar, ejecuta `sql/crear_usuario_oracle.sql` con una conexión administradora y sustituye el marcador de contraseña. El script también permite consultar el nombre del servicio. Puedes ajustar los tablespaces si tu instalación usa nombres distintos.
2. Arranca la aplicación con las variables de abajo. El nombre del servicio mostrado en SQL Developer puede diferir del alias que le diste a tu conexión. El ejemplo con `127.0.0.1` aplica cuando la API y Oracle están en la misma máquina.
3. Flyway creará las tablas en el esquema de `INSTRUCCIONFACIL` durante el primer arranque. La cuenta de la API es la cuenta de ese esquema.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'oracle'
$env:DB_URL = 'jdbc:oracle:thin:@//127.0.0.1:1521/SERVICIO_PDB'
$env:DB_USER = 'instruccionfacil'
$env:DB_PASSWORD = Read-Host 'Contraseña de Oracle' -MaskInput
$env:APP_JWT_SECRET = Read-Host 'Clave aleatoria de al menos 32 bytes' -MaskInput
$env:APP_ADMIN_EMAIL = 'admin@instruccionfacil.test'
$env:APP_ADMIN_PASSWORD = Read-Host 'Contraseña inicial de la API' -MaskInput
java -jar target/instruccionfacil-backend-0.1.0.jar
```

| Variable opcional | Uso |
| --- | --- |
| `APP_RESERVATION_MINUTES` | Plazo de revisión y pago entre 1 y 1440 minutos. |
| `APP_PAGO_YAPE`, `APP_PAGO_PLIN`, `APP_PAGO_TRANSFERENCIA` | Datos que devuelve la API para cada medio habilitado. Si los tres están vacíos, la lista queda vacía y pueden cargarse comprobantes para pruebas con cualquiera de los tres medios. |
| `APP_CORS_ORIGINS` | Orígenes Angular permitidos. |
| `SERVER_PORT` | Puerto HTTP interno, por defecto 8080. |
| `SERVER_BIND_ADDRESS` | Dirección de escucha en perfil Oracle, por defecto `127.0.0.1`. Para exponer la API, publica HTTPS mediante un proxy y deja el listener de Oracle en acceso privado. |

El alcance de esta versión comprende el flujo de reserva, revisión manual del pago y administración de la agenda. Quedan pendientes la API de WhatsApp, recuperación de contraseña, reembolsos, reportes exportables y facturación electrónica.

## Pagos en línea, Gmail y DNI

Las integraciones se configuran en el backend. Usa variables de entorno en IntelliJ o rellena tu `.env` para Docker Compose. Nunca agregues credenciales al frontend ni al repositorio.

| Integración | Variables | Configuración |
| --- | --- | --- |
| Mercado Pago Checkout Pro, Orders API | `APP_MP_TOKEN`, `APP_MP_MODO_PRUEBA`, `APP_MP_WEBHOOK_SECRET`, `APP_MP_FRONTEND_URL` | Para pruebas locales, activa `APP_MP_MODO_PRUEBA`. Para confirmación por webhook, indica la URL HTTPS del frontend y registra el evento **Order (Mercado Pago)** con la URL pública HTTPS del backend terminada en `/api/v1/webhooks/mercado-pago`. |
| Gmail API | `APP_GMAIL_CLIENT_ID`, `APP_GMAIL_CLIENT_SECRET`, `APP_GMAIL_REFRESH_TOKEN`, `APP_GMAIL_REMITENTE` | Habilita Gmail API y autoriza el remitente con OAuth 2.0, acceso sin conexión y el alcance `https://www.googleapis.com/auth/gmail.send`. El remitente debe ser la cuenta autorizada o un alias habilitado. |
| DNI peruano | `APP_DNI_TOKEN`, `APP_DNI_OBLIGATORIO` | Usa el token de Endpoint.pe. Configura `APP_DNI_OBLIGATORIO=true` si el entorno debe impedir altas y cambios de identidad cuando no hay credenciales. Con token configurado, una falla de consulta impide completar la operación y permite reintentar. |

### Flujo de cobro

- El alumno abre checkout con `POST /api/v1/citas/{id}/pagos/checkout`. El monto sale de la reserva y el backend reutiliza la orden para impedir cobros duplicados por reintentos de la aplicación. Durante la orden vigente se bloquean otros medios, la cancelación y la reprogramación de esa cita.
- El backend verifica la firma del webhook y consulta la orden con el token privado. Solo confirma automáticamente una orden acreditada con el importe y la moneda PEN correctos de una cita vigente. El regreso del navegador sirve para abrir la agenda.
- El PDF se encola al aprobar un pago si Gmail está configurado y hay correo registrado. `POST /api/v1/pagos/{id}/recibo/correo` permite verificar el estado y encolar un recibo pendiente de enviar. El destinatario se toma del perfil del alumno.
- La comparación de DNI usa el nombre completo tal como figura en el documento y omite diferencias de mayúsculas, tildes y puntuación. La consulta verifica la coincidencia registral. No se devuelven los datos obtenidos del proveedor.
- En **Ingresos**, administración ve el total bruto por fecha de aprobación (hora de Lima) y los medios de pago. El total corresponde al importe originalmente aprobado. Las discrepancias, pagos tardíos y reembolsos notificados aparecen para conciliación con Mercado Pago.
- En **Gestión de la escuela → Flota**, administración puede registrar, editar, bloquear y retirar vehículos. Los vehículos con citas activas requieren reprogramarlas antes de retirarlos.

Documentación de proveedores: [Mercado Pago Orders](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/create-order), [notificaciones de Mercado Pago](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/notifications), [envío por Gmail](https://developers.google.com/workspace/gmail/api/guides/sending), [OAuth de Google](https://developers.google.com/identity/protocols/oauth2/web-server) y [consulta de DNI](https://endpoint.pe/docs/apis/dni).

### Probar Mercado Pago desde localhost

1. En Mercado Pago Developers, abre tu integración de Checkout Pro y selecciona **Prueba → Credenciales**. Copia el **Access Token** de esa pestaña.
2. Configura `APP_MP_MODO_PRUEBA=true` y `APP_MP_TOKEN` con el token de prueba en la configuración de ejecución de IntelliJ o en tu `.env` privado para Docker Compose. Puedes dejar vacías `APP_MP_WEBHOOK_SECRET` y `APP_MP_FRONTEND_URL`. Usa una base local con datos de prueba.
3. Inicia el backend y el frontend. Entra como alumno, abre una reserva pendiente y elige **Preparar pago de prueba**. Copia el enlace **Abrir checkout de prueba** y ábrelo en una ventana de incógnito con una tarjeta de prueba del panel.
4. Vuelve a la agenda local y pulsa **Verificar pago de prueba**. `POST /api/v1/citas/{id}/pagos/checkout/verificar` consulta la orden guardada con Mercado Pago y confirma la reserva solo si la orden está acreditada y coincide con el importe y la moneda de la cita. El modo de prueba no requiere un túnel y no envía automáticamente recibos por correo.

