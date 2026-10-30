# Módulo de Tesorería

Los modelos y contratos están en `domain`, la API y los repositorios en `data`,
y las pantallas y ViewModels en `presentation`. Koin los conecta desde `app`.

Caja, tickets, ingresos, dashboard y egresos comparten el código de presentación
en `commonMain`. Impresión y compartición se implementan en `androidMain`.

La emisión de tickets requiere conexión y no se reintenta automáticamente: el
contrato externo no ofrece una clave de idempotencia. Se conserva el último
resultado confirmado en memoria para las lecturas.
