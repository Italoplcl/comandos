# EssLite

EssLite es un plugin ligero y modular de comandos y administración visual para servidores Minecraft modernos basados en Paper/Purpur.

## Versión 1.2.0 (preview)

Esta versión parte de la 1.1.2 compilable y comienza el rediseño visual/modular.

### Comandos base
- Homes: `/sethome`, `/home`, `/homes`, `/delhome`
- Warps: `/setwarp`, `/warp`, `/delwarp`
- Spawn: `/spawn`, `/setspawn`
- Utilidades: `/god`, `/heal`, `/fly`, `/back`, `/rtp`, `/tools`, `/ender`
- Administración: `/serverconfig`
- Novedades: `/changelog`

### Marcadores físicos
- Home: cartel luminoso en la superficie que mira el jugador.
- Warp: `/setwarp nombre` abre un selector de 9 colores; el estandarte se intenta colocar donde está parado el jugador.
- Spawn: estandarte verde lima donde está parado el administrador y partículas `HAPPY_VILLAGER` cuando hay jugadores cerca.
- Los marcadores son cosméticos: romperlos no elimina la ubicación.
- EssLite no reemplaza bloques para colocar marcadores; si no hay espacio, conserva la ubicación y muestra una advertencia.

### Paper / Purpur
EssLite detecta la plataforma. Las funciones generales están pensadas para Paper y Purpur. Las funciones exclusivas de Purpur se aíslan como módulos opcionales.

El módulo de monturas Purpur viene desactivado por defecto. Al activarlo, EssLite modifica el `purpur.yml` real de la raíz del servidor en `world-settings.default.mobs`, crea un backup en `plugins/EssLite/backups`, relee el archivo para verificar el cambio y solicita reiniciar el servidor. No ejecuta `/purpur reload` automáticamente.

### Dialogs
`/changelog` es la primera integración con los Dialogs nativos modernos de Paper. El objetivo de próximas iteraciones es usar Dialogs para formularios y edición de ServerConfig en vez de pedir números por chat.

## Configuración modular
Consulta `config.yml`. Los módulos nuevos se agrupan bajo `modules:` y las opciones visuales bajo `markers:`.

## Pendientes principales
- Registro persistente completo de marcadores, detección de duplicados/huérfanos y limpieza segura.
- Quick Actions (G) y accesos visuales desde interfaces nativas.
- Migrar edición de ServerConfig a Dialogs.
- Paper: Anti-Xray, Hoppers, Spawning, Entidades, Seguridad y Diagnóstico/Spark.
- Purpur: Mob Manager, Gameplay, Cría, Raids y Monturas opcionales.
- Compatibility Advisor para advertir configuraciones que puedan afectar otros plugins.
- Pruebas Spark antes/después de cambios relevantes.

## Rendimiento
Los efectos periódicos deben ejecutarse sólo cuando aportan algo. Las partículas del Spawn, por ejemplo, sólo se generan cuando hay jugadores dentro de la distancia configurada.

### Purpur configuration preview
The current test build adds `/serverconfig -> Modules -> Purpur` with opt-in Mounts plus Mob Manager, Gameplay, Breeding and Raids sections. Purpur writes are backed up and verified against the root `purpur.yml`; restart the server after changing Purpur settings.
