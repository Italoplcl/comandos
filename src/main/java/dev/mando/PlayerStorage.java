package dev.mando;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PlayerStorage {
    public static final int SCHEMA = 1;
    private final Mando plugin;
    private final Path players;
    private final Path backups;
    private final Map<UUID,YamlConfiguration> cache = new HashMap<>();
    private final Object lock = new Object();

    public PlayerStorage(Mando plugin) {
        this.plugin=plugin;
        this.players=plugin.getDataFolder().toPath().resolve("players");
        this.backups=plugin.getDataFolder().toPath().resolve("backups").resolve("players");
        try { Files.createDirectories(players); Files.createDirectories(backups); }
        catch(IOException e){ throw new IllegalStateException("No se pudo inicializar players/",e); }
    }

    public synchronized Set<UUID> knownPlayers() {
        Set<UUID> ids=new HashSet<>();
        try(var s=Files.list(players)){s.filter(p->p.getFileName().toString().endsWith(".yml")).forEach(p->{try{ids.add(UUID.fromString(p.getFileName().toString().replace(".yml","")));}catch(Exception ignored){}});}
        catch(IOException e){plugin.getLogger().warning("No se pudo listar players/: "+e.getMessage());}
        ids.addAll(cache.keySet()); return ids;
    }

    public synchronized YamlConfiguration read(UUID id) {
        return cache.computeIfAbsent(id,this::load);
    }

    public synchronized <T> T query(UUID id, Function<YamlConfiguration,T> fn) { return fn.apply(read(id)); }

    public synchronized void update(UUID id, Consumer<YamlConfiguration> fn) {
        YamlConfiguration y=read(id); fn.accept(y); touch(y); saveSnapshot(id,y.saveToString(),true);
    }

    public synchronized void updateNoBackup(UUID id, Consumer<YamlConfiguration> fn) {
        YamlConfiguration y=read(id); fn.accept(y); touch(y); saveSnapshot(id,y.saveToString(),false);
    }

    public synchronized void flushAll() {
        for(var e:cache.entrySet()) saveSnapshot(e.getKey(),e.getValue().saveToString(),false);
    }

    public synchronized void backupAll(String reason) {
        String stamp=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
        Path dir=backups.resolve(stamp+"-"+reason.replaceAll("[^a-zA-Z0-9_-]","_"));
        try { Files.createDirectories(dir); }
        catch(IOException e){plugin.getLogger().severe("No se pudo crear backup: "+e.getMessage());return;}
        for(UUID id:knownPlayers()){
            YamlConfiguration y=read(id);
            try { Files.writeString(dir.resolve(id+".yml"),y.saveToString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING); }
            catch(IOException e){plugin.getLogger().warning("Backup falló para "+id+": "+e.getMessage());}
        }
        rotateBackups();
    }

    public synchronized void importSection(UUID id,String path,Object value) {
        YamlConfiguration y=read(id); if(y.get(path)==null) y.set(path,value); touch(y); saveSnapshot(id,y.saveToString(),false);
    }

    private YamlConfiguration load(UUID id) {
        Path p=players.resolve(id+".yml"); YamlConfiguration y=new YamlConfiguration();
        if(Files.exists(p)) y=YamlConfiguration.loadConfiguration(p.toFile());
        touch(y); return y;
    }

    private void touch(YamlConfiguration y) {
        y.set("meta.schema",SCHEMA);
        if(!y.contains("meta.created-at")) y.set("meta.created-at",System.currentTimeMillis());
        y.set("meta.updated-at",System.currentTimeMillis());
    }

    private void saveSnapshot(UUID id,String data,boolean backupPrevious) {
        synchronized(lock){
            Path dst=players.resolve(id+".yml"), tmp=players.resolve(id+".yml.tmp");
            try {
                if(backupPrevious && Files.exists(dst)) Files.copy(dst,players.resolve(id+".yml.bak"),StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(tmp,data,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
                try { Files.move(tmp,dst,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
                catch(AtomicMoveNotSupportedException ex){ Files.move(tmp,dst,StandardCopyOption.REPLACE_EXISTING); }
            } catch(IOException e){ plugin.getLogger().severe("No se pudo guardar players/"+id+".yml: "+e.getMessage()); }
        }
    }

    private void rotateBackups() {
        int keep=Math.max(1,plugin.getConfig().getInt("storage.backups.keep",10));
        try(var s=Files.list(backups)){
            List<Path> dirs=s.filter(Files::isDirectory).sorted(Comparator.comparing(Path::getFileName).reversed()).toList();
            for(int i=keep;i<dirs.size();i++) deleteTree(dirs.get(i));
        }catch(IOException e){plugin.getLogger().warning("No se pudo rotar backups: "+e.getMessage());}
    }
    private void deleteTree(Path root) throws IOException {
        try(var s=Files.walk(root)){for(Path p:s.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
    }
}
