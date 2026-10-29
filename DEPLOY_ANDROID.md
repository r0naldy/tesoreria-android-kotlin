# Despliegue Android

Esta guía cubre únicamente el empaquetado y la distribución del cliente Android.
La API es externa y su código, infraestructura y despliegue no forman parte de
este repositorio. Publicar el código fuente tampoco publica el APK en una tienda.

No incluir keystore, contraseñas de firma, JWT, claves de Mistral/DeepSeek ni
secretos de la API. Las claves de IA permanecen exclusivamente en el servicio
externo. La firma institucional y las pruebas finales en un teléfono físico
siguen pendientes; una prueba en emulador no las reemplaza.

## 1. Decidir el canal

- **Google Play:** generar un Android App Bundle (`.aab`). Para una aplicación
  nueva se recomienda Play App Signing y conservar con respaldo la clave de
  carga. Desde el 31 de agosto de 2026, Google Play exige que las aplicaciones
  nuevas y sus actualizaciones para teléfonos apunten a Android 16 / API 36 o
  superior. Este proyecto conserva `targetSdk = 34` por alcance: el APK puede
  distribuirse institucionalmente, pero antes de enviarlo a Play se debe migrar
  a API 36 y validar los cambios de comportamiento. Requisito oficial:
  <https://support.google.com/googleplay/android-developer/answer/11926878?hl=es>.
- **Distribución institucional:** generar un APK release firmado, entregarlo por
  el canal privado de la institución y conservar el mismo keystore para todas
  las actualizaciones.

No instalar la variante `debug` como versión institucional: usa el identificador
`pe.edu.hzg.tesoreria.debug` y una firma de desarrollo.

## 2. Configurar la firma local

El repositorio no genera un keystore automáticamente. Si la aplicación reemplaza
una versión release ya instalada, se debe usar **el mismo applicationId y el
mismo certificado de firma**; de lo contrario Android no permitirá actualizarla
y será necesario desinstalarla, perdiendo sus datos locales.

1. Obtener el keystore institucional del responsable autorizado y guardarlo
   fuera del repositorio y de las carpetas sincronizadas públicamente.
2. Copiar `keystore.properties.example` como `keystore.properties`.
3. Completar localmente `storeFile`, `storePassword`, `keyAlias` y `keyPassword`.

`keystore.properties`, `*.jks` y `*.keystore` están ignorados por Git. No deben
subirse, enviarse por chat ni copiarse dentro de `app/src`.

Si no existe `keystore.properties`, la variante release sigue compilando, pero
su APK/AAB queda sin firma utilizable para una instalación o publicación final.
Si el archivo existe pero está incompleto, Gradle se detiene para evitar un
paquete firmado con una configuración equivocada.

## 3. Versión y URL de API

Cada entrega debe aumentar `APP_VERSION_CODE`: Google Play no permite reutilizar
un código ya publicado y Android bloquea por defecto la instalación de versiones
con un código menor. `APP_VERSION_NAME` es la versión visible. Véase la
[guía oficial de versionado](https://developer.android.com/studio/publish/versioning).
La URL debe usar HTTPS y no debe incluir `/api` al final.

Ejemplo PowerShell para la versión 1.0.0:

```powershell
.\gradlew.bat :app:bundleRelease `
  -PAPP_VERSION_CODE=1 `
  -PAPP_VERSION_NAME=1.0.0 `
  -PAPI_BASE_URL=https://sistema.cetprohzgj.edu.pe
```

Los valores predeterminados son:

```text
APP_VERSION_CODE=1
APP_VERSION_NAME=1.0.0
API_BASE_URL=https://sistema.cetprohzgj.edu.pe
```

`API_BASE_URL` no es un secreto: queda dentro de `BuildConfig`. Ningún token ni
clave privada debe pasarse como propiedad de Gradle o incorporarse al APK.

Antes de distribuir, confirmar que la API externa es compatible con los contratos
del cliente, incluidas las acciones administrativas indicadas en el
[README](README.md#api-externa-y-compatibilidad). Un APK actualizado no agrega
endpoints al servidor. Esta guía no despliega ni modifica la API.

## 4. Generar artefactos

Desde la raíz del repositorio clonado —donde está `gradlew.bat`— y con JDK 17 y
Android SDK configurados:

```powershell
# Pruebas y análisis antes del release
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug

# APK institucional
.\gradlew.bat :app:assembleRelease `
  -PAPP_VERSION_CODE=1 `
  -PAPP_VERSION_NAME=1.0.0 `
  -PAPI_BASE_URL=https://sistema.cetprohzgj.edu.pe

# AAB; requiere cumplir el targetSdk de Play antes de enviarlo a esa tienda
.\gradlew.bat :app:bundleRelease `
  -PAPP_VERSION_CODE=1 `
  -PAPP_VERSION_NAME=1.0.0 `
  -PAPI_BASE_URL=https://sistema.cetprohzgj.edu.pe
```

Salidas habituales:

```text
app/build/outputs/apk/release/app-release.apk          (con firma configurada)
app/build/outputs/apk/release/app-release-unsigned.apk (sin keystore.properties)
app/build/outputs/bundle/release/app-release.aab
```

Antes de distribuir, probar una instalación limpia y una actualización sobre la
última versión institucional en al menos un dispositivo Android real.

## 5. Verificar firma y certificado

Usar `apksigner` del Android SDK. La verificación debe terminar con
`Verified`/`Verifies` y mostrar el certificado institucional esperado:

```powershell
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\<version>\apksigner.bat"
& $apksigner verify --verbose --print-certs `
  app\build\outputs\apk\release\app-release.apk
```

Para confirmar que una APK anterior y la nueva admiten actualización, comparar
el `Signer #1 certificate SHA-256 digest` de ambas; debe ser idéntico.

También conviene calcular y registrar el hash del artefacto entregado:

```powershell
Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```

## 6. Lista final de producción

- API `/health` y login probados desde la red móvil de destino.
- Compatibilidad de las rutas administrativas de la API externa confirmada.
- URL HTTPS de producción compilada y sin proxies de depuración.
- `versionCode` incrementado y `versionName` documentado.
- Firma verificada y certificado comparado con la versión anterior.
- R8/lint/pruebas completados sin errores.
- Cámara, carga de comprobantes, sincronización offline y reintento probados.
- Emisión de ticket probada sin doble toque ni reintento automático.
- Roles Administrador/Finanzas validados con cuentas de prueba.
- APK institucional o AAB de Play archivado junto con su SHA-256.
- Keystore y contraseñas respaldados en el almacén seguro institucional.

No distribuir hasta completar estas comprobaciones, la firma institucional y la
validación en un teléfono real. Para Google Play también se debe completar la
migración del nivel de API objetivo y los requisitos de privacidad de la tienda.
La app no debe crear, rotar ni revelar las credenciales institucionales.
