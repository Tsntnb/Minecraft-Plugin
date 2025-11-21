## Installing the Plugin

1. Stop your Minecraft server
2. Copy the JAR file to your server's `plugins/` folder
3. Start your server
4. Configure the plugin (see Configuration section below)

## Configuration

The plugin needs to be configured with your specific details:

1. RCON connection details:
   - Host: Your server's IP (default: 127.0.0.1)
   - Port: RCON port (default: 25575)
   - Password: Your RCON password

2. Telegram bot details:
   - Bot token: From @BotFather
   - Bot username: Your bot's username

## Usage

Once installed and configured:

1. Use the command `/codeforbot` to generate the monthly access code
2. Share this code with the two users who need access to the Telegram bot
3. The code remains constant throughout the month and changes at the beginning of each new month

## Commands

- `/codeforbot` - Generates the monthly access code for the Telegram bot
  - Permission: `telegramrpg.codeforbot` (default: op)

## Troubleshooting

### Common Issues

1. **Maven not found**: Install Maven using your package manager
   ```bash
   # Ubuntu/Debian
   sudo apt install maven

   # CentOS/RHEL
   sudo yum install maven
   ```

2. **RCON not working**: Ensure RCON is enabled in your server.properties:
   ```
   enable-rcon=true
   rcon.password=your_password
   rcon.port=25575
   ```

3. **Plugin not loading**: Check the server console for error messages and ensure you're running Minecraft 1.20.1

### Verification Steps

1. Check if the plugin loaded successfully in server logs
2. Verify the plugin appears in the `/plugins` command output
3. Test the `/codeforbot` command as an operator

## Development

### Adding Features

The main plugin class is located at:
`src/main/java/com/example/TelegramRPG/TelegramRPG.java`

Key methods to modify:
- `initializeRCON()` - Add RCON functionality
- `initializeTelegramBot()` - Add Telegram bot integration
- `onCommand()` - Add new commands
- `generateMonthlyCode()` - Modify code generation algorithm

### Testing

1. Set up a local Minecraft 1.20.1 server for testing
2. Install the plugin
3. Test all commands and functionality
4. Verify RCON and Telegram integrations work as expected

## Publishing

To publish the plugin to Minecraft communities:

1. Ensure all functionality works as expected
2. Update the version in `pom.xml` and `plugin.yml`
3. Build the final JAR using `mvn clean package`
4. Follow the instructions in `PUBLISHING.md`

## Support

For support with this plugin:
- Check the documentation in README.md
- Review the setup guide (this file)
- Verify your server configuration
- Check server logs for error messages

## Contributing

To contribute to this project:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request with a clear description of your changes
