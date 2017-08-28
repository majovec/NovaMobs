package com.pikycz.novamobs.entities;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockFence;
import cn.nukkit.block.BlockFenceGate;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.block.BlockSlab;
import cn.nukkit.block.BlockStairs;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import com.pikycz.novamobs.configsection.MainConfig;

import com.pikycz.novamobs.entities.animal.Animal;
import com.pikycz.novamobs.utils.Utils;

public abstract class WalkingEntity extends BaseEntity {

    public WalkingEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    protected void checkTarget() {
        if (this.isKnockback()) {
            return;
        }

        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive()) {
            return;
        }

        if (itsTimeToChangeATarget(this.target)) {
            EntityCreature creatureTarget = findACreatureTarget();
            if (creatureTarget != null) {
                resetTarget(creatureTarget);
            } else {
                randomlyDecideATargetStaytimeAndMovetime();

            }
        }
    }

    private void randomlyDecideATargetStaytimeAndMovetime() {

        int x, z;
        Vector3 location = null;
        if (this.stayTime > 0) {
            if (Utils.rand(1, 100) <= 5) {                      //small probability event
                x = Utils.rand(10, 30);
                z = Utils.rand(10, 30);
                location = this.add(Utils.rand() ? x : -x, Utils.rand(-20, 20) / 10, Utils.rand() ? z : -z);
            } else {
                location = this.target;
            }
        } else if (this.moveTime > 0) {
            if (Utils.rand(1, 410) == 1) {                      //small probability event
                x = Utils.rand(10, 30);
                z = Utils.rand(10, 30);
                this.stayTime = Utils.rand(90, 400);
                location = this.add(Utils.rand() ? x : -x, Utils.rand(-20, 20) / 10, Utils.rand() ? z : -z);
            } else {
                location = this.target;
            }
        } else {
            x = Utils.rand(20, 100);
            z = Utils.rand(20, 100);
            this.stayTime = 0;
            this.moveTime = Utils.rand(300, 1200);
            location = this.add(Utils.rand() ? x : -x, 0, Utils.rand() ? z : -z);
        }

        this.target = location;
    }

    private EntityCreature findACreatureTarget() {
        double near = Integer.MAX_VALUE;
        EntityCreature tempCreatureTarget = null;
        for (Entity entity : this.getLevel().getEntities()) {

            if (entity == this || !(entity instanceof EntityCreature) || entity instanceof Animal) {
                continue;
            }

            EntityCreature creature = (EntityCreature) entity;
            if (creature instanceof BaseEntity && ((BaseEntity) creature).isFriendly() == this.isFriendly()) {
                continue;
            }

            double distance = this.distanceSquared(creature);
            if (distance > near || !this.targetOption(creature, distance)) {
                continue;
            }
            near = distance;
            tempCreatureTarget = creature;
        }
        return tempCreatureTarget;
    }

    private boolean itsTimeToChangeATarget(Vector3 target) {
        return !(target instanceof EntityCreature) || !this.targetOption((EntityCreature) target, this.distanceSquared(target));
    }

    private void resetTarget(Vector3 newTarget) {
        this.stayTime = 0;
        this.moveTime = 0;
        this.target = newTarget;
    }

    protected boolean checkJump(double dx, double dz) {
        if (this.motionY == this.getGravity() * 2) {
            return this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) this.y, NukkitMath.floorDouble(this.z))) instanceof BlockLiquid;
        } else {
            if (this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z))) instanceof BlockLiquid) {
                this.motionY = this.getGravity() * 2;
                return true;
            }
        }

        if (!this.onGround || this.stayTime > 0) {
            return false;
        }

        Block that = this.getLevel().getBlock(new Vector3(NukkitMath.floorDouble(this.x + dx), (int) this.y, NukkitMath.floorDouble(this.z + dz)));
        if (this.getDirection() == null) {
            return false;
        }

        Block block = that.getSide(this.getHorizontalFacing());
        if (!block.canPassThrough() && block.up().canPassThrough() && that.up(2).canPassThrough()) {
            if (block instanceof BlockFence || block instanceof BlockFenceGate) {
                this.motionY = this.getGravity();
            } else if (this.motionY <= this.getGravity() * 4) {
                this.motionY = this.getGravity() * 4;
            } else if (block instanceof BlockSlab && block instanceof BlockStairs) { // on stairs entities shouldnt jump THAT high
                this.motionY = this.getGravity() * 4;
            } else if (this.motionY <= (this.getGravity() * 8)) {
                this.motionY = this.getGravity() * 8;
            } else {
                this.motionY += this.getGravity() * 0.25;
            }
            return true;
        }
        return false;
    }

    @Override
    public Vector3 updateMove(int tickDiff) {
        if (MainConfig.MOB_AI_ENABLED) {
            if (!this.isMovement()) {
                return null;
            }

            if (this.isKnockback()) {
                this.move(this.motionX * tickDiff, this.motionY, this.motionZ * tickDiff);
                this.motionY -= this.getGravity() * tickDiff;
                this.updateMovement();
                return null;
            }

            if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive()) {
                double x = this.followTarget.x - this.x;
                double y = this.followTarget.y - this.y;
                double z = this.followTarget.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (this.stayTime > 0 || this.distance(this.followTarget) <= (this.getWidth() + 0.0d) / 2 + 0.05) {
                    this.motionX = 0;
                    this.motionZ = 0;
                } else {
                    this.motionX = this.getSpeed() * 0.15 * (x / diff);
                    this.motionZ = this.getSpeed() * 0.15 * (z / diff);
                }
                this.yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff));
                this.pitch = y == 0 ? 0 : Math.toDegrees(-Math.atan2(y, Math.sqrt(x * x + z * z)));
                return this.followTarget;
            }

            Vector3 before = this.target;
            this.checkTarget();
            if (this.target != null && this.target instanceof EntityCreature || before != this.target) {
                double x = this.target.x - this.x;
                double y = this.target.y - this.y;
                double z = this.target.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (this.stayTime > 0 || this.distance(this.target) <= (this.getWidth() + 0.0d) / 2 + 0.05) {
                    this.motionX = 0;
                    this.motionZ = 0;
                } else {
                    this.motionX = this.getSpeed() * 0.15 * (x / diff);
                    this.motionZ = this.getSpeed() * 0.15 * (z / diff);
                }
                this.yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff));
                this.pitch = y == 0 ? 0 : Math.toDegrees(-Math.atan2(y, Math.sqrt(x * x + z * z)));
            }

            double dx = this.motionX * tickDiff;
            double dz = this.motionZ * tickDiff;
            boolean isJump = this.checkJump(dx, dz);
            if (this.stayTime > 0) {
                this.stayTime -= tickDiff;
                this.move(0, this.motionY * tickDiff, 0);
            } else {
                Vector2 be = new Vector2(this.x, this.z);
                this.move(dx, this.motionY * tickDiff, dz);
                Vector2 af = new Vector2(this.x, this.z);

                if ((be.x != af.x || be.y != af.y) && !isJump) {
                    this.moveTime -= 90 * tickDiff;
                }
            }

            if (!isJump) {
                if (this.onGround) {
                    this.motionY = 0;
                } else if (this.motionY > -this.getGravity() * 4) {
                    if (!(this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z))) instanceof BlockLiquid)) {
                        this.motionY -= this.getGravity() * 1;
                    }
                } else {
                    this.motionY -= this.getGravity() * tickDiff;
                }
            }
            this.updateMovement();

        }
        return this.target;
    }

}
