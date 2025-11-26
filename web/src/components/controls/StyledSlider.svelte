<script lang="ts">
  interface Props {
    value?: number;
    min?: number;
    max?: number;
    step?: number;
    width?: string;
    disabled?: boolean;
    style?: string; // Explicitly define style prop
    oninput?: (val: number) => void;
    onchange?: (val: number) => void;
    [key: string]: any; // Allow rest props
  }

  let { 
    value = $bindable(0), 
    min = 0, 
    max = 100, 
    step = 1, 
    width = '100%',
    disabled = false,
    oninput,
    onchange,
    ...rest
  }: Props = $props();

  let percent = $derived(Math.min(100, Math.max(0, ((value - min) / (max - min)) * 100)) + '%');

  function handleInput(e: Event) {
      const target = e.target as HTMLInputElement;
      const val = parseFloat(target.value);
      // value is bound, so just notify
      oninput?.(val);
  }
  
  function handleChange(e: Event) {
      const target = e.target as HTMLInputElement;
      const val = parseFloat(target.value);
      // value is bound, so just notify
      onchange?.(val);
  }
</script>

<input 
  {...rest} 
  type="range" 
  {min} {max} {step} {disabled}
  bind:value={value}
  oninput={handleInput}
  onchange={handleChange}
  style="--percent: {percent}; width: {width};"
/>

<style>
  /* --- Ant Design Style Slider --- */
  input[type=range] {
      -webkit-appearance: none; /* Clear default styles */
      height: 4px; /* Track height */
      background: transparent;
      cursor: pointer;
      outline: none;
      margin: 0;
      display: block; /* Ensure width applies correctly */
  }

  /* Track - dynamic background via CSS var --percent */
  input[type=range]::-webkit-slider-runnable-track {
      width: 100%;
      height: 4px;
      border-radius: 2px;
      background: linear-gradient(to right, #1677ff 0%, #1677ff var(--percent), #e5e6eb var(--percent), #e5e6eb 100%);
      transition: background 0.1s;
  }

  /* Thumb */
  input[type=range]::-webkit-slider-thumb {
      -webkit-appearance: none;
      height: 14px;
      width: 14px;
      border-radius: 50%;
      background: #ffffff;
      border: 2px solid #1677ff;
      margin-top: -5px; /* (4 - 14) / 2 */
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      transition: transform 0.1s, box-shadow 0.1s;
  }

  /* Hover/Active states */
  input[type=range]:hover::-webkit-slider-thumb {
      transform: scale(1.2);
      box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.2);
  }
  input[type=range]:active::-webkit-slider-thumb {
      transform: scale(1.2);
      box-shadow: 0 0 0 5px rgba(22, 119, 255, 0.3);
  }
  
  input[type=range]:disabled {
      cursor: not-allowed;
      opacity: 0.6;
  }
  
  input[type=range]:disabled::-webkit-slider-runnable-track {
      background: #f5f5f5;
  }
  
  input[type=range]:disabled::-webkit-slider-thumb {
      border-color: #d9d9d9;
      background-color: #f5f5f5;
  }
</style>
