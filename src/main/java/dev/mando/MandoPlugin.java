package dev.mando;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class MandoPlugin extends JavaPlugin {
 private static final MiniMessage MM=MiniMessage.miniMessage();
 private HomeManager homes;private GodListener god;private WarpManager warps;private FlyListener fly;private BackListener back;
 private ModuleManager modules;private PlatformDetector platform;private LocationMarkerManager markers;private BackupManager backups;private SetupProtectionListener setupProtection;
 private TpaManager tpa;private MailManager mail;private PlayerDataManager playerData;private TeleportManager teleports;private DeathManager deaths;private IntegrationManager integrations;
 private AuditLogger audit;private DiagnosticManager diagnostics;private AuthGuardListener authGuard;private AfkManager afk;private LanguageManager languages;

 @Override public void onEnable(){
  saveDefaultConfig();
  languages=new LanguageManager(this);modules=new ModuleManager(this);backups=new BackupManager(this);setupProtection=new SetupProtectionListener(this);
  playerData=new PlayerDataManager(this);teleports=new TeleportManager(this);deaths=new DeathManager(this);integrations=new IntegrationManager(this);audit=new AuditLogger(this);diagnostics=new DiagnosticManager(this);authGuard=new AuthGuardListener(this);afk=new AfkManager(this);mail=new MailManager(this);tpa=new TpaManager(this,playerData,teleports);
  getServer().getPluginManager().registerEvents(setupProtection,this);getServer().getPluginManager().registerEvents(teleports,this);getServer().getPluginManager().registerEvents(deaths,this);getServer().getPluginManager().registerEvents(authGuard,this);getServer().getPluginManager().registerEvents(new PlayerSessionListener(this,playerData,mail,authGuard),this);getServer().getPluginManager().registerEvents(tpa,this);
  platform=new PlatformDetector();boolean firstSetup=!getConfig().getBoolean("setup.completed",false);
  getLogger().info("Mando "+getPluginMeta().getVersion()+" iniciando en "+platform.platform());getLogger().info("Minecraft "+getServer().getMinecraftVersion()+" | Java "+System.getProperty("java.version"));if(firstSetup)getLogger().warning("Primera configuración pendiente. Entra como OP para abrir el asistente de Mando.");else getLogger().info("Configuración inicial: completada.");
  MandoCommand mando=new MandoCommand(this);new UpdateChecker(this).checkAsync();register("mando",mando,mando);getServer().getPluginManager().registerEvents(new MandoJoinListener(this,mando),this);
  markers=new LocationMarkerManager(this);getServer().getPluginManager().registerEvents(markers,this);
  homes=new HomeManager(this);god=new GodListener();Rtp rtp=new Rtp(this);Commands commands=new Commands(this,homes,god,rtp,markers);
  for(String name:List.of("god","heal","rtp"))register(name,commands,commands);AdminTeleportCommands adminTp=new AdminTeleportCommands(this);for(String name:List.of("tp","tphere","tppos"))register(name,adminTp,adminTp);
  SocialCommands social=new SocialCommands(this,tpa,mail,homes,playerData,deaths,audit);for(String name:List.of("tpa","tpahere","tpaccept","tpdeny","tpcancel","tptoggle","mail","profile"))register(name,social,social);register("afk",afk,null);
  if(modules.enabled("homes"))for(String name:List.of("sethome","home","homes","delhome","renamehome","restorehome"))register(name,commands,commands);
  warps=new WarpManager(this);fly=new FlyListener();back=new BackListener(this);WarpColorMenu warpColors=new WarpColorMenu(this,warps,markers);getServer().getPluginManager().registerEvents(warpColors,this);
  ExtraCommands extra=new ExtraCommands(this,fly,back,warps,warpColors,markers);for(String name:List.of("fly","back","tools","ender"))register(name,extra,extra);if(modules.enabled("spawn"))for(String name:List.of("spawn","setspawn"))register(name,extra,extra);if(modules.enabled("warps"))for(String name:List.of("warp","setwarp","delwarp"))register(name,extra,extra);
  getServer().getPluginManager().registerEvents(god,this);getServer().getPluginManager().registerEvents(fly,this);getServer().getPluginManager().registerEvents(back,this);getServer().getPluginManager().registerEvents(new ToolsMenu.Events(this),this);getServer().getPluginManager().registerEvents(new HomesMenu.Events(this,homes),this);
  if(modules.enabled("changelog")){PluginCommand changelog=getCommand("changelog");if(changelog!=null)changelog.setExecutor(new ChangelogCommand(this));}
  if(modules.enabled("server-config")){ServerConfigMenu serverConfig=new ServerConfigMenu(this,modules,platform);register("serverconfig",serverConfig,serverConfig);getServer().getPluginManager().registerEvents(serverConfig,this);}
  if(getConfig().getBoolean("backups.scheduled.enabled",true))backups.startSchedule();
 }
 public BackupManager backups(){return backups;}public SetupProtectionListener setupProtection(){return setupProtection;}public ModuleManager modules(){return modules;}public PlatformDetector platform(){return platform;}public IntegrationManager integrations(){return integrations;}public PlayerDataManager playerData(){return playerData;}public DeathManager deaths(){return deaths;}public TeleportManager teleports(){return teleports;}public LocationMarkerManager markers(){return markers;}public DiagnosticManager diagnostics(){return diagnostics;}
 private void register(String name,org.bukkit.command.CommandExecutor executor,org.bukkit.command.TabCompleter completer){PluginCommand cmd=getCommand(name);if(cmd!=null){cmd.setExecutor(executor);cmd.setTabCompleter(completer);}}
 @Override public void onDisable(){if(god!=null)god.disableAll();if(fly!=null)fly.disableAll();if(homes!=null)homes.saveNow();if(warps!=null)warps.saveNow();if(mail!=null)mail.saveNow();if(deaths!=null)deaths.save();if(backups!=null&&getConfig().getBoolean("backups.shutdown.enabled",true))backups.createShutdown();}
 public Component text(String key,TagResolver... resolvers){String raw=languages.get("messages."+key,getConfig().getString("messages."+key,"<red>Falta el mensaje: "+key));return MM.deserialize(raw,resolvers);}
 public Component msg(String key,TagResolver... resolvers){Component prefix=MM.deserialize(languages.get("messages.prefix",getConfig().getString("messages.prefix","")));return prefix.append(text(key,resolvers));}
 public List<Component> list(String key,TagResolver... resolvers){List<Component> out=new ArrayList<>();List<String> source=languages.list("messages."+key);if(source.isEmpty())source=getConfig().getStringList("messages."+key);for(String line:source)out.add(MM.deserialize(line,resolvers));return out;}
}