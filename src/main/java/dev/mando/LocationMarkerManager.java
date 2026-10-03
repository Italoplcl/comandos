package dev.mando;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/** Marcadores cosmeticos. Nunca son la fuente de verdad de homes/warps/spawn. */
public final class LocationMarkerManager implements Listener {
    private final Mando plugin;
    private final NamespacedKey markerKey,typeKey,nameKey;
    public LocationMarkerManager(Mando plugin){
        this.plugin=plugin; markerKey=new NamespacedKey(plugin,"location_marker"); typeKey=new NamespacedKey(plugin,"marker_type"); nameKey=new NamespacedKey(plugin,"marker_name");
        if(plugin.getConfig().getBoolean("markers.spawn.particles.enabled",true)){
            long interval=Math.max(10L,plugin.getConfig().getLong("markers.spawn.particles.interval-ticks",20L));
            Bukkit.getScheduler().runTaskTimer(plugin,this::spawnParticles,interval,interval);
        }
    }
    public boolean createHomeSign(Player p,String homeName){
        if(!plugin.getConfig().getBoolean("markers.homes.enabled",true))return true;
        int distance=plugin.getConfig().getInt("markers.homes.max-distance",5);
        Block target=p.getTargetBlockExact(distance); BlockFace face=p.getTargetBlockFace(distance);
        if(target==null||face==null)return fallbackText(p.getLocation(), "home", p.getUniqueId()+":"+homeName, homeName+"\n"+p.getName());
        Block place=target.getRelative(face);
        if(!place.isEmpty())return fallbackText(p.getLocation(), "home", p.getUniqueId()+":"+homeName, homeName+"\n"+p.getName());
        Material mat=face==BlockFace.DOWN?Material.OAK_HANGING_SIGN:(face==BlockFace.UP?Material.OAK_SIGN:Material.OAK_WALL_SIGN);
        place.setType(mat,false);
        if(place.getBlockData() instanceof Directional d&&face!=BlockFace.UP&&face!=BlockFace.DOWN){d.setFacing(face);place.setBlockData(d,false);}
        else if(place.getBlockData() instanceof Rotatable r){r.setRotation(cardinal(p.getLocation().getYaw()));place.setBlockData(r,false);}
        if(!(place.getState() instanceof Sign sign)){place.setType(Material.AIR,false);return fallbackText(p.getLocation(),"home",p.getUniqueId()+":"+homeName,homeName+"\n"+p.getName());}
        String deco=plugin.getConfig().getString("markers.homes.decoration","--------");
        for(Side signFace:Side.values()){SignSide side=sign.getSide(signFace);side.line(0,Component.text(deco));side.line(1,Component.text(homeName));side.line(2,Component.text(p.getName()));side.line(3,Component.text(deco));side.setGlowingText(plugin.getConfig().getBoolean("markers.homes.glowing",true));}
        mark(sign,"home",p.getUniqueId()+":"+homeName);sign.update(true,false);return true;
    }
    public boolean createWarpBanner(Location source,String warpName,DyeColor color){
        if(!plugin.getConfig().getBoolean("markers.warps.enabled",true))return true;
        Block feet=source.getBlock();
        if(!feet.isEmpty()||!feet.getRelative(BlockFace.DOWN).getType().isSolid())return fallbackText(source,"warp",warpName,"Warp\n"+warpName);
        feet.setType(bannerMaterial(color),false); if(feet.getBlockData() instanceof Rotatable r){r.setRotation(cardinal(source.getYaw()).getOppositeFace());feet.setBlockData(r,false);}
        if(!(feet.getState() instanceof Banner b)){feet.setType(Material.AIR,false);return fallbackText(p.getLocation(),"warp",warpName,"Warp\n"+warpName);}
        mark(b,"warp",warpName);b.update(true,false);return true;
    }
    public boolean createSpawnBanner(Player p){
        if(!plugin.getConfig().getBoolean("markers.spawn.enabled",true))return true;
        Block feet=p.getLocation().getBlock();
        if(!feet.isEmpty()||!feet.getRelative(BlockFace.DOWN).getType().isSolid()){rememberSpawn(p.getLocation());return fallbackText(p.getLocation(),"spawn",p.getWorld().getName(),"Spawn");}
        feet.setType(Material.LIME_BANNER,false); if(feet.getBlockData() instanceof Rotatable r){r.setRotation(cardinal(p.getLocation().getYaw()).getOppositeFace());feet.setBlockData(r,false);}
        if(!(feet.getState() instanceof Banner b)){feet.setType(Material.AIR,false);rememberSpawn(p.getLocation());return fallbackText(p.getLocation(),"spawn",p.getWorld().getName(),"Spawn");}
        mark(b,"spawn",p.getWorld().getName());b.update(true,false);placeSpawnLight(feet);rememberSpawn(feet.getLocation());return true;
    }
    private void placeSpawnLight(Block banner){
        for(int dy=1;dy<=4;dy++){Block light=banner.getRelative(0,dy,0);if(!light.isEmpty()&&light.getType()!=Material.LIGHT)continue;light.setType(Material.LIGHT,false);if(light.getBlockData() instanceof org.bukkit.block.data.Levelled level){level.setLevel(15);light.setBlockData(level,false);}return;}
        plugin.getLogger().warning("No se encontró espacio libre para la luz nivel 15 sobre el banner de Spawn.");
    }
    private boolean fallbackText(Location loc,String type,String name,String text){
        if(!plugin.getConfig().getBoolean("markers.fallback-text-display",true)||loc.getWorld()==null)return false;
        Location at=loc.clone().add(.5,1.4,.5);
        TextDisplay d=loc.getWorld().spawn(at,TextDisplay.class,t->{t.text(Component.text(text));t.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);t.setSeeThrough(true);t.setPersistent(true);});
        PersistentDataContainer p=d.getPersistentDataContainer();p.set(markerKey,PersistentDataType.BYTE,(byte)1);p.set(typeKey,PersistentDataType.STRING,type);p.set(nameKey,PersistentDataType.STRING,name);return true;
    }
    private void rememberSpawn(Location l){plugin.getConfig().set("markers.spawn.location.world",l.getWorld().getName());plugin.getConfig().set("markers.spawn.location.x",l.getBlockX());plugin.getConfig().set("markers.spawn.location.y",l.getBlockY());plugin.getConfig().set("markers.spawn.location.z",l.getBlockZ());plugin.saveConfig();}
    private void mark(TileState s,String type,String name){PersistentDataContainer p=s.getPersistentDataContainer();p.set(markerKey,PersistentDataType.BYTE,(byte)1);p.set(typeKey,PersistentDataType.STRING,type);p.set(nameKey,PersistentDataType.STRING,name);}
    @EventHandler public void onBreak(BlockBreakEvent e){if(!(e.getBlock().getState() instanceof TileState s))return;if(!s.getPersistentDataContainer().has(markerKey,PersistentDataType.BYTE))return;e.setDropItems(false);if("spawn".equals(s.getPersistentDataContainer().get(typeKey,PersistentDataType.STRING))){for(int dy=1;dy<=4;dy++){Block light=e.getBlock().getRelative(0,dy,0);if(light.getType()==Material.LIGHT){light.setType(Material.AIR,false);break;}}}}
    private void spawnParticles(){String wn=plugin.getConfig().getString("markers.spawn.location.world","");if(wn==null||wn.isBlank())return;World w=Bukkit.getWorld(wn);if(w==null)return;int x=plugin.getConfig().getInt("markers.spawn.location.x"),y=plugin.getConfig().getInt("markers.spawn.location.y"),z=plugin.getConfig().getInt("markers.spawn.location.z");Location loc=new Location(w,x+.5,y+1.2,z+.5);double view=plugin.getConfig().getDouble("markers.spawn.particles.view-distance",16);if(w.getPlayers().stream().noneMatch(p->p.getLocation().distanceSquared(loc)<=view*view))return;w.spawnParticle(Particle.HAPPY_VILLAGER,loc,5,.65,.45,.65,0);}
    private static BlockFace cardinal(float yaw){int i=Math.floorMod(Math.round(yaw/45f),8);return new BlockFace[]{BlockFace.SOUTH,BlockFace.SOUTH_WEST,BlockFace.WEST,BlockFace.NORTH_WEST,BlockFace.NORTH,BlockFace.NORTH_EAST,BlockFace.EAST,BlockFace.SOUTH_EAST}[i];}
    private static Material bannerMaterial(DyeColor c){return Material.valueOf(c.name().toUpperCase(Locale.ROOT)+"_BANNER");}
}
