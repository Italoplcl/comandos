# Mando

Mando es un plugin ligero y modular de comandos y administración visual para servidores Minecraft modernos basados en Paper/Purpur.

## Versión 1.3.0-test

Esta versión continúa desde la 1.2.1 y prueba la interfaz híbrida de administración: inventarios para navegar/seleccionar y Dialogs nativos para editar valores.

### Comandos base
- Homes: `/sethome`, `/home`, `/homes`, `/delhome`
- Warps: `/setwarp`, `/warp`, `/delwarp`
- Spawn: `/spawn`, `/setspawn`
- Utilidades: `/god`, `/heal`, `/fly`, `/back`, `/rtp`, `/tools`, `/ender`
- Administración: `/serverconfig`
- Novedades: `/changelog`

### Marcadores físicos
- Home: cartel luminoso en la superficie que mira el jugador y texto en ambas caras.
- Warp: `/setwarp nombre` abre un selector de 9 colores; el estandarte se orienta según la mirada del jugador.
- Spawn: estandarte verde lima orientado según la mirada del administrador y partículas `HAPPY_VILLAGER` cuando hay jugadores cerca.
- Los marcadores son cosméticos: romperlos no elimina la ubicación.
- Mando no reemplaza bloques para colocar marcadores. Antes de colocar uno valida si la API considera que el bloque puede soportarlo; si no, conserva la ubicación y muestra una advertencia.

### Paper / Purpur
Mando detecta la plataforma. Las funciones generales están pensadas para Paper y Purpur. Las funciones exclusivas de Purpur se aíslan como módulos opcionales.

El módulo de monturas Purpur viene desactivado por defecto. Al activarlo, Mando modifica el `purpur.yml` real de la raíz del servidor, crea un backup en `plugins/Mando/backups`, relee el archivo para verificar el cambio y solicita reiniciar el servidor. No ejecuta `/purpur reload` automáticamente.

**Módulos de Mando** controla qué herramientas administrativas de Mando están disponibles. Desactivar una herramienta no revierte valores ya guardados en Purpur.

**Configuración de Purpur** modifica únicamente opciones reales que Mando encuentra en el `purpur.yml` del servidor. No inventa claves inexistentes.

### Interfaz híbrida
- GUI/inventarios: navegación y selección.
- Dialogs: edición de números, booleanos y confirmaciones.
- `/serverconfig -> Spawning`: selecciona una categoría y abre un Dialog para límite y ticks.
- `/serverconfig -> Configuración de Purpur -> Mob Manager`: selecciona un mob y abre un Dialog con las opciones booleanas compatibles encontradas.

Ejemplo: para reducir la categoría ambiental, abre `/serverconfig`, entra a **Spawning**, selecciona **Ambient** y cambia límite/ticks. Esto afecta la categoría AMBIENT, no exclusivamente a los murciélagos.

Ejemplo Purpur: activa **Mounts** en **Módulos de Mando**, abre **Configuración de Purpur -> Mounts -> Zombie**, activa la opción y reinicia cuando Mando lo indique. Para montar un zombie Purpur requiere el permiso `allow.ride.zombie`; ser OP no concede automáticamente los permisos especiales de Purpur.

## Configuración modular
Consulta `config.yml`. Los módulos se agrupan bajo `modules:` y las opciones visuales bajo `markers:`.

## Pendientes principales
- Registro persistente completo de marcadores, detección de duplicados/huérfanos y limpieza segura.
- Quick Actions (G) y accesos visuales desde interfaces nativas.
- Paper: Anti-Xray, Hoppers, Entidades, Seguridad y Diagnóstico/Spark.
- Ampliar Purpur: Gameplay, Cría, Raids, Mob Manager y Monturas sólo con claves verificadas.
- Compatibility Advisor para advertir configuraciones que puedan afectar otros plugins.
- Changelog 2.0 y módulo de anuncios.
- Pruebas Spark antes/después de cambios relevantes.

## Rendimiento
Los efectos periódicos deben ejecutarse sólo cuando aportan algo. Las partículas del Spawn, por ejemplo, sólo se generan cuando hay jugadores dentro de la distancia configurada. Las mecánicas nuevas relevantes deben revisarse con Spark, especialmente en servidores con pocos hilos disponibles.
