export type ConnectionStatus = 'init' | 'connecting' | 'connected' | 'error';

class ConnectionStore {
    serverPort = $state(0);
    connectionStatus = $state<ConnectionStatus>('init');

    setServerPort(port: number) {
        this.serverPort = port;
    }

    setConnectionStatus(status: ConnectionStatus) {
        this.connectionStatus = status;
    }
}

export const connectionStore = new ConnectionStore();
