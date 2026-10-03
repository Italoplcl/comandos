package dev.esslite;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public final class MailManager {
    public record Mail(String id, UUID from, String fromName, long sentAt, String body, boolean read) {}
    private final EssLite plugin; private final File file; private final Map<UUID,List<Mail>> boxes=new HashMap<>(); private final Map<UUID,Set<UUID>> blocks=new HashMap<>();
    public MailManager(EssLite plugin){this.plugin=plugin;file=new File(plugin.getDataFolder(),"mail.yml");load();}
    public synchronized String send(UUID from,String fromName,UUID to,String body){if(blocks.getOrDefault(to,Set.of()).contains(from))return "blocked";String id=Long.toString(System.currentTimeMillis(),36)+"-"+Integer.toString(new Random().nextInt(46656),36);boxes.computeIfAbsent(to,k->new ArrayList<>()).add(new Mail(id,from,fromName,System.currentTimeMillis(),body,false));save();return id;}
    public List<Mail> inbox(UUID id){return List.copyOf(boxes.getOrDefault(id,List.of()));}
    public Mail get(UUID owner,String id){return boxes.getOrDefault(owner,List.of()).stream().filter(m->m.id().equals(id)).findFirst().orElse(null);}
    public void markRead(UUID owner,String id){List<Mail> l=boxes.get(owner);if(l==null)return;for(int i=0;i<l.size();i++){Mail m=l.get(i);if(m.id().equals(id)){l.set(i,new Mail(m.id(),m.from(),m.fromName(),m.sentAt(),m.body(),true));save();return;}}}
    public boolean delete(UUID owner,String id){List<Mail> l=boxes.get(owner);boolean ok=l!=null&&l.removeIf(m->m.id().equals(id));if(ok)save();return ok;}
    public boolean toggleBlock(UUID owner,UUID target){Set<UUID>s=blocks.computeIfAbsent(owner,k->new HashSet<>());boolean now;if(s.remove(target))now=false;else{ s.add(target);now=true;}save();return now;}
    public boolean blocked(UUID owner,UUID target){return blocks.getOrDefault(owner,Set.of()).contains(target);}
    public int unread(UUID id){return (int)boxes.getOrDefault(id,List.of()).stream().filter(m->!m.read()).count();}
    private void load(){YamlConfiguration y=YamlConfiguration.loadConfiguration(file);ConfigurationSection root=y.getConfigurationSection("boxes");if(root!=null)for(String u:root.getKeys(false)){try{UUID id=UUID.fromString(u);ConfigurationSection sec=root.getConfigurationSection(u);if(sec==null)continue;for(String k:sec.getKeys(false)){ConfigurationSection m=sec.getConfigurationSection(k);if(m==null)continue;boxes.computeIfAbsent(id,x->new ArrayList<>()).add(new Mail(k,UUID.fromString(m.getString("from")),m.getString("fromName","?"),m.getLong("sentAt"),m.getString("body",""),m.getBoolean("read")));}}catch(Exception ignored){}}ConfigurationSection br=y.getConfigurationSection("blocks");if(br!=null)for(String u:br.getKeys(false)){try{UUID id=UUID.fromString(u);Set<UUID>s=new HashSet<>();for(String v:br.getStringList(u))try{s.add(UUID.fromString(v));}catch(Exception ignored){}blocks.put(id,s);}catch(Exception ignored){}}}
    public synchronized void save(){YamlConfiguration y=new YamlConfiguration();for(var e:boxes.entrySet())for(Mail m:e.getValue()){String b="boxes."+e.getKey()+"."+m.id();y.set(b+".from",m.from().toString());y.set(b+".fromName",m.fromName());y.set(b+".sentAt",m.sentAt());y.set(b+".body",m.body());y.set(b+".read",m.read());}for(var e:blocks.entrySet())y.set("blocks."+e.getKey(),e.getValue().stream().map(UUID::toString).toList());try{y.save(file);}catch(IOException ex){plugin.getLogger().severe("No se pudo guardar mail.yml: "+ex.getMessage());}}
}
