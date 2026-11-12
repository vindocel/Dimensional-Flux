package dimensionalflux.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import dimensionalflux.network.c2s.*;
import dimensionalflux.network.s2c.*;

/**
 * Registers every payload of the mod. Requests from clients are handled here on the server thread; the payloads
 * sent to clients get their handlers on the client, in {@code ClientPayloads}.
 */
public final class DfPayloads {

    /** Raise when a payload changes shape, so older clients cannot join. */
    public static final String VERSION = "1";

    private DfPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(CreateNetworkRequest.TYPE, CreateNetworkRequest.STREAM_CODEC, ServerPayloads::onCreateNetwork);
        registrar.playToServer(DeleteNetworkRequest.TYPE, DeleteNetworkRequest.STREAM_CODEC, ServerPayloads::onDeleteNetwork);
        registrar.playToServer(EditDeviceRequest.TYPE, EditDeviceRequest.STREAM_CODEC, ServerPayloads::onEditDevice);
        registrar.playToServer(LinkDeviceRequest.TYPE, LinkDeviceRequest.STREAM_CODEC, ServerPayloads::onLinkDevice);
        registrar.playToServer(LinkConfiguratorRequest.TYPE, LinkConfiguratorRequest.STREAM_CODEC, ServerPayloads::onLinkConfigurator);
        registrar.playToServer(EditMemberRequest.TYPE, EditMemberRequest.STREAM_CODEC, ServerPayloads::onEditMember);
        registrar.playToServer(EditNetworkRequest.TYPE, EditNetworkRequest.STREAM_CODEC, ServerPayloads::onEditNetwork);
        registrar.playToServer(EditConnectionsRequest.TYPE, EditConnectionsRequest.STREAM_CODEC, ServerPayloads::onEditConnections);
        registrar.playToServer(NetworkViewRequest.TYPE, NetworkViewRequest.STREAM_CODEC, ServerPayloads::onNetworkView);
        registrar.playToServer(WirelessModeRequest.TYPE, WirelessModeRequest.STREAM_CODEC, ServerPayloads::onWirelessMode);
        registrar.playToServer(DisconnectDevicesRequest.TYPE, DisconnectDevicesRequest.STREAM_CODEC, ServerPayloads::onDisconnectDevices);
        registrar.playToServer(ConnectionViewRequest.TYPE, ConnectionViewRequest.STREAM_CODEC, ServerPayloads::onConnectionView);
        registrar.playToServer(TeslaMachinesRequest.TYPE, TeslaMachinesRequest.STREAM_CODEC, ServerPayloads::onTeslaMachines);
        registrar.playToServer(EditMachineRequest.TYPE, EditMachineRequest.STREAM_CODEC, ServerPayloads::onEditMachine);
        registrar.playToServer(NetworkAlertsRequest.TYPE, NetworkAlertsRequest.STREAM_CODEC, ServerPayloads::onNetworkAlerts);
        registrar.playToServer(HudStatisticsRequest.TYPE, HudStatisticsRequest.STREAM_CODEC, ServerPayloads::onHudStatistics);
        registrar.playToServer(TeamSharingRequest.TYPE, TeamSharingRequest.STREAM_CODEC, ServerPayloads::onTeamSharing);

        registrar.playToClient(DeviceGuiSync.TYPE, DeviceGuiSync.STREAM_CODEC);
        registrar.playToClient(DeviceEnergySync.TYPE, DeviceEnergySync.STREAM_CODEC);
        registrar.playToClient(ResponsePayload.TYPE, ResponsePayload.STREAM_CODEC);
        registrar.playToClient(PlayerCapabilityPayload.TYPE, PlayerCapabilityPayload.STREAM_CODEC);
        registrar.playToClient(NetworkSummaryPayload.TYPE, NetworkSummaryPayload.STREAM_CODEC);
        registrar.playToClient(NetworkMembersPayload.TYPE, NetworkMembersPayload.STREAM_CODEC);
        registrar.playToClient(NetworkConnectionsPayload.TYPE, NetworkConnectionsPayload.STREAM_CODEC);
        registrar.playToClient(NetworkStatisticsPayload.TYPE, NetworkStatisticsPayload.STREAM_CODEC);
        registrar.playToClient(NetworkRankingPayload.TYPE, NetworkRankingPayload.STREAM_CODEC);
        registrar.playToClient(NetworkDeletedPayload.TYPE, NetworkDeletedPayload.STREAM_CODEC);
        registrar.playToClient(TeslaMachinesPayload.TYPE, TeslaMachinesPayload.STREAM_CODEC);
    }
}
