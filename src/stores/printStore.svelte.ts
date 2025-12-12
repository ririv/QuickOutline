export class PrintState {
    mode = $state<"Native" | "Headless" | "HeadlessChrome">("Native");

    setMode(mode: "Native" | "Headless" | "HeadlessChrome") {
        this.mode = mode;
    }
}

export const printStore = new PrintState();
