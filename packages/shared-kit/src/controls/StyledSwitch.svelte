<script lang="ts">
  interface Props {
      checked?: boolean;
      disabled?: boolean;
      size?: 'default' | 'small';
      onchange?: (e: Event) => void;
  }

  let {
      checked = $bindable(false),
      disabled = false,
      size = 'default',
      onchange
  }: Props = $props();

  function handleChange(e: Event) {
      if (disabled) return;
      checked = (e.target as HTMLInputElement).checked;
      if (onchange) onchange(e);
  }
</script>

<label class="switch {size}" class:disabled>
  <input type="checkbox" {checked} {disabled} onchange={handleChange} />
  <span class="slider round"></span>
</label>

<style>
  .switch {
      position: relative;
      display: inline-block;
      width: 36px;
      height: 20px;
  }
  
  .switch.small {
      width: 28px;
      height: 16px;
  }

  .switch input {
      opacity: 0;
      width: 0;
      height: 0;
  }

  .slider {
      position: absolute;
      cursor: pointer;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: #ccc;
      transition: .4s;
      border-radius: 34px;
  }

  .slider:before {
      position: absolute;
      content: "";
      height: 16px;
      width: 16px;
      left: 2px;
      bottom: 2px;
      background-color: white;
      transition: .4s;
      border-radius: 50%;
  }
  
  .switch.small .slider:before {
      height: 12px;
      width: 12px;
      left: 2px;
      bottom: 2px;
  }

  input:checked + .slider {
      background-color: #2196F3;
  }

  input:focus + .slider {
      box-shadow: 0 0 1px #2196F3;
  }

  input:checked + .slider:before {
      transform: translateX(16px);
  }
  
  .switch.small input:checked + .slider:before {
      transform: translateX(12px);
  }

  /* Disabled state */
  .switch.disabled .slider {
      background-color: #e6e6e6;
      cursor: not-allowed;
  }
</style>