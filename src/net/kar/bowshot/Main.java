/*
KarBowShot Code v1.3
更新内容: 
1. 更好的代码布局，更多的注释，更加方便java/Bukkit插件小白的阅读。
2.添加声音修改功能，你可以在配置文件中修改声音，若不知道有哪些声音，装载插件后输入/sounds即可
(*关于这个功能没有源码，因为对于小白来说太难懂了,并且也不属于该插件实际内容)。

*/

package net.kar.bowshot;

import java.text.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin //继承JavaPlugin，使本类"成为一个插件"
implements Listener/*实现监听器接口(使本类成为一个监听器)*/{

    //精度，指精确到小数点后几位
    int accuracy = 1;
    //是否开启声音
    boolean sound = false;
    //声音的名字
    String soundName;
    //本插件
    public static Main instance;
    
    @Override
    public void onEnable(){//插件的入口
        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l载入插件KarBowShot"));//将符号'&'替换成颜色符号"§",否则不会显示出颜色
        
        Bukkit.getPluginManager().registerEvents(this/*注册的监听器对象*/, this/*主类地址*/);//注册事件(由于本类是监听器的子类(或者说本类就是一个监听器),所以这里用一个this表示)
        //如果监听器在其他类，将第一个this改成 new 监听器类名() 即可
        
        //读取配置文件
        this.accuracy = getConfig().getInt("Accuracy");
        this.sound = getConfig().getBoolean("Sound");
        this.soundName = getConfig().getString("SoundName");
        
        instance = this;
        
        //为防止某些腐竹错误地填写精度,我们要对其进行判断
        if(this.accuracy > 7 || this.accuracy < 1){
            this.accuracy = 1;
            getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l错误的精度"));
        }
        
        //生成配置文件
        saveDefaultConfig();
        reloadConfig();
    }
    
    
    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        //实现鱼竿勾到人时的效果
        if ((event.getDamager() instanceof FishHook)) {
            FishHook arrow = (FishHook) event.getDamager();
            if ((arrow.getShooter() instanceof Player)) {
                Player shooter = (Player) arrow.getShooter(); //得到使用鱼竿的人
                Damageable targetEntity = (Damageable) event.getEntity();//得到被鱼竿勾中的实体
                if ((targetEntity instanceof Player)) { //判断被鱼竿勾中的实体是否为玩家
                    Player targetPlayer = (Player) targetEntity; //得到被鱼竿勾中的玩家
                    double targetPlayer_Health = targetEntity.getHealth(); 
                    Double damage = (double) event.getFinalDamage(); //得到实际伤害值(如果用getDamage()的话，得到的是没有被减免的伤害)
                    if (!targetEntity.isDead()) { //如果对方已经死亡，那么就没必要知道对方的血量了
                        Double realHealth = (double) (targetPlayer_Health - damage.intValue());
                        
                        //对伤害值的精度(即小数点后的位数)进行限制
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setMaximumFractionDigits(this.accuracy);
                        
                        if (realHealth.intValue() > 0) {
                            shooter.sendMessage("§a§l" + targetPlayer.getName() + "§7剩余血量§f§l" + nf.format(realHealth) + "/"+targetPlayer.getMaxHealth()+"§c❤");
                        }
                    }
                }
            }
        }
        
        //实现箭射到人时的效果，同理
        if ((event.getDamager() instanceof Arrow)) {
            Arrow arrow = (Arrow) event.getDamager();
            if ((arrow.getShooter() instanceof Player)) {
                Player shooter = (Player) arrow.getShooter();
                Entity targetEntity = event.getEntity();
                if ((targetEntity instanceof Player)) {
                    Player targetPlayer = (Player) targetEntity;
                    double targetPlayer_Health = targetPlayer.getHealth();
                    Double damage = (double) event.getFinalDamage();
                    
                    double flc = KarMath.getDistance(shooter.getLocation(), targetPlayer.getLocation());//算出两名玩家之间的距离
                    
                    if (!targetEntity.isDead()) {
                        Double realHealth = (double) (targetPlayer_Health - damage.intValue());
                        
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setMaximumFractionDigits(this.accuracy);
                        
                        if (realHealth.intValue() > 0) {
                            shooter.sendMessage("§7你在§f§l"+nf.format(flc)+"§7米外对 §a§l" + targetPlayer.getName() + " §7造成了§f§l" + nf.format(event.getFinalDamage()) + "§7点伤害,剩余血量§f" + nf.format(realHealth) + "/"+targetPlayer.getMaxHealth()+"§c❤");
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        Player p = event.getEntity();//得到死亡的玩家,注意这里不是getPlayer();
        EntityDamageEvent ede = p.getLastDamageCause(); // 得到玩家受到的最后一次伤害事件
        EntityDamageEvent.DamageCause dc = ede.getCause(); //得到玩家受到的最后一次伤害
        
        if (dc == EntityDamageEvent.DamageCause.PROJECTILE) { //判断玩家受到的最后一次伤害是否为弓箭伤害
            if (p.getKiller() != null) { //判断杀手是否存在(有可能会是发射器射出的箭杀死的玩家)，防止NullPointerException的发生
                Player killer = p.getKiller();//得到杀手
                
                double flc = KarMath.getDistance(p.getLocation(), killer.getLocation());//得到两名玩家之间的距离
                
                //对伤害值的精度(即小数点后的位数)进行限制
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(this.accuracy);
                
                double health = killer.getHealth();
                event.setDeathMessage("§c§l§m" + p.getDisplayName() + "§7 被 §a§l" + killer.getDisplayName() + "§7(§c§l" + nf.format(health) + "§c❤§7) 射死了(§f§l" + nf.format(flc) + "§7米远)");
            }
        }
        
        if(sound){//给杀手发送一段声音
            if(p.getKiller() != null){
                p.getKiller().playSound(p.getKiller().getLocation(), soundName, 1, 1);
            }
        }
        
    }
}

//写一个工具类，如果工具(方法)数量多，建议这样做
class KarMath{
    
    //工具类中全是静态函数，为防止该类被实例化，将其构造函数私有化
    private KarMath(){}
    
    public static double getDistance(Location lc1,Location lc2){
        //关于三维坐标系中两点的距离算法，请自行百度
        return Math.sqrt(Math.pow(lc1.getX() - lc2.getX() , 2)+ Math.pow(lc1.getY() - lc2.getY() , 2)+ Math.pow(lc1.getZ() - lc2.getZ() , 2));
    }
    
}