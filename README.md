# Tesorería móvil — Android Kotlin

Cliente Android nativo para la gestión institucional de tesorería, con Kotlin,
Jetpack Compose Material 3, arquitectura por capas y MVVM.

Este repositorio contiene **solo la aplicación Android**. La API es un servicio
externo: aquí no se incluyen su código, base de datos, infraestructura ni
credenciales. No es un proyecto KMP y no incluye iOS.

## Alcance y estado

La app incluye caja, tickets, reportes, egresos con captura de comprobantes,
dashboard, comparativa, programas, servicios, estudiantes, registros de matrícula,
períodos, usuarios, configuración y auditoría. Las acciones disponibles dependen
del rol del usuario y del contrato que ofrezca la API configurada.

La firma institucional y la validación final en un teléfono físico siguen
pendientes. Las pruebas instrumentadas con repositorios simulados no sustituyen
las pruebas de cámara, impresión, conectividad y sincronización con una API real.

El proyecto utiliza `minSdk = 24` y `compileSdk/targetSdk = 34`. Para publicar una
aplicación móvil nueva o una actualización en Google Play debe migrarse y
validarse API 36, según el
[requisito oficial vigente desde el 31 de agosto de 2026](https://support.google.com/googleplay/android-developer/answer/11926878?hl=es).
Publicar el código fuente no equivale a publicar la app en una tienda.

## Arquitectura y organización

La interfaz observa estados de los ViewModels; los repositorios coordinan los
contratos REST, la persistencia local y las reglas de sincronización. La inyección
de dependencias permite sustituir los repositorios en las pruebas.

```text
app/src/
├── main/java/pe/edu/hzg/tesoreria/
│   ├── domain/          modelos, contratos y validaciones
│   ├── data/            Ktor, DTO, persistencia, sesión y sincronización
│   ├── features/
│   │   ├── treasury/    caja, tickets y reportes
│   │   └── academic/    estudiantes, registros, períodos y comparativa
│   ├── admin/           usuarios, configuración y auditoría
│   ├── presentation/    ViewModels y estados de los módulos principales
│   ├── ui/              navegación, pantallas y tema Material 3
│   ├── security/        validación de enlaces e imágenes remotas
│   ├── camera/          captura mediante FileProvider
│   └── di/              configuración de dependencias
├── main/sqldelight/     esquema, consultas y migraciones locales
├── test/                pruebas JVM de contratos, reglas y repositorios
└── androidTest/         recorridos de interfaz y permisos con datos de prueba
```

Tecnologías principales: Kotlin 2.1, Java 17, Compose, Ktor, SQLDelight, Koin,
WorkManager, Coroutines, Flow y kotlinx.serialization. Las versiones se
centralizan en `gradle/libs.versions.toml`. El wrapper usa Gradle 8.9 con
verificación SHA-256 y el proyecto usa AGP 8.7.3.

El diseño toma la paleta azul marino y verde petróleo de ASISTO, con pesos
tipográficos normales/medios y modo oscuro. Los catálogos emplean listas/grillas
lazy, sin imágenes decorativas pesadas ni animaciones continuas.

## API externa y compatibilidad

La URL predeterminada es `https://sistema.cetprohzgj.edu.pe`. Para usar un entorno
autorizado de pruebas, compilar desde la raíz del repositorio:

```powershell
.\gradlew.bat :app:assembleDebug -PAPI_BASE_URL=https://staging.ejemplo.pe
```

La URL base debe ser HTTPS, sin credenciales y sin `/api` al final. La aplicación
consume `/api/auth`, `/api/dashboard`, `/api/alumnos`, `/api/programas`,
`/api/servicios`, `/api/turnos`, `/api/tickets`, `/api/reportes`,
`/api/comprobantes`, `/api/comparativa`, `/api/periodos`, `/api/registros`,
`/api/usuarios`, `/api/configuracion` y `/api/auditoria`.

Las acciones administrativas de edición y eliminación necesitan, además del
resto del contrato, las rutas `PUT /api/alumnos/{id}`, `DELETE /api/alumnos/{id}`,
`PUT /api/usuarios/{id}` y `PUT /api/registros/{id}`. Su compatibilidad debe
confirmarse con el responsable de la API externa. Cambiar o instalar el APK no
agrega rutas al servidor ni modifica su despliegue.

El cliente no incorpora claves de integración servidor-a-servidor. El SSO desde
ASISTO entrega un pase de corta duración a `POST /api/auth/sso`; el dominio debe
tener una asociación App Links válida para el certificado institucional.
Consulte [SSO desde ASISTO](docs/SSO-ASISTO.md).

### CRUD y conservación del historial

- Programas y servicios: consulta, búsqueda, creación, edición, cambio de estado
  y eliminación confirmada por la API, con permisos administrativos.
- Estudiantes: registro y corrección de datos personales; la matrícula y el DNI
  no se cambian desde el formulario de edición. Con historial corresponde un
  retiro administrativo, no un borrado.
- Registros: alta, edición de datos base y eliminación sujeta a las restricciones
  académicas de la API; los datos particulares por semestre se conservan.
- Usuarios: alta, nombre visible, rol, estado y eliminación con protecciones
  administrativas. La pantalla no cambia contraseñas.
- Períodos: apertura y cierre. Auditoría y reportes: consulta. No se ofrecen
  borrados de tickets, egresos ni movimientos históricos.

Los formularios conservan los datos y muestran el motivo cuando falla una
operación. No debe confundirse un rechazo por historial o permisos con una
eliminación pendiente: la API es la autoridad final sobre estas reglas.

## Persistencia y sincronización

Programas y servicios se pueden consultar desde la caché local. Sus nuevas
modificaciones requieren conexión y confirmación del servidor. Los borradores
heredados de la cola se identifican por propietario; descartarlos elimina solo
la propuesta local después de recuperar la versión del servidor.

El flujo de egresos es:

```text
Cámara → archivo privado → registro y operación local con propietario
       → WorkManager espera red y la misma sesión
       → API externa guarda y procesa el comprobante
       → confirmación persistida → eliminación de la foto temporal
```

La foto no se guarda en la galería. Una respuesta ambigua después del envío deja
el comprobante para verificación manual y no provoca otro POST automático.
La emisión de tickets requiere conexión y bloquea los envíos simultáneos.

El comprobante, sus conceptos y la operación pendiente se escriben en una misma
transacción. Los borradores locales solo son visibles para su propietario; los
registros sin propietario confirmado se mantienen fuera de la cola activa. Las
cancelaciones de tareas se propagan sin convertirse en reintentos de envío.

## Seguridad

- JWT y metadatos de sesión cifrados con AES-GCM en Android Keystore.
- Bearer limitado al origen HTTPS configurado; no se reenvía por redirecciones.
- Backups desactivados, archivos privados y FileProvider no exportado.
- `FLAG_SECURE` protege capturas y vistas recientes en la aplicación normal.
- Validación del origen, tipo y tamaño de las imágenes remotas.
- Sin registros de contraseñas, JWT, fotos o cuerpos sensibles en los logs.
- Release con R8 y reducción de recursos.

Mistral OCR y DeepSeek son servicios utilizados por la API externa, no por el
APK directamente. No se deben agregar sus claves, secretos JWT, datos reales de
estudiantes, certificados privados ni contraseñas a código, recursos,
`BuildConfig` o propiedades versionadas. La URL pública de la API no es un
secreto. `local.properties` y los archivos de firma se mantienen locales.

## Compilar y probar

Requisitos: JDK 17, Android SDK Platform 34 y acceso a los repositorios de
dependencias. Abra la raíz clonada, donde están `gradlew.bat` y
`settings.gradle.kts`, en Android Studio. Configure allí el SDK, o cree un
`local.properties` local con su ruta:

```properties
sdk.dir=C\:\\Users\\usuario\\AppData\\Local\\Android\\Sdk
```

Desde esa misma raíz, en PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

# Con un dispositivo o emulador de pruebas conectado
.\gradlew.bat :app:connectedDebugAndroidTest

# Empaquetado release; sin configuración de firma, queda sin firmar
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

En macOS/Linux use `./gradlew` en lugar de `.\gradlew.bat`. Las pruebas
instrumentadas del catálogo utilizan repositorios de prueba y no deben escribir
en producción. Las pruebas de integración manual deben usar cuentas y datos de
prueba autorizados.

Validación local del 28 de octubre de 2026: 107 pruebas JVM aprobadas, sin fallos
ni omisiones; lint sin errores; debug, release minificado y AAB compilados.
Las advertencias restantes de lint no se han ocultado y se pueden consultar en
`app/build/reports/lint-results-debug.html`.

El flujo `.github/workflows/android.yml` ejecuta pruebas JVM, lint y compilación
debug en cada push a `main` y en las solicitudes de cambios. Utiliza una URL de
API de ejemplo, no credenciales institucionales, y no despliega el backend ni
publica aplicaciones. Los recorridos instrumentados se ejecutan aparte en un
emulador o dispositivo autorizado.

Salidas:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
app/build/outputs/bundle/release/app-release.aab
```

La variante debug usa `pe.edu.hzg.tesoreria.debug`; no es la entrega
institucional. Para firma, versionado, verificación y lista de comprobación
previa a distribución, consulte [Despliegue Android](DEPLOY_ANDROID.md).
