package dev.mando;

import net.kyori.adventure.text.Component;
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
    private final Mando plugin;
    private final ModuleManager modules;
    private final PlatformDetector platform;
    private final SetupManager setup;
    private final IntegrationManager integrations;
    private final Map<UUID, World> selectedWorld = new HashMap<>();
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();
    private final Map<UUID,Integer> mountPage = new HashMap<>();
    private final Map<UUID,String> mobSource = new HashMap<>();

    private static final String MAIN = "§8Mando • Server Config";
    private static final String SPAWN = "§8Mando • Spawning";
    private static final String MODULES = "§8Mando • Modules";
    private static final String PURPUR = "§8Mando • Purpur";
    private static final String GAMEPLAY = "§8Mando • Purpur Gameplay";
    private static final String BREEDING = "§8Mando • Purpur Breeding";
    private static final String RAIDS = "§8Mando • Purpur Raids";
    private static final String MOUNTS = "§8Mando • Monturas Purpur";
    private static final String MOB_MANAGER = "§8Mando • Mob Manager";
    private static final String MOB_PREFIX = "§8Mando • Mob: ";

    private static final List<SpawnCategory> CATS = List.of(SpawnCategory.MONSTER, SpawnCategory.ANIMAL, SpawnCategory.AMBIENT,
            SpawnCategory.WATER_ANIMAL, SpawnCategory.WATER_AMBIENT, SpawnCategory.WATER_UNDERGROUND_CREATURE, SpawnCategory.AXOLOTL);
    private static final List<String> MOBS = List.of("allay","armadillo","axolotl","bat","bee","blaze","bogged","breeze","camel","cat","cave_spider","chicken","cod","cow","creeper","dolphin","donkey","drowned","elder_guardian","ender_dragon","enderman","endermite","evoker","fox","frog","ghast","giant","glow_squid","goat","guardian","hoglin","horse","husk","illusioner","iron_golem","llama","magma_cube","mooshroom","mule","ocelot","panda","parrot","phantom","pig","piglin","piglin_brute","pillager","polar_bear","pufferfish","rabbit","ravager","salmon","sheep","shulker","silverfish","skeleton","skeleton_horse","slime","snow_golem","spider","squid","stray","strider","tadpole","trader_llama","tropical_fish","turtle","vex","villager","vindicator","wandering_trader","warden","witch","wither","wither_skeleton","wolf","zoglin","zombie","zombie_horse","zombie_villager","zombified_piglin");

    record PendingInput(String kind, String key, World world) {}

    public ServerConfigMenu(Mando plugin, ModuleManager modules, PlatformDetector platform, SetupManager setup, IntegrationManager integrations) {
        this.plugin = plugin; this.modules = modules; this.platform = platform; this.setup=setup; this.integrations=integrations;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Este comando debe usarse dentro del juego."); return true; }
        if (!p.hasPermission("mando.serverconfig")) { p.sendMessage(Component.text("No tienes permiso.")); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("setup")) { if(args.length>1&&args[1].equalsIgnoreCase("defaults")){setup.defaults();setup.complete(true);sender.sendMessage("Mando: configuración base aplicada.");}else setup.show(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("commands")) { openCommands(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("integrations")) { p.performCommand("mando integrations"); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) { openSpawn(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("modules")) { openModules(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("purpur")) { openPurpur(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("mounts")) { openMountsChecked(p,0); return true; }
        openMain(p); return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return args.length == 1 ? List.of("setup","commands","integrations","spawn", "modules", "purpur", "mounts") : List.of();
    }

    private World world(Player p) { return selectedWorld.computeIfAbsent(p.getUniqueId(), u -> p.getWorld()); }

    private void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN);
        inv.setItem(4,item(Material.COMPARATOR,"§bSetup","§7Idioma, módulos, teleport, AFK e integraciones","§eClick para abrir"));
        inv.setItem(10, item(Material.ZOMBIE_HEAD, "§aSpawning", "§7Límites y frecuencia por mundo", "§eClick para abrir"));
        inv.setItem(12, item(Material.REPEATER, "§dModules", "§7Activa/desactiva funciones de Mando", "§eClick para abrir"));
        if (platform.isPurpur() && modules.purpurEnabled())
            inv.setItem(13, item(Material.AMETHYST_SHARD, "§5Purpur", "§aDetectado", "§7Configura funciones exclusivas", "§eClick para abrir"));
        else inv.setItem(13, item(Material.GRAY_DYE, "§7Purpur", platform.isPurpur()?"§cMódulo desactivado":"§cNo detectado"));
        inv.setItem(14, item(Material.GRASS_BLOCK, "§bMundo: §f" + world(p).getName(), "§7Click para cambiar de mundo"));
        inv.setItem(15,item(Material.COMMAND_BLOCK,"§eComandos","§7Estado real de módulos y comandos","§eClick para abrir"));
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
    private ItemStack moduleItem(Material m,String name,String path,boolean def){boolean on=plugin.getConfig().getBoolean(path,def);return item(m,"§e"+name,"§7Estado: "+(on?"§aACTIVADO":"§cDESACTIVADO"),"§eClick para alternar","§8"+path);}
    private void toggleModule(Player p,String path,boolean def){boolean now=plugin.getConfig().getBoolean(path,def);plugin.getConfig().set(path,!now);plugin.saveConfig();p.sendMessage("§6Mando §8» §fMódulo "+(now?"§cdesactivado":"§aactivado")+"§f: §7"+path);openModules(p);}

    private void cycleWorld(Player p) { List<World> worlds=Bukkit.getWorlds(); int i=worlds.indexOf(world(p)); selectedWorld.put(p.getUniqueId(),worlds.get((i+1)%worlds.size())); openMain(p); }

    private void openSpawn(Player p) {
        World w=world(p); Inventory inv=Bukkit.createInventory(null,27,SPAWN);
        for(int i=0;i<CATS.size();i++){SpawnCategory cat=CATS.get(i);int limit=w.getSpawnLimit(cat);long ticks=w.getTicksPerSpawns(cat);inv.setItem(9+i,item(icon(cat),"§e"+pretty(cat.name()),"§fLímite: §a"+limit,"§fTicks/intento: §b"+ticks,"","§eClick izquierdo: cambiar límite","§6Click derecho: cambiar ticks"));}
        inv.setItem(22,item(Material.ARROW,"§fVolver"));inv.setItem(26,item(Material.COMPASS,"§b"+w.getName(),"§7Se configura por mundo"));p.openInventory(inv);
    }
    private Material icon(SpawnCategory c){return switch(c){case MONSTER->Material.ZOMBIE_HEAD;case ANIMAL->Material.COW_SPAWN_EGG;case AMBIENT->Material.BAT_SPAWN_EGG;case WATER_ANIMAL->Material.SQUID_SPAWN_EGG;case WATER_AMBIENT->Material.COD_SPAWN_EGG;case WATER_UNDERGROUND_CREATURE->Material.GLOW_SQUID_SPAWN_EGG;case AXOLOTL->Material.AXOLOTL_SPAWN_EGG;default->Material.SPAWNER;};}
    private void askSpawn(Player p,SpawnCategory cat,boolean ticks){p.closeInventory();pending.put(p.getUniqueId(),new PendingInput(ticks?"ticks":"limit",cat.name(),world(p)));p.sendMessage("§6Mando §8» §fEscribe el nuevo "+(ticks?"intervalo en ticks":"límite")+" para §e"+pretty(cat.name())+"§f. Escribe §ccancelar§f para salir.");}

    private File purpurFile(){return new File("purpur.yml");}
    private YamlConfiguration purpur(Player p){File f=purpurFile();if(!f.isFile()){p.sendMessage("§cNo encontré purpur.yml en la raíz del servidor.");return null;}return YamlConfiguration.loadConfiguration(f);}
    private boolean purpurReady(Player p){if(!platform.isPurpur()){p.sendMessage("§cEsta sección requiere Purpur.");return false;}if(!modules.purpurEnabled()){p.sendMessage("§cEl módulo Purpur de Mando está desactivado.");return false;}return purpurFile().isFile() || missingPurpur(p);}
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
    private ItemStack purpurModuleItem(Material m,String name,boolean on){return item(m,(on?"§a":"§7")+name,"§7Módulo: "+(on?"§aACTIVADO":"§cDESACTIVADO"),on?"§eClick para abrir":"§8Actívalo en /serverconfig → Modules");}

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
        if(page>0)inv.setItem(45,item(Material.ARROW,"§fPágina anterior"));if(page<max)inv.setItem(53,item(Material.ARROW,"§fPágina siguiente"));inv.setItem(49,item(Material.BARRIER,"§fVolver"));inv.setItem(50,item(Material.COMPASS,"§bConfiguración global","§7Sólo opciones existentes en purpur.yml"));p.openInventory(inv);
    }
    private Material egg(String mob){Material m=Material.matchMaterial(mob.toUpperCase(Locale.ROOT)+"_SPAWN_EGG");return m==null?Material.SPAWNER:m;}
    private void openMob(Player p,String mob){
        YamlConfiguration y=purpur(p);if(y==null)return;String base="world-settings.default.mobs."+mob;Inventory inv=Bukkit.createInventory(null,27,MOB_PREFIX+mob);int slot=10;
        slot=addMobToggle(inv,y,base,slot,Material.SADDLE,"Montable","ridable");
        slot=addMobToggle(inv,y,base,slot,Material.REPEATER,"Controlable WASD","controllable");
        slot=addMobToggle(inv,y,base,slot,Material.WATER_BUCKET,"Montable en agua","ridable-in-water");
        slot=addMobToggle(inv,y,base,slot,Material.EXPERIENCE_BOTTLE,"Siempre entrega XP","always-drop-exp");
        inv.setItem(22,item(Material.ARROW,"§fVolver"));p.openInventory(inv);
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

    private void askPurpurNumber(Player p,String kind,String path){p.closeInventory();pending.put(p.getUniqueId(),new PendingInput(kind,path,null));p.sendMessage("§6Mando §8» §fEscribe el nuevo valor numérico para §e"+path+"§f. Escribe §ccancelar§f para salir.");}
    private void togglePurpurPath(Player p,String path,Runnable reopen){File f=purpurFile();if(!f.isFile()){missingPurpur(p);return;}try{YamlConfiguration y=YamlConfiguration.loadConfiguration(f);if(!y.contains(path)||!(y.get(path) instanceof Boolean)){p.sendMessage("§cEsa opción no existe como boolean en tu purpur.yml; no se modificó nada.");return;}boolean wanted=!y.getBoolean(path);writePurpurVerified(f,path,wanted);p.sendMessage("§6Mando §8» §fGuardado: §e"+path+" §7→ "+(wanted?"§aON":"§cOFF")+" §7(backup creado; reinicia el servidor)");Bukkit.getScheduler().runTaskLater(plugin,reopen,2L);}catch(Exception ex){purpurError(p,ex);}}
    private void toggleMobKey(Player p,String mob,String key){String path="world-settings.default.mobs."+mob+"."+key;togglePurpurPath(p,path,()->openMob(p,mob));}
    private void writePurpurVerified(File f,String path,Object wanted)throws IOException{backup(f);YamlConfiguration y=YamlConfiguration.loadConfiguration(f);y.set(path,wanted);y.save(f);YamlConfiguration verify=YamlConfiguration.loadConfiguration(f);Object saved=verify.get(path);if(saved==null||!String.valueOf(saved).equals(String.valueOf(wanted)))throw new IOException("La verificación posterior al guardado falló para "+path);}
    private void purpurError(Player p,Exception ex){p.sendMessage("§cNo pude modificar purpur.yml: "+ex.getMessage());plugin.getLogger().warning("Purpur edit error: "+ex);}
    private void backup(File f)throws IOException{Path dir=plugin.getDataFolder().toPath().resolve("backups");Files.createDirectories(dir);String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));Files.copy(f.toPath(),dir.resolve("purpur-"+stamp+".yml"),StandardCopyOption.REPLACE_EXISTING);}

    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String title=e.getView().getTitle();if(!title.startsWith("§8Mando •"))return;e.setCancelled(true);int s=e.getRawSlot();if(s<0)return;
        if(title.equals(MAIN)){if(s==4)setup.show(p);else if(s==15)openCommands(p);else if(s==10)openSpawn(p);else if(s==12)openModules(p);else if(s==13&&platform.isPurpur()&&modules.purpurEnabled())openPurpur(p);else if(s==14)cycleWorld(p);return;}
        if(title.equals("§8Mando • Comandos")){if(s==49)openMain(p);return;}
        if(title.equals(MODULES)){if(s==10)toggleModule(p,"modules.server-config.purpur.mounts.enabled",false);else if(s==11)toggleModule(p,"modules.server-config.purpur.mobs.enabled",true);else if(s==12)toggleModule(p,"modules.server-config.purpur.gameplay.enabled",true);else if(s==13)toggleModule(p,"modules.server-config.purpur.breeding.enabled",true);else if(s==14)toggleModule(p,"modules.server-config.purpur.raids.enabled",true);else if(s==22)openMain(p);return;}
        if(title.equals(PURPUR)){if(s==10&&modules.mountsEnabled())openMounts(p,0);else if(s==11&&modules.mobsEnabled())openMobManager(p,0);else if(s==12&&modules.gameplayEnabled())openGameplay(p);else if(s==13&&modules.breedingEnabled())openBreeding(p);else if(s==14&&modules.raidsEnabled())openRaids(p);else if(s==22)openMain(p);return;}
        if(title.equals(SPAWN)){if(s>=9&&s<9+CATS.size())askSpawn(p,CATS.get(s-9),e.isRightClick());else if(s==22)openMain(p);return;}
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
            try{long v=Long.parseLong(msg);if(in.kind().equals("limit")||in.kind().equals("ticks")){if(v < -1 || v > 1000000)throw new NumberFormatException();SpawnCategory cat=SpawnCategory.valueOf(in.key());if(in.kind().equals("limit")){if(v>10000)throw new NumberFormatException();in.world().setSpawnLimit(cat,(int)v);}else in.world().setTicksPerSpawns(cat,(int)v);p.sendMessage("§6Mando §8» §aCambio aplicado §7("+pretty(cat.name())+": "+v+")");openSpawn(p);return;}
                if(v<0||v>86400)throw new NumberFormatException();File f=purpurFile();writePurpurVerified(f,in.key(),v);p.sendMessage("§6Mando §8» §aValor Purpur guardado: §f"+v+" §7(backup creado; reinicia el servidor)");reopenPending(p,in.kind());
            }catch(Exception ex){p.sendMessage("§cValor inválido o no se pudo guardar. Para estos controles usa 0..86400.");reopenPending(p,in.kind());}
        });
    }
    private void reopenPending(Player p,String kind){if(kind.equals("breeding"))openBreeding(p);else if(kind.equals("raids"))openRaids(p);else openSpawn(p);}


    private void openCommands(Player p){
        Inventory inv=Bukkit.createInventory(null,54,"§8Mando • Comandos");
        String[][] groups={{"Homes","homes","/sethome /home /homes /delhome /edithome /restorehome"},{"Warps","warps","/warp /warps /setwarp /delwarp"},{"Spawn","spawn","/spawn /setspawn"},{"Profile","profile","/profile"},{"Mail","mail","/mail"},{"TPA","tpa","/tpa /tpaccept /tpdeny /tpatoggle"},{"Changelog","changelog","/changelog"},{"Server Config","server-config","/serverconfig"}};
        int slot=10;for(String[] g:groups){boolean on=modules.enabled(g[1]);inv.setItem(slot++,item(on?Material.LIME_DYE:Material.GRAY_DYE,"§f"+g[0],"§7Módulo: "+(on?"§aACTIVO":"§cINACTIVO"),"§7"+g[2]));if(slot==17)slot=19;}
        inv.setItem(40,item(Material.ENDER_PEARL,"§bTeleport","§7Warmup: §f"+plugin.getConfig().getInt("teleport.default-warmup-seconds")+"s","§7Cooldown: §f"+plugin.getConfig().getInt("teleport.default-cooldown-seconds")+"s","§7Daño cancela: §f"+plugin.getConfig().getBoolean("teleport.cancel-on-damage")));
        inv.setItem(41,item(Material.CLOCK,"§bAFK","§7Proveedor: §f"+integrations.afkProvider()));
        inv.setItem(42,item(Material.EMERALD,"§bVault","§7Sólo lectura: §aSí","§7Disponible: §f"+integrations.has("Vault")));
        inv.setItem(49,item(Material.ARROW,"§fVolver"));p.openInventory(inv);
    }

    private ItemStack item(Material m,String name,String... lore){ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));it.setItemMeta(meta);return it;}
    private String pretty(String s){String[] a=s.toLowerCase(Locale.ROOT).split("_");StringBuilder b=new StringBuilder();for(String x:a){if(x.isEmpty())continue;if(!b.isEmpty())b.append(' ');b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));}return b.toString();}
}
