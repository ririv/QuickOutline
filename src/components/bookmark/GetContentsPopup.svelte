<script lang="ts">
    import ArrowPopup from "../controls/ArrowPopup.svelte";
    import SegmentedControl from '../controls/SegmentedControl.svelte';
    import bookmarkIcon from '@/assets/icons/bookmark.svg';
    import tocIcon from '@/assets/icons/toc.svg';

    type SelectionType = 'bookmark' | 'toc';

    interface Props {
        triggerEl: HTMLElement;
        onSelect: (type: SelectionType) => void;
        selected: SelectionType; // Controlled prop
    }
    let { triggerEl, onSelect, selected }: Props = $props();

    const options = [
        { value: 'bookmark', label: 'Bookmark', icon: bookmarkIcon },
        { value: 'toc', label: 'ToC', icon: tocIcon }
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
        <SegmentedControl 
            options={options} 
            bind:value={groupValue} 
            itemClass="w-[80px]" 
            onchange={(val) => onSelect(val as SelectionType)}
        />
    </div>
</ArrowPopup>

<style>
    .title {
        font-size: 12px;
        font-weight: bold;
        color: #9198a1;
        margin-bottom: 10px;
        display: block;
    }
</style>
