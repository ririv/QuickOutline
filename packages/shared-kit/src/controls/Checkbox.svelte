<script lang="ts">
  interface Props {
    checked?: boolean;
    disabled?: boolean;
    indeterminate?: boolean;
    onchange?: (checked: boolean) => void;
    class?: string;
  }

  let { 
    checked = $bindable(false), 
    disabled = false, 
    indeterminate = false,
    onchange,
    class: className = ''
  }: Props = $props();

  function handleChange(e: Event) {
    const target = e.target as HTMLInputElement;
    checked = target.checked;
    onchange?.(checked);
  }

  /* 使用 $effect 处理 indeterminate 状态，因为它只能通过 JS 设置 */
  $effect(() => {
    if (inputRef) {
      inputRef.indeterminate = indeterminate;
    }
  });

  let inputRef: HTMLInputElement | undefined = $state();
</script>

<input 
  bind:this={inputRef}
  type="checkbox"
  {checked}
  {disabled}
  class="
    appearance-none w-5 h-5 rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800
    cursor-pointer flex-shrink-0 transition-colors duration-200
    checked:bg-violet-500 checked:border-violet-500 
    checked:bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIzIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiPjxwb2x5bGluZSBwb2ludHM9IjIwIDYgOSAxNyA0IDEyIi8+PC9zdmc+')]
    checked:bg-[length:70%] checked:bg-center checked:bg-no-repeat
    focus:outline-none
    disabled:opacity-50 disabled:cursor-not-allowed
    indeterminate:bg-violet-500 indeterminate:border-violet-500
    indeterminate:bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIzIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiPjxsaW5lIHgxPSI1IiB5MT0iMTIiIHgyPSIxOSIgeTI9IjEyIi8+PC9zdmc+')]
    {className}
  "
  onchange={handleChange}
/>
