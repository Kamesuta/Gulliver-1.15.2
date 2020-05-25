package com.artemis.artemislib.attributes;

import com.artemis.artemislib.compatibilities.sizeCap.ISizeCap;
import com.artemis.artemislib.compatibilities.sizeCap.SizeCapPro;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.teamfruit.gulliver.event.EntitySizeEvent;

public class AttributesHandler {
    @SubscribeEvent
    public void attachAttributes(EntityEvent.EntityConstructing event) {
        if (event.getEntity() instanceof LivingEntity) {
            final LivingEntity entity = (LivingEntity) event.getEntity();
            final AbstractAttributeMap map = entity.getAttributes();

            map.registerAttribute(Attributes.ENTITY_HEIGHT);
            map.registerAttribute(Attributes.ENTITY_WIDTH);
        }
    }

    @SubscribeEvent
    public void onEntityGetSize(EntitySizeEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;
        final LivingEntity entity = (LivingEntity) event.getEntity();

        LazyOptional<ISizeCap> lazyCap = entity.getCapability(SizeCapPro.sizeCapability);
        lazyCap.ifPresent(cap -> {
            final boolean hasHeightModifier = entity.getAttribute(Attributes.ENTITY_HEIGHT).func_225505_c_().isEmpty();
            final boolean hasWidthModifier = entity.getAttribute(Attributes.ENTITY_WIDTH).func_225505_c_().isEmpty();

            final double heightAttribute = entity.getAttribute(Attributes.ENTITY_HEIGHT).getValue();
            final double widthAttribute = entity.getAttribute(Attributes.ENTITY_WIDTH).getValue();

            final EntitySize oldSize = event.getOldSize();
            float height = (float) (oldSize.height * heightAttribute);
            float width = (float) (oldSize.width * widthAttribute);

            /* Makes Sure to only Run the Code IF the Entity Has Modifiers */
            if (hasHeightModifier != true || hasWidthModifier != true) {
                /* If the Entity Does have a Modifier get it's size before changing it's size */
                if (!cap.getTrans()) {
                    cap.setDefaultHeight(oldSize.height);
                    cap.setDefaultWidth(oldSize.width);
                    cap.setTrans(true);
                } else {
                    /* Handles Resizing while true */
                    width = MathHelper.clamp(width, 0.15F, width);
                    height = MathHelper.clamp(height, 0.25F, height);
                    event.setNewSize(EntitySize.flexible(width, height));
                }
            } else /* If the Entity Does not have any Modifiers */ {
                /* Returned the Entities Size Back to Normal */
                if (cap.getTrans()) {
                    event.setNewSize(oldSize);
                    cap.setTrans(false);
                }
            }
        });
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        final PlayerEntity player = event.player;
        LazyOptional<ISizeCap> lazyCap = player.getCapability(SizeCapPro.sizeCapability);
        lazyCap.ifPresent(cap -> {
            final boolean hasHeightModifier = player.getAttributes().getAttributeInstance(Attributes.ENTITY_HEIGHT).func_225505_c_().isEmpty();
            final boolean hasWidthModifier = player.getAttributes().getAttributeInstance(Attributes.ENTITY_WIDTH).func_225505_c_().isEmpty();

            final double heightAttribute = player.getAttributes().getAttributeInstance(Attributes.ENTITY_HEIGHT).getValue();
            final double widthAttribute = player.getAttributes().getAttributeInstance(Attributes.ENTITY_WIDTH).getValue();
            float height = (float) (cap.getDefaultHeight() * heightAttribute);
            float width = (float) (cap.getDefaultWidth() * widthAttribute);

            /* Makes Sure to only Run the Code IF the Entity Has Modifiers */
            if (hasHeightModifier != true || hasWidthModifier != true) {
                /* If the Entity Does have a Modifier get it's size before changing it's size */
                if (cap.getTrans() != true) {
                    cap.setDefaultHeight(1.8f);
                    cap.setDefaultWidth(0.6f);
                    cap.setTrans(true);
                }
                /* Handles Resizing while true */
                if (cap.getTrans() == true) {
                    float eyeHeight = (float) (player.getStandingEyeHeight(player.getPose(), player.getSize(player.getPose())) * heightAttribute);
                    if (player.isSneaking()) {
                        height *= 0.91666666666f;
                        eyeHeight *= 0.9382716f;
                    }
                    if (player.isElytraFlying()) {
                        height *= 0.33f;
                    }
                    if (player.isSleeping()) {
                        width = 0.2F;
                        height = 0.2F;
                    }
                    if (player.isPassenger()) {
                        //eyeHeight = (float) (player.getDefaultEyeHeight() * heightAttribute)*1.4f;
                        //height = height*1.4f;
                    }

                    width = MathHelper.clamp(width, 0.15F, width);
                    height = MathHelper.clamp(height, 0.25F, height);
                    if (height >= 1.6F) player.eyeHeight = eyeHeight;
                    else player.eyeHeight = (eyeHeight * 0.9876542F);
                    player.size = EntitySize.flexible(width, height);

                    final double d0 = width / 2.0D;
                    final AxisAlignedBB aabb = player.getBoundingBox();
                    player.setBoundingBox(new AxisAlignedBB(player.getPosX() - d0, aabb.minY, player.getPosZ() - d0,
                            player.getPosX() + d0, aabb.minY + player.getHeight(), player.getPosZ() + d0));
                }
            } else /* If the Entity Does not have any Modifiers */ {
                /* Returned the Entities Size Back to Normal */
                if (cap.getTrans() == true) {
                    player.size = EntitySize.flexible(width, height);
                    final double d0 = width / 2.0D;
                    final AxisAlignedBB aabb = player.getBoundingBox();
                    player.setBoundingBox(new AxisAlignedBB(player.getPosX() - d0, aabb.minY, player.getPosZ() - d0,
                            player.getPosX() + d0, aabb.minY + height, player.getPosZ() + d0));
                    player.eyeHeight = player.getSize(player.getPose()).height * 0.85F;
                    cap.setTrans(false);
                }
            }
        });
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {

        final LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof PlayerEntity)) {
            LazyOptional<ISizeCap> lazyCap = entity.getCapability(SizeCapPro.sizeCapability);
            lazyCap.ifPresent(cap -> {
                final boolean hasHeightModifier = entity.getAttributes().getAttributeInstance(Attributes.ENTITY_HEIGHT).func_225505_c_().isEmpty();
                final boolean hasWidthModifier = entity.getAttributes().getAttributeInstance(Attributes.ENTITY_WIDTH).func_225505_c_().isEmpty();
                final double heightAttribute = entity.getAttributes().getAttributeInstance(Attributes.ENTITY_HEIGHT).getValue();
                final double widthAttribute = entity.getAttributes().getAttributeInstance(Attributes.ENTITY_WIDTH).getValue();
                float height = (float) (cap.getDefaultHeight() * heightAttribute);
                float width = (float) (cap.getDefaultWidth() * widthAttribute);

                /* Makes Sure to only Run the Code IF the Entity Has Modifiers */
                if (hasHeightModifier != true || hasWidthModifier != true) {
                    /* If the Entity Does have a Modifier get it's size before changing it's size */
                    if (cap.getTrans() != true) {
                        cap.setDefaultHeight(entity.getHeight());
                        cap.setDefaultWidth(entity.getWidth());
                        cap.setTrans(true);
                    }

                    /* Handles Resizing while true */
                    if (cap.getTrans() == true) {
                        width = MathHelper.clamp(width, 0.04F, width);
                        height = MathHelper.clamp(height, 0.08F, height);
                        entity.size = EntitySize.flexible(width, height);

                        final double d0 = width / 2.0D;
                        final AxisAlignedBB aabb = entity.getBoundingBox();
                        entity.setBoundingBox(new AxisAlignedBB(entity.getPosX() - d0, aabb.minY, entity.getPosZ() - d0,
                                entity.getPosX() + d0, aabb.minY + entity.getHeight(), entity.getPosZ() + d0));
                    }
                } else /* If the Entity Does not have any Modifiers */ {
                    /* Returned the Entities Size Back to Normal */
                    if (cap.getTrans() == true) {
                        entity.size = EntitySize.flexible(width, height);
                        final double d0 = width / 2.0D;
                        final AxisAlignedBB aabb = entity.getBoundingBox();
                        entity.setBoundingBox(new AxisAlignedBB(entity.getPosX() - d0, aabb.minY, entity.getPosZ() - d0,
                                entity.getPosX() + d0, aabb.minY + height, entity.getPosZ() + d0));
                        cap.setTrans(false);
                    }
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onEntityRenderPre(RenderLivingEvent.Pre event) {
        final LivingEntity entity = event.getEntity();

        LazyOptional<ISizeCap> lazyCap = entity.getCapability(SizeCapPro.sizeCapability);
        lazyCap.ifPresent(cap -> {
            if (cap.getTrans() == true) {
                float scaleHeight = entity.getHeight() / cap.getDefaultHeight();
                float scaleWidth = entity.getWidth() / cap.getDefaultWidth();

                if (entity instanceof PlayerEntity) {
                    if (entity.getRidingEntity() instanceof AbstractHorseEntity) {
                        event.getMatrixStack().translate(0, (1-scaleHeight) * .62F, 0);
                    }
                }

                event.getMatrixStack().push();
                event.getMatrixStack().scale(scaleWidth, scaleHeight, scaleWidth);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onLivingRenderPost(RenderLivingEvent.Post event) {
        final LivingEntity entity = event.getEntity();

        LazyOptional<ISizeCap> lazyCap = entity.getCapability(SizeCapPro.sizeCapability);
        lazyCap.ifPresent(cap -> {
            if (cap.getTrans() == true) {
                event.getMatrixStack().pop();
            }
        });
    }
}
