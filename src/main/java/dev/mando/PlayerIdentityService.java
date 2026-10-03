package dev.mando;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerIdentityService implements Listener {
    public record Identity(UUID uuid,String name,boolean online,boolean manual) {}
    private final Mando plugin; private final PlayerStorage storage; private final File file; private final YamlConfiguration aliases;
    public PlayerIdentityService(Mando p,PlayerStorage s){plugin=p;storage=s;file=new File(p.getDataFolder(),"identities.yml");aliases=YamlConfiguration.loadConfiguration(file);}
    public Identity resolve(String input){
        try{UUID id=UUID.fromString(input);OfflinePlayer op=Bukkit.getOfflinePlayer(id);return new Identity(id,op.getName(),op.isOnline(),false);}catch(Exception ignored){}
        Player live=Bukkit.getPlayerExact(input);if(live!=null)return remember(live.getUniqueId(),live.getName(),true,false);
        String raw=aliases.getString("names."+input.toLowerCase(Locale.ROOT));
        if(raw!=null)try{UUID id=UUID.fromString(raw);OfflinePlayer op=Bukkit.getOfflinePlayer(id);return new Identity(id,op.getName()!=null?op.getName():input,op.isOnline(),aliases.getBoolean("manual."+input.toLowerCase(Locale.ROOT),false));}catch(Exception ignored){}
        for(UUID id:storage.knownPlayers()){String n=storage.query(id,y->y.getString("identity.last-name"));if(n!=null&&n.equalsIgnoreCase(input))return new Identity(id,n,Bukkit.getPlayer(id)!=null,false);}
        return null;
    }
    public Identity remember(UUID id,String name,boolean online,boolean manual){if(name!=null&&!name.isBlank()){aliases.set("names."+name.toLowerCase(Locale.ROOT),id.toString());aliases.set("manual."+name.toLowerCase(Locale.ROOT),manual);save();storage.updateNoBackup(id,y->{y.set("identity.uuid",id.toString());y.set("identity.last-name",name);});}return new Identity(id,name,online,manual);}
    public boolean bindManual(String name,UUID id){if(name==null||name.isBlank())return false;remember(id,name,false,true);return true;}
    @EventHandler public void join(PlayerJoinEvent e){remember(e.getPlayer().getUniqueId(),e.getPlayer().getName(),true,false);}
    private void save(){try{aliases.save(file);}catch(IOException e){plugin.getLogger().severe("No se pudo guardar identities.yml: "+e.getMessage());}}
}
