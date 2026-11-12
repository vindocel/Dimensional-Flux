package dimensionalflux.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.client.ClientCache;
import dimensionalflux.client.render.TeslaViewportCache;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.NetworkSummary;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.network.s2c.*;

import java.util.ArrayList;
import java.util.List;

/** Handles the payloads the server sends, on the client thread. Client only. */
public final class ClientPayloads {

    private ClientPayloads() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(DeviceGuiSync.TYPE, ClientPayloads::onDeviceGuiSync);
        event.register(DeviceEnergySync.TYPE, ClientPayloads::onDeviceEnergySync);
        event.register(ResponsePayload.TYPE, ClientPayloads::onResponse);
        event.register(PlayerCapabilityPayload.TYPE, ClientPayloads::onPlayerCapability);
        event.register(NetworkSummaryPayload.TYPE, ClientPayloads::onNetworkSummary);
        event.register(NetworkMembersPayload.TYPE, ClientPayloads::onNetworkMembers);
        event.register(NetworkConnectionsPayload.TYPE, ClientPayloads::onNetworkConnections);
        event.register(NetworkStatisticsPayload.TYPE, ClientPayloads::onNetworkStatistics);
        event.register(NetworkRankingPayload.TYPE, ClientPayloads::onNetworkRanking);
        event.register(NetworkDeletedPayload.TYPE, ClientPayloads::onNetworkDeleted);
        event.register(TeslaMachinesPayload.TYPE, ClientPayloads::onTeslaMachines);
    }

    private static LocalPlayer player(IPayloadContext context) {
        return (LocalPlayer) context.player();
    }

    /** Lets the open screen know a request was answered or a view arrived. */
    private static void notifyScreen(LocalPlayer player, int token, int key, int code) {
        if ((token < 0 || player.containerMenu.containerId == token) && player.containerMenu instanceof DimensionalMenu menu
                && menu.mOnResultListener != null) {
            menu.mOnResultListener.onResult(menu, key, code);
        }
    }

    private static void viewArrived(IPayloadContext context) {
        notifyScreen(player(context), -1, DimensionalConstants.REQUEST_UPDATE_NETWORK, 0);
    }

    private static void onDeviceGuiSync(DeviceGuiSync msg, IPayloadContext context) {
        if (player(context).level().getBlockEntity(msg.pos()) instanceof TileDimensionalDevice device) {
            device.applyGuiSync(msg.change(), msg.buffer(), msg.input(), msg.output());
        }
    }

    private static void onDeviceEnergySync(DeviceEnergySync msg, IPayloadContext context) {
        if (player(context).level().getBlockEntity(msg.pos()) instanceof TileDimensionalDevice device) {
            device.getTransferHandler().syncBuffer(msg.energy());
        }
    }

    private static void onResponse(ResponsePayload msg, IPayloadContext context) {
        notifyScreen(player(context), msg.token(), msg.key(), msg.code());
    }

    private static void onPlayerCapability(PlayerCapabilityPayload msg, IPayloadContext context) {
        ClientCache.sSuperAdmin = msg.superAdmin();
    }

    private static void onNetworkSummary(NetworkSummaryPayload msg, IPayloadContext context) {
        for (NetworkSummary summary : msg.networks()) {
            ClientCache.updateNetwork(summary);
        }
        viewArrived(context);
    }

    private static void onNetworkMembers(NetworkMembersPayload msg, IPayloadContext context) {
        ClientCache.updateMembers(msg.networkId(), msg.members());
        viewArrived(context);
    }

    private static void onNetworkConnections(NetworkConnectionsPayload msg, IPayloadContext context) {
        ClientCache.updateConnections(msg.networkId(), msg.connections(), msg.complete());
        if (msg.complete()) {
            viewArrived(context);
        } else {
            notifyScreen(player(context), -1, DimensionalConstants.REQUEST_UPDATE_CONNECTION, 0);
        }
    }

    private static void onNetworkStatistics(NetworkStatisticsPayload msg, IPayloadContext context) {
        ClientCache.updateStatistics(msg.networkId(), msg.statistics());
        viewArrived(context);
    }

    private static void onNetworkRanking(NetworkRankingPayload msg, IPayloadContext context) {
        ClientCache.updateRankings(msg.networkId(), msg.rankings());
        viewArrived(context);
    }

    private static void onNetworkDeleted(NetworkDeletedPayload msg, IPayloadContext context) {
        ClientCache.deleteNetwork(msg.networkId());
        notifyScreen(player(context), -1, DimensionalConstants.REQUEST_DELETE_NETWORK, 0);
    }

    private static void onTeslaMachines(TeslaMachinesPayload msg, IPayloadContext context) {
        List<TeslaViewportCache.Machine> machines = new ArrayList<>(msg.machines().size());
        for (TeslaMachinesPayload.Machine machine : msg.machines()) {
            BlockState state = Block.stateById(machine.stateId());
            if (state.isAir()) {
                continue;
            }
            List<TeslaViewportCache.Part> parts = new ArrayList<>(machine.parts().size());
            for (TeslaMachinesPayload.Part part : machine.parts()) {
                parts.add(new TeslaViewportCache.Part(part.offset(), Block.stateById(part.stateId())));
            }
            machines.add(new TeslaViewportCache.Machine(machine.offset(), state, machine.delivered(), machine.name(),
                    machine.settings(), machine.history(), List.copyOf(parts), machine.look()));
        }
        if (msg.history()) {
            TeslaViewportCache.updateStatistics(msg.broadcaster(), machines);
        } else {
            TeslaViewportCache.update(msg.broadcaster(), machines);
        }
    }
}
