package dev.mando;
import java.io.*;import java.nio.file.*;import java.time.*;import java.time.format.*;import java.util.Comparator;import java.util.zip.*;
public final class BackupManager{
 private final MandoPlugin plugin;private String last="ninguno";
 public BackupManager(MandoPlugin p){plugin=p;}
 public String lastBackup(){return last;}
 public String createManual(){try{Path root=plugin.getDataFolder().toPath();Path dir=root.resolve("backups/manual");Files.createDirectories(dir);String name="mando-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+".zip";Path out=dir.resolve(name);try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(out))){if(Files.exists(root))Files.walk(root).filter(Files::isRegularFile).filter(p->!p.startsWith(root.resolve("backups"))).sorted(Comparator.naturalOrder()).forEach(p->{try{z.putNextEntry(new ZipEntry(root.relativize(p).toString().replace('\\','/')));Files.copy(p,z);z.closeEntry();}catch(IOException ex){throw new UncheckedIOException(ex);}});}last=name;return name;}catch(Exception ex){plugin.getLogger().severe("Backup falló: "+ex.getMessage());return null;}}
}
