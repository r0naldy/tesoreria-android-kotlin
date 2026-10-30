# Código fuente del proyecto Tesorería móvil

El código de la aplicación se mantiene en el repositorio
[r0naldy/tesoreria-android-kotlin](https://github.com/r0naldy/tesoreria-android-kotlin).
La rama de entrega es `main`.

## Consultar y descargar

```powershell
git clone https://github.com/r0naldy/tesoreria-android-kotlin.git
cd tesoreria-android-kotlin
git log -1 --oneline
```

Abra esta carpeta en Android Studio. Los requisitos de compilación, la conexión
a la API y las pruebas están descritos en el [README](../README.md).

## Organización del código

| Carpeta | Responsabilidad |
| --- | --- |
| `app` | Punto de entrada Android, recursos y composición de dependencias |
| `domain` | Modelos de negocio, contratos, validaciones y casos de uso |
| `data` | API HTTP, DTO, repositorios, persistencia y sincronización |
| `presentation` | Pantallas, navegación, estado y adaptadores de interfaz |

El [informe de arquitectura](../ARCHITECTURE.md) explica las dependencias entre
estos módulos y la separación entre `commonMain` y `androidMain`.

## Referencia del software

Solano Ito Lopez, R. (2026). *Tesorería móvil* [Código fuente]. GitHub.
https://github.com/r0naldy/tesoreria-android-kotlin

Para identificar una entrega exacta, registre el hash obtenido con
`git rev-parse HEAD`. La referencia a la rama `main` permite consultar el estado
actual, mientras que el hash identifica una versión inmutable.
