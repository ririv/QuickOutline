<script lang="ts">
    import Modal from '@/components/Modal.svelte';
    import PageLabelForm from './PageLabelForm.svelte';
    import { pageLabelStore } from '@/stores/pageLabelStore.svelte.js';

    function handleSuccess() {
        pageLabelStore.isFormOpen = false;
    }
</script>

<Modal bind:isOpen={pageLabelStore.isFormOpen} title="Manage Page Label Rule">
    <PageLabelForm onSuccess={handleSuccess} />
</Modal>
