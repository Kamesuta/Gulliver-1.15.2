package net.teamfruit.gulliver.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.Pose;
import net.minecraftforge.event.entity.EntityEvent;

public class EntitySizeEvent extends EntityEvent {
    private final Pose pose;
    private final EntitySize oldSize;
    private EntitySize newSize;

    public EntitySizeEvent(Entity entity, Pose pose, EntitySize defaultSize) {
        super(entity);
        this.pose = pose;
        this.oldSize = defaultSize;
        this.newSize = defaultSize;
    }

    public Pose getPose() {
        return pose;
    }

    public EntitySize getOldSize() {
        return oldSize;
    }

    public EntitySize getNewSize() {
        return newSize;
    }

    public void setNewSize(EntitySize newSize) {
        this.newSize = newSize;
    }
}
