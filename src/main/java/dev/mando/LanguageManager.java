package dev.mando;
import org.bukkit.configuration.file.YamlConfiguration;import java.io.*;import java.util.*;
public final class LanguageManager{
 private final MandoPlugin plugin;private final File dir;private YamlConfiguration primary,fallback;
 public LanguageManager(MandoPlugin p){plugin=p;dir=new File(p.getDataFolder(),"languages");dir.mkdirs();copy("es.yml");copy("en.yml");reload();}
 private void copy(String n){File f=new File(dir,n);if(f.exists())return;try(var in=plugin.getResource("languages/"+n)){if(in!=null)java.nio.file.Files.copy(in,f.toPath());}catch(Exception e){plugin.getLogger().warning("Idioma "+n+": "+e.getMessage());}}
 public void reload(){String lang=plugin.getConfig().getString("language","es");primary=YamlConfiguration.loadConfiguration(new File(dir,lang+".yml"));fallback=YamlConfiguration.loadConfiguration(new File(dir,"en.yml"));}
 public String get(String key,String def){String v=primary.getString(key);if(v==null)v=fallback.getString(key);return v==null?def:v;}
 public List<String> list(String key){List<String> v=primary.getStringList(key);if(v.isEmpty())v=fallback.getStringList(key);return v;}
}