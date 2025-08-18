# GEMINI Project: QuickOutline

## Project Overview

QuickOutline is a cross-platform desktop application built with Java and JavaFX that allows users to add, edit, and manage the table of contents (outlines) of PDF files. The application provides a user-friendly interface for manipulating PDF outlines, with features like automatic indentation, page offsets, and integration with Visual Studio Code for advanced text editing. It leverages the iText and Apache PDFBox libraries for PDF manipulation and uses Gradle for build automation, with support for creating native installers for Windows, macOS, and Linux using the jpackage tool.

## Building and Running

### Prerequisites

*   **Java:** Version 21 (LTS)
*   **JavaFX:** Version 21 (LTS)
*   **Gradle:** Version 8.12

### Key Commands

*   **Run the application:**
    ```bash
    ./gradlew run
    ```

*   **Build the application:**
    ```bash
    ./gradlew build
    ```

*   **Create a distributable image:**
    ```bash
    ./gradlew jpackageImage
    ```

*   **Create a native installer:**
    ```bash
    ./gradlew jpackage
    ```
    **Note:** Creating a native installer requires platform-specific tools:
    *   **Windows:** WiX Toolset v3
    *   **macOS:** Xcode and a developer signature
    *   **Linux:** `dpkg` or `rpm`

## Development Conventions

*   **Build System:** The project uses Gradle for dependency management and build automation. The `build.gradle` file defines the project's dependencies, plugins, and build tasks.
*   **Modularity:** The project is modular, as required by the `jlink` packaging tool. The `org.javamodularity.moduleplugin` Gradle plugin is used to handle non-modular dependencies like iText.
*   **User Interface:** The UI is built with JavaFX and FXML. The main application view is defined in `src/main/resources/com/ririv/quickoutline/view/App.fxml`.
*   **Internationalization:** The application supports both English and Chinese, automatically switching based on the user's system language.
*   **Continuous Integration:** The project is set up with GitHub Actions for automated builds and packaging, as defined in the `.github/workflows` directory.
*   **IDE Configuration:** The `.idea` directory contains project files for IntelliJ IDEA, including run configurations and compiler settings.
*   **Code Style:** The source code is consistently formatted, with UTF-8 encoding.
*   **Documentation:** The `README.md` file provides comprehensive user documentation, while the `Dev_doc.md` file offers detailed instructions for developers.
