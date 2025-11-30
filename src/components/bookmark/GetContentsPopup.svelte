<script lang="ts">
    import ArrowPopup from "../controls/ArrowPopup.svelte";
    import StyledRadioButtonGroup from '../controls/StyledRadioButtonGroup.svelte';

    type SelectionType = 'bookmark' | 'toc';

    interface Props {
        triggerEl: HTMLElement;
        onSelect: (type: SelectionType) => void;
        selected: SelectionType; // Controlled prop
    }
    let { triggerEl, onSelect, selected }: Props = $props();

    const options = [
        { value: 'bookmark', label: 'Bookmark' },
        { value: 'toc', label: 'ToC' }
    ];

    // Proxy state to handle binding with type casting
    let groupValue = $state<string | null>(selected);

    // Sync Prop -> Local
    $effect(() => {
        if (groupValue !== selected) {
            groupValue = selected;
        }
    });

</script>

<ArrowPopup placement="top" {triggerEl} padding="15px" minWidth="200px">
    <div class="flex flex-col items-center gap-2.5">
        <span class="title">Get Contents From</span>
        <StyledRadioButtonGroup 
            {options} 
            bind:value={groupValue} 
            name="get-contents" 
            allowDeselect={false}
            hasBorder={true}
            onchange={(val) => { if (val) onSelect(val as SelectionType); }}
        />
    </div>
</ArrowPopup>

<style>
    /*
      The .title class is still defined in global.css.
    */
</style>
