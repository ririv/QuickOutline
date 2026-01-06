<script lang="ts">
    import BaseModal from './common/BaseModal.svelte';

    let { isOpen = $bindable(false), title, blur = false, children }: { isOpen: boolean, title: string, blur?: boolean, children: any } = $props();

    function close() {
        isOpen = false;
    }
</script>

<BaseModal {isOpen} onClose={close} {blur} width="max-w-sm">
    <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100 bg-gray-50/50">
        <h3 class="text-sm font-semibold text-gray-700">{title}</h3>
        <button onclick={close} class="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-md hover:bg-gray-200/50" aria-label="Close">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
    </div>
    <div class="p-5">
        {@render children()}
    </div>
</BaseModal>
