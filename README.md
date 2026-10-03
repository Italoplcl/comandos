# Mando

**Mando** es un plugin modular de administración y calidad de vida para servidores **Paper/Purpur 26.3**, desarrollado para **Java 25**.

La idea de Mando es concentrar funciones habituales de un servidor en un solo plugin, pero sin intentar apropiarse de sistemas que otros plugins ya administran mejor. Homes, Warps, Spawn, teletransportes, perfiles, correo, TPA, administración y herramientas de servidor comparten una base común, mientras que integraciones como AuthMe, Vault, EssentialsX, CMI, TAB o ChatControl siguen siendo opcionales.

> Estado actual: **1.7.0-test**. La compilación A–H está terminada y comienza la etapa de pruebas reales en servidor.

## Características principales

### Homes
- `/sethome [nombre]`, `/home [nombre]`, `/homes` y `/delhome <nombre>`.
- Límite configurable de Homes por jugador.
- Permiso para Homes ilimitados, manteniendo el límite práctico del menú.
- Iconos editables con `/edithome <nombre> icon <material>`.
- Recuperación de Home eliminado o posición anterior mediante `/restorehome`.
- Persistencia por UUID.
- Marcadores visuales opcionales.
- Teletransporte mediante el pipeline seguro común de Mando.

### Warps
- `/warp [nombre]` y `/warps`.
- Creación y eliminación administrativa con `/setwarp` y `/delwarp`.
- Menú/listado de destinos.
- Marcadores visuales configurables.
- Selector visual de color cuando está habilitado.
- Validación segura antes del teletransporte.

### Spawn
- `/spawn` y `/setspawn`.
- Spawn configurable para el mundo principal.
- Marcador visual opcional.
- Partículas configurables con distancia de visualización e intervalo.

### Teletransportes
Mando centraliza Home, Warp, Spawn, Back, TPA, RTP y teletransporte administrativo mediante un servicio común.

Incluye:
- warmup configurable;
- cooldown configurable por tipo;
- cancelación al recibir daño;
- cancelación por distancia acumulada recorrida;
- comprobación del destino al momento de ejecutar;
- búsqueda segura alrededor de ubicaciones almacenadas;
- permisos para omitir warmup/cooldown;
- generación controlada de la ubicación de `/back`.

### Back
- `/back` regresa a la ubicación anterior registrada.
- Puede conservar la ubicación relacionada con muerte según el flujo de Back.
- Utiliza las mismas comprobaciones de seguridad del sistema de teletransporte.

### RTP
- `/rtp` busca una ubicación aleatoria segura.
- Mundos permitidos configurables.
- Radio mínimo y máximo configurable.
- Número máximo de intentos configurable.
- Centro configurable entre spawn del mundo o posición del jugador.
- Búsqueda separada del hilo principal y teletransporte final mediante el pipeline común.
- Un único cooldown: no existe un segundo cooldown independiente de RTP.

### TPA
- `/tpa <jugador>`
- `/tpaccept`
- `/tpdeny`
- `/tpatoggle`
- Solicitudes con expiración configurable.
- Preferencia de recepción persistente por jugador.
- Destino validado al ejecutar el teletransporte.

### Profile
`/profile` funciona como centro de información del jugador.

La base de Profile integra información persistente del jugador, Homes, Mail, historial de muertes y preferencias. Los administradores pueden consultar perfiles conocidos y usar `/profiletp` para las funciones administrativas de teletransporte asociadas al perfil.

La resolución de jugadores offline utiliza UUID/identidades conocidas por Mando y permite asociación administrativa manual. Mando no inventa UUIDs a partir de nombres desconocidos.

### Mail
Sistema de correo persistente entre jugadores:
- bandeja de entrada;
- envío;
- lectura;
- eliminación;
- bloqueo/desbloqueo de remitentes;
- IDs persistentes;
- almacenamiento dentro del archivo UUID del jugador.

Comando principal: `/mail`.

### Herramientas
- `/tools`: menú de estaciones de trabajo.
- `/ender`: abre el Ender Chest.
- `/god`: modo dios administrativo.
- `/fly`: vuelo administrativo.
- `/heal`: restaura hambre y saturación; la restauración de vida es configurable.

## Almacenamiento

Cada jugador utiliza:

```text
plugins/Mando/players/<UUID>.yml
```

