package com.gmail.mattdiamond98.coronacraft.abilities;

import com.sun.istack.internal.Nullable;
import org.bukkit.entity.Projectile;

public abstract class ProjectileAbilityStyle extends AbilityStyle {

    public ProjectileAbilityStyle(String name, String[] description, @Nullable String permission, int ModelData) {
        super(name, description, permission, ModelData);
    }

    public ProjectileAbilityStyle(String name, String[] description, int ModelData) {
        this(name, description, null, ModelData);
    }

    public abstract int onShoot(Projectile p);

}
