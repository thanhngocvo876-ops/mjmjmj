package com.mjmod.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Key bindings for the Void Tears, pure-fire and blood-exchange systems. */
public final class ModKeyBindings {
    private ModKeyBindings() {}

    public static final String CATEGORY = "key.categories.mjmod";
    public static final KeyMapping VOID_TEARS_SWITCH = new KeyMapping(
            "key.mjmod.void_tears_switch", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping VOID_TEARS_CAST = new KeyMapping(
            "key.mjmod.void_tears_cast", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping BLOOD_EXCHANGE = new KeyMapping(
            "key.mjmod.blood_exchange", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping OPEN_VOID_TEARS_POINTS = new KeyMapping(
            "key.mjmod.open_void_tears_points", GLFW.GLFW_KEY_U, CATEGORY);
    public static final KeyMapping OPEN_VOID_TEARS_HEART = new KeyMapping(
            "key.mjmod.open_void_tears_heart", GLFW.GLFW_KEY_P, CATEGORY);
    public static final KeyMapping PURE_FIRE_SWITCH = new KeyMapping(
            "key.mjmod.pure_fire_switch", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping PURE_FIRE_CAST = new KeyMapping(
            "key.mjmod.pure_fire_cast", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping PURE_FIRE_MEDITATE = new KeyMapping(
            "key.mjmod.pure_fire_meditate", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping OPEN_PURE_FIRE_TALENT = new KeyMapping(
            "key.mjmod.open_pure_fire_talent", GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping THUNDER_ACCEL = new KeyMapping(
            "key.mjmod.thunder_accel", GLFW.GLFW_KEY_B, CATEGORY);
}
