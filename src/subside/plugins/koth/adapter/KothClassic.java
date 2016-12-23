package subside.plugins.koth.adapter;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;

import lombok.Getter;
import subside.plugins.koth.ConfigHandler;
import subside.plugins.koth.KothPlugin;
import subside.plugins.koth.Lang;
import subside.plugins.koth.adapter.CapInfo.CapStatus;
import subside.plugins.koth.adapter.captypes.Capper;
import subside.plugins.koth.events.KothEndEvent;
import subside.plugins.koth.utils.MessageBuilder;

/**
 * @author Thomas "SubSide" van den Bulk
 */
public class KothClassic implements RunningKoth {
    private @Getter Koth koth;
    private @Getter CapInfo capInfo;
    private int captureTime;
    
    private @Getter String lootChest;
    private int timeNotCapped;
    private int lootAmount;
    private int captureCooldown;

    private @Getter int maxRunTime;
    private int timeRunning;
    
    private CapStatus prevStatus = CapStatus.EMPTY;
    
    @Override
    public void init(StartParams params){
        this.koth = params.getKoth();
        this.captureCooldown = 0;
        this.captureTime = params.getCaptureTime();
        this.lootChest = params.getLootChest();
        this.lootAmount = params.getLootAmount();
        this.maxRunTime = params.getMaxRunTime() * 60;
        
        this.timeNotCapped = 0;
        this.capInfo = new CapInfo(this, this.koth, KothHandler.getInstance().getCapEntityRegistry().getCaptureTypeClass(params.getEntityType()));
        if(ConfigHandler.getInstance().getKoth().isRemoveChestAtStart()){
            koth.removeLootChest();
        }
        koth.setLastWinner(null);
        new MessageBuilder(Lang.KOTH_PLAYING_STARTING).maxTime(maxRunTime).time(getTimeObject()).koth(koth).buildAndBroadcast();
    }
    
    @Override
    public Capper getCapper(){
        return getCapInfo().getCapper();
    }

    /**
     * Get the TimeObject for the running KoTH
     * @return The TimeObject
     */
    public TimeObject getTimeObject() {
        return new TimeObject(captureTime, capInfo.getTimeCapped());
    }

    public void endKoth(EndReason reason) {
        boolean shouldCreateChest = true;
        if (reason == EndReason.WON || reason == EndReason.GRACEFUL) {
            if (capInfo.getCapper() != null) {
                new MessageBuilder(Lang.KOTH_PLAYING_WON).maxTime(maxRunTime).capper(capInfo.getCapper().getName()).koth(koth).exclude(capInfo.getCapper()).buildAndBroadcast();
                new MessageBuilder(Lang.KOTH_PLAYING_WON_CAPPER).maxTime(maxRunTime).capper(capInfo.getCapper().getName()).koth(koth).buildAndSend(capInfo.getCapper());
            }
        } else if (reason == EndReason.TIMEUP) {
            new MessageBuilder(Lang.KOTH_PLAYING_TIME_UP).maxTime(maxRunTime).koth(koth).buildAndBroadcast();
            shouldCreateChest = ConfigHandler.getInstance().getKoth().isFfaChestTimeLimit();
        }


        KothEndEvent event = new KothEndEvent(koth, capInfo.getCapper(), reason);
        event.setCreatingChest(shouldCreateChest);
        
        Bukkit.getServer().getPluginManager().callEvent(event);

        koth.setLastWinner(capInfo.getCapper());
        if (event.isCreatingChest()) {
            Bukkit.getScheduler().runTask(KothPlugin.getPlugin(), new Runnable() {
                public void run() {
                    koth.triggerLoot(lootAmount, lootChest);
                }
            });
        }
        
        
        final KothClassic thisObj = this;
        Bukkit.getScheduler().runTask(KothPlugin.getPlugin(), new Runnable() {
            @SuppressWarnings("deprecation")
            public void run() {
                KothHandler.getInstance().remove(thisObj);
            }
        });
    }

