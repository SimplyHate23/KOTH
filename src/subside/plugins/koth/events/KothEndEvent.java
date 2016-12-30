package subside.plugins.koth.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.Setter;
import subside.plugins.koth.adapter.Koth;
import subside.plugins.koth.adapter.RunningKoth;
import subside.plugins.koth.adapter.RunningKoth.EndReason;
import subside.plugins.koth.adapter.captypes.Capper;

/**
 * @author Thomas "SubSide" van den Bulk
 *
 */
public class KothEndEvent extends Event implements IEvent {
    private @Getter Capper winner;
    private @Getter @Setter boolean triggerLoot;
    private @Getter EndReason reason;
    private @Getter RunningKoth runningKoth;
    
    
    public KothEndEvent(RunningKoth koth, Capper capper, EndReason reason){
        this.runningKoth = koth;
        this.winner = capper;
        this.triggerLoot = true;
        this.reason = reason;
    }
    
    public Koth getKoth(){
        return runningKoth.getKoth();
    }
    
    @Deprecated
    public void setCreatingChest(boolean creatingChest){
        this.triggerLoot = creatingChest;
    }
    
    @Deprecated
    public boolean isCreatingChest(){
        return this.triggerLoot;
    }

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
