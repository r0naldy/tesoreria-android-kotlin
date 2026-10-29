# Acceso móvil desde ASISTO

La aplicación acepta el mismo pase de corta duración que la web:

```text
https://sistema.cetprohzgj.edu.pe/sso#token=<JWT firmado por ASISTO>
```

El APK **no contiene** `SsoSecreto` ni la clave de integración. Solo entrega el
token recibido a `POST /api/auth/sso`; el backend valida firma, emisor,
audiencia, vencimiento y `jti` de un solo uso antes de emitir el JWT normal de
Tesorería. El cliente rechaza tokens enviados como query string para que no
lleguen a logs de servidores, proxies o analítica.

## Requisito de despliegue para que el App Link sea seguro

Antes de habilitar el botón móvil en ASISTO, publicar en:

```text
https://sistema.cetprohzgj.edu.pe/.well-known/assetlinks.json
```

una asociación Android App Links para el paquete de producción
`pe.edu.hzg.tesoreria` y la huella SHA-256 **real** del certificado release. La
huella no se puede completar hasta que la institución entregue ese keystore.
No se debe sustituir este paso con un esquema personalizado
`hzgtesoreria://`: otra aplicación podría registrar el mismo esquema y robar el
pase de un solo uso.

Ejemplo estructural (reemplazar la huella, no copiarla como está):

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "pe.edu.hzg.tesoreria",
      "sha256_cert_fingerprints": ["HUELLA_SHA256_DEL_CERTIFICADO_RELEASE"]
    }
  }
]
```

ASISTO puede seguir emitiendo la URL web existente. Con la asociación
verificada Android la abre directamente en la app; sin la app instalada se
mantiene el flujo web. El token debe permanecer en el fragmento `#token=...`.