    @Deprecated
    public void update() {
        timeRunning++;
        timeNotCapped++;
        
        
        // Handling capture cooldown
        if (captureCooldown > 0) {
            captureCooldown--;
            return;
        }
        // Capture info update and cooldown initiator
        CapStatus status = capInfo.update();
        
        if(prevStatus == CapStatus.CAPPING && status == CapStatus.EMPTY){
            captureCooldown = ConfigHandler.getInstance().getKoth().getCaptureCooldown();
        }
        prevStatus = status;
        
        if(status == CapStatus.CHANNELING) prevStatus = CapStatus.EMPTY;
        if(status == CapStatus.KNOCKED) prevStatus = CapStatus.CAPPING;
        ////////

        
        if (capInfo.getCapper() != null) {
            timeNotCapped = 0;
            if (capInfo.getTimeCapped() < captureTime) {
                if (capInfo.getTimeCapped() % 30 == 0 && capInfo.getTimeCapped() != 0) {
                    new MessageBuilder(Lang.KOTH_PLAYING_CAPTIME).maxTime(maxRunTime).time(getTimeObject()).capper(capInfo.getCapper().getName()).koth(koth).exclude(capInfo.getCapper()).buildAndBroadcast();
                    new MessageBuilder(Lang.KOTH_PLAYING_CAPTIME_CAPPER).maxTime(maxRunTime).time(getTimeObject()).capper(capInfo.getCapper().getName()).koth(koth).buildAndSend(capInfo.getCapper());
                }
            } else {
                endKoth(EndReason.WON);
            }
            return;
        }
        
        if(ConfigHandler.getInstance().getGlobal().getNoCapBroadcastInterval() != 0 && timeNotCapped % ConfigHandler.getInstance().getGlobal().getNoCapBroadcastInterval() == 0) {
            new MessageBuilder(Lang.KOTH_PLAYING_NOT_CAPPING).maxTime(maxRunTime).time(getTimeObject()).koth(koth).buildAndBroadcast();
        }

        if (maxRunTime > 0 && timeRunning > maxRunTime) {
            endKoth(EndReason.TIMEUP);
            return;
        }
    }
    
    public MessageBuilder fillMessageBuilder(MessageBuilder mB){
        return mB.maxTime(maxRunTime).time(getTimeObject()).capper(getCapInfo().getName()).koth(koth);
    }
    
    public String getType(){
        return "classic";
    }
    
    @SuppressWarnings("unchecked")
    public JSONObject save(){
        JSONObject obj = new JSONObject();
        obj.put("koth", koth.getName());
        obj.put("capperType", KothHandler.getInstance().getCapEntityRegistry().getIdentifierFromClass(capInfo.getOfType()));
        obj.put("capperTime", capInfo.getTimeCapped());
        if(capInfo.getCapper() != null){
            obj.put("capperEntity", capInfo.getCapper().getUniqueObjectIdentifier());
        }
        
        obj.put("captureTime", this.captureTime);
        obj.put("lootChest", this.lootChest);
        obj.put("lootAmount", this.lootAmount);
        obj.put("captureCooldown", this.captureCooldown);
        obj.put("maxRunTime", this.maxRunTime);
        obj.put("timeRunning", this.timeRunning);

        return obj;
    }
    
    public KothClassic load(JSONObject obj){
        this.koth = KothHandler.getInstance().getKoth((String)obj.get("koth"));
        this.capInfo = new CapInfo(this, this.koth, KothHandler.getInstance().getCapEntityRegistry().getCaptureClass((String)obj.get("capperType")));
        this.capInfo.setTimeCapped((int) (long) obj.get("capperTime"));
        if(obj.containsKey("capperEntity")){
            this.capInfo.setCapper(KothHandler.getInstance().getCapEntityRegistry().getCapperFromType((String)obj.get("capperType"), (String)obj.get("capperEntity")));
        }
        
        this.captureTime = (int) (long) obj.get("captureTime");
        this.lootChest = (String) obj.get("lootChest");
        this.lootAmount = (int) (long) obj.get("lootAmount");
        this.captureCooldown = (int) (long) obj.get("captureCooldown");
        this.maxRunTime = (int) (long) obj.get("maxRunTime");
        this.timeRunning = (int) (long) obj.get("timeRunning");
        
        return this;
    }
}
