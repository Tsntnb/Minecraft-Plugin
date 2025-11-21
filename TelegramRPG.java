import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class TelegramRPG extends JavaPlugin {
    private static final Logger logger = Logger.getLogger("Minecraft");
    private String telegramBotToken;
    private String serverAddress;
    private int rconPort;
    private String rconPassword;
    private Socket rconSocket;
    private DataOutputStream rconOut;
    private BufferedReader rconIn;
    private Map<String, Long> authorizedUsers; // Telegram user IDs
    private String monthlyCode;
    private LocalDate currentMonth;
    
    @Override
    public void onEnable() {
        loadConfig();
        initializeMonthlyCode();
        connectToRCON();
        startTelegramBot();
        logger.info("TelegramRPG plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        disconnectRCON();
        logger.info("TelegramRPG plugin disabled!");
    }
    
    private void loadConfig() {
        // Load configuration from plugin config
        saveDefaultConfig();
        telegramBotToken = getConfig().getString("telegram_bot_token", "");
        serverAddress = getConfig().getString("server_address", "localhost");
        rconPort = getConfig().getInt("rcon_port", 25575);
        rconPassword = getConfig().getString("rcon_password", "");
        
        if (telegramBotToken.isEmpty() || rconPassword.isEmpty()) {
            logger.severe("Missing required configuration values! Please check your config.yml");
        }
    }
    
    private void initializeMonthlyCode() {
        LocalDate now = LocalDate.now();
        String monthYear = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // Check if we have a stored code for this month
        File codeFile = new File(getDataFolder(), "monthly_code.txt");
        if (codeFile.exists()) {
            try (Scanner scanner = new Scanner(codeFile)) {
                String[] parts = scanner.nextLine().split(":");
                if (parts.length == 2 && parts[0].equals(monthYear)) {
                    monthlyCode = parts[1];
                    currentMonth = now;
                    return;
                }
            } catch (Exception e) {
                logger.warning("Could not read monthly code file: " + e.getMessage());
            }
        }
        
        // Generate new code for this month
        generateNewMonthlyCode();
    }
    
    private void generateNewMonthlyCode() {
        LocalDate now = LocalDate.now();
        String monthYear = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        currentMonth = now;
        
        // Generate a simple code based on the month (deterministic)
        String baseString = "TELEGRAMRPG" + monthYear;
        int hash = baseString.hashCode();
        monthlyCode = String.valueOf(Math.abs(hash)).substring(0, Math.min(8, String.valueOf(Math.abs(hash)).length())).toUpperCase();
        
        // Save the code for this month
        File codeFile = new File(getDataFolder(), "monthly_code.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(codeFile))) {
            writer.println(monthYear + ":" + monthlyCode);
        } catch (IOException e) {
            logger.warning("Could not save monthly code: " + e.getMessage());
        }
    }
    
    private void connectToRCON() {
        try {
            rconSocket = new Socket(serverAddress, rconPort);
            rconOut = new DataOutputStream(rconSocket.getOutputStream());
            rconIn = new BufferedReader(new InputStreamReader(rconSocket.getInputStream()));
            
            // Authenticate with RCON
            authenticateRCON();
        } catch (Exception e) {
            logger.severe("Failed to connect to RCON: " + e.getMessage());
        }
    }
    
    private void authenticateRCON() {
        try {
            // Send RCON authentication packet
            int requestId = 1;
            String command = "login " + rconPassword;
            
            sendRCONPacket(requestId, 3, command); // 3 = login type
            
            // Read response
            readRCONPacket();
        } catch (Exception e) {
            logger.severe("Failed to authenticate with RCON: " + e.getMessage());
        }
    }
    
    private void sendRCONPacket(int requestId, int type, String command) throws IOException {
        byte[] commandBytes = command.getBytes("UTF-8");
        byte[] typeBytes = intToBytes(type);
        byte[] idBytes = intToBytes(requestId);
        
        int packetSize = 4 + 4 + commandBytes.length + 2; // size, id, type, command, null terminator
        byte[] packet = new byte[packetSize + 4]; // +4 for size field at beginning
        
        // Size field
        byte[] sizeBytes = intToBytes(packetSize);
        System.arraycopy(sizeBytes, 0, packet, 0, 4);
        
        // ID
        System.arraycopy(idBytes, 0, packet, 4, 4);
        
        // Type
        System.arraycopy(typeBytes, 0, packet, 8, 4);
        
        // Command
        System.arraycopy(commandBytes, 0, packet, 12, commandBytes.length);
        
        rconOut.write(packet);
        rconOut.flush();
    }
    
    private byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
        };
    }
    
    private String readRCONPacket() throws IOException {
        // Read packet size (first 4 bytes)
        byte[] sizeBytes = new byte[4];
        rconIn.readFully(sizeBytes);
        int size = bytesToInt(sizeBytes);
        
        // Read the rest of the packet
        byte[] packet = new byte[size];
        rconIn.readFully(packet);
        
        // Extract ID, Type, and Body
        int id = bytesToInt(new byte[]{packet[0], packet[1], packet[2], packet[3]});
        int type = bytesToInt(new byte[]{packet[4], packet[5], packet[6], packet[7]});
        
        // Body is from position 8 to size-2 (excluding null terminator)
        byte[] bodyBytes = new byte[size - 10]; // excluding id(4) + type(4) + null(2)
        System.arraycopy(packet, 8, bodyBytes, 0, bodyBytes.length);
        String body = new String(bodyBytes, "UTF-8");
        
        return body;
    }
    
    private int bytesToInt(byte[] bytes) {
        return (bytes[0] & 0xFF) |
               ((bytes[1] & 0xFF) << 8) |
               ((bytes[2] & 0xFF) << 16) |
               ((bytes[3] & 0xFF) << 24);
    }
    
    private void disconnectRCON() {
        try {
            if (rconOut != null) rconOut.close();
            if (rconIn != null) rconIn.close();
            if (rconSocket != null) rconSocket.close();
        } catch (IOException e) {
            logger.warning("Error closing RCON connection: " + e.getMessage());
        }
    }
    
    private void startTelegramBot() {
        // In a real implementation, you would use a Telegram Bot API library
        // For this example, I'll simulate the bot functionality
        new BukkitRunnable() {
            @Override
            public void run() {
                // This would be where you'd poll for Telegram messages
                // For now, we'll just log that the bot should be running
                logger.info("Telegram bot polling would start here");
            }
        }.runTaskTimerAsynchronously(this, 0L, 20L * 30); // Run every 30 seconds
    }
    
    // This would be called when a message is received from the Telegram bot
    private boolean handleTelegramMessage(String telegramUserId, String message) {
        // Check if user is authorized
        if (!isUserAuthorized(telegramUserId)) {
            return false;
        }
        
        // Process the command through RCON
        try {
            // Send the message as a command to the Minecraft server
            sendRCONPacket(2, 2, message); // 2 = command type
            String response = readRCONPacket();
            // In a real implementation, you would send this response back to the Telegram user
            logger.info("RCON Response: " + response);
            return true;
        } catch (Exception e) {
            logger.severe("Error executing RCON command: " + e.getMessage());
            return false;
        }
    }
    
    private boolean isUserAuthorized(String telegramUserId) {
        // Check if user is in the authorized list
        return authorizedUsers != null && authorizedUsers.containsKey(telegramUserId);
    }
    
    // Command to generate access code for Telegram bot
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("codeforbot")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players in-game.");
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("telegramrpg.codeforbot")) {
                player.sendMessage("You don't have permission to use this command.");
                return true;
            }
            
            player.sendMessage("Your monthly access code for the Telegram bot is: " + monthlyCode);
            player.sendMessage("This code is valid until the end of the month.");
            return true;
        }
        
        return false;
    }
    
    // Method to authorize a user with the code
    public boolean authorizeUserWithCode(String telegramUserId, String code) {
        if (monthlyCode.equals(code)) {
            if (authorizedUsers == null) {
                authorizedUsers = new HashMap<>();
            }
            
            // Only allow 2 users to be authorized
            if (authorizedUsers.size() >= 2) {
                // Check if this user is already authorized
                if (!authorizedUsers.containsKey(telegramUserId)) {
                    return false; // Max users reached
                }
            }
            
            // Add or update user authorization (valid for one month from now)
            authorizedUsers.put(telegramUserId, System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); // 30 days
            return true;
        }
        return false;
    }
    
    // Method to check if an authorized user's access has expired
    private void checkAuthorizationExpiry() {
        if (authorizedUsers == null) return;
        
        long now = System.currentTimeMillis();
        authorizedUsers.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}