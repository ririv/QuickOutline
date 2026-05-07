import type { MessageType, Message } from 'shared-kit/controls/Message.svelte';

class MessageStore {
    list = $state<Message[]>([]);

    add = (text: string, type: MessageType = 'INFO', duration: number = 3000) => {
        const id = Date.now() + Math.random();
        const message: Message = { id, text, type, duration };
        this.list.push(message);
    }

    remove = (id: number) => {
        this.list = this.list.filter(msg => msg.id !== id);
    }
}

export const messageStore = new MessageStore();
