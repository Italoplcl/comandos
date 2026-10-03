# Mando — documentación de prueba 1.7.0-test

## Persistencia
Mando usa YAML. Los datos generales de jugadores se identifican por UUID en players/<UUID>.yml. Homes, historial de Homes, markers, correo, muertes, Back, Warps y configuración se mantienen separados. Los marcadores son cosméticos y nunca son la fuente de verdad.

## Teletransporte
Homes llegan con pitch 0 y conservan yaw. Warp y Spawn conservan orientación. Home, Warp, Spawn, Back y TPA usan el pipeline común configurable de warmup, cooldown y cancelación por movimiento/daño. RTP mantiene búsqueda asíncrona, world border, intentos y validación de superficie.

## Social
/profile abre el perfil y da acceso al correo/Homes. /mail inbox lista mensajes; /mail read <n> abre y marca leído; /mail delete <n> elimina; /mail <jugador> <texto> envía. Administradores con permiso pueden inspeccionar correo sin marcarlo leído y la acción se audita. TPA incluye expiración, anti-spam, toggle persistente y pipeline de teleport.

## Homes
Límites mediante mando.homes.1/.3/.5/.10/unlimited. /delhome confirma. /renamehome renombra. Se conserva una ubicación anterior y los últimos eliminados según homes.deleted-history. /restorehome permite recuperación autorizada.

## Integraciones
Mando detecta Essentials, CMI, HuskHomes, BetterRTP, TAB, ChatControl, PlaceholderAPI, LuckPerms, Vault y AuthMe. La selección de providers se configura bajo providers. AuthMe bloquea comandos Mando antes de autenticar. Vault se usa solo para lectura de saldo en Profile cuando está disponible. AFK intenta primero la API disponible en Purpur y usa fallback propio.

## Administración
/mando status muestra plataforma, módulos, backup e integraciones. /mando backup crea backup manual. Existen backups programados y de shutdown con rotación. /mando cleanup markers limpia entradas registradas inválidas sin escanear el mundo. /mando diagnose crea un ZIP sanitizado. Las inspecciones privadas se escriben en logs/admin-actions.log.

## Idiomas
languages/es.yml es el idioma por defecto y languages/en.yml el fallback. Los mensajes heredados que todavía no tengan clave externa conservan fallback desde config.yml durante esta build de prueba.

## Pruebas
Esta versión debe probarse en Purpur 26.3 con Java 25. Los fallos observados durante la prueba se corrigen después sin reducir el alcance funcional de esta build.


## Setup y configuración (Lote F)
En la primera instalación, un administrador recibe el Setup moderno de Mando. `/serverconfig setup` permite reabrirlo y `/serverconfig setup defaults` aplica una base segura sin borrar valores ya configurados. El Setup cubre idioma, Homes, markers, teletransporte/Back/seguridad, RTP, Mail, AFK, Vault e integraciones. `/serverconfig commands` muestra el estado real de los módulos y comandos; `/serverconfig integrations` abre la selección de proveedores. Vault permanece sólo lectura. Mando guarda una firma del ecosistema detectado y muestra un Dialog a administradores cuando Paper/Purpur o las integraciones cambian entre arranques. Las opciones Purpur continúan bajo `/serverconfig purpur` y sólo se ofrecen cuando Purpur está disponible.


## Administración (Lote G)
`/mando status` resume versión, plataforma, jugadores, integraciones y debug. `/mando backup` fuerza flush y backup; `/mando diagnose` crea un ZIP sanitizado con estado, resumen de configuración y `mando.log`. `/mando debug on|off` controla trazas administrativas. Shutdown y restart requieren el literal `CONFIRM` y ejecutan flush + backup antes de detener el servidor. Announcements admite CHAT, ACTIONBAR, TITLE y BOSSBAR combinables, rotación, join-delay, duración y comando opcional al finalizar. El Update Checker usa metadata HTTPS configurable y está desactivado por defecto. Changelog conserva historial, muestra tres cambios principales y valida enlaces HTTP/HTTPS antes de hacerlos clicables.
