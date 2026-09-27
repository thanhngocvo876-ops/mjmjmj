package com.mjmod.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Inner white-hot core spark bound to a player. Its orbit and height are
 * recomputed from the owner's current position every tick, matching the outer
 * flame layer during movement, jumping and camera-mode changes.
 */
public final class PureFireAuraCoreParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final int ownerId;
    private final float orbitAngle;
    private final float orbitRadius;
    private final float startHeight;
    private final float riseSpeed;
    private final float baseScale;
    private final float phase;
    private final float orbitSpeed;


    private PureFireAuraCoreParticle(ClientLevel level, double x, double y, double z,
                                     double ownerIdArg, double angleArg, double radiusArg,
                                     SpriteSet sprites) {
        super(level, x, y, z);
        RandomSource random = level.random;
        this.sprites = sprites;
        this.ownerId = (int) Math.round(ownerIdArg);
        this.orbitAngle = (float) angleArg;
        int scheme = Mth.clamp((int) Math.floor(radiusArg / 10.0D), 0, 2);
        float decodedRadius = (float) (radiusArg - scheme * 10.0D);
        this.orbitRadius = Mth.clamp(decodedRadius, 0.12F, 0.48F);
        this.startHeight = resolveStartHeight(level, y, ownerId);
        this.riseSpeed = switch (scheme) {
            case 1 -> 0.024F + random.nextFloat() * 0.020F;
            case 2 -> 0.030F + random.nextFloat() * 0.026F;
            default -> 0.042F + random.nextFloat() * 0.030F;
        };
        this.baseScale = switch (scheme) {
            case 1 -> 0.13F + random.nextFloat() * 0.09F;
            case 2 -> 0.10F + random.nextFloat() * 0.08F;
            default -> 0.085F + random.nextFloat() * 0.065F;
        };
        this.orbitSpeed = switch (scheme) {
            case 1 -> 0.045F;
            case 2 -> 0.068F;
            default -> 0.095F;
        };
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.lifetime = 18 + random.nextInt(12);
        this.rCol = 1.0F;
        this.gCol = scheme == 1
                ? 0.72F + random.nextFloat() * 0.18F
                : 0.86F + random.nextFloat() * 0.12F;
        this.bCol = scheme == 0
                ? 0.66F + random.nextFloat() * 0.24F
                : 0.50F + random.nextFloat() * 0.25F;
        this.friction = 0.98F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.quadSize = baseScale;
        this.alpha = 0.90F;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }

        Player owner = owner();
        if (owner == null || owner.isRemoved()) {
            remove();
            return;
        }

        float progress = age / (float) lifetime;
        float envelope = 1.0F - progress;
        quadSize = baseScale * (1.0F - progress * 0.45F);
        float alphaCap = isLocalFirstPerson(owner) ? 0.58F : 0.90F;
        alpha = Mth.clamp(envelope * 1.05F, 0.0F, alphaCap);

        float angle = orbitAngle - age * orbitSpeed
                + Mth.sin(age * 0.25F + phase) * 0.08F;
        float radius = orbitRadius + Mth.sin(age * 0.34F + phase) * 0.025F;
        float maxHeight = owner.getBbHeight() + 0.15F;
        if (isLocalFirstPerson(owner)) {
            maxHeight = Math.max(0.20F, owner.getEyeHeight() - 0.45F);
        }
        double height = Math.min(maxHeight,
                startHeight + age * riseSpeed + Mth.sin(age * 0.40F + phase) * 0.025F);
        x = owner.getX() + Mth.cos(angle) * radius;
        y = owner.getY() + height;
        z = owner.getZ() + Mth.sin(angle) * radius;
        setSpriteFromAge(sprites);
    }

    private Player owner() {
        Entity entity = level.getEntity(ownerId);
        return entity instanceof Player player ? player : null;
    }

    private static float resolveStartHeight(ClientLevel level, double y, int ownerId) {
        Entity entity = level.getEntity(ownerId);
        if (entity instanceof Player player) {
            return Mth.clamp((float) (y - player.getY()), 0.05F, player.getBbHeight());
        }
        return 0.20F;
    }

    private static boolean isLocalFirstPerson(Player owner) {
        Minecraft minecraft = Minecraft.getInstance();
        return owner != null && owner == minecraft.player
                && minecraft.options.getCameraType().isFirstPerson();
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240 | (240 << 16);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double ownerId, double angle, double radius) {
            return new PureFireAuraCoreParticle(level, x, y, z,
                    ownerId, angle, radius, sprites);
        }
    }
}
