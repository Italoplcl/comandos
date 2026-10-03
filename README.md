# Mando

Plugin moderno de administración para Paper/Purpur 26.3, Java 25.

## 1.7.0-test — cierre funcional

Mando incluye almacenamiento por UUID en `players/<UUID>.yml`, Homes, Warps, Spawn, Back, RTP, TPA, Mail, Profile, historial de muertes, teletransporte común con warmup/cooldown/seguridad, Setup moderno, selección AFK (Purpur/EssentialsX/CMI/Mando), gate opcional AuthMe, lectura opcional Vault, Changelog, Announcements, Update Checker configurable, backups, diagnóstico y administración Paper/Purpur.

Comandos principales: `/mando`, `/profile`, `/mail`, `/sethome`, `/home`, `/homes`, `/edithome`, `/restorehome`, `/warp`, `/warps`, `/spawn`, `/back`, `/rtp`, `/tpa`, `/tpaccept`, `/tpdeny`, `/tpatoggle`, `/changelog` y `/serverconfig`.

Integraciones externas son soft-dependencies. TAB y ChatControl conservan ownership de presentación/chat; Mando no modifica scoreboards ni nametags. Vault es sólo lectura. AuthMe bloquea los comandos de Mando antes de autenticar cuando su API está disponible.

Los textos de jugador usan `languages/es.yml` con fallback `languages/en.yml`; `config.yml/messages` se conserva como fallback legado durante 1.7.0-test.

Una build verde valida compilación. La validación final de comportamiento y rendimiento requiere prueba real en Purpur 26.3; Spark debe ejecutarse allí para TPS/MSPT/CPU.
