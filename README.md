Natürlich! Hier ist die README im englischen Stil:

---

# BrowserLauncher

**BrowserLauncher** is a lightweight Python tool that allows you to open websites in specific browsers directly from the command line. It's particularly useful for web developers, testers, and anyone who frequently needs to switch between different browsers.

## Features

- Open websites in a specified browser
- Supports popular browsers like Chrome, Firefox, Edge, Safari, and more
- Cross-platform: Works on Windows, macOS, and Linux
- Simple CLI tool for quick and efficient usage

## Installation

1. Clone the repository:
    ```bash
    git clone https://github.com/tomtastisch/BrowserLauncher.git
    ```
2. Navigate into the project directory:
    ```bash
    cd BrowserLauncher
    ```
3. Install the required dependencies:
    ```bash
    pip install -r requirements.txt
    ```

## Usage

BrowserLauncher can be run directly from the command line. You can specify a URL and choose which browser to open it in.

### Example Commands

- Open a website in Chrome:
    ```bash
    python launcher.py --url https://example.com --browser chrome
    ```

- Open a website in Firefox:
    ```bash
    python launcher.py --url https://example.com --browser firefox
    ```

- Display available options:
    ```bash
    python launcher.py --help
    ```

## Supported Browsers

Currently, **BrowserLauncher** supports the following browsers:

- Google Chrome
- Mozilla Firefox
- Microsoft Edge
- Safari (macOS only)

If your preferred browser isn't listed, you can easily add it to the `browsers.json` configuration file.

## Configuration

The available browsers are configured in the `browsers.json` file. You can adjust the paths to the executable files if they differ from the default locations.

## Roadmap

- Support for opening multiple URLs simultaneously
- Integration with testing frameworks for automated browser testing
- Addition of a simple graphical user interface (GUI)
- Advanced profiles and configuration options

## Contributing

Contributions are welcome! If you find a bug or have an idea for a new feature, feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

This version follows the typical structure and style of a README file commonly found on GitHub projects. Let me know if you need any more adjustments!
