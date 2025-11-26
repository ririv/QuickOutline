<script lang="ts">
    import ArrowPopup from "../ArrowPopup.svelte";

    interface Props {
        triggerEl: HTMLElement;
        onSelect: (type: 'bookmark' | 'toc') => void;
    }
    let { triggerEl, onSelect }: Props = $props();

    let selected = $state('bookmark');

    function handleSelect(type: 'bookmark' | 'toc') {
        selected = type;
        onSelect(type);
    }

</script>

<ArrowPopup placement="top" {triggerEl} padding="15px">
    <div class="popup-content">
        <span class="title">Get Contents From</span>
        <div class="radio-group">
            <label class:selected={selected === 'bookmark'}>
                <input type="radio" name="get-contents" value="bookmark" bind:group={selected} onchange={() => handleSelect('bookmark')} />
                From Bookmark
            </label>
            <label class:selected={selected === 'toc'}>
                <input type="radio" name="get-contents" value="toc" bind:group={selected} onchange={() => handleSelect('toc')} />
                From ToC
            </label>
        </div>
    </div>
</ArrowPopup>

<style>
    .popup-content {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }
    .radio-group {
        display: flex;
        gap: 10px;
        background-color: #f5f7fa;
        border-radius: 6px;
        padding: 4px;
    }
    .radio-group label {
        flex: 1;
        padding: 4px 10px;
        border-radius: 4px;
        cursor: pointer;
        text-align: center;
        font-size: 13px;
        transition: all 0.2s;
        border: 1px solid transparent;
    }
    .radio-group label.selected {
        background-color: white;
        border-color: #dcdfe6;
        box-shadow: 0 1px 2px rgba(0,0,0,0.05);
        font-weight: 500;
    }
    input[type="radio"] {
        display: none;
    }
</style>
