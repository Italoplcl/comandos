# Mando — documentación 1.7.0-test

## Persistencia
Storage API interna con `players/<UUID>.yml` como fuente de verdad por jugador. Incluye identidad, Homes actuales/anteriores/eliminados, Mail, muertes y preferencias TPA. Escrituras atómicas, copia `.bak`, backups rotativos y migración única de `homes.yml`, `mail.yml` y `deaths.yml`.

## Teletransporte
Home, Warp, Spawn, Back, TPA, RTP y TP administrativo usan `TeleportService`. Warmup/cooldown, cancelación acumulada por movimiento, cancelación por daño, validación al ejecutar, radio seguro 3 para ubicaciones guardadas y generación de Back. RTP conserva búsqueda asíncrona pero su cooldown pertenece únicamente al pipeline común.

## Homes / Warps / Spawn
Homes soporta iconos, previous/deleted y restauración. Warps y Spawn mantienen marcadores cosméticos; los datos persistentes son la fuente de verdad.

## Profile / Mail / TPA
Profile centraliza datos personales/administrativos. Mail es persistente y TPA mantiene toggle persistente y expiración. Resolución offline usa identidades conocidas o asociación manual; no inventa UUID por nombre.

## Integraciones
AFK seleccionable entre AUTO/Purpur/EssentialsX/CMI/Mando/NONE. AuthMe actúa como gate previo de comandos Mando. Vault sólo lee saldo. TAB y ChatControl conservan sus sistemas; Mando no gestiona scoreboards, nametags ni formato de chat. PlaceholderAPI y LuckPerms no son dependencias requeridas.

## Setup
`/serverconfig setup`, `setup defaults`, `commands` e `integrations`. Mando guarda firma del ecosistema y avisa al administrador mediante Dialog si cambia. La edición Purpur conserva backup y verificación.

## Administración
`/mando status`, `backup`, `diagnose`, `update`, `debug`, `integrations`, `identity`, `shutdown CONFIRM` y `restart CONFIRM`. Diagnóstico ZIP sanitizado y `logs/mando.log`. Shutdown/restart realizan flush y backup.

## Announcements y Changelog
Announcements: CHAT, ACTIONBAR, TITLE y BOSSBAR combinables, rotación, join-delay, duración y comando opcional final. Update Checker usa metadata HTTPS configurable y viene desactivado por defecto. Changelog mantiene historial, tres cambios principales y enlaces HTTP/HTTPS validados.

## Idiomas
`languages/es.yml` y `languages/en.yml`; inglés es fallback. Los mensajes heredados de `config.yml/messages` permanecen sólo como fallback de transición. Algunas etiquetas del panel técnico Purpur siguen ligadas a la UI administrativa y se revisan en la prueba visual final.

## Seguridad y compatibilidad
GUIs de inventario con holder propio cancelan clicks/drag; comandos sensibles requieren permisos y las operaciones críticas requieren `CONFIRM`. Las integraciones son soft-dependencies. No existe ningún namespace, package, permiso o ruta EssLite.

## Prueba final
Compilar no sustituye prueba real. Validar en Purpur 26.3/Java 25: arranque limpio, AuthMe, proveedores AFK, teletransportes, reinicio, backups, Dialogs y GUI. Ejecutar Spark en servidor real para TPS/MSPT/CPU después de las pruebas funcionales.
