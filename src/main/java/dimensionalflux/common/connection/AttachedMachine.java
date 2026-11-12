package dimensionalflux.common.connection;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * A machine a device powers or takes energy from: the machine a Receiver or Emitter is mounted on, or each machine a
 * Tesla Broadcaster powers. The connections list shows these machines in place of the devices.
 *
 * @param item       the item of the block that stands for the machine, for its icon
 * @param nameKey    the translation key of that block, which the client shows in its own language
 * @param pos        where the machine is
 * @param change     the energy the machine received (negative) or gave (positive) in the last cycle
 * @param customName the name the player gave the machine, or an empty string
 * @param settings   the transfer settings of a machine that has its own (a machine of a Tesla Broadcaster)
 */
public record AttachedMachine(Identifier item, String nameKey, BlockPos pos, long change, String customName,
                              Optional<TransferSettings> settings) {

    public static final StreamCodec<ByteBuf, AttachedMachine> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, AttachedMachine::item,
            ByteBufCodecs.STRING_UTF8, AttachedMachine::nameKey,
            BlockPos.STREAM_CODEC, AttachedMachine::pos,
            ByteBufCodecs.LONG, AttachedMachine::change,
            ByteBufCodecs.STRING_UTF8, AttachedMachine::customName,
            ByteBufCodecs.optional(TransferSettings.STREAM_CODEC), AttachedMachine::settings,
            AttachedMachine::new);

    /** The machine a block state stands for, without its own settings. */
    public static AttachedMachine of(BlockState state, BlockPos pos, long change, String customName) {
        return new AttachedMachine(BuiltInRegistries.ITEM.getKey(state.getBlock().asItem()),
                dimensionalflux.common.integration.MinerCompat.nameKey(state), pos.immutable(), change, customName,
                Optional.empty());
    }

    public AttachedMachine withSettings(TransferSettings settings) {
        return new AttachedMachine(item, nameKey, pos, change, customName, Optional.of(settings));
    }
}
