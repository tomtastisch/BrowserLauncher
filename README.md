
# BrowserLauncher

**BrowserLauncher** is a versatile cross-platform tool designed to simplify the process of launching the default web browser or a specific browser via the command line. Whether you're a developer automating workflows or a system administrator running scripts, BrowserLauncher allows you to programmatically open URLs in various browsers without manual intervention.

## Key Features

### 1. Launch the Default Web Browser
BrowserLauncher makes it easy to open URLs directly in the system’s default browser. This feature is especially useful when writing scripts or batch processes that need to access web resources without requiring additional browser-specific configurations.

**Example Usage:**

```bash
java -jar BrowserLauncher.jar https://example.com
```

This command opens the specified URL in the default browser, whether you’re on Windows, macOS, or Linux.

### 2. Launch a Specific Browser
In situations where you need to test or view content in a specific browser, BrowserLauncher provides an option to choose which browser to use. You can specify browsers like Chrome, Firefox, Safari (on macOS), or Edge. The tool detects and uses the browser installed on your system.

**Example Usage:**

```bash
java -jar BrowserLauncher.jar https://example.com --browser chrome
```

This command specifically opens the URL in Google Chrome.

Supported browser options (depending on your OS) include:
- `chrome`
- `firefox`
- `safari`
- `edge`

### 3. Cross-Platform Compatibility
BrowserLauncher is designed to work seamlessly across multiple platforms:
- **Windows:** Utilizes the default protocol handlers and installed browsers.
- **macOS:** Leverages the `defaultbrowser` utility for better control (see setup steps below).
- **Linux:** Uses the common `xdg-open` tool to handle URL opening, with the ability to specify particular browsers as needed.

### 4. Flexible Configuration
BrowserLauncher is easy to configure and extend, allowing you to adapt it to different environments or add more browser options as needed. It’s lightweight and designed with simplicity in mind, making it an ideal tool for quick scripts, automation workflows, or testing environments.

## Installation and Setup

### macOS Requirements

For macOS, there are a few prerequisites. The package manager [Homebrew](https://brew.sh/) is required to install the necessary utility for managing the default browser.

#### Installing Homebrew

Run the following command in your terminal if Homebrew is not already installed:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### Installing `defaultbrowser`

The `defaultbrowser` utility is necessary for managing the default browser settings on macOS. Install it using Homebrew:

```bash
brew install defaultbrowser
```

### Project Installation

1. Clone the repository:

    ```bash
    git clone https://github.com/tomtastisch/BrowserLauncher.git
    ```

2. Navigate to the project directory:

    ```bash
    cd BrowserLauncher
    ```

### Running the Application

To run BrowserLauncher, ensure that Java (version 8 or higher) is installed. You can then use the following commands:

#### Opening a URL in the Default Browser:

```bash
java -jar BrowserLauncher.jar <URL>
```

#### Opening a URL in a Specific Browser:

```bash
java -jar BrowserLauncher.jar <URL> --browser <BROWSER_NAME>
```

For example:

```bash
java -jar BrowserLauncher.jar https://example.com --browser firefox
```

### Examples and Use Cases

1. **Automation Workflows**: Integrate BrowserLauncher into your scripts to automatically open monitoring dashboards, access web services, or trigger web-based actions.
2. **Cross-Browser Testing**: Easily open the same URL across multiple browsers during web development to ensure consistent behavior.
3. **Custom Browsing Experiences**: Force certain URLs to always open in specific browsers, depending on the context or requirements.

## License

This project is licensed under the **Apache License 2.0**. For more details, see the [LICENSE](LICENSE) file.

## Project Documentation

### Purpose and Benefits

BrowserLauncher is designed to be a straightforward and efficient tool for opening URLs via the command line. It is aimed at developers, system administrators, and anyone who needs to programmatically launch web resources across various platforms. The tool’s flexibility and ease of use make it an ideal choice for a wide range of scenarios:

- **Automated Testing**: Quickly launch URLs in multiple browsers to perform cross-browser testing.
- **Task Automation**: Embed BrowserLauncher in your scripts to automatically open URLs as part of scheduled jobs or monitoring tasks.
- **Web-Based Tools**: Use BrowserLauncher to integrate with internal tools that need to access specific web resources without manual interaction.

### Extensibility

The project is modular and easy to extend. You can easily add support for additional browsers or customize behavior for specific environments. The codebase is designed to be lightweight, making it simple to adapt to various needs without introducing unnecessary complexity.

---

Thank you for using BrowserLauncher! If you have any questions, suggestions, or want to contribute, please feel free to open an issue or submit a pull request.
