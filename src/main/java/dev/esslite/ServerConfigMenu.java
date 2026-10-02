package dev.esslite;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerConfigMenu implements Listener, CommandExecutor, TabCompleter {
    private final EssLite plugin;
    private final ModuleManager modules;
    private final PlatformDetector platform;
    private final Map<UUID, World> selectedWorld = new HashMap<>();
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();
    private final Map<UUID,Integer> mountPage = new HashMap<>();
    private final Map<UUID,String> mobSource = new HashMap<>();

    private static final String MAIN = "§8EssLite • Server Config";
    private static final String SPAWN = "§8EssLite • Spawning";
    private static final String MODULES = "§8EssLite • Módulos de EssLite";
    private static final String PURPUR = "§8EssLite • Configuración de Purpur";
    private static final String GAMEPLAY = "§8EssLite • Purpur Gameplay";
    private static final String BREEDING = "§8EssLite • Purpur Breeding";
    private static final String RAIDS = "§8EssLite • Purpur Raids";
    private static final String MOUNTS = "§8EssLite • Monturas Purpur";
    private static final String MOB_MANAGER = "§8EssLite • Mob Manager";
    private static final String MOB_PREFIX = "§8EssLite • Mob: ";

    private static final List<SpawnCategory> CATS = List.of(SpawnCategory.MONSTER, SpawnCategory.ANIMAL, SpawnCategory.AMBIENT,
            SpawnCategory.WATER_ANIMAL, SpawnCategory.WATER_AMBIENT, SpawnCategory.WATER_UNDERGROUND_CREATURE, SpawnCategory.AXOLOTL);
    private static final List<String> MOBS = List.of("allay","armadillo","axolotl","bat","bee","blaze","bogged","breeze","camel","cat","cave_spider","chicken","cod","cow","creeper","dolphin","donkey","drowned","elder_guardian","ender_dragon","enderman","endermite","evoker","fox","frog","ghast","giant","glow_squid","goat","guardian","hoglin","horse","husk","illusioner","iron_golem","llama","magma_cube","mooshroom","mule","ocelot","panda","parrot","phantom","pig","piglin","piglin_brute","pillager","polar_bear","pufferfish","rabbit","ravager","salmon","sheep","shulker","silverfish","skeleton","skeleton_horse","slime","snow_golem","spider","squid","stray","strider","tadpole","trader_llama","tropical_fish","turtle","vex","villager","vindicator","wandering_trader","warden","witch","wither","wither_skeleton","wolf","zoglin","zombie","zombie_horse","zombie_villager","zombified_piglin");

    record PendingInput(String kind, String key, World world) {}

    public ServerConfigMenu(EssLite plugin, ModuleManager modules, PlatformDetector platform) {
        this.plugin = plugin; this.modules = modules; this.platform = platform;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Este comando debe usarse dentro del juego."); return true; }
        if (!p.hasPermission("esslite.serverconfig")) { p.sendMessage(Component.text("No tienes permiso.")); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) { openSpawn(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("modules")) { openModules(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("purpur")) { openPurpur(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("mounts")) { openMountsChecked(p,0); return true; }
        openMain(p); return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return args.length == 1 ? List.of("spawn", "modules", "purpur", "mounts") : List.of();
    }

    private World world(Player p) { return selectedWorld.computeIfAbsent(p.getUniqueId(), u -> p.getWorld()); }

    private void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN);
        inv.setItem(10, item(Material.ZOMBIE_HEAD, "§aSpawning", "§7Límites y frecuencia por mundo", "§eClick para abrir"));
        inv.setItem(12, item(Material.REPEATER, "§dMódulos de EssLite", "§7Activa/desactiva herramientas administrativas", "§7No cambia por sí solo la configuración de Purpur", "§eClick para abrir"));
        if (platform.isPurpur() && modules.purpurEnabled())
            inv.setItem(13, item(Material.AMETHYST_SHARD, "§5Configuración de Purpur", "§aPurpur detectado", "§7Edita opciones reales de purpur.yml", "§eClick para abrir"));
        else inv.setItem(13, item(Material.GRAY_DYE, "§7Purpur", platform.isPurpur()?"§cMódulo desactivado":"§cNo detectado"));
        inv.setItem(14, item(Material.GRASS_BLOCK, "§bMundo: §f" + world(p).getName(), "§7Click para cambiar de mundo"));
        inv.setItem(16, item(Material.REDSTONE_TORCH, "§cCompatibilidad", "§7Plataforma: §f"+platform.platform(), "§7Cambios Purpur: backup + verificación", "§7Reinicio recomendado tras editar Purpur"));
        p.openInventory(inv);
    }

    private void openModules(Player p) {
        Inventory inv=Bukkit.createInventory(null,27,MODULES);
        inv.setItem(10,moduleItem(Material.SADDLE,"Purpur • Mounts","modules.server-config.purpur.mounts.enabled",false));
        inv.setItem(11,moduleItem(Material.ZOMBIE_HEAD,"Purpur • Mob Manager","modules.server-config.purpur.mobs.enabled",true));
        inv.setItem(12,moduleItem(Material.REDSTONE,"Purpur • Gameplay","modules.server-config.purpur.gameplay.enabled",true));
        inv.setItem(13,moduleItem(Material.WHEAT,"Purpur • Breeding","modules.server-config.purpur.breeding.enabled",true));
        inv.setItem(14,moduleItem(Material.OMINOUS_BOTTLE,"Purpur • Raids","modules.server-config.purpur.raids.enabled",true));
        inv.setItem(22,item(Material.ARROW,"§fVolver")); p.openInventory(inv);
    }
    private ItemStack moduleItem(Material m,String name,String path,boolean def){boolean on=plugin.getConfig().getBoolean(path,def);return item(m,"§e"+name,"§7Herramienta EssLite: "+(on?"§aACTIVADA":"§cDESACTIVADA"),"§7No revierte valores existentes de Purpur","§eClick para alternar","§8"+path);}
    private void toggleModule(Player p,String path,boolean def){boolean now=plugin.getConfig().getBoolean(path,def);plugin.getConfig().set(path,!now);plugin.saveConfig();p.sendMessage("§6EssLite §8» §fMódulo "+(now?"§cdesactivado":"§aactivado")+"§f: §7"+path);openModules(p);}

    private void cycleWorld(Player p) { List<World> worlds=Bukkit.getWorlds(); int i=worlds.indexOf(world(p)); selectedWorld.put(p.getUniqueId(),worlds.get((i+1)%worlds.size())); openMain(p); }

    private void openSpawn(Player p) {
        World w=world(p); Inventory inv=Bukkit.createInventory(null,27,SPAWN);
        for(int i=0;i<CATS.size();i++){SpawnCategory cat=CATS.get(i);int limit=w.getSpawnLimit(cat);long ticks=w.getTicksPerSpawns(cat);inv.setItem(9+i,item(icon(cat),"§e"+pretty(cat.name()),"§fLímite: §a"+limit,"§fTicks/intento: §b"+ticks,"","§eClick para editar en Dialog"));}
        inv.setItem(22,item(Material.ARROW,"§fVolver"));inv.setItem(26,item(Material.COMPASS,"§b"+w.getName(),"§7Se configura por mundo"));p.openInventory(inv);
    }
    private Material icon(SpawnCategory c){return switch(c){case MONSTER->Material.ZOMBIE_HEAD;case ANIMAL->Material.COW_SPAWN_EGG;case AMBIENT->Material.BAT_SPAWN_EGG;case WATER_ANIMAL->Material.SQUID_SPAWN_EGG;case WATER_AMBIENT->Material.COD_SPAWN_EGG;case WATER_UNDERGROUND_CREATURE->Material.GLOW_SQUID_SPAWN_EGG;case AXOLOTL->Material.AXOLOTL_SPAWN_EGG;default->Material.SPAWNER;};}
    private void openSpawnDialog(Player p, SpawnCategory cat) {
        World w = world(p);
        String limit = String.valueOf(w.getSpawnLimit(cat));
        String ticks = String.valueOf(w.getTicksPerSpawns(cat));
        ActionButton save = ActionButton.builder(Component.text("Guardar"))
                .tooltip(Component.text("Aplicar límite y frecuencia a " + w.getName()))
                .action(DialogAction.customClick((response, audience) -> {
                    if (!(audience instanceof Player player)) return;
                    try {
                        int newLimit = Integer.parseInt(Objects.requireNonNullElse(response.getText("limit"), limit).trim());
                        int newTicks = Integer.parseInt(Objects.requireNonNullElse(response.getText("ticks"), ticks).trim());
                        if (newLimit < -1 || newLimit > 10000 || newTicks < -1 || newTicks > 1000000) throw new NumberFormatException();
                        w.setSpawnLimit(cat, newLimit);
                        w.setTicksPerSpawns(cat, newTicks);
                        player.sendMessage("§6EssLite §8» §aSpawning actualizado §7(" + pretty(cat.name()) + ": límite " + newLimit + ", ticks " + newTicks + ")");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openSpawn(player), 1L);
                    } catch (NumberFormatException ex) {
                        player.sendMessage("§cValores inválidos. Límite: -1..10000. Ticks: -1..1000000.");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openSpawnDialog(player, cat), 1L);
                    }
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();
        ActionButton cancel = ActionButton.builder(Component.text("Cancelar")).build();
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Spawning · " + pretty(cat.name())))
                        .body(List.of(DialogBody.plainMessage(Component.text("Mundo: " + w.getName() + "\n-1 conserva el comportamiento predeterminado cuando la API lo admite."))))
                        .inputs(List.of(
                                DialogInput.text("limit", Component.text("Límite de entidades")).initial(limit).maxLength(8).width(220).build(),
                                DialogInput.text("ticks", Component.text("Ticks entre intentos")).initial(ticks).maxLength(10).width(220).build()
                        )).build())
                .type(DialogType.confirmation(save, cancel)));
        p.showDialog(dialog);
    }

    private File purpurFile(){return new File("purpur.yml");}
    private YamlConfiguration purpur(Player p){File f=purpurFile();if(!f.isFile()){p.sendMessage("§cNo encontré purpur.yml en la raíz del servidor.");return null;}return YamlConfiguration.loadConfiguration(f);}
    private boolean purpurReady(Player p){if(!platform.isPurpur()){p.sendMessage("§cEsta sección requiere Purpur.");return false;}if(!modules.purpurEnabled()){p.sendMessage("§cEl módulo Purpur de EssLite está desactivado.");return false;}return purpurFile().isFile() || missingPurpur(p);}
    private boolean missingPurpur(Player p){p.sendMessage("§cNo encontré purpur.yml en la raíz del servidor.");return false;}

    private void openPurpur(Player p){
        if(!purpurReady(p))return;Inventory inv=Bukkit.createInventory(null,27,PURPUR);
        inv.setItem(10,purpurModuleItem(Material.SADDLE,"Mounts",modules.mountsEnabled()));
        inv.setItem(11,purpurModuleItem(Material.ZOMBIE_HEAD,"Mob Manager",modules.mobsEnabled()));
        inv.setItem(12,purpurModuleItem(Material.REDSTONE,"Gameplay",modules.gameplayEnabled()));
        inv.setItem(13,purpurModuleItem(Material.WHEAT,"Breeding",modules.breedingEnabled()));
        inv.setItem(14,purpurModuleItem(Material.OMINOUS_BOTTLE,"Raids",modules.raidsEnabled()));
        inv.setItem(18,item(Material.PAPER,"§aArchivo encontrado","§fpurpur.yml","§7Cambios sobre world-settings.default"));
        inv.setItem(22,item(Material.ARROW,"§fVolver"));p.openInventory(inv);
    }
    private ItemStack purpurModuleItem(Material m,String name,boolean on){return item(m,(on?"§a":"§7")+name,"§7Herramienta EssLite: "+(on?"§aACTIVADA":"§cDESACTIVADA"),on?"§eClick para configurar":"§8Actívala en Módulos de EssLite");}

    private void openMountsChecked(Player p,int page){if(!purpurReady(p))return;if(!modules.mountsEnabled()){p.sendMessage("§cMounts está desactivado. Actívalo en /serverconfig → Modules.");return;}openMounts(p,page);}
    private void openMounts(Player p,int page){
        mobSource.put(p.getUniqueId(), "mounts");
        YamlConfiguration y=purpur(p);if(y==null)return;List<String> visible=new ArrayList<>();for(String mob:MOBS)if(y.contains("world-settings.default.mobs."+mob+".ridable"))visible.add(mob);int per=45,max=Math.max(0,(visible.size()-1)/per);page=Math.max(0,Math.min(max,page));mountPage.put(p.getUniqueId(),page);Inventory inv=Bukkit.createInventory(null,54,MOUNTS+" §7"+(page+1)+"/"+(max+1));
        int slot=0;for(int idx=page*per;idx<visible.size()&&slot<per;idx++){String mob=visible.get(idx),base="world-settings.default.mobs."+mob;boolean rid=y.getBoolean(base+".ridable");inv.setItem(slot++,item(egg(mob),"§f"+pretty(mob),"§7Montable: "+(rid?"§aSí":"§cNo"),"§eClick para configurar"));}
        if(page>0)inv.setItem(45,item(Material.ARROW,"§fPágina anterior"));if(page<max)inv.setItem(53,item(Material.ARROW,"§fPágina siguiente"));inv.setItem(49,item(Material.BARRIER,"§fVolver"));inv.setItem(50,item(Material.COMPASS,"§bConfiguración global","§7world-settings.default"));p.openInventory(inv);
    }

    private void openMobManager(Player p,int page){
        mobSource.put(p.getUniqueId(), "mobs");
        YamlConfiguration y=purpur(p);if(y==null)return;List<String> visible=new ArrayList<>();for(String mob:MOBS)if(y.isConfigurationSection("world-settings.default.mobs."+mob))visible.add(mob);int per=45,max=Math.max(0,(visible.size()-1)/per);page=Math.max(0,Math.min(max,page));mountPage.put(p.getUniqueId(),page);Inventory inv=Bukkit.createInventory(null,54,MOB_MANAGER+" §7"+(page+1)+"/"+(max+1));
        int slot=0;for(int idx=page*per;idx<visible.size()&&slot<per;idx++){String mob=visible.get(idx),base="world-settings.default.mobs."+mob;List<String> lore=new ArrayList<>();if(y.contains(base+".ridable"))lore.add("§7Montable: "+(y.getBoolean(base+".ridable")?"§aSí":"§cNo"));if(y.contains(base+".always-drop-exp"))lore.add("§7XP siempre: "+(y.getBoolean(base+".always-drop-exp")?"§aSí":"§cNo"));lore.add("§eClick para configurar");inv.setItem(slot++,item(egg(mob),"§f"+pretty(mob),lore.toArray(String[]::new)));}
        if(page>0)inv.setItem(45,item(Material.ARROW,"§fPágina anterior"));if(page<max)inv.setItem(53,item(Material.ARROW,"§fPágina siguiente"));inv.setItem(49,item(Material.BARRIER,"§fVolver"));p.openInventory(inv);
    }
    private Material egg(String mob){Material m=Material.matchMaterial(mob.toUpperCase(Locale.ROOT)+"_SPAWN_EGG");return m==null?Material.SPAWNER:m;}
    private void openMob(Player p,String mob){
        YamlConfiguration y=purpur(p); if(y==null)return;
        String base="world-settings.default.mobs."+mob;
        LinkedHashMap<String,String> options=new LinkedHashMap<>();
        options.put("ridable","Montable");
        options.put("controllable","Controlable WASD");
        options.put("ridable-in-water","Montable en agua");
        options.put("always-drop-exp","Siempre entrega XP");
        List<DialogInput> inputs=new ArrayList<>();
        List<String> existing=new ArrayList<>();
        for(var entry:options.entrySet()){
            String optionPath=base+"."+entry.getKey();
            if(y.contains(optionPath) && y.get(optionPath) instanceof Boolean){
                existing.add(entry.getKey());
                inputs.add(DialogInput.bool(entry.getKey(),Component.text(entry.getValue())).initial(y.getBoolean(optionPath)).build());
            }
        }
        if(inputs.isEmpty()){p.sendMessage("§7Este mob no tiene opciones booleanas compatibles expuestas por EssLite.");return;}
        String permissionInfo = y.contains(base+".ridable") ? "\nPermiso de Purpur para montar: allow.ride."+mob+"\nOP no implica automáticamente este permiso." : "";
        ActionButton save=ActionButton.builder(Component.text("Guardar"))
                .tooltip(Component.text("Guardar cambios en purpur.yml"))
                .action(DialogAction.customClick((response,audience)->{
                    if(!(audience instanceof Player player))return;
                    try{
                        LinkedHashMap<String,Object> changes=new LinkedHashMap<>();
                        for(String key:existing){Boolean value=response.getBoolean(key);if(value!=null)changes.put(base+"."+key,value);}
                        writePurpurVerifiedBatch(purpurFile(),changes);
                        player.sendMessage("§6EssLite §8» §aConfiguración de "+pretty(mob)+" guardada. §7Backup creado; reinicia el servidor.");
                    }catch(Exception ex){purpurError(player,ex);}
                },ClickCallback.Options.builder().uses(1).build())).build();
        Dialog dialog=Dialog.create(builder->builder.empty()
                .base(DialogBase.builder(Component.text("Purpur · "+pretty(mob)))
                        .body(List.of(DialogBody.plainMessage(Component.text("Sólo se muestran opciones existentes en tu purpur.yml."+permissionInfo))))
                        .inputs(inputs).build())
                .type(DialogType.confirmation(save,ActionButton.builder(Component.text("Cancelar")).build())));
        p.showDialog(dialog);
    }
    private int addMobToggle(Inventory inv,YamlConfiguration y,String base,int slot,Material mat,String name,String key){if(y.contains(base+"."+key)){inv.setItem(slot,toggleItem(mat,name,y.getBoolean(base+"."+key),key));return slot+2;}return slot;}
    private ItemStack toggleItem(Material mat,String name,boolean value,String key){return item(mat,"§e"+name,"§7Configurado: "+(value?"§aACTIVADO":"§cDESACTIVADO"),"§8"+key,"§eClick para alternar","§7Requiere reiniciar el servidor");}

    private void openGameplay(Player p){
        if(!modules.gameplayEnabled()){p.sendMessage("§cGameplay Purpur está desactivado.");return;}YamlConfiguration y=purpur(p);if(y==null)return;Inventory inv=Bukkit.createInventory(null,27,GAMEPLAY);String b="world-settings.default.gameplay-mechanics.";int s=10;
        s=addPathToggle(inv,y,s,Material.RED_BED,"La lluvia termina al dormir",b+"rain-stops-after-sleep");
        s=addPathToggle(inv,y,s,Material.LIGHTNING_ROD,"La tormenta termina al dormir",b+"thunder-stops-after-sleep");
        s=addPathToggle(inv,y,s,Material.ENDER_PEARL,"Entidades pueden usar portales",b+"entities-can-use-portals");
        addPathToggle(inv,y,s,Material.POWERED_RAIL,"Mobs ignoran raíles",b+"mobs-ignore-rails");inv.setItem(22,item(Material.ARROW,"§fVolver"));p.openInventory(inv);
    }
    private int addPathToggle(Inventory inv,YamlConfiguration y,int slot,Material mat,String name,String path){if(y.contains(path)){inv.setItem(slot,item(mat,"§e"+name,"§7Configurado: "+(y.getBoolean(path)?"§aACTIVADO":"§cDESACTIVADO"),"§8"+path,"§eClick para alternar","§7Requiere reiniciar el servidor"));return slot+2;}return slot;}

    private void openBreeding(Player p){
        if(!modules.breedingEnabled()){p.sendMessage("§cBreeding Purpur está desactivado.");return;}YamlConfiguration y=purpur(p);if(y==null)return;Inventory inv=Bukkit.createInventory(null,27,BREEDING);String path="world-settings.default.gameplay-mechanics.animal-breeding-cooldown-seconds";if(y.contains(path))inv.setItem(11,item(Material.WHEAT,"§eCooldown global de reproducción","§7Actual: §a"+y.getLong(path)+" segundos","§eClick para cambiar","§7Requiere reiniciar el servidor","§8"+path));inv.setItem(15,item(Material.COW_SPAWN_EGG,"§eCooldown por especie","§7Se respeta breeding-delay-ticks","§7de cada mob en purpur.yml","§8Edición individual: próxima ampliación"));inv.setItem(22,item(Material.ARROW,"§fVolver"));p.openInventory(inv);
    }
    private void openRaids(Player p){
        if(!modules.raidsEnabled()){p.sendMessage("§cRaids Purpur está desactivado.");return;}YamlConfiguration y=purpur(p);if(y==null)return;Inventory inv=Bukkit.createInventory(null,27,RAIDS);String path="world-settings.default.gameplay-mechanics.raid-cooldown-seconds";if(y.contains(path))inv.setItem(11,item(Material.OMINOUS_BOTTLE,"§eCooldown entre raids","§7Actual: §a"+y.getLong(path)+" segundos","§eClick para cambiar","§7Requiere reiniciar el servidor","§8"+path));String patrol="world-settings.default.gameplay-mechanics.mob-spawning.raid-patrols";if(y.contains(patrol))inv.setItem(15,item(Material.CROSSBOW,"§ePatrullas de raid","§7Valor Purpur: §a"+y.getString(patrol),"§7Se muestra sin modificar por ahora","§8"+patrol));inv.setItem(22,item(Material.ARROW,"§fVolver"));p.openInventory(inv);
    }

    private void askPurpurNumber(Player p,String kind,String path){p.closeInventory();pending.put(p.getUniqueId(),new PendingInput(kind,path,null));p.sendMessage("§6EssLite §8» §fEscribe el nuevo valor numérico para §e"+path+"§f. Escribe §ccancelar§f para salir.");}
    private void togglePurpurPath(Player p,String path,Runnable reopen){File f=purpurFile();if(!f.isFile()){missingPurpur(p);return;}try{YamlConfiguration y=YamlConfiguration.loadConfiguration(f);if(!y.contains(path)||!(y.get(path) instanceof Boolean)){p.sendMessage("§cEsa opción no existe como boolean en tu purpur.yml; no se modificó nada.");return;}boolean wanted=!y.getBoolean(path);writePurpurVerified(f,path,wanted);p.sendMessage("§6EssLite §8» §fGuardado: §e"+path+" §7→ "+(wanted?"§aON":"§cOFF")+" §7(backup creado; reinicia el servidor)");Bukkit.getScheduler().runTaskLater(plugin,reopen,2L);}catch(Exception ex){purpurError(p,ex);}}
    private void toggleMobKey(Player p,String mob,String key){String path="world-settings.default.mobs."+mob+"."+key;togglePurpurPath(p,path,()->openMob(p,mob));}
    private void writePurpurVerified(File f,String path,Object wanted)throws IOException{backup(f);YamlConfiguration y=YamlConfiguration.loadConfiguration(f);y.set(path,wanted);y.save(f);YamlConfiguration verify=YamlConfiguration.loadConfiguration(f);Object saved=verify.get(path);if(saved==null||!String.valueOf(saved).equals(String.valueOf(wanted)))throw new IOException("La verificación posterior al guardado falló para "+path);}
    private void writePurpurVerifiedBatch(File f,Map<String,Object> changes)throws IOException{
        if(changes.isEmpty())return;
        backup(f);YamlConfiguration y=YamlConfiguration.loadConfiguration(f);
        for(var e:changes.entrySet()){if(!y.contains(e.getKey()))throw new IOException("La opción ya no existe: "+e.getKey());y.set(e.getKey(),e.getValue());}
        y.save(f);YamlConfiguration verify=YamlConfiguration.loadConfiguration(f);
        for(var e:changes.entrySet()){Object saved=verify.get(e.getKey());if(saved==null||!String.valueOf(saved).equals(String.valueOf(e.getValue())))throw new IOException("La verificación posterior al guardado falló para "+e.getKey());}
    }
    private void purpurError(Player p,Exception ex){p.sendMessage("§cNo pude modificar purpur.yml: "+ex.getMessage());plugin.getLogger().warning("Purpur edit error: "+ex);}
    private void backup(File f)throws IOException{Path dir=plugin.getDataFolder().toPath().resolve("backups");Files.createDirectories(dir);String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));Files.copy(f.toPath(),dir.resolve("purpur-"+stamp+".yml"),StandardCopyOption.REPLACE_EXISTING);}

    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String title=e.getView().getTitle();if(!title.startsWith("§8EssLite •"))return;e.setCancelled(true);int s=e.getRawSlot();if(s<0)return;
        if(title.equals(MAIN)){if(s==10)openSpawn(p);else if(s==12)openModules(p);else if(s==13&&platform.isPurpur()&&modules.purpurEnabled())openPurpur(p);else if(s==14)cycleWorld(p);return;}
        if(title.equals(MODULES)){if(s==10)toggleModule(p,"modules.server-config.purpur.mounts.enabled",false);else if(s==11)toggleModule(p,"modules.server-config.purpur.mobs.enabled",true);else if(s==12)toggleModule(p,"modules.server-config.purpur.gameplay.enabled",true);else if(s==13)toggleModule(p,"modules.server-config.purpur.breeding.enabled",true);else if(s==14)toggleModule(p,"modules.server-config.purpur.raids.enabled",true);else if(s==22)openMain(p);return;}
        if(title.equals(PURPUR)){if(s==10&&modules.mountsEnabled())openMounts(p,0);else if(s==11&&modules.mobsEnabled())openMobManager(p,0);else if(s==12&&modules.gameplayEnabled())openGameplay(p);else if(s==13&&modules.breedingEnabled())openBreeding(p);else if(s==14&&modules.raidsEnabled())openRaids(p);else if(s==22)openMain(p);return;}
        if(title.equals(SPAWN)){if(s>=9&&s<9+CATS.size()){p.closeInventory();openSpawnDialog(p,CATS.get(s-9));}else if(s==22)openMain(p);return;}
        if(title.startsWith(MOUNTS)){int page=mountPage.getOrDefault(p.getUniqueId(),0);if(s<45){YamlConfiguration y=YamlConfiguration.loadConfiguration(purpurFile());List<String> visible=new ArrayList<>();for(String mob:MOBS)if(y.contains("world-settings.default.mobs."+mob+".ridable"))visible.add(mob);int idx=page*45+s;if(idx<visible.size())openMob(p,visible.get(idx));}else if(s==45)openMounts(p,page-1);else if(s==53)openMounts(p,page+1);else if(s==49)openPurpur(p);return;}
        if(title.startsWith(MOB_MANAGER)){int page=mountPage.getOrDefault(p.getUniqueId(),0);if(s<45){YamlConfiguration y=YamlConfiguration.loadConfiguration(purpurFile());List<String> visible=new ArrayList<>();for(String mob:MOBS)if(y.isConfigurationSection("world-settings.default.mobs."+mob))visible.add(mob);int idx=page*45+s;if(idx<visible.size())openMob(p,visible.get(idx));}else if(s==45)openMobManager(p,page-1);else if(s==53)openMobManager(p,page+1);else if(s==49)openPurpur(p);return;}
        if(title.startsWith(MOB_PREFIX)){String mob=title.substring(MOB_PREFIX.length());if(s==22){if("mobs".equals(mobSource.get(p.getUniqueId())))openMobManager(p,mountPage.getOrDefault(p.getUniqueId(),0));else openMounts(p,mountPage.getOrDefault(p.getUniqueId(),0));return;}ItemStack clicked=e.getCurrentItem();if(clicked==null||!clicked.hasItemMeta()||clicked.getItemMeta().getLore()==null)return;for(String line:clicked.getItemMeta().getLore())if(line.startsWith("§8")){toggleMobKey(p,mob,line.substring(2));return;}return;}
        if(title.equals(GAMEPLAY)){if(s==22){openPurpur(p);return;}ItemStack clicked=e.getCurrentItem();if(clicked!=null&&clicked.hasItemMeta()&&clicked.getItemMeta().getLore()!=null)for(String line:clicked.getItemMeta().getLore())if(line.startsWith("§8world-settings.")){togglePurpurPath(p,line.substring(2),()->openGameplay(p));return;}return;}
        if(title.equals(BREEDING)){if(s==11)askPurpurNumber(p,"breeding","world-settings.default.gameplay-mechanics.animal-breeding-cooldown-seconds");else if(s==22)openPurpur(p);return;}
        if(title.equals(RAIDS)){if(s==11)askPurpurNumber(p,"raids","world-settings.default.gameplay-mechanics.raid-cooldown-seconds");else if(s==22)openPurpur(p);return;}
    }

    @SuppressWarnings("deprecation") @EventHandler public void chat(AsyncPlayerChatEvent e){
        PendingInput in=pending.remove(e.getPlayer().getUniqueId());if(in==null)return;e.setCancelled(true);String msg=e.getMessage().trim();Player p=e.getPlayer();Bukkit.getScheduler().runTask(plugin,()->{
            if(msg.equalsIgnoreCase("cancelar")){p.sendMessage("§7Cambio cancelado.");reopenPending(p,in.kind());return;}
            try{long v=Long.parseLong(msg);if(in.kind().equals("limit")||in.kind().equals("ticks")){if(v < -1 || v > 1000000)throw new NumberFormatException();SpawnCategory cat=SpawnCategory.valueOf(in.key());if(in.kind().equals("limit")){if(v>10000)throw new NumberFormatException();in.world().setSpawnLimit(cat,(int)v);}else in.world().setTicksPerSpawns(cat,(int)v);p.sendMessage("§6EssLite §8» §aCambio aplicado §7("+pretty(cat.name())+": "+v+")");openSpawn(p);return;}
                if(v<0||v>86400)throw new NumberFormatException();File f=purpurFile();writePurpurVerified(f,in.key(),v);p.sendMessage("§6EssLite §8» §aValor Purpur guardado: §f"+v+" §7(backup creado; reinicia el servidor)");reopenPending(p,in.kind());
            }catch(Exception ex){p.sendMessage("§cValor inválido o no se pudo guardar. Para estos controles usa 0..86400.");reopenPending(p,in.kind());}
        });
    }
    private void reopenPending(Player p,String kind){if(kind.equals("breeding"))openBreeding(p);else if(kind.equals("raids"))openRaids(p);else openSpawn(p);}

    private ItemStack item(Material m,String name,String... lore){ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));it.setItemMeta(meta);return it;}
    private String pretty(String s){String[] a=s.toLowerCase(Locale.ROOT).split("_");StringBuilder b=new StringBuilder();for(String x:a){if(x.isEmpty())continue;if(!b.isEmpty())b.append(' ');b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));}return b.toString();}
}
