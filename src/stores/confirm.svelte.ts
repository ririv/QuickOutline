// Svelte 5 Runes State for Confirmation Dialog

interface ConfirmOptions {
    title?: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    type?: 'info' | 'warning' | 'error';
}

class ConfirmState {
    isOpen = $state(false);
    title = $state('');
    message = $state('');
    confirmText = $state('Confirm');
    cancelText = $state('Cancel');
    type = $state<'info' | 'warning' | 'error'>('info');
    
    // Promise resolver
    private resolvePromise: ((value: boolean) => void) | null = null;

    request(options: ConfirmOptions): Promise<boolean> {
        this.title = options.title || 'Confirmation';
        this.message = options.message;
        this.confirmText = options.confirmText || 'Confirm';
        this.cancelText = options.cancelText || 'Cancel';
        this.type = options.type || 'info';
        this.isOpen = true;

        return new Promise((resolve) => {
            this.resolvePromise = resolve;
        });
    }

    close(result: boolean) {
        this.isOpen = false;
        if (this.resolvePromise) {
            this.resolvePromise(result);
            this.resolvePromise = null;
        }
    }
}

// Export a singleton instance
export const confirmState = new ConfirmState();

// Export a simplified helper function
export function confirm(
    message: string, 
    title: string = 'Confirm', 
    options: Partial<Omit<ConfirmOptions, 'message' | 'title'>> = {}
) {
    return confirmState.request({ message, title, ...options });
}
