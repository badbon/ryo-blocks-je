package com.go4no.ryoblocks.entity;

import com.go4no.ryoblocks.mixin.WitherAttackCooldownAccessor;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldEvents;

public final class KitaAngelBossEntity extends WitherEntity {
    public static final String OBSTACLE_SHOT_TAG = "ryo_blocks_obstacle_clearing";
    private int blockedTicks;
    private int obstacleShotCooldown;

    public KitaAngelBossEntity(EntityType<? extends WitherEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        var rangedGoals = goalSelector.getGoals().stream().map(goal -> goal.getGoal())
            .filter(goal -> goal instanceof ProjectileAttackGoal).toList();
        rangedGoals.forEach(goalSelector::remove);
        goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, KitaAttackTiming.MAIN_INTERVAL, 20.0F));
    }

    @Override
    protected void mobTick() {
        int[] cooldowns = ((WitherAttackCooldownAccessor)(Object)this).ryoBlocks$getSkullCooldowns();
        int previousLeft = cooldowns[0];
        int previousRight = cooldowns[1];
        super.mobTick();
        // Scale newly scheduled delays once, including idle checks, without accelerating other Wither behavior.
        if (cooldowns[0] != previousLeft && cooldowns[0] > age) {
            cooldowns[0] = age + KitaAttackTiming.faster(cooldowns[0] - age);
        }
        if (cooldowns[1] != previousRight && cooldowns[1] > age) {
            cooldowns[1] = age + KitaAttackTiming.faster(cooldowns[1] - age);
        }
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (!getWorld().isClient && !isAiDisabled() && getTrackedEntityId(0) > 0 && getInvulnerableTimer() == 0
            && !isTouchingWater() && !isInLava()) {
            var target = getWorld().getEntityById(getTrackedEntityId(0));
            if (target != null && target.isAlive()) {
                // Replace the Wither's abrupt lift threshold with a gentle approach to the same altitude.
                double altitude = target.getY() + (shouldRenderOverlay() ? 0.0 : 5.0);
                double verticalSpeed = MathHelper.clamp((altitude - getY()) * 0.05, -0.10, 0.15);
                Vec3d velocity = getVelocity();
                setVelocity(velocity.x, verticalSpeed, velocity.z);
            }
        }
        super.travel(movementInput);
    }

    @Override
    public void move(MovementType type, Vec3d movement) {
        Vec3d before = getPos();
        Box bounds = getBoundingBox();
        super.move(type, movement);
        if (!getWorld().isClient && type == MovementType.SELF) clearBlockingObstacle(before, movement, bounds);
    }

    private void clearBlockingObstacle(Vec3d before, Vec3d intended, Box bounds) {
        if (obstacleShotCooldown > 0) obstacleShotCooldown--;
        if (isAiDisabled() || getInvulnerableTimer() > 0 || !isAlive()
            || !getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            blockedTicks = 0;
            return;
        }
        Vec3d moved = getPos().subtract(before);
        // Check each blocked axis: sliding along a wall must not hide a blocked ascent or pursuit.
        Vec3d blocked = new Vec3d(
            Math.abs(intended.x) > 0.0001 && Math.abs(moved.x) < Math.abs(intended.x) * 0.1 ? intended.x : 0,
            intended.y > 0.0001 && moved.y < intended.y * 0.1 ? intended.y : 0,
            Math.abs(intended.z) > 0.0001 && Math.abs(moved.z) < Math.abs(intended.z) * 0.1 ? intended.z : 0);
        if (blocked.lengthSquared() == 0) {
            blockedTicks = 0;
            return;
        }
        Box probe = bounds.offset(blocked.normalize().multiply(0.25)).contract(0.001);
        BlockPos obstacle = null;
        double nearest = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(BlockPos.ofFloored(probe.minX, probe.minY, probe.minZ),
                BlockPos.ofFloored(probe.maxX, probe.maxY, probe.maxZ))) {
            var state = getWorld().getBlockState(pos);
            if (!WitherEntity.canDestroy(state)) continue;
            for (Box shape : state.getCollisionShape(getWorld(), pos).getBoundingBoxes()) {
                if (shape.offset(pos).intersects(probe)) {
                    double distance = pos.toCenterPos().squaredDistanceTo(getEyePos());
                    if (distance < nearest) {
                        obstacle = pos.toImmutable();
                        nearest = distance;
                    }
                }
            }
        }
        if (obstacle == null) {
            blockedTicks = 0;
            return;
        }
        blockedTicks++;
        if (blockedTicks < KitaAttackTiming.STUCK_WAIT || obstacleShotCooldown > 0) return;

        java.util.List<BlockPos> surrounding = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(obstacle.add(-1, -1, -1), obstacle.add(1, 1, 1))) {
            var state = getWorld().getBlockState(pos);
            if (!pos.equals(obstacle) && pos.getY() >= bounds.minY && WitherEntity.canDestroy(state)
                && !state.getCollisionShape(getWorld(), pos).isEmpty()) surrounding.add(pos.toImmutable());
        }
        BlockPos center = obstacle;
        surrounding.sort(java.util.Comparator.comparingDouble(pos -> pos.getSquaredDistance(center)));
        shootObstacleFeather(obstacle);
        for (int i = 0; i < 2; i++) {
            shootObstacleFeather(i < surrounding.size() ? surrounding.get(i) : obstacle);
        }
        if (!isSilent()) getWorld().syncWorldEvent(null, WorldEvents.WITHER_SHOOTS, getBlockPos(), 0);
        obstacleShotCooldown = KitaAttackTiming.CLEARING_INTERVAL;
        blockedTicks = 0;
    }

    private void shootObstacleFeather(BlockPos obstacle) {
        Vec3d origin = getEyePos();
        Vec3d direction = obstacle.toCenterPos().subtract(origin);
        WitherSkullEntity feather = new WitherSkullEntity(getWorld(), this, direction.x, direction.y, direction.z);
        feather.setPosition(origin);
        // Charged vanilla resistance rules let the clearing burst work on logs and masonry too.
        feather.setCharged(true);
        feather.addCommandTag(OBSTACLE_SHOT_TAG);
        getWorld().spawnEntity(feather);
    }
}
