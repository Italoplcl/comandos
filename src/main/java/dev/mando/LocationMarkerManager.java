package dev.mando;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.UUID;
import java.io.*;
import org.bukkit.configuration.file.YamlConfiguration;

/** Marcadores cosméticos. Nunca son la fuente de verdad de homes/warps/spawn. */
public final class LocationMarkerManager implements Listener {
    private final MandoPlugin plugin;
    private final NamespacedKey markerKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey nameKey;
    private final NamespacedKey idKey;
    private final File registryFile;
    private final YamlConfiguration registry;

    public LocationMarkerManager(MandoPlugin plugin) {
        this.plugin = plugin;
        markerKey = new NamespacedKey(plugin, "location_marker");
        typeKey = new NamespacedKey(plugin, "marker_type");
        nameKey = new NamespacedKey(plugin, "marker_name");
        idKey = new NamespacedKey(plugin, "marker_id");
        registryFile=new File(plugin.getDataFolder(),"markers.yml");
        registry=YamlConfiguration.loadConfiguration(registryFile);
        if (plugin.getConfig().getBoolean("markers.spawn.particles.enabled", true)) {
            long interval = Math.max(10L, plugin.getConfig().getLong("markers.spawn.particles.interval-ticks", 20L));
            Bukkit.getScheduler().runTaskTimer(plugin, this::spawnParticles, interval, interval);
        }
    }