El almacenamiento incluye identidad, Homes, Mail, historial de muertes y preferencias persistentes.

Mando utiliza:
- escritura mediante archivo temporal;
- reemplazo atómico cuando el sistema lo permite;
- copia `.bak`;
- backups de jugadores;
- rotación configurable;
- migración de los archivos globales anteriores `homes.yml`, `mail.yml` y `deaths.yml`.

Los datos persistentes son la fuente de verdad; entidades o marcadores visuales nunca sustituyen esos datos.

## Integraciones

Todas las integraciones son opcionales.

### AFK
Proveedor seleccionable:
- `AUTO`
- `PURPUR`
- `ESSENTIALSX`
- `CMI`
- `MANDO`
- `NONE`

En modo automático Mando intenta utilizar un proveedor disponible antes de recurrir al sistema propio.

### AuthMe
Si AuthMe está instalado, Mando utiliza su estado de autenticación como compuerta. Los comandos de Mando no deben ejecutarse para el jugador antes de completar la autenticación.

### Vault
Integración **sólo lectura**. Mando puede consultar saldo cuando existe un proveedor económico, pero no modifica la economía.

### TAB y ChatControl
Mando no intenta reemplazarlos:
- no administra nametags;
- no administra scoreboards;
- no reemplaza el formateo de chat;
- TAB queda como capa visual externa;
- ChatControl conserva el control del chat.

PlaceholderAPI y LuckPerms son compatibles como plugins opcionales y no son requisitos para arrancar Mando.

## Setup y configuración

El panel principal es:

```text
/serverconfig
```

Alias:
- `/scfg`
- `/purpurgui`

Funciones relevantes:
- `/serverconfig setup`
- `/serverconfig setup defaults`
- `/serverconfig commands`
- `/serverconfig integrations`
- `/serverconfig spawn`
- `/serverconfig modules`
- `/serverconfig purpur`
- `/serverconfig mounts`

El Setup reúne idioma, Homes, marcadores, teletransporte, Back, seguridad, RTP, Mail, AFK, Vault e integraciones.

Mando guarda una firma del ecosistema detectado. Si la plataforma o las integraciones cambian entre arranques, muestra un aviso al administrador para revisar la configuración en lugar de modificar silenciosamente las decisiones del servidor.

## Administración Purpur

Cuando Purpur está disponible, `/serverconfig` expone controles específicos para:
- spawning;
- mobs;
- gameplay;
- breeding;
- raids;
- mounts.

Las modificaciones de `purpur.yml` realizadas por Mando generan backup y posteriormente verifican el valor escrito.

Los módulos Purpur pueden desactivarse desde la configuración si no se quieren utilizar.

## Administración de Mando

El comando administrativo principal es:

```text
/mando
```

Subcomandos actuales:
- `/mando status` — estado de Mando, plataforma e integraciones.
- `/mando backup` — fuerza flush y backup.
- `/mando diagnose` — genera un ZIP de diagnóstico sanitizado.
- `/mando update` — muestra información disponible del Update Checker.
- `/mando debug on|off` — activa/desactiva trazas administrativas.
- `/mando integrations` — estado y proveedor AFK.
- `/mando identity resolve <nombre|UUID>` — resuelve una identidad conocida.
- `/mando identity bind <nombre> <UUID>` — asociación manual.
- `/mando shutdown CONFIRM` — backup y apagado protegido.
- `/mando restart CONFIRM` — backup y reinicio protegido.

Mando mantiene además:

```text
plugins/Mando/logs/mando.log
```

## Diagnóstico y backups

`/mando diagnose` genera un ZIP con información útil para soporte sin incluir deliberadamente todo el contenido privado de los archivos de jugadores.

Las operaciones críticas de apagado/reinicio:
1. requieren el literal `CONFIRM`;
2. fuerzan escritura de datos;
3. realizan backup;
4. sólo entonces ejecutan la operación.

## Announcements

Sistema configurable de anuncios, desactivado por defecto.

Puede mostrar un mismo anuncio mediante uno o varios canales:
- `CHAT`
- `ACTIONBAR`
- `TITLE`
- `BOSSBAR`

Permite:
- mensajes MiniMessage;
- múltiples anuncios rotativos;
- intervalo configurable;
- mensaje al entrar;
- retraso después del login;
- duración;
- BossBar con progreso/cuenta regresiva;
- comando opcional al finalizar.

