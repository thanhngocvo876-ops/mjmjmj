package com.mjmod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A concentrated pure-fire ember: hot white/yellow core, red-orange edge,
 * tapered motion and a short turbulent lifetime instead of vanilla FLAME's
 * blocky rising puff.
 */
public final class PureFireParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialSize;
    private final float phase;
    private final float drift;

    private PureFireParticle(ClientLevel level, double x, double y, double z,
                             double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        RandomSource random = level.random;
        this.sprites = sprites;
        this.friction = 0.90F;
        this.gravity = -0.010F;
        this.hasPhysics = false;
        this.initialSize = 0.075F + random.nextFloat() * 0.075F;
        this.quadSize = initialSize;
        this.lifetime = 12 + random.nextInt(9);
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.drift = 0.004F + random.nextFloat() * 0.008F;
        this.xd = xd * 0.35D + (random.nextDouble() - 0.5D) * 0.008D;
        this.yd = yd * 0.35D + 0.012D + random.nextDouble() * 0.012D;
        this.zd = zd * 0.35D + (random.nextDouble() - 0.5D) * 0.008D;
        this.alpha = 0.90F;
        pickSprite(sprites);
        updateColour(0.0F);
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
        float progress = age / (float) lifetime;
        float envelope = 1.0F - progress;
        float pulse = 0.92F + Mth.sin(age * 0.85F + phase) * 0.08F;
        quadSize = initialSize * (0.55F + envelope * 0.75F) * pulse;
        alpha = Mth.clamp(envelope * 1.25F, 0.0F, 0.90F);
        xd += Mth.sin(age * 0.55F + phase) * drift;
        zd += Mth.cos(age * 0.48F + phase) * drift;
        yd += gravity * (0.65D + progress);
        move(xd, yd, zd);
        xd *= friction;
        yd *= 0.975D;
        zd *= friction;
        updateColour(progress);
        setSpriteFromAge(sprites);
    }

    private void updateColour(float progress) {
        // White-hot core at birth, then a warmer orange/red edge as the ember
        // fades. This colour animation is intentionally smoother than FLAME.
        float heat = 1.0F - Mth.clamp(progress * 1.15F, 0.0F, 1.0F);
        rCol = 1.0F;
        gCol = 0.24F + heat * 0.66F;
        bCol = 0.015F + heat * 0.34F;
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
                                       double xd, double yd, double zd) {
            return new PureFireParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
