package com.camellias.gulliverreborn;

import com.artemis.artemislib.compatibilities.sizeCap.ISizeCap;
import com.artemis.artemislib.compatibilities.sizeCap.SizeCapPro;
import com.artemis.artemislib.util.attributes.ArtemisLibAttributes;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.capabilities.morphing.Morphing;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Method;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.UUID;

@Mod(
        modid = GulliverReborn.MODID,
        name = GulliverReborn.NAME,
        version = GulliverReborn.VERSION,
        acceptedMinecraftVersions = GulliverReborn.MCVERSION,
        dependencies = GulliverReborn.DEPENDENCIES)
public class GulliverReborn {
    public static final String MODID = "gulliverreborn";
    public static final String NAME = "Gulliver Reborn";
    public static final String VERSION = "1.10";
    public static final String MCVERSION = "1.12.2";
    public static final String DEPENDENCIES = "required-after:forge@[14.23.5.2795,];" + "required-after:artemislib@[1.0.6,];";
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static File config;

    public static DamageSource causeCrushingDamage(EntityLivingBase entity) {
        return new EntityDamageSource(MODID + ".crushing", entity);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.registerConfig(event);
        MinecraftForge.EVENT_BUS.register(new GulliverReborn());
    }

    @EventHandler
    public void serverRegistries(FMLServerStartingEvent event) {
        event.registerServerCommand(new MyResizeCommand());
        event.registerServerCommand(new OthersResizeCommand());
    }

    @SubscribeEvent
    public void onPlayerFall(LivingFallEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            if (Config.SCALED_FALL_DAMAGE) event.setDistance(event.getDistance() / (player.height * 0.6F));
            if (player.height < 0.45F) event.setDistance(0);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = event.getEntityLiving().world;

        for (EntityLivingBase entities : world.getEntitiesWithinAABB(EntityLivingBase.class, entity.getEntityBoundingBox())) {
            if (!entity.isSneaking() && Config.GIANTS_CRUSH_ENTITIES) {
                if (entity.height / entities.height >= 4 && entities.getRidingEntity() != entity) {
                    entities.attackEntityFrom(causeCrushingDamage(entity), entity.height - entities.height);
                }
            }
        }
    }

