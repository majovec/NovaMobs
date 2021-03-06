package com.pikycz.novamobs.entities.monster.walking;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemSkull;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

import co.aikar.timings.Timings;

import com.pikycz.novamobs.entities.monster.WalkingMonster;
import com.pikycz.novamobs.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Zombie extends WalkingMonster implements EntityAgeable {

    public static final int NETWORK_ID = 32;

    public Zombie(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }
    
    @Override
    public String getName() {
        return "Zombie";
    }
    
    @Override
    public float getWidth() {
        return 0.72f;
    }

    @Override
    public float getHeight() {
        return 1.8f;
    }

    @Override
    public double getSpeed() {
        return 1.1;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setDamage(new int[]{0, 2, 3, 4});
        setMaxHealth(20);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);

        if (this.isAlive()) {
            if (15 < this.getHealth()) {
                this.setDamage(new int[]{0, 2, 3, 4});
            } else if (10 < this.getHealth()) {
                this.setDamage(new int[]{0, 3, 4, 6});
            } else if (5 < this.getHealth()) {
                this.setDamage(new int[]{0, 3, 5, 7});
            } else {
                this.setDamage(new int[]{0, 4, 6, 9});
            }
        }
    }

    @Override
    public void attackEntity(Entity player) {
        if (this.attackDelay > 10 && player.distanceSquared(this) <= 1) {
            this.attackDelay = 0;
            player.attack(new EntityDamageByEntityEvent(this, player, DamageCause.ENTITY_ATTACK, getDamage()));
        }
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean hasUpdate = false;
        Timings.entityBaseTickTimer.startTiming();

        hasUpdate = super.entityBaseTick(tickDiff);

        int time = this.getLevel().getTime() % Level.TIME_FULL;
        if (!this.isOnFire() && !this.level.isRaining() && !(time > Level.TIME_SUNSET && time >= Level.TIME_NIGHT && time < Level.TIME_SUNRISE)) {
            this.setOnFire(100);
        }

        Timings.entityBaseTickTimer.stopTiming();
        return hasUpdate;
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();
        if (this.lastDamageCause instanceof EntityDamageByEntityEvent) {
            int rottenFlesh = Utils.rand(0, 3); // drops 0-2 rotten flesh
            int skull = Utils.rand(0, 101) <= 9 ? 1 : 0; // with a 8,5% chance to Skull is dropped
            for (int i = 0; i < rottenFlesh; i++) {
                drops.add(Item.get(Item.ROTTEN_FLESH, 0, 1));
            }
            for (int i = 0; i < skull; i++) {
                drops.add(Item.get(ItemSkull.ZOMBIE_HEAD, 0, 1));
            }
        }
        return drops.toArray(new Item[drops.size()]);
    }

    @Override
    public int getKillExperience() {
        return 5; // gain 5 experience
    }

}
