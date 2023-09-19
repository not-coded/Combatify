package net.atlas.combatify.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.logging.LogUtils;
import commonnetwork.api.Network;
import io.netty.buffer.Unpooled;
import net.atlas.combatify.Combatify;
import net.atlas.combatify.mixin.ServerGamePacketListenerAccessor;
import net.atlas.combatify.networking.C2SConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

public class ConfigSynchronizer {

    private static final Map<Connection, BiMap<String, SynchableOption<?>>> CLIENT_OPTION_STORAGE = new WeakHashMap<>();
	public static void init() {

	}

    /**
     * Retrieve the options which the given player's client
     * sent to the server during config synchronization
     *
     * @param player     The player for which to retrieve the client values
     * @return The player's client's values of the given config options,
     * or {@code null} if no config with the given name was synced
     */
    public static @Nullable Map<String, SynchableOption<?>> getClientOptions(ServerPlayer player) {
		return CLIENT_OPTION_STORAGE.get(((ServerGamePacketListenerAccessor) player.connection).combatify$getConnection());
    }

    public static void write(FriendlyByteBuf packet, int ordinal) {
        packet.writeVarInt(1);

        var configBuf = new FriendlyByteBuf(Unpooled.buffer());
        var optionBuf = new FriendlyByteBuf(Unpooled.buffer());

        Combatify.CONFIG.options.forEach((key, option) -> {
            if (2 < ordinal) return;

            configBuf.writeUtf(key);

            optionBuf.resetReaderIndex().resetWriterIndex();
            option.writeToBuf(optionBuf);

            configBuf.writeVarInt(optionBuf.readableBytes());
            configBuf.writeBytes(optionBuf);
        });

        packet.writeVarInt(configBuf.readableBytes());
        packet.writeBytes(configBuf);
    }

    public static void read(FriendlyByteBuf buf, BiConsumer<SynchableOption<?>, FriendlyByteBuf> optionConsumer) {
        var config = Combatify.CONFIG;
        int optionCount = buf.readVarInt();
        for (int j = 0; j < optionCount; j++) {
			var name = buf.readUtf();
            var option = config.options.get(name);
            if (option == null) {
                Combatify.LOGGER.error("Received override for unknown option '" + name + "' in config `combatify-config`, skipping");

                // skip size of current option
                buf.skipBytes(buf.readVarInt());
                continue;
            }

            // ignore size
            buf.readVarInt();

            optionConsumer.accept(option, buf);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void applyClient(FriendlyByteBuf buf) {
		Minecraft client = Minecraft.getInstance();
		ClientPacketListener handler = client.getConnection();
        Combatify.LOGGER.info("Applying server overrides");
        var mismatchedOptions = new HashMap<SynchableOption<?>, Object>();

        if (!(client.isSingleplayer() && Objects.requireNonNull(client.getCurrentServer()).isLan())) {
            read(buf, (option, packetByteBuf) -> {
                var mismatchedValue = option.findMismatch(packetByteBuf);
                if (mismatchedValue != null) mismatchedOptions.put(option, mismatchedValue);
            });

            if (!mismatchedOptions.isEmpty()) {
                Combatify.LOGGER.error("Aborting connection, non-syncable config values were mismatched");
                mismatchedOptions.forEach((option, serverValue) -> LogUtils.getLogger().error("- Option {} in config '{}' has value '{}' but server requires '{}'",
						Combatify.CONFIG.options.inverse().get(option), "combatify-config", option.get(), serverValue));
				MutableComponent error = Component.literal("Aborting connection, non-syncable config values were mismatched.\n");
				mismatchedOptions.forEach(((option, o) -> error.append(Component.literal(Combatify.CONFIG.options.inverse().get(option) + " does not match the server's value of " + o + ".\n"))));
                handler.getConnection().disconnect(error);
                return;
            }
        }

        Combatify.LOGGER.info("Responding with client values");
        Network.getNetworkHandler().sendToServer(new C2SConfigPacket());
    }

    public static void applyServer(ServerPlayer player, FriendlyByteBuf buf) {
        Combatify.LOGGER.info("Receiving client config");
        var connection = ((ServerGamePacketListenerAccessor) player.connection).combatify$getConnection();

        read(buf, (option, optionBuf) -> {
            var config = CLIENT_OPTION_STORAGE.computeIfAbsent(connection, $ -> HashBiMap.create());
            config.put(config.inverse().get(option), option.trySync(optionBuf));
        });
    }
}