package net.rg.mymod.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class PearlLauncherItem extends Item {

    private static final String PEARLS_KEY = "Pearls";
    private static final int MAX_PEARLS = 4;
    private static final int COOLDOWN_TICKS = 10; // 0.5s
    private static final int MAX_USE_TIME = 20; // 1 second (same as bow)

    public PearlLauncherItem(Settings settings) {
        super(settings);
    }

    /* ===============================
       DATA COMPONENT HELPERS
       =============================== */

    private NbtCompound getData(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private void saveData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private int getPearls(ItemStack stack) {
        NbtCompound data = getData(stack);
        return data.contains(PEARLS_KEY) ? data.getInt(PEARLS_KEY) : 0;
    }

    private void setPearls(ItemStack stack, int amount) {
        NbtCompound nbt = getData(stack);
        nbt.putInt(PEARLS_KEY, amount);
        saveData(stack, nbt);
    }

    private boolean consumePearl(ItemStack stack) {
        int pearls = getPearls(stack);
        if (pearls <= 0) return false;
        setPearls(stack, pearls - 1);
        return true;
    }

    /* ===============================
       MAIN LOGIC
       =============================== */

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int pearls = getPearls(stack);
        tooltip.add(Text.literal("Ender Pearls: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(pearls + "/" + MAX_PEARLS)
                        .formatted(pearls > 0 ? Formatting.AQUA : Formatting.RED)));

        tooltip.add(Text.literal("Speed: 3.5x")
                .formatted(Formatting.DARK_PURPLE));

        tooltip.add(Text.literal("Hold to load pearls")
                .formatted(Formatting.GOLD));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_USE_TIME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // Play loading sound effect periodically
        if (!world.isClient && remainingUseTicks % 5 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 0.5F, 1.0F);
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world.isClient || !(user instanceof PlayerEntity player)) {
            return stack;
        }

        // Load ONE pearl when charging finishes
        int stored = getPearls(stack);
        if (stored < MAX_PEARLS) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack inv = player.getInventory().getStack(i);
                if (inv.isOf(Items.ENDER_PEARL)) {
                    inv.decrement(1);
                    setPearls(stack, stored + 1);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 1.0F, 1.2F);
                    break;
                }
            }
        }

        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack launcher = player.getStackInHand(hand);

        // ===== SHIFT + RIGHT-CLICK → START LOADING =====
        if (player.isSneaking()) {
            int stored = getPearls(launcher);
            if (stored < MAX_PEARLS) {
                player.setCurrentHand(hand);
                return TypedActionResult.consume(launcher);
            }
            return TypedActionResult.pass(launcher);
        }

        // ===== NORMAL RIGHT-CLICK → SHOOT =====
        if (world.isClient()) return TypedActionResult.success(launcher);

        if (!consumePearl(launcher)) {
            player.playSound(SoundEvents.BLOCK_DISPENSER_FAIL, 1.0F, 1.0F);
            return TypedActionResult.fail(launcher);
        }

        // Spawn EnderPearlEntity
        EnderPearlEntity pearl = new EnderPearlEntity(world, player);
        pearl.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());

        Vec3d look = player.getRotationVector();
        double speed = 3.5; // Fast - good balance between distance and control
        pearl.setVelocity(look.x * speed, look.y * speed, look.z * speed);

        world.spawnEntity(pearl);

        // Apply cooldown
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        // Reduce durability
        EquipmentSlot slot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        launcher.damage(1, player, slot);

        // Play sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);

        return TypedActionResult.success(launcher);
    }
}