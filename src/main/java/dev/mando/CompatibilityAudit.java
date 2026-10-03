package dev.mando;
import org.bukkit.Bukkit;
public final class CompatibilityAudit {
 private final Mando plugin;
 public CompatibilityAudit(Mando p){plugin=p;}
 public void run(){
  check("TAB","presentación externa: Mando no modifica TAB, scoreboards ni nametags");
  check("ChatControl","chat externo: Mando no registra renderer/formateador de chat");
  check("PlaceholderAPI","opcional; Mando no lo requiere");
  check("LuckPerms","permisos consumidos vía Bukkit, sin ownership");
  check("AuthMe","gate de autenticación activo si está instalado");
 }
 private void check(String name,String policy){if(Bukkit.getPluginManager().getPlugin(name)!=null)plugin.getLogger().info("Compatibilidad "+name+": "+policy);}
}