    @SubscribeEvent
    public void onTargetEntity(LivingSetAttackTargetEvent event) {
        if (event.getTarget() instanceof EntityPlayer && event.getEntityLiving() instanceof EntityLiving && Config.SMALL_IS_INVISIBLE_TO_NONCATS_OR_NONSPIDERS) {
            EntityPlayer player = (EntityPlayer) event.getTarget();
            EntityLiving entity = (EntityLiving) event.getEntityLiving();

            if (!(entity instanceof EntitySpider || entity instanceof EntityOcelot)) {
                if (player.height <= 0.45F) {
                    entity.setAttackTarget(null);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        EntityPlayer player = event.player;
        World world = event.player.world;

        player.stepHeight = player.height / 3F;
        player.jumpMovementFactor *= (player.height / 1.8F);

        if (player.height < 0.9F) {
            BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            float ratio = (player.height / 1.8F) / 2;

            if (block instanceof BlockRedFlower
                    || state == Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.ROSE)
                    && Config.ROSES_HURT) {
                player.attackEntityFrom(DamageSource.CACTUS, 1);
            }

            if (!player.capabilities.isFlying
                    && Config.PLANTS_SLOW_SMALL_DOWN
                    && (block instanceof BlockBush)
                    || (block instanceof BlockCarpet)
                    || (block instanceof BlockFlower)
                    || (block instanceof BlockReed)
                    || (block instanceof BlockSnow)
                    || (block instanceof BlockWeb)
                    || (block instanceof BlockSoulSand)) {
                player.motionX *= ratio;
                if (block instanceof BlockWeb) player.motionY *= ratio;
                player.motionZ *= ratio;
            }
        }

        if (player.height <= 0.45F) {
            EnumFacing facing = player.getHorizontalFacing();
            BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
            IBlockState state = world.getBlockState(pos.add(0, 0, 0).offset(facing));
            Block block = state.getBlock();
            boolean canPass = block.isPassable(world, pos.offset(facing));

            if (ClimbingHandler.canClimb(player, facing)
                    && Config.CLIMB_SOME_BLOCKS
                    && (block instanceof BlockDirt)
                    || (block instanceof BlockGrass)
                    || (block instanceof BlockMycelium)
                    || (block instanceof BlockLeaves)
                    || (block instanceof BlockSand)
                    || (block instanceof BlockSoulSand)
                    || (block instanceof BlockConcretePowder)
                    || (block instanceof BlockFarmland)
                    || (block instanceof BlockGrassPath)
                    || (block instanceof BlockGravel)
                    || (block instanceof BlockClay)) {
                if (player.collidedHorizontally) {
                    if (!player.isSneaking()) {
                        player.motionY = 0.1D;
                    }

                    if (player.isSneaking()) {
                        player.motionY = 0.0D;
                    }
                }
            }

            for (ItemStack stack : player.getHeldEquipment()) {
                if (stack.getItem() == Items.SLIME_BALL || stack.getItem() == Item.getItemFromBlock(Blocks.SLIME_BLOCK) && Config.CLIMB_WITH_SLIME) {
                    if (ClimbingHandler.canClimb(player, facing)) {
                        if (player.collidedHorizontally) {
                            if (!player.isSneaking()) {
                                player.motionY = 0.1D;
                            }

                            if (player.isSneaking()) {
                                player.motionY = 0.0D;
                            }
                        }
                    }
                }

                if (stack.getItem() == Items.PAPER && Config.GLIDE_WITH_PAPER) {
                    if (!player.onGround) {
                        player.jumpMovementFactor = 0.02F * 1.75F;
                        player.fallDistance = 0;

                        if (player.motionY < 0D) {
                            player.motionY *= 0.6D;
                        }

                        if (player.isSneaking()) {
                            player.jumpMovementFactor *= 3.50F;
                        }

                        for (double blockY = player.posY; !player.isSneaking() &&
                                ((world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.AIR) ||
                                        (world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.LAVA) ||
                                        (world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.FIRE) ||
                                        (world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.LIT_FURNACE) ||
                                        (world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.MAGMA)) &&
                                player.posY - blockY < 25;
                             blockY--) {
                            if ((world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.LAVA) ||
                                    (world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.FIRE) ||
                                    (world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.LIT_FURNACE) ||
                                    (world.getBlockState(new BlockPos(player.posX, blockY, player.posZ)).getBlock() == Blocks.MAGMA) &&
                                            Config.HOT_BLOCKS_GIVE_LIFT) {
                                player.motionY += MathHelper.clamp(0.07D, Double.MIN_VALUE, 0.1D);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityInteract(EntityInteract event) {
        if (event.getTarget() instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) event.getTarget();
            EntityPlayer player = event.getEntityPlayer();

            if (target.height / 2 >= player.height && Config.RIDE_BIG_ENTITIES) {
                for (ItemStack stack : player.getHeldEquipment()) {
                    if (stack.getItem() == Items.STRING) {
                        player.startRiding(target);
                    }
                }
            }

            if (target.height * 2 <= player.height && Config.PICKUP_SMALL_ENTITIES) {
                target.startRiding(player);
            }

            if (player.getHeldItemMainhand().isEmpty() && player.isBeingRidden() && player.isSneaking()) {
                for (Entity entities : player.getPassengers()) {
                    if (entities instanceof EntityLivingBase) {
                        entities.dismountRidingEntity();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJump(LivingJumpEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer && Config.JUMP_MODIFIER) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            float jumpHeight = (player.height / 1.8F);

            jumpHeight = MathHelper.clamp(jumpHeight, 0.65F, jumpHeight);
            player.motionY *= jumpHeight;

            if (player.isSneaking() || player.isSprinting()) {
                if (player.height < 1.8F) player.motionY = 0.42F;
            }
        }
    }

    @SubscribeEvent
    public void onHarvest(BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();

        if (Config.HARVEST_MODIFIER) event.setNewSpeed(event.getOriginalSpeed() * (player.height / 1.8F));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onFOVChange(FOVUpdateEvent event) {
        if (event.getEntity() != null) {
            EntityPlayer player = event.getEntity();
            GameSettings settings = Minecraft.getMinecraft().gameSettings;
            PotionEffect speed = player.getActivePotionEffect(MobEffects.SPEED);
            float fov = settings.fovSetting / settings.fovSetting;

            if (player.isSprinting()) {
                event.setNewfov(speed != null ? fov + ((0.1F * (speed.getAmplifier() + 1)) + 0.15F) : fov + 0.1F);
            } else {
                event.setNewfov(speed != null ? fov + (0.1F * (speed.getAmplifier() + 1)) : fov);
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onCameraSetup(CameraSetup event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        float scale = player.height / 1.8F;

        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
            if (player.height > 1.8F) GL11.glTranslatef(0, 0, -scale * 2);
        }

        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            if (player.height > 1.8F) GL11.glTranslatef(0, 0, scale * 2);
        }
    }

    private static UUID uuidHeight = UUID.fromString("5440b01a-974f-4495-bb9a-c7c87424bca4");
    private static UUID uuidWidth = UUID.fromString("3949d2ed-b6cc-4330-9c13-98777f48ea51");
    private static UUID uuidReach1 = UUID.fromString("854e0004-c218-406c-a9e2-590f1846d80b");
    private static UUID uuidReach2 = UUID.fromString("216080dc-22d3-4eff-a730-190ec0210d5c");
    private static UUID uuidHealth = UUID.fromString("3b901d47-2d30-495c-be45-f0091c0f6fb2");
    private static UUID uuidStrength = UUID.fromString("558f55be-b277-4091-ae9b-056c7bc96e84");
    private static UUID uuidSpeed = UUID.fromString("f2fb5cda-3fbe-4509-a0af-4fc994e6aeca");

    @Method(modid = "metamorph")
    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.hasCapability(SizeCapPro.sizeCapability, (EnumFacing) null)) {
            ISizeCap cap = (ISizeCap) entity.getCapability(SizeCapPro.sizeCapability, (EnumFacing) null);

            IMorphing imorph = Morphing.get(event.getEntityPlayer());
            if (!(imorph instanceof Morphing))
                return;
            Morphing morph = (Morphing) imorph;
            AbstractMorph amorph = morph.getCurrentMorph();
            if (!(amorph instanceof EntityMorph))
                return;
            EntityMorph emorph = (EntityMorph) amorph;
            Entity morphentity = emorph.getEntity();
            if (!(morphentity instanceof EntityLivingBase))
                return;
            EntityLivingBase living = (EntityLivingBase) morphentity;

            {
                Multimap<String, AttributeModifier> attributes = HashMultimap.create();
                attributes.put(ArtemisLibAttributes.ENTITY_HEIGHT.getName(), entity.getEntityAttribute(ArtemisLibAttributes.ENTITY_HEIGHT).getModifier(uuidHeight));
                attributes.put(ArtemisLibAttributes.ENTITY_WIDTH.getName(), entity.getEntityAttribute(ArtemisLibAttributes.ENTITY_WIDTH).getModifier(uuidWidth));
                attributes.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getModifier(uuidSpeed));
                attributes.put(EntityPlayer.REACH_DISTANCE.getName(), entity.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getModifier(uuidReach1));
                attributes.put(EntityPlayer.REACH_DISTANCE.getName(), entity.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getModifier(uuidReach2));
                attributes.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getModifier(uuidStrength));
                attributes.put(SharedMonsterAttributes.MAX_HEALTH.getName(), entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getModifier(uuidHealth));
                attributes.values().removeIf(Predicates.isNull());
                living.getAttributeMap().applyAttributeModifiers(attributes);
            }

            if (living.hasCapability(SizeCapPro.sizeCapability, (EnumFacing) null)) {
                ISizeCap cap2 = (ISizeCap) living.getCapability(SizeCapPro.sizeCapability, (EnumFacing) null);
                cap2.setTrans(cap.getTrans());
                cap2.setDefaultWidth(cap.getDefaultWidth());
                cap2.setDefaultHeight(cap.getDefaultHeight());
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onEntityRenderPre(RenderLivingEvent.Pre event) {
        if (Config.DO_ADJUSTED_RENDER) {
            final EntityLivingBase entity = event.getEntity();

            if (entity.hasCapability(SizeCapPro.sizeCapability, null)) {
                final ISizeCap cap = entity.getCapability(SizeCapPro.sizeCapability, null);

                if (cap.getTrans() == true) {
                    float scale = entity.height / cap.getDefaultHeight();

                    if (scale < 0.4F) {
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(scale * 2.5F, 1, scale * 2.5F);
                        GlStateManager.translate(event.getX() / scale * 2.5F - event.getX(),
                                event.getY() / scale * 2.5F - event.getY(), event.getZ() / scale * 2.5F - event.getZ());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onLivingRenderPost(RenderLivingEvent.Post event) {
        if (Config.DO_ADJUSTED_RENDER) {
            final EntityLivingBase entity = event.getEntity();

            if (entity.hasCapability(SizeCapPro.sizeCapability, null)) {
                final ISizeCap cap = entity.getCapability(SizeCapPro.sizeCapability, null);

                if (cap.getTrans() == true) {
                    float scale = entity.height / cap.getDefaultHeight();

                    if (scale < 0.4F) {
                        GlStateManager.popMatrix();
                    }
                }
            }
        }
    }
}
