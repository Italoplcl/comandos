package dev.esslite;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
    private final Map<UUID, World> selectedWorld = new HashMap<>();
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();
    private static final String MAIN = "§8EssLite • Server Config";
    private static final String SPAWN = "§8EssLite • Spawning";
    private static final String MOUNTS = "§8EssLite • Monturas Purpur";
    private static final String MOB_PREFIX = "§8EssLite • Mob: ";
    private static final List<SpawnCategory> CATS = List.of(SpawnCategory.MONSTER, SpawnCategory.ANIMAL, SpawnCategory.AMBIENT,
            SpawnCategory.WATER_ANIMAL, SpawnCategory.WATER_AMBIENT, SpawnCategory.WATER_UNDERGROUND_CREATURE, SpawnCategory.AXOLOTL);
    private static final List<String> MOBS = List.of("allay","bat","bee","blaze","bogged","cat","cave_spider","chicken","cod","cow","creeper","dolphin","donkey","drowned","elder_guardian","enderman","endermite","evoker","fox","frog","ghast","giant","glow_squid","goat","guardian","hoglin","horse","husk","illusioner","iron_golem","llama","magma_cube","mooshroom","ocelot","panda","parrot","phantom","pig","piglin","piglin_brute","pillager","polar_bear","pufferfish","rabbit","ravager","salmon","sheep","shulker","silverfish","skeleton","skeleton_horse","slime","snow_golem","spider","squid","stray","strider","tadpole","trader_llama","tropical_fish","turtle","vex","villager","vindicator","wandering_trader","warden","witch","wither","wither_skeleton","wolf","zoglin","zombie","zombie_horse","zombie_villager","zombified_piglin");
    private final Map<UUID,Integer> mountPage = new HashMap<>();

    record PendingInput(String kind, String key, World world) {}

    public ServerConfigMenu(EssLite plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Este comando debe usarse dentro del juego."); return true; }
        if (!p.hasPermission("esslite.serverconfig")) { p.sendMessage(Component.text("No tienes permiso.")); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) { openSpawn(p); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("mounts")) { openMounts(p, 0); return true; }
        openMain(p); return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return args.length == 1 ? List.of("spawn", "mounts") : List.of();
    }

    private World world(Player p) { return selectedWorld.computeIfAbsent(p.getUniqueId(), u -> p.getWorld()); }

    private void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN);
        inv.setItem(10, item(Material.ZOMBIE_HEAD, "§aSpawning", "§7Límites y frecuencia por mundo", "§eClick para abrir"));
        inv.setItem(12, item(Material.SADDLE, "§6Monturas Purpur", "§7Ridable, controlable y agua", "§eClick para abrir"));
        inv.setItem(14, item(Material.GRASS_BLOCK, "§bMundo: §f" + world(p).getName(), "§7Click para cambiar de mundo"));
        inv.setItem(16, item(Material.REDSTONE_TORCH, "§cCompatibilidad", "§7Paper/Purpur 26.3", "§7Los cambios de spawn usan API", "§7Purpur se respalda antes de editar"));
        p.openInventory(inv);
    }

    private void cycleWorld(Player p) {
        List<World> worlds = Bukkit.getWorlds(); int i = worlds.indexOf(world(p));
        selectedWorld.put(p.getUniqueId(), worlds.get((i + 1) % worlds.size())); openMain(p);
    }

    private void openSpawn(Player p) {
        World w = world(p); Inventory inv = Bukkit.createInventory(null, 27, SPAWN);
        for (int i=0;i<CATS.size();i++) {
            SpawnCategory cat=CATS.get(i); int limit=w.getSpawnLimit(cat); long ticks=w.getTicksPerSpawns(cat);
            inv.setItem(9+i, item(icon(cat), "§e"+pretty(cat.name()), "§fLímite: §a"+limit, "§fTicks/intento: §b"+ticks,
                    "", "§eClick izquierdo: cambiar límite", "§6Click derecho: cambiar ticks"));
        }
        inv.setItem(22, item(Material.ARROW, "§fVolver"));
        inv.setItem(26, item(Material.COMPASS, "§b"+w.getName(), "§7Se configura por mundo"));
        p.openInventory(inv);
    }

    private Material icon(SpawnCategory c) { return switch(c) {
        case MONSTER -> Material.ZOMBIE_HEAD; case ANIMAL -> Material.COW_SPAWN_EGG; case AMBIENT -> Material.BAT_SPAWN_EGG;
        case WATER_ANIMAL -> Material.SQUID_SPAWN_EGG; case WATER_AMBIENT -> Material.COD_SPAWN_EGG;
        case WATER_UNDERGROUND_CREATURE -> Material.GLOW_SQUID_SPAWN_EGG; case AXOLOTL -> Material.AXOLOTL_SPAWN_EGG;
        default -> Material.SPAWNER; };
    }

    private void askSpawn(Player p, SpawnCategory cat, boolean ticks) {
        p.closeInventory(); pending.put(p.getUniqueId(), new PendingInput(ticks?"ticks":"limit", cat.name(), world(p)));
        p.sendMessage("§6EssLite §8» §fEscribe el nuevo " + (ticks?"intervalo en ticks":"límite") + " para §e"+pretty(cat.name())+"§f. Escribe §ccancelar§f para salir.");
    }

    private boolean isPurpur() { return Bukkit.getName().toLowerCase(Locale.ROOT).contains("purpur") || Bukkit.getVersion().toLowerCase(Locale.ROOT).contains("purpur"); }
    private File purpurFile() { return new File("purpur.yml"); }

    private void openMounts(Player p, int page) {
        if (!isPurpur()) { p.sendMessage("§cEsta sección requiere Purpur."); return; }
        int per=45, max=Math.max(0,(MOBS.size()-1)/per); page=Math.max(0,Math.min(max,page)); mountPage.put(p.getUniqueId(),page);
        Inventory inv=Bukkit.createInventory(null,54,MOUNTS+" §7"+(page+1)+"/"+(max+1));
        YamlConfiguration y=YamlConfiguration.loadConfiguration(purpurFile()); String wn=world(p).getName();
        for(int i=0;i<per;i++){int idx=page*per+i;if(idx>=MOBS.size())break;String mob=MOBS.get(idx);String base="world-settings."+wn+".mobs."+mob;
            boolean rid=y.getBoolean(base+".ridable", y.getBoolean("world-settings.default.mobs."+mob+".ridable",false));
            inv.setItem(i,item(egg(mob),"§f"+pretty(mob),"§7Montable: "+(rid?"§aSí":"§cNo"),"§eClick para configurar"));}
        if(page>0)inv.setItem(45,item(Material.ARROW,"§fPágina anterior")); if(page<max)inv.setItem(53,item(Material.ARROW,"§fPágina siguiente"));
        inv.setItem(49,item(Material.BARRIER,"§fVolver")); inv.setItem(50,item(Material.COMPASS,"§b"+world(p).getName(),"§7Opciones Purpur por mundo")); p.openInventory(inv);
    }

    private Material egg(String mob){ Material m=Material.matchMaterial(mob.toUpperCase(Locale.ROOT)+"_SPAWN_EGG"); return m==null?Material.SPAWNER:m; }

    private void openMob(Player p,String mob){
        YamlConfiguration y=YamlConfiguration.loadConfiguration(purpurFile()); String base="world-settings."+world(p).getName()+".mobs."+mob;
        Inventory inv=Bukkit.createInventory(null,27,MOB_PREFIX+mob);
        inv.setItem(10,toggleItem(Material.SADDLE,"Montable",readBool(y,base,mob,"ridable",false),"ridable"));
        inv.setItem(12,toggleItem(Material.REPEATER,"Controlable WASD",readBool(y,base,mob,"controllable",true),"controllable"));
        inv.setItem(14,toggleItem(Material.WATER_BUCKET,"Montable en agua",readBool(y,base,mob,"ridable-in-water",true),"ridable-in-water"));
        inv.setItem(16,toggleItem(Material.EXPERIENCE_BOTTLE,"Siempre entrega XP",readBool(y,base,mob,"always-drop-exp",false),"always-drop-exp"));
        inv.setItem(22,item(Material.ARROW,"§fVolver")); p.openInventory(inv);
    }
    private boolean readBool(YamlConfiguration y,String base,String mob,String key,boolean def){String path=base+"."+key;if(y.contains(path))return y.getBoolean(path);String d="world-settings.default.mobs."+mob+"."+key;return y.contains(d)?y.getBoolean(d):def;}
    private ItemStack toggleItem(Material mat,String name,boolean value,String key){return item(mat,"§e"+name,"§7Actual: "+(value?"§aACTIVADO":"§cDESACTIVADO"),"§8"+key,"§eClick para alternar","§7Se ejecutará /purpur reload");}

    private void togglePurpur(Player p,String mob,String key){
        File f=purpurFile(); if(!f.isFile()){p.sendMessage("§cNo encontré purpur.yml en la raíz del servidor.");return;}
        try{ backup(f); YamlConfiguration y=YamlConfiguration.loadConfiguration(f);String base="world-settings."+world(p).getName()+".mobs."+mob;boolean old=readBool(y,base,mob,key,key.equals("controllable")||key.equals("ridable-in-water"));y.set(base+"."+key,!old);y.save(f);Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"purpur reload");p.sendMessage("§6EssLite §8» §f"+pretty(mob)+" §e"+key+"§f: "+(old?"§cOFF":"§aON")+" §7(backup creado)");Bukkit.getScheduler().runTaskLater(plugin,()->openMob(p,mob),2L);
        }catch(Exception ex){p.sendMessage("§cNo pude modificar purpur.yml: "+ex.getMessage());plugin.getLogger().warning("Purpur edit error: "+ex);}
    }
    private void backup(File f)throws IOException{Path dir=plugin.getDataFolder().toPath().resolve("backups");Files.createDirectories(dir);String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));Files.copy(f.toPath(),dir.resolve("purpur-"+stamp+".yml"),StandardCopyOption.REPLACE_EXISTING);}

    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String title=e.getView().getTitle();if(!title.startsWith("§8EssLite •"))return;e.setCancelled(true);int s=e.getRawSlot();if(s<0)return;
        if(title.equals(MAIN)){if(s==10)openSpawn(p);else if(s==12)openMounts(p,0);else if(s==14)cycleWorld(p);return;}
        if(title.equals(SPAWN)){if(s>=9&&s<9+CATS.size())askSpawn(p,CATS.get(s-9),e.isRightClick());else if(s==22)openMain(p);return;}
        if(title.startsWith(MOUNTS)){int page=mountPage.getOrDefault(p.getUniqueId(),0);if(s<45){int idx=page*45+s;if(idx<MOBS.size())openMob(p,MOBS.get(idx));}else if(s==45)openMounts(p,page-1);else if(s==53)openMounts(p,page+1);else if(s==49)openMain(p);return;}
        if(title.startsWith(MOB_PREFIX)){String mob=title.substring(MOB_PREFIX.length());if(s==22){openMounts(p,mountPage.getOrDefault(p.getUniqueId(),0));return;}String key=s==10?"ridable":s==12?"controllable":s==14?"ridable-in-water":s==16?"always-drop-exp":null;if(key!=null)togglePurpur(p,mob,key);}
    }

    @SuppressWarnings("deprecation") @EventHandler public void chat(AsyncPlayerChatEvent e){PendingInput in=pending.remove(e.getPlayer().getUniqueId());if(in==null)return;e.setCancelled(true);String msg=e.getMessage().trim();Player p=e.getPlayer();Bukkit.getScheduler().runTask(plugin,()->{if(msg.equalsIgnoreCase("cancelar")){p.sendMessage("§7Cambio cancelado.");openSpawn(p);return;}try{long v=Long.parseLong(msg);if(v < -1 || v > 1000000)throw new NumberFormatException();SpawnCategory cat=SpawnCategory.valueOf(in.key());if(in.kind().equals("limit")){if(v>10000)throw new NumberFormatException();in.world().setSpawnLimit(cat,(int)v);}else in.world().setTicksPerSpawns(cat,(int)v);p.sendMessage("§6EssLite §8» §aCambio aplicado §7("+pretty(cat.name())+": "+v+")");openSpawn(p);}catch(Exception ex){p.sendMessage("§cValor inválido. Usa un número entre -1 y "+(in.kind().equals("limit")?"10000":"1000000")+".");openSpawn(p);}});}

    private ItemStack item(Material m,String name,String... lore){ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));it.setItemMeta(meta);return it;}
    private String pretty(String s){String[] a=s.toLowerCase(Locale.ROOT).split("_");StringBuilder b=new StringBuilder();for(String x:a){if(!b.isEmpty())b.append(' ');b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));}return b.toString();}
}