    public boolean removeHomeMarker(Player p, String homeName) {
        String key=p.getUniqueId()+":"+homeName,path="home."+key;String entityId=registry.getString(path+".entity");if(entityId!=null){try{org.bukkit.entity.Entity e=Bukkit.getEntity(UUID.fromString(entityId));if(e!=null&&e.getPersistentDataContainer().has(markerKey,PersistentDataType.BYTE))e.remove();}catch(Exception ignored){}registry.set(path,null);saveRegistry();return true;}String worldName=registry.getString(path+".world");
        if(worldName==null)return true;World w=Bukkit.getWorld(worldName);if(w==null)return false;
        int x=registry.getInt(path+".x"),y=registry.getInt(path+".y"),z=registry.getInt(path+".z");
        if(!w.isChunkLoaded(x>>4,z>>4))return false;
        Block b=w.getBlockAt(x,y,z);if(b.getState() instanceof TileState state){PersistentDataContainer pdc=state.getPersistentDataContainer();String type=pdc.get(typeKey,PersistentDataType.STRING),name=pdc.get(nameKey,PersistentDataType.STRING);String markerId=pdc.get(idKey,PersistentDataType.STRING),expected=registry.getString(path+".marker-id");if(pdc.has(markerKey,PersistentDataType.BYTE)&&"home".equals(type)&&key.equals(name)&&(expected==null||expected.equals(markerId)))b.setType(Material.AIR,false);}
        registry.set(path,null);saveRegistry();return true;
    }
    public boolean renameHomeMarker(Player p,String oldName,String newName,String homeId){String oldKey=p.getUniqueId()+":"+oldName,newKey=p.getUniqueId()+":"+newName,oldPath="home."+oldKey,newPath="home."+newKey;if(!registry.contains(oldPath))return true;String entity=registry.getString(oldPath+".entity");if(entity!=null){try{org.bukkit.entity.Entity e=Bukkit.getEntity(UUID.fromString(entity));if(e instanceof TextDisplay td){td.text(net.kyori.adventure.text.Component.text(newName+" · "+p.getName()));td.getPersistentDataContainer().set(nameKey,PersistentDataType.STRING,newKey);}}catch(Exception ignored){}}else{String wn=registry.getString(oldPath+".world");World w=Bukkit.getWorld(wn);int x=registry.getInt(oldPath+".x"),y=registry.getInt(oldPath+".y"),z=registry.getInt(oldPath+".z");if(w==null||!w.isChunkLoaded(x>>4,z>>4))return false;Block b=w.getBlockAt(x,y,z);if(b.getState() instanceof Sign sign){for(Side face:Side.values())sign.getSide(face).line(1,net.kyori.adventure.text.Component.text(newName));sign.getPersistentDataContainer().set(nameKey,PersistentDataType.STRING,newKey);sign.update(true,false);}}Object raw=registry.get(oldPath);registry.set(newPath,raw);registry.set(oldPath,null);registry.set(newPath+".marker-id",homeId);saveRegistry();return true;}
    public boolean createHomeSign(Player p,String homeName,String homeId){
        if(!plugin.getConfig().getBoolean("markers.homes.enabled",true))return true;if(!removeHomeMarker(p,homeName))return false;Block origin=p.getLocation().getBlock();String key=p.getUniqueId()+":"+homeName;
        for(int r=1;r<=2;r++)for(int dy=0;dy<=2;dy++)for(BlockFace wall:new BlockFace[]{BlockFace.NORTH,BlockFace.EAST,BlockFace.SOUTH,BlockFace.WEST}){Block solid=origin.getRelative(wall,r).getRelative(BlockFace.UP,dy);if(!solid.getType().isSolid())continue;Block place=solid.getRelative(wall.getOppositeFace());if(!place.isEmpty())continue;BlockData candidate=Material.OAK_WALL_SIGN.createBlockData();if(candidate instanceof Directional d)d.setFacing(wall.getOppositeFace());if(!candidate.isSupported(place))continue;place.setBlockData(candidate,false);if(!(place.getState() instanceof Sign sign)){place.setType(Material.AIR,false);continue;}String deco=plugin.getConfig().getString("markers.homes.decoration","--------");for(Side signFace:Side.values()){SignSide side=sign.getSide(signFace);side.line(0,net.kyori.adventure.text.Component.text(deco));side.line(1,net.kyori.adventure.text.Component.text(homeName));side.line(2,net.kyori.adventure.text.Component.text(p.getName()));side.line(3,net.kyori.adventure.text.Component.text(deco));side.setGlowingText(plugin.getConfig().getBoolean("markers.homes.glowing",true));}mark(sign,"home",key,homeId);sign.update(true,false);record("home",key,place,homeId);return true;}String mode=plugin.getConfig().getString("markers.homes.mode","SIGN_FALLBACK_HOLOGRAM");if(mode.equalsIgnoreCase("SIGN"))return false;if(mode.equalsIgnoreCase("NONE"))return true;Location at=p.getLocation().clone().add(0,2.2,0);TextDisplay td=p.getWorld().spawn(at,TextDisplay.class,t->{t.text(net.kyori.adventure.text.Component.text(homeName+" · "+p.getName()));t.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);t.setPersistent(true);t.getPersistentDataContainer().set(markerKey,PersistentDataType.BYTE,(byte)1);t.getPersistentDataContainer().set(typeKey,PersistentDataType.STRING,"home");t.getPersistentDataContainer().set(nameKey,PersistentDataType.STRING,key);t.getPersistentDataContainer().set(idKey,PersistentDataType.STRING,homeId);});String path="home."+key;registry.set(path+".marker-id",homeId);registry.set(path+".entity",td.getUniqueId().toString());registry.set(path+".world",p.getWorld().getName());registry.set(path+".x",td.getLocation().getBlockX());registry.set(path+".y",td.getLocation().getBlockY());registry.set(path+".z",td.getLocation().getBlockZ());saveRegistry();return true;
    }

    public boolean removeWarpMarker(String warpName){return removeRegistered("warp",warpName);}
    private boolean removeRegistered(String type,String name){String path=type+"."+name,wname=registry.getString(path+".world");if(wname==null)return true;World w=Bukkit.getWorld(wname);if(w==null)return false;int x=registry.getInt(path+".x"),y=registry.getInt(path+".y"),z=registry.getInt(path+".z");if(!w.isChunkLoaded(x>>4,z>>4))return false;Block b=w.getBlockAt(x,y,z);if(b.getState() instanceof TileState ts){String expected=registry.getString(path+".marker-id"),actual=ts.getPersistentDataContainer().get(idKey,PersistentDataType.STRING);if(ts.getPersistentDataContainer().has(markerKey,PersistentDataType.BYTE)&&(expected==null||expected.equals(actual)))b.setType(Material.AIR,false);}registry.set(path,null);saveRegistry();return true;}

    public boolean createWarpBanner(Player p, String warpName, DyeColor color) {
        if (!plugin.getConfig().getBoolean("markers.warps.enabled", true)) return true;
        if(!removeWarpMarker(warpName))return false;
        Block feet = p.getLocation().getBlock();
        if (!feet.isEmpty() || !feet.getRelative(BlockFace.DOWN).getType().isSolid()) return false;
        BlockData candidate = bannerMaterial(color).createBlockData();
        if (candidate instanceof Rotatable rotatable) rotatable.setRotation(cardinal(p.getLocation().getYaw()+180f));
        if (!candidate.isSupported(feet)) return false;
        feet.setBlockData(candidate, false);
        if (!(feet.getState() instanceof Banner banner)) { feet.setType(Material.AIR, false); return false; }
        mark(banner, "warp", warpName, UUID.randomUUID().toString());
        banner.update(true, false);
        return true;
    }

    public boolean createSpawnBanner(Player p) {
        if (!plugin.getConfig().getBoolean("markers.spawn.enabled", true)) return true;
        Block feet = p.getLocation().getBlock();
        if (!feet.isEmpty() || !feet.getRelative(BlockFace.DOWN).getType().isSolid()) return false;
        BlockData candidate = Material.LIME_BANNER.createBlockData();
        if (candidate instanceof Rotatable rotatable) rotatable.setRotation(cardinal(p.getLocation().getYaw()));
        if (!candidate.isSupported(feet)) return false;
        feet.setBlockData(candidate, false);
        if (!(feet.getState() instanceof Banner banner)) { feet.setType(Material.AIR, false); return false; }
        mark(banner, "spawn", p.getWorld().getName(), UUID.randomUUID().toString());
        banner.update(true, false);
        plugin.getConfig().set("markers.spawn.location.world", feet.getWorld().getName());
        plugin.getConfig().set("markers.spawn.location.x", feet.getX());
        plugin.getConfig().set("markers.spawn.location.y", feet.getY());
        plugin.getConfig().set("markers.spawn.location.z", feet.getZ());
        plugin.saveConfig();
        return true;
    }

    private void mark(TileState state, String type, String name, String markerId) {
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte)1);
        pdc.set(typeKey, PersistentDataType.STRING, type);
        pdc.set(nameKey, PersistentDataType.STRING, name);
        pdc.set(idKey, PersistentDataType.STRING, markerId);
    }

    @EventHandler public void onBreak(BlockBreakEvent e) {
        if (!(e.getBlock().getState() instanceof TileState state)) return;
        if (!state.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) return;
        // El marcador es cosmético: romperlo jamás elimina la ubicación real.
        e.setDropItems(false);
        String type=state.getPersistentDataContainer().get(typeKey,PersistentDataType.STRING),name=state.getPersistentDataContainer().get(nameKey,PersistentDataType.STRING);if(type!=null&&name!=null){registry.set(type+"."+name,null);saveRegistry();}
    }

    private void record(String type,String name,Block b,String markerId){String p=type+"."+name;registry.set(p+".marker-id",markerId);registry.set(p+".world",b.getWorld().getName());registry.set(p+".x",b.getX());registry.set(p+".y",b.getY());registry.set(p+".z",b.getZ());registry.set(p+".updated",java.time.Instant.now().toString());saveRegistry();}
    private synchronized void saveRegistry(){try{registryFile.getParentFile().mkdirs();registry.save(registryFile);}catch(IOException e){plugin.getLogger().warning("Marker registry: "+e.getMessage());}}
    public int cleanupRegistered(){int removed=0;for(String type:new java.util.ArrayList<>(registry.getKeys(false))){var sec=registry.getConfigurationSection(type);if(sec==null)continue;for(String name:new java.util.ArrayList<>(sec.getKeys(false))){String p=type+"."+name,wname=registry.getString(p+".world");World w=Bukkit.getWorld(wname);if(w==null)continue;int bx=registry.getInt(p+".x"),bz=registry.getInt(p+".z");if(!w.isChunkLoaded(bx>>4,bz>>4))continue;Block b=w.getBlockAt(bx,registry.getInt(p+".y"),bz);if(!(b.getState() instanceof TileState ts)||!ts.getPersistentDataContainer().has(markerKey,PersistentDataType.BYTE)){registry.set(p,null);removed++;}}}saveRegistry();return removed;}

    private void spawnParticles() {
        String worldName = plugin.getConfig().getString("markers.spawn.location.world", "");
        if (worldName == null || worldName.isBlank()) return;
        World w = Bukkit.getWorld(worldName); if (w == null) return;
        int x=plugin.getConfig().getInt("markers.spawn.location.x"), y=plugin.getConfig().getInt("markers.spawn.location.y"), z=plugin.getConfig().getInt("markers.spawn.location.z");
        Location loc = new Location(w, x + .5, y + 1.2, z + .5);
        double view = plugin.getConfig().getDouble("markers.spawn.particles.view-distance", 16.0);
        boolean nearby = w.getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(loc) <= view * view);
        if (!nearby) return;
        w.spawnParticle(Particle.HAPPY_VILLAGER, loc, 5, .65, .45, .65, 0.0);
    }

    private static BlockFace cardinal(float yaw) {
        int i = Math.floorMod(Math.round(yaw / 45f), 8);
        return new BlockFace[]{BlockFace.SOUTH,BlockFace.SOUTH_WEST,BlockFace.WEST,BlockFace.NORTH_WEST,BlockFace.NORTH,BlockFace.NORTH_EAST,BlockFace.EAST,BlockFace.SOUTH_EAST}[i];
    }
    private static Material bannerMaterial(DyeColor c) { return Material.valueOf(c.name().toUpperCase(Locale.ROOT) + "_BANNER"); }
}
