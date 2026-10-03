package dev.mando;
import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.regex.*;
public final class UpdateChecker{
 private final MandoPlugin plugin;public UpdateChecker(MandoPlugin p){plugin=p;}
 public void checkAsync(){if(!plugin.getConfig().getBoolean("updates.enabled",true))return;try{HttpRequest q=HttpRequest.newBuilder(URI.create("https://api.github.com/repos/Italoplcl/comandos/releases/latest")).timeout(Duration.ofSeconds(5)).header("Accept","application/vnd.github+json").build();HttpClient.newHttpClient().sendAsync(q,HttpResponse.BodyHandlers.ofString()).thenAccept(r->{Matcher m=Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)").matcher(r.body());if(m.find())plugin.getLogger().info("GitHub: última versión publicada "+m.group(1)+" | instalada "+plugin.getPluginMeta().getVersion());else plugin.getLogger().info("GitHub: no hay una release estable publicada para comparar.");}).exceptionally(ex->{plugin.getLogger().warning("No se pudo comprobar actualizaciones: "+ex.getMessage());return null;});}catch(Exception ex){plugin.getLogger().warning("Update checker no disponible: "+ex.getMessage());}}
}
