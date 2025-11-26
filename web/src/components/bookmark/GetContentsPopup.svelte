<script lang="ts">
    import ArrowPopup from "../ArrowPopup.svelte";

    interface Props {
        triggerEl: HTMLElement;
        onSelect: (type: 'bookmark' | 'toc') => void;
        selected: 'bookmark' | 'toc'; // Controlled prop
    }
    let { triggerEl, onSelect, selected }: Props = $props();

    let currentSelection = $state(selected); // Internal state, controlled by prop

    // Update parent when internal selection changes
    $effect(() => {
        if (currentSelection !== selected) { // Only call onSelect if internal state truly changed
             onSelect?.(currentSelection);
        }
    });

    // Update internal state if parent's selected prop changes
    $effect(() => {
        if (selected !== currentSelection) {
            currentSelection = selected;
        }
    });
</script>

<ArrowPopup placement="top" {triggerEl} padding="15px">
    <div class="popup-content">
        <span class="title">Get Contents From</span>
        <div class="radio-button-group">
            <label class="radio-button-label-wrapper">
                <input type="radio" name="get-contents" value="bookmark" bind:group={currentSelection} />
                <span class="radio-button-label {currentSelection === 'bookmark' ? 'active' : ''}">From Bookmark</span>
            </label>
            <label class="radio-button-label-wrapper">
                <input type="radio" name="get-contents" value="toc" bind:group={currentSelection} />
                <span class="radio-button-label {currentSelection === 'toc' ? 'active' : ''}">From ToC</span>
            </label>
        </div>
    </div>
</ArrowPopup>

<style>
    .popup-content {
        display: flex;
        flex-direction: column;
        gap: 10px;
        align-items: center;
    }
    .radio-button-group {
        display: inline-flex;
        border: 1px solid #d9d9d9;
        border-radius: 6px;
        overflow: hidden;
    }
    .radio-button-group label {
        position: relative;
        flex: 1 1 0%; /* Make buttons equal width, explicitly setting flex-basis to 0 */
        display: flex; /* Make label a flex container */
        justify-content: center; /* Center content within label */
    }
    .radio-button-group label:not(:last-child) .radio-button-label {
        border-right: 1px solid #d9d9d9;
    }
    .radio-button-label {
        display: block;
        padding: 4px 15px;
        cursor: pointer;
        font-size: 13px;
        transition: all 0.2s;
        background-color: #fff;
        color: #606266;
        text-align: center; /* Center the text */
        width: 100%; /* Force span to fill parent label's width */
    }
    .radio-button-label:hover {
        color: var(--el-color-primary);
    }
    .radio-button-label.active {
        background-color: var(--el-color-primary);
        color: white;
    }
    input[type="radio"] {
        position: absolute;
        opacity: 0;
        width: 0;
        height: 0;
    }
    .radio-button-label-wrapper {
        width: 50%; /* Ensure equal width for each label wrapper */
    }
</style>
