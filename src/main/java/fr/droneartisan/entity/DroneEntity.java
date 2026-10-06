package fr.droneartisan.entity;

import fr.droneartisan.item.DroneRemoteItem;
import fr.droneartisan.menu.DroneBaseMenu;
import fr.droneartisan.menu.DroneControlMenu;
import fr.droneartisan.network.DronePilotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import java.util.Comparator;
import java.util.UUID;

public final class DroneEntity extends PathfinderMob {
    private static final int MAX_BATTERY = 24_000;
    private static final float MAX_HEALTH = 40.0F;
    private static final float LASER_DAMAGE = 6.0F;
    private static final TagKey<Block> MINEABLE_BLOCKS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("droneartisan", "drone_mineable"));
    private static final EntityDataAccessor<Integer> SYNC_BATTERY =
            SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SYNC_TASK =
            SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SYNC_OWNER =
            SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    private final SimpleContainer cargo = new SimpleContainer(9);
    private UUID ownerId;
    private UUID pilotControllerId;
    private DroneTask task = DroneTask.FOLLOW;
    private int battery = MAX_BATTERY;
    private BlockPos homePos;
    private int taskCooldown;
    private BlockPos lastLocatedOre;
    private Vec3 pilotAnchor;
    private int pilotInputExpiry;
    private int pilotActionCooldown;
    private int lastPilotMineTick = -1;
    private int laserCooldown;
    private float pilotForward;
    private float pilotStrafe;
    private float pilotVertical;
    private boolean pilotSprinting;
    private BlockPos pilotMineTarget;
    private float pilotMineProgress;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 12, true);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DroneRegistry.DRONE.get(), createAttributes().build());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FLYING_SPEED, 0.65)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SYNC_BATTERY, MAX_BATTERY);
        builder.define(SYNC_TASK, DroneTask.FOLLOW.ordinal());
        builder.define(SYNC_OWNER, "");
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) return;
        if (laserCooldown > 0) laserCooldown--;
        if (pilotActionCooldown > 0) pilotActionCooldown--;
        if (pilotInputExpiry > tickCount) {
            Player controller = pilotControllerId == null ? null : serverLevel.getPlayerByUUID(pilotControllerId);
            if (controller != null && pilotAnchor != null) {
                controller.setPos(pilotAnchor.x, pilotAnchor.y, pilotAnchor.z);
                controller.setDeltaMovement(Vec3.ZERO);
                controller.fallDistance = 0.0F;
            }
            if (tickCount % 20 == 0) {
                if (homePos != null && distanceToSqr(Vec3.atCenterOf(homePos)) <= 36.0) {
                    battery = Math.min(MAX_BATTERY, battery + 20);
                } else {
                    battery = Math.max(0, battery - 1);
                }
                entityData.set(SYNC_BATTERY, battery);
            }
            applyPilotMovement();
            return;
        }
        clearPilotMineProgress(serverLevel);
        pilotForward = 0.0F;
        pilotStrafe = 0.0F;
        pilotVertical = 0.0F;
        pilotSprinting = false;
        pilotControllerId = null;
        pilotAnchor = null;
        if (tickCount % 20 == 0 && battery > 0) {
            if (homePos != null && distanceToSqr(Vec3.atCenterOf(homePos)) <= 36.0) {
                battery = Math.min(MAX_BATTERY, battery + 20);
            } else {
                battery--;
            }
            entityData.set(SYNC_BATTERY, battery);
        } else if (tickCount % 20 == 0 && battery == 0 && homePos != null
                && distanceToSqr(Vec3.atCenterOf(homePos)) <= 36.0) {
            battery = 20;
            entityData.set(SYNC_BATTERY, battery);
        }
        if (battery <= 0) {
            getNavigation().stop();
            return;
        }
        Player owner = getOwnerPlayer();
        if (taskCooldown > 0) taskCooldown--;
        if (task == DroneTask.FOLLOW && owner != null && owner.isAlive()
                && distanceToSqr(owner) > 9.0 && tickCount % 10 == 0) {
            getNavigation().moveTo(owner, 1.1);
        } else if (task == DroneTask.RETURN_BASE && homePos != null) {
            if (distanceToSqr(Vec3.atCenterOf(homePos)) > 6.0) {
                if (tickCount % 10 == 0) getNavigation().moveTo(
                        homePos.getX() + 0.5, homePos.getY() + 1.5, homePos.getZ() + 0.5, 1.1);
            } else {
                getNavigation().stop();
            }
        } else if (task == DroneTask.STAY) {
            getNavigation().stop();
        }
        if (task == DroneTask.COLLECT && tickCount % 5 == 0) collectItems();
        if (task == DroneTask.HARVEST && tickCount % 40 == 0) harvestCrop(serverLevel);
        if (task == DroneTask.MINE && tickCount % 30 == 0) mineNearbyBlock(serverLevel);
        if (task == DroneTask.LOCATE_ORE && tickCount % 40 == 0) locateNearestOre(serverLevel, owner);
        if (task == DroneTask.DEFEND) attackNearbyHostile(serverLevel);
    }

    private void mineNearbyBlock(ServerLevel serverLevel) {
        if (!serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) return;
        Player owner = getOwnerPlayer();
        if (owner == null) return;
        BlockPos target = BlockPos.betweenClosedStream(blockPosition().offset(-4, -3, -4),
                        blockPosition().offset(4, 3, 4))
                .filter(pos -> serverLevel.hasChunkAt(pos))
                .filter(pos -> serverLevel.mayInteract(owner, pos))
                .filter(pos -> {
                    BlockState state = serverLevel.getBlockState(pos);
                    return state.is(MINEABLE_BLOCKS) && !state.isAir()
                            && serverLevel.getBlockEntity(pos) == null
                            && state.getDestroySpeed(serverLevel, pos) >= 0.0F;
                })
                .min(Comparator.comparingDouble(pos -> distanceToSqr(Vec3.atCenterOf(pos))))
                .map(BlockPos::immutable).orElse(null);
        if (target != null && serverLevel.destroyBlock(target, true, this)) drainBattery(80);
    }

    private void locateNearestOre(ServerLevel serverLevel, Player owner) {
        if (owner == null || !owner.isAlive() || taskCooldown > 0) return;
        BlockPos target = BlockPos.betweenClosedStream(blockPosition().offset(-12, -8, -12),
                        blockPosition().offset(12, 8, 12))
                .filter(pos -> serverLevel.hasChunkAt(pos))
                .filter(pos -> serverLevel.getBlockState(pos).is(MINEABLE_BLOCKS))
                .min(Comparator.comparingDouble(pos -> distanceToSqr(Vec3.atCenterOf(pos))))
                .map(BlockPos::immutable).orElse(null);
        if (target != null && !target.equals(lastLocatedOre)) {
            lastLocatedOre = target;
            owner.displayClientMessage(Component.translatable("message.droneartisan.ore_found",
                    target.getX(), target.getY(), target.getZ()), false);
            drainBattery(10);
            taskCooldown = 100;
        }
    }

    private void attackNearbyHostile(ServerLevel serverLevel) {
        if (taskCooldown > 0) return;
        Monster target = serverLevel.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(10.0),
                        monster -> monster.isAlive() && monster.distanceToSqr(this) <= 100.0)
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target != null) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            target.hurt(serverLevel.damageSources().mobAttack(this), 4.0F);
            drainBattery(30);
            taskCooldown = 20;
        }
    }

    private void collectItems() {
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(2.0))) {
            ItemStack remaining = cargo.addItem(item.getItem().copy());
            if (remaining.isEmpty()) item.discard();
            else if (remaining.getCount() < item.getItem().getCount()) item.setItem(remaining);
        }
    }

    private void harvestCrop(ServerLevel serverLevel) {
        if (!serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) return;
        Player owner = getOwnerPlayer();
        if (owner == null) return;
        BlockPos crop = BlockPos.betweenClosedStream(blockPosition().offset(-3, -2, -3),
                        blockPosition().offset(3, 2, 3))
                .filter(pos -> serverLevel.hasChunkAt(pos) && serverLevel.mayInteract(owner, pos))
                .filter(pos -> isMatureCrop(serverLevel.getBlockState(pos)))
                .min(Comparator.comparingDouble(pos -> distanceToSqr(Vec3.atCenterOf(pos))))
                .map(BlockPos::immutable).orElse(null);
        if (crop == null) return;
        BlockState state = serverLevel.getBlockState(crop);
        IntegerProperty age = ageProperty(state.getBlock());
        if (age == null) return;
        BlockState replanted = state.setValue(age, age.getPossibleValues().stream().mapToInt(Integer::intValue).min()
                .orElse(0));
        if (serverLevel.destroyBlock(crop, true, this) && replanted.canSurvive(serverLevel, crop)) {
            serverLevel.setBlockAndUpdate(crop, replanted);
            drainBattery(20);
        }
    }

    private static boolean isMatureCrop(BlockState state) {
        if (state.getBlock() != Blocks.WHEAT && state.getBlock() != Blocks.CARROTS
                && state.getBlock() != Blocks.POTATOES && state.getBlock() != Blocks.BEETROOTS
                && state.getBlock() != Blocks.NETHER_WART) return false;
        IntegerProperty age = ageProperty(state.getBlock());
        return age != null && state.getValue(age).equals(
                age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0));
    }

    private static IntegerProperty ageProperty(Block block) {
        var property = block.getStateDefinition().getProperty("age");
        return property instanceof IntegerProperty age ? age : null;
    }

    public boolean placeBlock(Player player, BlockPos supportPos, Direction face, ItemStack tool) {
        if (level().isClientSide || !isOwnedBy(player) || battery < 40
                || (!isBeingPilotedBy(player) && distanceToSqr(player) > 64.0)
                || (!isBeingPilotedBy(player) && player.distanceToSqr(Vec3.atCenterOf(supportPos)) > 36.0)
                || distanceToSqr(Vec3.atCenterOf(supportPos)) > 36.0) return false;
        BlockPos targetPos = supportPos.relative(face);
        if (!level().hasChunkAt(targetPos) || !level().getBlockState(supportPos).isSolid()
                || !level().getBlockState(targetPos).isAir()
                || level().getBlockEntity(targetPos) != null
                || !level().mayInteract(player, supportPos)
                || !level().mayInteract(player, targetPos)) return false;
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(supportPos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5),
                face, supportPos, false);
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            ItemStack stack = cargo.getItem(slot);
            if (!(stack.getItem() instanceof BlockItem blockItem)
                    || blockItem.getBlock() instanceof EntityBlock) continue;
            BlockPlaceContext context = new BlockPlaceContext(level(), player, InteractionHand.MAIN_HAND, stack, hit);
            if (!context.getClickedPos().equals(targetPos)
                    || !player.mayUseItemAt(supportPos, face, stack)
                    || !level().getBlockState(targetPos).canBeReplaced(context)) continue;
            if (blockItem.place(context).consumesAction()) {
                cargo.setChanged();
                drainBattery(40);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!isOwnedBy(player)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof DroneRemoteItem remote) {
            remote.openControl(player, this);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new DroneControlMenu.Provider(this), buffer -> buffer.writeVarInt(getId()));
            return InteractionResult.CONSUME;
        }
        if (held.is(Items.IRON_INGOT) && getHealth() < getMaxHealth()) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            heal(10.0F);
            player.displayClientMessage(Component.translatable("message.droneartisan.repaired"), true);
            return InteractionResult.CONSUME;
        }
        if (held.is(Items.REDSTONE) && battery < MAX_BATTERY) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            battery = Math.min(MAX_BATTERY, battery + 4_000);
            entityData.set(SYNC_BATTERY, battery);
            player.displayClientMessage(Component.translatable("message.droneartisan.charged"), true);
            return InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof BlockItem && !(held.getItem() instanceof DroneRemoteItem)) {
            ItemStack deposited = held.copyWithCount(1);
            if (!cargo.addItem(deposited).isEmpty()) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild) held.shrink(1);
            cargo.setChanged();
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
                ItemStack removed = cargo.removeItem(slot, 1);
                if (!removed.isEmpty()) {
                    if (!player.getInventory().add(removed)) player.drop(removed, false);
                    return InteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public static DroneEntity findOwned(Player player) {
        return player.level().getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(64.0),
                        drone -> drone.isOwnedBy(player) && drone.distanceToSqr(player) <= 64.0 * 64.0)
                .stream().min(Comparator.comparingDouble(drone -> drone.distanceToSqr(player))).orElse(null);
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
        entityData.set(SYNC_OWNER, ownerId == null ? "" : ownerId.toString());
    }

    public void setHomePos(BlockPos homePos) {
        this.homePos = homePos == null ? null : homePos.immutable();
    }

    public boolean isBasedAt(BlockPos pos) {
        return homePos != null && homePos.equals(pos);
    }

    public boolean isPilotConsoleOpen(Player player) {
        if (pilotInputExpiry > tickCount && player.getUUID().equals(pilotControllerId)
                && isOwnedBy(player)) return true;
        if (player.getMainHandItem().getItem() instanceof DroneRemoteItem
                || player.getOffhandItem().getItem() instanceof DroneRemoteItem) return true;
        if (player.containerMenu instanceof DroneControlMenu menu && menu.stillValid(player)) return true;
        return player.containerMenu instanceof DroneBaseMenu menu
                && menu.stillValid(player) && isBasedAt(menu.basePos());
    }

    public void applyPilotInput(Player player, DronePilotPayload input) {
        if (level().isClientSide || !isOwnedBy(player) || !input.hasValidControls()) return;
        if (pilotInputExpiry <= tickCount) {
            pilotControllerId = player.getUUID();
            pilotAnchor = player.position();
        } else if (!player.getUUID().equals(pilotControllerId)) {
            return;
        }
        pilotForward = Mth.clamp(input.forward(), -1.0F, 1.0F);
        pilotStrafe = Mth.clamp(input.strafe(), -1.0F, 1.0F);
        pilotVertical = Mth.clamp(input.vertical(), -1.0F, 1.0F);
        pilotSprinting = input.sprinting();
        setYRot(Mth.wrapDegrees(input.yaw()));
        setYHeadRot(getYRot());
        setXRot(Mth.clamp(input.pitch(), -90.0F, 90.0F));
        pilotInputExpiry = tickCount + 5;
        if (input.action() != DronePilotPayload.NONE) {
            performPilotAction(player, input);
        } else if (level() instanceof ServerLevel serverLevel) {
            clearPilotMineProgress(serverLevel);
        }
    }

    public boolean isBeingPilotedBy(Player player) {
        return pilotInputExpiry > tickCount && player.getUUID().equals(pilotControllerId)
                && isOwnedBy(player);
    }

    private void applyPilotMovement() {
        getNavigation().stop();
        if (battery <= 0) return;
        float yawRadians = getYRot() * ((float) Math.PI / 180.0F);
        float pitchRadians = getXRot() * ((float) Math.PI / 180.0F);
        double forwardSpeed = pilotForward * Math.cos(pitchRadians);
        double x = -Math.sin(yawRadians) * forwardSpeed + Math.cos(yawRadians) * pilotStrafe;
        double z = Math.cos(yawRadians) * forwardSpeed + Math.sin(yawRadians) * pilotStrafe;
        double y = pilotVertical - pilotForward * Math.sin(pitchRadians);
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length > 1.0) {
            x /= length;
            y /= length;
            z /= length;
        }
        double speed = pilotSprinting ? 0.2 : 0.1;
        move(MoverType.SELF, new Vec3(x * speed, y * speed, z * speed));
        setDeltaMovement(Vec3.ZERO);
    }

    private void performPilotAction(Player player, DronePilotPayload input) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (input.action() == DronePilotPayload.STOP_PILOT) {
            clearPilotMineProgress(serverLevel);
            pilotInputExpiry = tickCount;
            pilotControllerId = null;
            pilotAnchor = null;
            pilotForward = 0.0F;
            pilotStrafe = 0.0F;
            pilotVertical = 0.0F;
            pilotSprinting = false;
            return;
        }
        if (input.action() == DronePilotPayload.START_PILOT) return;
        if (battery <= 0) return;
        if (input.action() == DronePilotPayload.LASER) {
            clearPilotMineProgress(serverLevel);
            fireLaser(serverLevel, player, input.entityId());
            return;
        }
        if (input.action() != DronePilotPayload.MINE) clearPilotMineProgress(serverLevel);
        if (pilotActionCooldown > 0) return;
        if (input.action() == DronePilotPayload.INTERACT_ENTITY) {
            interactWithEntity(serverLevel, player, input.entityId());
            pilotActionCooldown = 4;
            return;
        }
        BlockPos pos = input.blockPos();
        Direction face = Direction.from3DDataValue(input.face());
        if (!serverLevel.hasChunkAt(pos) || distanceToSqr(Vec3.atCenterOf(pos)) > 36.0
                || !serverLevel.mayInteract(player, pos)
                || !hasPilotLineOfSight(serverLevel, pos, face)) return;
        if (input.action() == DronePilotPayload.MINE) {
            minePilotBlock(serverLevel, player, pos);
            return;
        }
        if (input.action() != DronePilotPayload.INTERACT_BLOCK) return;
        BlockState state = serverLevel.getBlockState(pos);
        BlockHitResult hit = pilotBlockHit(pos, face);
        InteractionHand hand = pilotInteractionHand(player);
        ItemStack held = player.getItemInHand(hand);
        if (!player.mayUseItemAt(pos, face, held)
                || !serverLevel.mayInteract(player, pos.relative(face))) return;
        if (!held.isEmpty() && !(held.getItem() instanceof DroneRemoteItem)) {
            InteractionResult itemResult = held.useOn(new UseOnContext(player, hand, hit));
            if (itemResult.consumesAction()) {
                pilotActionCooldown = 4;
                return;
            }
        }
        if (state.useWithoutItem(serverLevel, player, hit).consumesAction()) {
            pilotActionCooldown = 4;
            return;
        }
        if (placeBlock(player, pos, face, ItemStack.EMPTY)) {
            pilotActionCooldown = 4;
            return;
        }
        pilotActionCooldown = 4;
    }

    private void minePilotBlock(ServerLevel serverLevel, Player player, BlockPos pos) {
        if (!serverLevel.getGameRules().getBoolean(
                net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) return;
        if (lastPilotMineTick == tickCount) return;
        lastPilotMineTick = tickCount;
        BlockState state = serverLevel.getBlockState(pos);
        if (state.isAir() || serverLevel.getBlockEntity(pos) != null
                || state.getDestroySpeed(serverLevel, pos) < 0.0F || battery < 1) {
            clearPilotMineProgress(serverLevel);
            return;
        }
        if (!pos.equals(pilotMineTarget)) {
            clearPilotMineProgress(serverLevel);
            pilotMineTarget = pos.immutable();
        }
        ItemStack tool = getPilotTool(player, state);
        float hardness = state.getDestroySpeed(serverLevel, pos);
        float speed = tool.isEmpty() ? 1.0F : tool.getDestroySpeed(state);
        if (player.getAbilities().instabuild) {
            pilotMineProgress = 1.0F;
        } else if (hardness == 0.0F) {
            pilotMineProgress = 1.0F;
        } else {
            float divisor = !state.requiresCorrectToolForDrops()
                    || !tool.isEmpty() && tool.isCorrectToolForDrops(state) ? 30.0F : 100.0F;
            pilotMineProgress += speed / hardness / divisor;
        }
        drainBattery(1);
        serverLevel.destroyBlockProgress(getId(), pos, Math.min(9, (int) (pilotMineProgress * 10.0F)));
        if (pilotMineProgress < 1.0F) return;

        var blockEntity = serverLevel.getBlockEntity(pos);
        if (!serverLevel.destroyBlock(pos, false, this)) {
            clearPilotMineProgress(serverLevel);
            return;
        }
        if (!state.requiresCorrectToolForDrops()
                || !tool.isEmpty() && tool.isCorrectToolForDrops(state)) {
            Block.dropResources(state, serverLevel, pos, blockEntity, player, tool);
            int experience = state.getExpDrop(serverLevel, pos, blockEntity, player, tool);
            if (experience > 0) ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), experience);
        }
        if (!tool.isEmpty()) tool.mineBlock(serverLevel, state, pos, player);
        clearPilotMineProgress(serverLevel);
    }

    private static ItemStack getPilotTool(Player player, BlockState state) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && !(mainHand.getItem() instanceof DroneRemoteItem)) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && !(offHand.getItem() instanceof DroneRemoteItem)) return offHand;
        return player.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && !(stack.getItem() instanceof DroneRemoteItem))
                .filter(stack -> stack.isCorrectToolForDrops(state) || stack.getDestroySpeed(state) > 1.0F)
                .max(Comparator.comparingDouble(stack -> stack.getDestroySpeed(state)
                        + (stack.isCorrectToolForDrops(state) ? 100.0F : 0.0F)))
                .orElse(ItemStack.EMPTY);
    }

    private static InteractionHand pilotInteractionHand(Player player) {
        return player.getMainHandItem().getItem() instanceof DroneRemoteItem
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private void clearPilotMineProgress(ServerLevel serverLevel) {
        if (pilotMineTarget != null) {
            serverLevel.destroyBlockProgress(getId(), pilotMineTarget, -1);
            pilotMineTarget = null;
        }
        pilotMineProgress = 0.0F;
    }

    private boolean hasPilotLineOfSight(ServerLevel serverLevel, BlockPos pos, Direction face) {
        BlockHitResult result = serverLevel.clip(new ClipContext(getEyePosition(),
                pilotBlockHit(pos, face).getLocation(), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, this));
        return result.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && result.getBlockPos().equals(pos);
    }

    private BlockHitResult pilotBlockHit(BlockPos pos, Direction face) {
        Vec3 location = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5,
                face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new BlockHitResult(location, face, pos, false);
    }

    private void fireLaser(ServerLevel serverLevel, Player owner, int entityId) {
        if (laserCooldown > 0 || battery < 20) return;
        Monster target;
        if (serverLevel.getEntity(entityId) instanceof Monster selected) {
            target = selected;
        } else {
            target = serverLevel.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(24.0),
                            candidate -> candidate.isAlive() && candidate.distanceToSqr(this) <= 24.0 * 24.0
                                    && hasLineOfSight(candidate) && isOnLaserSight(candidate))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        if (target == null || !target.isAlive() || target.distanceToSqr(this) > 24.0 * 24.0
                || !hasLineOfSight(target) || !isOnLaserSight(target)) return;
        target.hurt(serverLevel.damageSources().playerAttack(owner), LASER_DAMAGE);
        Vec3 start = getEyePosition();
        Vec3 end = target.getBoundingBox().getCenter();
        Vec3 beam = end.subtract(start);
        int particles = Math.max(1, (int) (beam.length() * 2.0));
        for (int step = 0; step <= particles; step++) {
            Vec3 point = start.add(beam.scale((double) step / particles));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        drainBattery(20);
        laserCooldown = 12;
    }

    private boolean isOnLaserSight(LivingEntity target) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(getEyePosition());
        return toTarget.lengthSqr() > 0.0 && getLookAngle().dot(toTarget.normalize()) >= 0.997;
    }

    private void interactWithEntity(ServerLevel serverLevel, Player player, int entityId) {
        if (!(serverLevel.getEntity(entityId) instanceof LivingEntity target)
                || !target.isAlive() || target.distanceToSqr(this) > 36.0
                || !hasLineOfSight(target) || !serverLevel.mayInteract(player, target.blockPosition())) return;
        InteractionHand hand = pilotInteractionHand(player);
        InteractionResult entityResult = target.interact(player, hand);
        ItemStack held = player.getItemInHand(hand);
        if (!entityResult.consumesAction() && !held.isEmpty()
                && !(held.getItem() instanceof DroneRemoteItem)) {
            held.interactLivingEntity(player, target, hand);
        }
    }

    @Override
    public boolean isPushable() {
        return pilotInputExpiry <= tickCount && super.isPushable();
    }

    @Override
    public boolean canCollideWith(Entity other) {
        if (pilotInputExpiry > tickCount && other instanceof LivingEntity) return false;
        return super.canCollideWith(other);
    }

    public int getBattery() {
        return level().isClientSide ? entityData.get(SYNC_BATTERY) : battery;
    }

    public float getBatteryPercent() {
        return (float) getBattery() / MAX_BATTERY;
    }

    public float getHealthPercent() {
        return getMaxHealth() <= 0 ? 0.0F : getHealth() / getMaxHealth();
    }

    public SimpleContainer getCargo() {
        return cargo;
    }

    public boolean isOwnedBy(Player player) {
        UUID currentOwner = ownerId;
        if (level().isClientSide) {
            String synced = entityData.get(SYNC_OWNER);
            if (synced.isEmpty()) return false;
            try {
                currentOwner = UUID.fromString(synced);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return currentOwner != null && currentOwner.equals(player.getUUID());
    }

    private Player getOwnerPlayer() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    public DroneTask getTask() {
        int taskId = level().isClientSide ? entityData.get(SYNC_TASK) : task.ordinal();
        return taskId >= 0 && taskId < DroneTask.values().length ? DroneTask.values()[taskId] : DroneTask.FOLLOW;
    }

    public void setTask(DroneTask task) {
        this.task = task;
        entityData.set(SYNC_TASK, task.ordinal());
        if (task == DroneTask.LOCATE_ORE) lastLocatedOre = null;
        if (task != DroneTask.FOLLOW) getNavigation().stop();
        Player owner = getOwnerPlayer();
        if (owner != null && !level().isClientSide) {
            owner.displayClientMessage(Component.translatable("message.droneartisan.task", task.translation()), true);
        }
    }

    private void drainBattery(int amount) {
        battery = Math.max(0, battery - amount);
        entityData.set(SYNC_BATTERY, battery);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !isOwnedBy(player)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putString("Task", task.name());
        tag.putInt("Battery", battery);
        if (homePos != null) tag.putLong("HomePos", homePos.asLong());
        ContainerHelper.saveAllItems(tag, cargo.getItems(), registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setOwner(tag.hasUUID("Owner") ? tag.getUUID("Owner") : null);
        try {
            task = DroneTask.valueOf(tag.getString("Task"));
        } catch (IllegalArgumentException exception) {
            task = DroneTask.FOLLOW;
        }
        battery = Math.max(0, Math.min(MAX_BATTERY, tag.getInt("Battery")));
        homePos = tag.contains("HomePos") ? BlockPos.of(tag.getLong("HomePos")) : null;
        ContainerHelper.loadAllItems(tag, cargo.getItems(), registryAccess());
        entityData.set(SYNC_BATTERY, battery);
        entityData.set(SYNC_TASK, task.ordinal());
    }
}