Ejemplo conceptual: una BossBar puede anunciar un reinicio durante 10 segundos y ejecutar el comando configurado al terminar, o simplemente desaparecer sin ejecutar nada.

## Changelog

`/changelog` muestra las novedades mediante Dialog moderno.

Soporta:
- historial de versiones;
- resumen de cambios;
- tres novedades principales;
- enlaces externos validados antes de convertirlos en enlaces clicables;
- registro de la última versión leída por jugador.

## Update Checker

El comprobador de actualizaciones está **desactivado por defecto**.

Para usarlo se debe configurar un endpoint HTTPS de metadata. Mando puede mostrar:
- versión instalada;
- versión disponible;
- severidad;
- enlace de descarga/publicación.

Mando no trae una URL ficticia ni realiza esta consulta hasta que el administrador lo habilita.

## Idiomas

Archivos incluidos:

```text
plugins/Mando/languages/es.yml
plugins/Mando/languages/en.yml
```

El idioma se selecciona con:

```yaml
language: es
```

Si falta una traducción, Mando intenta usar inglés. Los mensajes heredados de `config.yml/messages` permanecen como fallback durante la etapa 1.7.0-test.

## Módulos

Desde `config.yml` pueden activarse o desactivarse los módulos principales:
- Homes
- Warps
- Spawn
- Changelog
- Profile
- Mail
- TPA
- Server Config

Server Config contiene además módulos Purpur independientes para mounts, mobs, gameplay, breeding y raids.

## Permisos principales

| Permiso | Uso |
|---|---|
| `mando.admin` | Administración general |
| `mando.home` / `mando.sethome` / `mando.homes` | Homes |
| `mando.homes.unlimited` | Ignorar límite normal de Homes |
| `mando.warp` | Usar Warps |
| `mando.setwarp` / `mando.delwarp` | Administrar Warps |
| `mando.spawn` / `mando.setspawn` | Spawn |
| `mando.back` | Back |
| `mando.rtp` | RTP |
| `mando.tpa` | TPA |
| `mando.mail` | Mail |
| `mando.profile` | Profile |
| `mando.profile.admin` | Administración de perfiles |
| `mando.teleport.bypass-warmup` | Omitir warmup |
| `mando.teleport.bypass-cooldown` | Omitir cooldown |
| `mando.serverconfig` | Panel administrativo |
| `mando.god` / `mando.fly` / `mando.heal` | Utilidades administrativas |

La lista definitiva de permisos siempre está en `plugin.yml`.

## Instalación

1. Utiliza un servidor compatible con **Paper/Purpur 26.3**.
2. Ejecuta el servidor con **Java 25**.
3. Copia el JAR de Mando a la carpeta `plugins/`.
4. Inicia el servidor.
5. Revisa `plugins/Mando/config.yml`.
6. Entra como administrador y abre `/serverconfig setup`.
7. Reinicia después de modificar opciones de Purpur que lo requieran.

No es obligatorio instalar AuthMe, Vault, EssentialsX, CMI, TAB, ChatControl, PlaceholderAPI ni LuckPerms.

## Compatibilidad

Objetivo actual:
- Paper 26.3
- Purpur 26.3
- Java 25

Mando utiliza APIs modernas de Paper, incluidos Dialogs. No mantiene una capa de retrocompatibilidad con versiones antiguas de Minecraft.

## Estado de pruebas

Los lotes de desarrollo **A–H compilan correctamente**.

La siguiente etapa es validación real:
- arranque limpio;
- generación de archivos;
- permisos;
- Homes/Warps/Spawn;
- teletransportes;
- RTP;
- Profile/Mail/TPA;
- AuthMe;
- proveedores AFK;
- Vault;
- Dialogs/GUI;
- administración Purpur;
- backups;
- restart/shutdown;
- Announcements;
- Changelog;
- diagnóstico.

Después de las pruebas funcionales se debe medir rendimiento con **Spark**, especialmente TPS, MSPT y CPU. Una build verde confirma compilación; no sustituye la prueba dentro de un servidor real.

## Desarrollo

Repositorio orientado actualmente a la rama 1.7.0-test. Los problemas encontrados durante la prueba real deben registrarse como bugs reproducibles indicando versión del servidor, Java, plugins instalados y pasos para reproducirlos.
