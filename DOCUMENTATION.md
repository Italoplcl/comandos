# Mando — Documentación técnica

## Filosofía
Mando mantiene separada la información funcional de la presentación. Un Home, Warp o Spawn nunca depende de su cartel, estandarte, partícula o futura etiqueta visual.

## Plataformas
- Paper: plataforma base prevista.
- Purpur: hereda la funcionalidad Paper y añade módulos opcionales.
- Las opciones exclusivas de Purpur deben comprobar plataforma y módulo antes de exponerse.

## Módulos
`modules.<nombre>.enabled` permite apagar funcionalidades. Las integraciones Purpur se encuentran bajo `modules.server-config.purpur`.

**Módulos de Mando** y **Configuración de Purpur** no son lo mismo:
- Módulos activa/desactiva la herramienta administrativa de Mando.
- Configuración de Purpur modifica valores reales existentes en `purpur.yml`.
- Desactivar una herramienta Mando no revierte valores que ya hayan sido guardados en Purpur.

## Marcadores

### Home
Después de guardar el Home, Mando intenta colocar un cartel en la cara del bloque que mira el jugador. Su contenido es decoración / nombre del Home / jugador / decoración y se escribe en ambas caras.

Ejemplo:
1. Mira una superficie válida.
2. Ejecuta `/sethome mina`.
3. El Home queda guardado aunque el marcador físico no pueda colocarse.

### Warp
`/setwarp <nombre>` abre una GUI con nueve colores de estandarte. Tras seleccionar uno se guarda el Warp y se intenta colocar el banner en el bloque de los pies. El banner se orienta según la mirada del jugador.

Ejemplo: `/setwarp mercado`.

### Spawn
`/setspawn` actualiza primero el Spawn real y después intenta colocar un `LIME_BANNER` orientado según la mirada del administrador. Las partículas `HAPPY_VILLAGER` sólo se procesan cuando hay jugadores cerca.

### Seguridad de marcadores
Los TileStates creados se marcan con PDC (`location_marker`, `marker_type`, `marker_name`). Romper un marcador cancela sus drops especiales, pero jamás elimina la ubicación funcional.

Antes de colocar un marcador Mando consulta la validación de soporte de la API. Si nieve, alfombra u otra geometría parcial/especial no puede soportar realmente el cartel o banner, Mando no modifica el terreno y conserva la ubicación lógica.

## ServerConfig / Purpur
Ruta del archivo: `./purpur.yml` (raíz del servidor).

Flujo de escritura:
1. Verificar que Purpur y el módulo estén habilitados.
2. Comprobar que la clave que se va a modificar existe.
3. Crear backup en `plugins/Mando/backups/`.
4. Modificar el valor.
5. Guardar.
6. Releer el archivo y comprobar el valor.
7. Informar que se requiere/recomienda reinicio.

No se ejecuta `/purpur reload` automáticamente.

### Monturas
El módulo Mounts es opt-in y viene desactivado por defecto. La interfaz sólo muestra mobs para los que encuentra la opción compatible en el `purpur.yml` real.

Ejemplo Zombie:
1. `/serverconfig`.
2. **Módulos de Mando -> Purpur • Mounts -> ACTIVADO**.
3. **Configuración de Purpur -> Mounts -> Zombie**.
4. Activa `Montable` y guarda.
5. Reinicia el servidor cuando Mando lo indique.
6. Concede `allow.ride.zombie` mediante un gestor de permisos compatible.

Los permisos especiales de Purpur, como `allow.ride.<mob>`, no se consideran concedidos automáticamente por ser OP.

### Mob Manager
Mob Manager configura comportamiento/propiedades de mobs; no controla cuántos aparecen. Mando sólo muestra las opciones booleanas compatibles que encuentra en la sección real del mob.

No existe un botón de “Configuración global” sin función real. Las opciones visibles deben ejecutar una acción operativa.

## Spawning
Spawning controla límites y frecuencia por mundo y categoría mediante la API del servidor.

Flujo:
1. Ejecuta `/serverconfig`.
2. Selecciona el mundo desde la pantalla principal si corresponde.
3. Abre **Spawning**.
4. Selecciona una categoría.
5. El Dialog muestra límite y ticks entre intentos.
6. Modifica ambos valores y pulsa **Guardar**.

Ejemplo ambiental: seleccionar **Ambient** modifica la categoría `AMBIENT`. Esto no es un limitador exclusivo de `BAT`; otros mobs de esa categoría pueden verse afectados.

## Interfaz híbrida de ServerConfig (1.3.0-test)
Mando usa inventarios para **navegar y seleccionar** y Dialogs nativos para **editar valores o confirmar cambios**.

Esquema:
```
/serverconfig
├── Spawning
├── Módulos de Mando
└── Configuración de Purpur
    ├── Mounts
    ├── Mob Manager
    ├── Gameplay
    ├── Breeding
    └── Raids
```

Los módulos desactivados se muestran como no disponibles en la sección de configuración correspondiente.

## Changelog
Actualmente `/changelog` abre un Dialog moderno. La ampliación Changelog 2.0 (estado leído por jugador, historial y comportamiento al entrar) permanece planificada y no debe confundirse con funcionalidad ya implementada.

## Anuncios
El sistema general de anuncios, BossBars con cuenta regresiva y plantillas sigue planificado; no forma parte de 1.3.0-test.

## Rendimiento y Spark
Toda mecánica periódica nueva debe revisarse con Spark, comparando cuando sea posible antes/después: MSPT/TPS, CPU, tareas/listeners de Mando y hotspots. Especial atención a partículas, marcadores, spawning, entidades y ServerConfig.

La arquitectura debe evitar tareas periódicas innecesarias. En particular, los efectos del Spawn sólo realizan trabajo visible cuando existen jugadores dentro de la distancia configurada.

## Estado de 1.3.0-test
Implementado en esta versión de prueba:
- Interfaz híbrida GUI + Dialogs para Spawning y edición compatible de mobs Purpur.
- Separación visual/funcional entre Módulos de Mando y Configuración de Purpur.
- Backups y verificación posterior de escrituras Purpur.
- Eliminación del placeholder global sin acción.
- Información del permiso `allow.ride.<mob>` en configuración de monturas.
- Home en ambas caras.
- Orientación de banners Warp/Spawn según la mirada.
- Validación de soporte antes de colocar marcadores.

Planificado para versiones posteriores:
- Registro/limpieza persistente completa de marcadores.
- Changelog 2.0.
- Anuncios.
- Quick Actions.
- Nuevos módulos Paper y ampliaciones Purpur.
