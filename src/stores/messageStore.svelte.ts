export type MessageType = 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR';

export interface Message {
    id: number;
    text: string;
    type: MessageType;
    duration?: number;
}

class MessageStore {
    list = $state<Message[]>([]);

    add(text: string, type: MessageType = 'INFO', duration: number = 3000) {
        const id = Date.now() + Math.random();
        const message: Message = { id, text, type, duration };
        this.list.push(message);
    }

    remove(id: number) {
        this.list = this.list.filter(msg => msg.id !== id);
    }
}

export const messageStore = new MessageStore();