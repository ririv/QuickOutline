<script lang="ts">
    import ArrowPopup from "../ArrowPopup.svelte";
    import StyledRadioButtonGroup from '../controls/StyledRadioButtonGroup.svelte';

    type SelectionType = 'bookmark' | 'toc';

    interface Props {
        triggerEl: HTMLElement;
        onSelect: (type: SelectionType) => void;
        selected: SelectionType; // Controlled prop
    }
    let { triggerEl, onSelect, selected }: Props = $props();

    // The value to be bound to the radio group.
    // It's controlled by the parent via the 'selected' prop.
    let value = $state(selected);

    // When the internal value changes (e.g., user clicks a radio button),
    // propagate the change to the parent.
    $effect(() => {
        if (value !== selected) {
             onSelect?.(value);
        }
    });

    // When the parent's prop changes, update the internal value.
    $effect(() => {
        if (selected !== value) {
            value = selected;
        }
    });

    const options = [
        { value: 'bookmark', label: 'From Bookmark' },
        { value: 'toc', label: 'From ToC' }
    ];
</script>

<ArrowPopup placement="top" {triggerEl} padding="15px">
    <div class="flex flex-col items-center gap-2.5">
        <span class="title">Get Contents From</span>
        <StyledRadioButtonGroup {options} bind:value name="get-contents" />
    </div>
</ArrowPopup>

<style>
    /*
      The .title class is still defined in global.css and is not part of this refactoring.
      The rest of the radio button styles have been removed as they are now handled by
      the StyledRadioButtonGroup component and Tailwind CSS.
    */
</style>
