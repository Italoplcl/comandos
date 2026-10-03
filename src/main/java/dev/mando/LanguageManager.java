package dev.mando;
import net.kyori.adventure.text.Component;import net.kyori.adventure.text.minimessage.MiniMessage;import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;import java.io.*;import java.util.*;
public final class LanguageManager {
 private static final MiniMessage MM=MiniMessage.miniMessage();private final Mando plugin;private YamlConfiguration current,en;
 public LanguageManager(Mando p){plugin=p;reload();}
 public void reload(){String code=plugin.getConfig().getString("language","es").toLowerCase(Locale.ROOT);en=load("en");current=code.equals("en")?en:load(code);}
 private YamlConfiguration load(String code){File f=new File(plugin.getDataFolder(),"languages/"+code+".yml");if(!f.exists()){plugin.saveResource("languages/"+code+".yml",false);}return YamlConfiguration.loadConfiguration(f);}
 public Component text(String key,TagResolver... r){String raw=current.getString(key);if(raw==null)raw=en.getString(key);if(raw==null)raw=plugin.getConfig().getString("messages."+key,"<red>Missing message: "+key);return MM.deserialize(raw,r);}
 public Component msg(String key,TagResolver... r){return text("prefix").append(text(key,r));}
 public List<Component> list(String key,TagResolver... r){List<String> v=current.getStringList(key);if(v.isEmpty())v=en.getStringList(key);if(v.isEmpty())v=plugin.getConfig().getStringList("messages."+key);List<Component> out=new ArrayList<>();for(String x:v)out.add(MM.deserialize(x,r));return out;}
}
