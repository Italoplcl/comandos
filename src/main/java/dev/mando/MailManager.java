package dev.mando;
import org.bukkit.configuration.ConfigurationSection;import java.util.*;
public final class MailManager {
 public record Mail(String id,UUID from,String fromName,long sentAt,String body,boolean read){}
 private final PlayerStorage storage;
 public MailManager(Mando p,PlayerStorage s){storage=s;}
 public synchronized String send(UUID from,String fromName,UUID to,String body){if(blocked(to,from))return "blocked";String id=UUID.randomUUID().toString();storage.update(to,y->{String b="mail.messages."+id;y.set(b+".from",from.toString());y.set(b+".from-name",fromName);y.set(b+".sent-at",System.currentTimeMillis());y.set(b+".body",body);y.set(b+".read",false);});return id;}
 public List<Mail> inbox(UUID id){return storage.query(id,y->{ConfigurationSection s=y.getConfigurationSection("mail.messages");if(s==null)return List.of();List<Mail> l=new ArrayList<>();for(String k:s.getKeys(false)){String b="mail.messages."+k;try{l.add(new Mail(k,UUID.fromString(y.getString(b+".from")),y.getString(b+".from-name","?"),y.getLong(b+".sent-at"),y.getString(b+".body",""),y.getBoolean(b+".read")));}catch(Exception ignored){}}l.sort(Comparator.comparingLong(Mail::sentAt));return List.copyOf(l);});}
 public Mail get(UUID owner,String id){return inbox(owner).stream().filter(m->m.id().equals(id)).findFirst().orElse(null);}
 public void markRead(UUID owner,String id){if(get(owner,id)!=null)storage.update(owner,y->y.set("mail.messages."+id+".read",true));}
 public boolean delete(UUID owner,String id){if(get(owner,id)==null)return false;storage.update(owner,y->y.set("mail.messages."+id,null));return true;}
 public boolean toggleBlock(UUID owner,UUID target){Set<String>s=new HashSet<>(storage.query(owner,y->y.getStringList("preferences.mail.blocked")));boolean on=!s.remove(target.toString());if(on)s.add(target.toString());storage.update(owner,y->y.set("preferences.mail.blocked",new ArrayList<>(s)));return on;}
 public boolean blocked(UUID owner,UUID target){return storage.query(owner,y->y.getStringList("preferences.mail.blocked").contains(target.toString()));}
 public int unread(UUID id){return(int)inbox(id).stream().filter(m->!m.read()).count();}
 public void save(){storage.flushAll();}
}
