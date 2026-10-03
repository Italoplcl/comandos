package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.plugin.Plugin;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;
public final class IntegrationManager{
 public enum State{ACTIVE,STANDBY,DISABLED,CONFLICT,UNAVAILABLE}
 public record Provider(String capability,String selected,State state,String reason){}
 private final MandoPlugin plugin;private final Map<String,State> states=new LinkedHashMap<>();private final Map<String,Provider> providers=new LinkedHashMap<>();private String fingerprint="",previousFingerprint="";
 private static final List<String> KNOWN=List.of("Essentials","CMI","HuskHomes","BetterRTP","TAB","ChatControl","LPC","InteractiveChat","DiscordSRV","PlaceholderAPI","LuckPerms","Vault","AuthMe");
 public IntegrationManager(MandoPlugin p){plugin=p;previousFingerprint=p.getConfig().getString("integrations.last-fingerprint","");refresh();}
 public void refresh(){
  states.clear();providers.clear();for(String n:KNOWN)states.put(n,present(n)?State.STANDBY:State.UNAVAILABLE);
  capability("homes",List.of("HuskHomes","Essentials","CMI"));capability("warps",List.of("HuskHomes","Essentials","CMI"));capability("spawn",List.of("HuskHomes","Essentials","CMI"));capability("back",List.of("HuskHomes","Essentials","CMI"));capability("rtp",List.of("BetterRTP","HuskHomes","Essentials","CMI"));capability("afk",List.of("Purpur","Essentials","CMI"));
  for(String n:List.of("Vault","AuthMe","PlaceholderAPI","LuckPerms"))if(present(n))states.put(n,State.ACTIVE);
  String next=makeFingerprint();if(!fingerprint.isEmpty()&&!fingerprint.equals(next))plugin.getLogger().warning("Cambió el ecosistema de plugins/integraciones. Revisa /mando status antes de cambiar proveedores.");fingerprint=next;if(!fingerprint.equals(previousFingerprint)){if(!previousFingerprint.isBlank())plugin.getLogger().warning("El ecosistema cambió desde el último inicio. Revisa proveedores antes de confirmar cambios.");plugin.getConfig().set("integrations.last-fingerprint",fingerprint);plugin.saveConfig();}
 }
 private void capability(String cap,List<String> candidates){
  String selected=plugin.getConfig().getString("providers."+cap,"Mando");State st;String reason;
  if(selected.equalsIgnoreCase("Mando")){st=State.ACTIVE;reason="Mando seleccionado";for(String n:candidates)if(present(n)&&states.get(n)==State.STANDBY)states.put(n,State.STANDBY);}
  else if(selected.equalsIgnoreCase("Purpur")&&cap.equals("afk")&&plugin.platform().isPurpur()){st=State.ACTIVE;reason="Purpur seleccionado";}
  else if(present(selected)){st=State.ACTIVE;reason=selected+" seleccionado";states.put(selected,State.ACTIVE);}
  else{st=State.CONFLICT;reason="Proveedor seleccionado no está disponible: "+selected;}
  providers.put(cap,new Provider(cap,selected,st,reason));
 }
 private boolean present(String n){Plugin p=Bukkit.getPluginManager().getPlugin(n);return p!=null&&p.isEnabled();}
 private String makeFingerprint(){StringBuilder b=new StringBuilder();KNOWN.forEach(n->{Plugin p=Bukkit.getPluginManager().getPlugin(n);if(p!=null)b.append(n).append(':').append(p.getPluginMeta().getVersion()).append(':').append(p.isEnabled()).append(';');});providers.forEach((k,v)->b.append(k).append('=').append(v.selected()).append(';'));try{byte[] d=MessageDigest.getInstance("SHA-256").digest(b.toString().getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(d,0,8);}catch(Exception e){return Integer.toHexString(b.toString().hashCode());}}
 public boolean changedSinceLastStart(){return !previousFingerprint.isBlank()&&!previousFingerprint.equals(fingerprint);}
 public boolean isMandoProvider(String capability){Provider p=providers.get(capability);return p!=null&&p.state()==State.ACTIVE&&p.selected().equalsIgnoreCase("Mando");}
 public State state(String n){return states.getOrDefault(n,State.UNAVAILABLE);}public Map<String,State> states(){return Collections.unmodifiableMap(states);}public Map<String,Provider> providers(){return Collections.unmodifiableMap(providers);}public String fingerprint(){return fingerprint;}
 public String summary(){StringBuilder s=new StringBuilder("Fingerprint: ").append(fingerprint).append("\
");providers.values().forEach(v->s.append(v.capability()).append(": ").append(v.selected()).append(" · ").append(v.state()).append("\
"));states.forEach((k,v)->{if(v!=State.UNAVAILABLE)s.append(k).append(": ").append(v).append("\
");});return s.toString().trim();}
}