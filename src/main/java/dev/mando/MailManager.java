package dev.mando;
import org.bukkit.configuration.file.YamlConfiguration;import java.io.*;import java.time.*;import java.util.*;
public final class MailManager{
 public record Message(UUID from,String fromName,String sent,boolean read,String text){}
 private final MandoPlugin plugin;private final File file;private final YamlConfiguration y;private final Map<UUID,Long> lastSend=new HashMap<>();
 public MailManager(MandoPlugin p){plugin=p;file=new File(p.getDataFolder(),"mail.yml");y=YamlConfiguration.loadConfiguration(file);}
 public synchronized String send(UUID from,String fromName,UUID to,String toName,String text){
  if(from.equals(to))return "No puedes enviarte correo a ti mismo.";
  if(text==null||text.isBlank())return "El mensaje está vacío.";
  int maxLen=plugin.getConfig().getInt("mail.max-length",500);if(text.length()>maxLen)return "El mensaje supera "+maxLen+" caracteres.";
  if(plugin.playerData().mailBlocked(to,from))return "No se pudo entregar el mensaje.";
  long now=System.currentTimeMillis(),wait=Math.max(0,plugin.getConfig().getLong("mail.cooldown-seconds",10))*1000L;long remain=wait-(now-lastSend.getOrDefault(from,0L));if(remain>0)return "Espera "+Math.max(1,(remain+999)/1000)+"s antes de enviar otro correo.";
  String base=to+".messages";List<Map<?,?>> old=y.getMapList(base);if(old.size()>=plugin.getConfig().getInt("mail.max-messages",50))return "El buzón está lleno.";
  List<Map<String,Object>> list=copy(old);Map<String,Object> m=new LinkedHashMap<>();m.put("id",UUID.randomUUID().toString());m.put("from",from.toString());m.put("from-name",fromName);m.put("sent",Instant.now().toString());m.put("read",false);m.put("text",text);list.add(m);y.set(base,list);lastSend.put(from,now);saveNow();return "Mensaje enviado a "+toName+".";
 }
 public synchronized List<Message> messages(UUID id){List<Message> out=new ArrayList<>();for(Map<?,?> m:y.getMapList(id+".messages"))try{out.add(new Message(UUID.fromString(String.valueOf(m.get("from"))),String.valueOf(m.get("from-name")),String.valueOf(m.get("sent")),Boolean.TRUE.equals(m.get("read")),String.valueOf(m.get("text"))));}catch(Exception ignored){}return out;}
 public synchronized Message read(UUID id,int index){List<Map<?,?>> raw=y.getMapList(id+".messages");if(index<0||index>=raw.size())return null;List<Map<String,Object>> list=copy(raw);Map<String,Object> m=list.get(index);m.put("read",true);y.set(id+".messages",list);saveNow();return decode(m,true);}
 public synchronized Message inspect(UUID id,int index){List<Map<?,?>> raw=y.getMapList(id+".messages");if(index<0||index>=raw.size())return null;Map<?,?> m=raw.get(index);try{return new Message(UUID.fromString(String.valueOf(m.get("from"))),String.valueOf(m.get("from-name")),String.valueOf(m.get("sent")),Boolean.TRUE.equals(m.get("read")),String.valueOf(m.get("text")));}catch(Exception e){return null;}}
 private Message decode(Map<String,Object> m,boolean read){try{return new Message(UUID.fromString(String.valueOf(m.get("from"))),String.valueOf(m.get("from-name")),String.valueOf(m.get("sent")),read,String.valueOf(m.get("text")));}catch(Exception e){return null;}}
 public synchronized boolean delete(UUID id,int index){List<Map<?,?>> raw=y.getMapList(id+".messages");if(index<0||index>=raw.size())return false;List<Map<String,Object>> list=copy(raw);list.remove(index);y.set(id+".messages",list);saveNow();return true;}
 public int total(UUID id){return y.getMapList(id+".messages").size();}public int unread(UUID id){int n=0;for(Map<?,?> m:y.getMapList(id+".messages"))if(!Boolean.TRUE.equals(m.get("read")))n++;return n;}
 private List<Map<String,Object>> copy(List<Map<?,?>> old){List<Map<String,Object>> list=new ArrayList<>();for(Map<?,?> x:old){Map<String,Object> n=new LinkedHashMap<>();x.forEach((k,v)->n.put(String.valueOf(k),v));list.add(n);}return list;}
 public synchronized void saveNow(){try{file.getParentFile().mkdirs();y.save(file);}catch(IOException ex){plugin.getLogger().severe("Mail save: "+ex.getMessage());}}
}