package com.mjmod.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Outer flame-tongue particle for the pure-fire aura.
 *
 * The first three particle arguments are deliberately used as an attachment
 * payload: owner entity id, initial orbit angle, and orbit radius. Every tick
 * the particle resolves the owner again and recomputes its position from the
 * owner's current position. It therefore follows moving, jumping and rotating
 * players instead of leaving a world-space trail behind.
 */
public final class PureFireAuraParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final int ownerId;
    private final float orbitAngle;
    private final float orbitRadius;
    private final float startHeight;
    private final float riseSpeed;
    private final float baseScale;
    private final float phase;
    private final float orbitSpeed;
    private final float orbitWobble;


    private PureFireAuraParticle(ClientLevel level, double x, double y, double z,
                                 double ownerIdArg, double angleArg, double radiusArg,
                                 SpriteSet sprites) {
        super(level, x, y, z);
        RandomSource random = level.random;
        this.sprites = sprites;
        this.ownerId = (int) Math.round(ownerIdArg);
        this.orbitAngle = (float) angleArg;
        int scheme = Mth.clamp((int) Math.floor(radiusArg / 10.0D), 0, 2);
        float decodedRadius = (float) (radiusArg - scheme * 10.0D);
        this.orbitRadius = Mth.clamp(decodedRadius, 0.55F, 1.15F);
        this.startHeight = resolveStartHeight(level, y, ownerId);
        this.riseSpeed = switch (scheme) {
            case 1 -> 0.014F + random.nextFloat() * 0.014F;
            case 2 -> 0.021F + random.nextFloat() * 0.019F;
            default -> 0.030F + random.nextFloat() * 0.026F;
        };
        this.baseScale = switch (scheme) {
            case 1 -> 0.39F + random.nextFloat() * 0.24F;
            case 2 -> 0.34F + random.nextFloat() * 0.21F;
            default -> 0.28F + random.nextFloat() * 0.17F;
        };
        this.orbitSpeed = switch (scheme) {
            case 1 -> 0.030F;
            case 2 -> 0.050F;
            default -> 0.082F;
        };
        this.orbitWobble = scheme == 1 ? 0.065F : (scheme == 2 ? 0.090F : 0.125F);
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.lifetime = switch (scheme) {
            case 1 -> 34 + random.nextInt(16);
            case 2 -> 28 + random.nextInt(15);
            default -> 22 + random.nextInt(12);
        };

        float heat = 0.28F + random.nextFloat() * 0.72F;
        this.rCol = 1.0F;
        this.gCol = switch (scheme) {
            case 1 -> 0.13F + heat * 0.43F;
            case 2 -> 0.19F + heat * 0.57F;
            default -> 0.30F + heat * 0.65F;
        };
        this.bCol = scheme == 0 ? 0.07F + heat * 0.13F : 0.012F + heat * 0.10F;
        this.friction = 0.98F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.quadSize = baseScale;
        this.alpha = isLocalFirstPerson(owner()) ? 0.32F : 0.55F;
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
        float pulse = 0.90F + Mth.sin(age * 0.62F + phase) * 0.10F;
        quadSize = baseScale * (0.72F + envelope * 0.70F) * pulse;
        float alphaCap = isLocalFirstPerson(owner) ? 0.32F : 0.55F;
        alpha = Mth.clamp(envelope * 0.78F, 0.0F, alphaCap);

        // Rebuild the position relative to the owner every tick. The orbit
        // motion is local to the player, while the vertical motion creates the
        // upward flowing flame tongue seen in the DragonMineZ aura.
        float angle = orbitAngle + age * orbitSpeed
                + Mth.sin(age * 0.18F + phase) * orbitWobble;
        float radius = orbitRadius + Mth.sin(age * 0.23F + phase)
                * (orbitWobble * 0.55F);
        float maxHeight = owner.getBbHeight() + 0.30F;
        if (isLocalFirstPerson(owner)) {
            maxHeight = Math.max(0.25F, owner.getEyeHeight() - 0.35F);
        }
        double height = Math.min(maxHeight,
                startHeight + age * riseSpeed + Mth.sin(age * 0.32F + phase) * 0.035F);
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
        return 0.35F;
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
            return new PureFireAuraParticle(level, x, y, z,
                    ownerId, angle, radius, sprites);
        }
    }
}
