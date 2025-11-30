import { writable } from 'svelte/store';

export type MessageType = 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR';

export interface Message {
    id: number;
    text: string;
    type: MessageType;
    duration?: number; // Duration is now optional and passed to the component
}

const createMessageStore = () => {
    const { subscribe, update } = writable<Message[]>([]);

    function add(text: string, type: MessageType = 'INFO', duration: number = 3000) {
        const id = Date.now() + Math.random(); // Add random number to prevent rare collisions
        const message: Message = { id, text, type, duration };
        update(messages => [...messages, message]);
    }

    function remove(id: number) {
        update(messages => messages.filter(msg => msg.id !== id));
    }

    return {
        subscribe,
        add,
        remove,
    };
}

export const messageStore = createMessageStore();
