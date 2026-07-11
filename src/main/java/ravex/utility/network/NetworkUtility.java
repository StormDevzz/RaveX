package ravex.utility.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class NetworkUtility {
    private static Minecraft mc() { return Minecraft.getInstance(); }

    public static void sendStartDestroy(BlockPos pos, Direction dir, int seq) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir, seq));
    }

    public static void sendStopDestroy(BlockPos pos, Direction dir, int seq) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir, seq));
    }

    public static void sendDropAll(BlockPos pos, Direction dir) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS, pos, dir, 0));
    }

    public static void sendDropStack() {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN, 0));
    }

    public static void sendSetCarriedItem(int slot) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundSetCarriedItemPacket(slot));
    }

    public static void sendTeleportConfirm(int teleportId) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundAcceptTeleportationPacket(teleportId));
    }

    public static void sendMovePacket(Vec3 pos, boolean onGround, boolean horizontalCollision) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, onGround, horizontalCollision));
    }

    public static void sendSwing(InteractionHand hand) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundSwingPacket(hand));
    }

    public static void sendMoveRelative(double x, double y, double z, boolean onGround, boolean horizontalCollision) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horizontalCollision));
    }

    public static void sendInteractAttack(Entity target, boolean isShiftKeyDown) {
        var c = mc().getConnection();
        if (c != null)
            c.send(ServerboundInteractPacket.createAttackPacket(target, isShiftKeyDown));
    }

    public static void sendInteract(Entity target, boolean isShiftKeyDown, InteractionHand hand) {
        var c = mc().getConnection();
        if (c != null)
            c.send(ServerboundInteractPacket.createInteractionPacket(target, isShiftKeyDown, hand));
    }

    public static void sendUseItem(InteractionHand hand, float yRot, float xRot) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundUseItemPacket(hand, 0, yRot, xRot));
    }

    public static void sendUseItemOn(InteractionHand hand, BlockHitResult hitResult) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundUseItemOnPacket(hand, hitResult, 0));
    }

    public static void sendPlayerCommand(ServerboundPlayerCommandPacket.Action action) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerCommandPacket(mc().player, action));
    }

    public static boolean isMovePacket(Packet<?> packet) {
        return packet instanceof ServerboundMovePlayerPacket;
    }

    public static boolean isInteractPacket(Packet<?> packet) {
        return packet instanceof ServerboundInteractPacket;
    }

    public static boolean isSwingPacket(Packet<?> packet) {
        return packet instanceof ServerboundSwingPacket;
    }

    public static boolean isUsePacket(Packet<?> packet) {
        return packet instanceof ServerboundUseItemPacket
            || packet instanceof ServerboundUseItemOnPacket;
    }

    public static boolean isChatPacket(Packet<?> packet) {
        return packet instanceof ServerboundChatPacket
            || packet instanceof ServerboundChatCommandPacket;
    }

    public static boolean isInputPacket(Packet<?> packet) {
        return packet instanceof ServerboundPlayerInputPacket;
    }

    public static boolean isCommandPacket(Packet<?> packet) {
        return packet instanceof ServerboundPlayerCommandPacket;
    }

    public static String packetName(Packet<?> packet) {
        String name = packet.getClass().getSimpleName();
        if (name.startsWith("Serverbound")) name = name.substring(11);
        else if (name.startsWith("Clientbound")) name = name.substring(11);
        if (name.endsWith("Packet")) name = name.substring(0, name.length() - 6);
        return name;
    }

    public static void displayClientMessage(String message) {
        Minecraft mc = mc();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }

    public static boolean isAdMessage(String msg) {
        String lower = msg.toLowerCase();
        if (lower.contains("discord.gg/") || lower.contains("discord.com/invite/")) return true;
        if (lower.contains("discordapp.com/invite/")) return true;
        if (lower.matches(".*\\b(?:buy|sell|cheap|op|dupe|hack|exploit|bypass|donate|raid|free|wins|rank|vouch|trusted)\\b.*")) return true;
        if (lower.contains(".ru/") || lower.endsWith(".ru") || lower.contains(".рф") || lower.contains(".su") || lower.contains(".cfd") || lower.contains(".gdn") || lower.contains(".click") || lower.contains(".top") || lower.contains(".xyz") || lower.contains(".best")) return true;
        if (lower.contains("тг:") || lower.contains("телеграм") || lower.contains("t.me/") || lower.contains("t.me")) return true;
        if (lower.contains("вк.") || lower.contains(".vk.") || lower.contains("vk.com") || lower.contains("vkontakte")) return true;
        if (lower.contains("купить") || lower.contains("продать") || lower.contains("дюп") || lower.contains("дуп") || lower.contains("чита") || lower.contains("хак") || lower.contains("читер") || lower.contains("раздача") || lower.contains("бесплатно") || lower.contains("подарок") || lower.contains("халява")) return true;
        if (lower.contains("заходи") || lower.contains("зайди") || lower.contains("играй")) {
            if (lower.contains("сервер") || lower.contains("сервак") || lower.contains("проект") || lower.contains("топ")) return true;
        }
        if (lower.contains("топ ") && (lower.contains("проект") || lower.contains("сервер") || lower.contains("игра"))) return true;
        if (lower.contains("набор") && (lower.contains("staff") || lower.contains("админ") || lower.contains("модер") || lower.contains("хелп") || lower.contains("helper"))) return true;
        if (lower.contains("донат") || lower.contains("привилеги") || lower.contains("кейс") || lower.contains("case") || lower.contains("бан") || lower.contains("разбан")) return true;
        if (lower.contains("ип:") || lower.contains("ip:") || lower.contains("ip ")) {
            if (lower.contains(".") && (lower.contains("mc") || lower.contains("play") || lower.contains("game") || lower.contains("server"))) return true;
        }
        return false;
    }

    public static String formatComponents(ItemStack stack) {
        StringBuilder sb = new StringBuilder();
        if (stack.isEmpty()) return sb.toString();

        var components = stack.getComponents();
        boolean first = true;
        for (var entry : components) {
            if (!first) sb.append("\n");
            first = false;
            var type = entry.type();
            String typeName = type.toString();
            if (typeName.startsWith("minecraft:")) typeName = typeName.substring(10);
            Object value = entry.value();
            sb.append("§e").append(typeName).append("§7: ");
            if (value == null) {
                sb.append("§7null");
            } else {
                sb.append("§b").append(value.toString());
            }
        }
        return sb.toString();
    }
}
