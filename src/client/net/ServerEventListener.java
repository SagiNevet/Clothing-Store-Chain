package client.net;

import common.protocol.ServerEvent;

public interface ServerEventListener {

    void onServerEvent(ServerEvent event);
}
