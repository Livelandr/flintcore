package com.livelandr.flintcore.core.util;

import com.livelandr.flintcore.core.guns.GunBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HookSystem {

    public static String CALCULATE_DAMAGE_MODIFIER = "calculateDamageModifier";
    public static String CALCULATE_RECOIL_MODIFIER_X = "calculateRecoilModifierX";
    public static String CALCULATE_RECOIL_MODIFIER_Y = "calculateRecoilModifierY";
    public static String CALCULATE_PROPELLANT_MODIFIER = "calculatePropellantModifier";
    public static String CALCULATE_ACCURACY_MODIFIER = "calculateAccuracyModifier";
    public static String ALLOW_PRESSING_TRIGGER = "allowPressingTrigger";
    public static String TRY_SHOOT = "tryShoot";
    public static String ON_SHOOT = "onShoot";
    public static String PROCESS_SHOOTING = "processShooting";

    // TODO: REPLACE HOOKS TO OTHER STATIC CLASS
    public static Map<String, List<FlintcoreHook>> hooks = new HashMap<>();
    static {
        hooks.put(CALCULATE_DAMAGE_MODIFIER, new ArrayList<>());
        hooks.put(CALCULATE_RECOIL_MODIFIER_X, new ArrayList<>());
        hooks.put(CALCULATE_RECOIL_MODIFIER_Y, new ArrayList<>());
        hooks.put(CALCULATE_PROPELLANT_MODIFIER, new ArrayList<>());
        hooks.put(CALCULATE_ACCURACY_MODIFIER, new ArrayList<>());

        hooks.put(ALLOW_PRESSING_TRIGGER, new ArrayList<>());
        hooks.put(TRY_SHOOT, new ArrayList<>());
        hooks.put(ON_SHOOT, new ArrayList<>());
        hooks.put(PROCESS_SHOOTING, new ArrayList<>());
    }

    public static boolean calculateHookBool(HookContext context) {
        return (calculateHookSum(context, 1) != 0);
    }

    public static float calculateHookSum(HookContext context, float baseValue) {
        List<FlintcoreHook> funcs = hooks.get(context.getContextId());
        if (funcs == null || funcs.isEmpty()) {
            return 1;
        }

        float baseVal = baseValue;

        for (FlintcoreHook hook : funcs) {
            baseVal *= hook.process(context);
        }

        return baseVal;
    };

    public static void triggerHooks(HookContext context) {
        List<FlintcoreHook> funcs = hooks.get(context);
        if (funcs == null || funcs.isEmpty()) {
            return;
        }

        for (FlintcoreHook hook : funcs) {
            hook.process(context);
        }
    }

    public static void addHook(String hookID, FlintcoreHook hookFunc) {
        HookSystem.hooks.get(hookID).add(hookFunc);
    }
    // HOOKS SYSTEM END
}
