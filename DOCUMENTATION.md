# EssLite — Documentación técnica

## Filosofía
EssLite mantiene separada la información funcional de la presentación. Un Home, Warp o Spawn nunca depende de su cartel, estandarte, partícula o futura etiqueta visual.

## Plataformas
- Paper: plataforma base prevista.
- Purpur: hereda la funcionalidad Paper y añade módulos opcionales.
- Las opciones exclusivas de Purpur deben comprobar plataforma y módulo antes de exponerse.

## Módulos
`modules.<nombre>.enabled` permite apagar funcionalidades. Las integraciones Purpur se encuentran bajo `modules.server-config.purpur`.

## Marcadores
### Home
Después de guardar el home, EssLite intenta colocar un cartel en la cara del bloque que mira el jugador. Su contenido es: decoración / nombre del home / jugador / decoración.

### Warp
`/setwarp <nombre>` abre una GUI con nueve colores de estandarte. Tras seleccionar uno se guarda el Warp y se intenta colocar el banner en el bloque de los pies, únicamente si está libre y existe soporte sólido debajo.

### Spawn
`/setspawn` actualiza primero el spawn real y después intenta colocar un `LIME_BANNER` en la posición del jugador. Las partículas `HAPPY_VILLAGER` sólo se procesan cuando hay jugadores cerca.

### Seguridad
Los TileStates creados se marcan con PDC (`location_marker`, `marker_type`, `marker_name`). Romper un marcador cancela sus drops especiales, pero jamás elimina la ubicación funcional.

## ServerConfig / Purpur
Ruta del archivo: `./purpur.yml` (raíz del servidor).

Flujo de escritura:
1. Verificar que Purpur y el módulo estén habilitados.
2. Crear backup en `plugins/EssLite/backups/`.
3. Modificar `world-settings.default.mobs.<mob>.<opción>`.
4. Guardar.
5. Releer el archivo y comprobar el valor.
6. Informar que se requiere reinicio.

No se ejecuta `/purpur reload` automáticamente.

## Dialogs
La primera pantalla nativa es `/changelog`. Paper ofrece Dialogs desde versiones modernas y EssLite los utilizará progresivamente para sustituir entradas por chat en configuración administrativa.

## Rendimiento y Spark
Toda mecánica periódica nueva debe revisarse con Spark, comparando cuando sea posible antes/después: MSPT/TPS, CPU, tareas/listeners de EssLite y hotspots. Especial atención a partículas, marcadores, spawning, entidades y ServerConfig.
