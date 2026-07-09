package ravex.gui.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
<<<<<<< HEAD
import ravex.proxy.Proxy;
=======
import ravex.modules.client.Settings;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.utility.render.FontRenderUtility;

import java.util.List;

public class ProxyConfigScreen extends Screen {

    private final Screen parent;

    private EditBox hostField;
    private EditBox portField;
    private EditBox usernameField;
    private EditBox passwordField;

    private Button toggleBtn;
    private Button saveBtn;

    private String typeLabel = "Type: SOCKS5";
    private int selectedType = 0;
<<<<<<< HEAD
    private final List<String> types = List.of("SOCKS5", "SOCKS4", "HTTP", "SHADOWSOCKS");
=======
    private final List<String> types = List.of("SOCKS5", "SOCKS4", "HTTP");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    private boolean authEnabled = false;
    private String statusMessage = "";
    private int statusTimer = 0;

    public ProxyConfigScreen(Screen parent) {
        super(Component.literal("Proxy Configuration"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        Font font = Minecraft.getInstance().font;
        int cx = width / 2;

<<<<<<< HEAD
        hostField = new EditBox(font, cx - 90, 80, 180, 18, Component.literal("Proxy Host"));
        hostField.setValue(Proxy.getHost());
        hostField.setResponder(Proxy::setHost);

        portField = new EditBox(font, cx - 90, 115, 180, 18, Component.literal("Proxy Port"));
        portField.setValue(String.valueOf(Proxy.getPort()));
        portField.setFilter(val -> val.isEmpty() || val.matches("\\d{0,5}"));
        portField.setResponder(val -> {
            if (!val.isEmpty()) Proxy.setPort(Math.max(1, Math.min(65535, Integer.parseInt(val))));
=======
        Settings s = Settings.INSTANCE;

        hostField = new EditBox(font, cx - 90, 80, 180, 18, Component.literal("Proxy Host"));
        hostField.setValue(s.proxyHost.getValue());
        hostField.setResponder(val -> s.proxyHost.setValue(val));

        portField = new EditBox(font, cx - 90, 115, 180, 18, Component.literal("Proxy Port"));
        portField.setValue(String.valueOf(s.proxyPort.getValue().intValue()));
        portField.setFilter(val -> val.isEmpty() || val.matches("\\d{0,5}"));
        portField.setResponder(val -> {
            if (!val.isEmpty()) {
                int p = Math.max(1, Math.min(65535, Integer.parseInt(val)));
                s.proxyPort.setValue((double) p);
            }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        });
        portField.setMaxLength(5);

        usernameField = new EditBox(font, cx - 90, 165, 180, 18, Component.literal("Username"));
<<<<<<< HEAD
        usernameField.setValue(Proxy.getUsername());
        usernameField.setResponder(Proxy::setUsername);

        passwordField = new EditBox(font, cx - 90, 200, 180, 18, Component.literal("Password"));
        passwordField.setValue(Proxy.getPassword());
        passwordField.setResponder(Proxy::setPassword);
=======
        usernameField.setValue(s.proxyUsername.getValue());
        usernameField.setResponder(val -> s.proxyUsername.setValue(val));

        passwordField = new EditBox(font, cx - 90, 200, 180, 18, Component.literal("Password"));
        passwordField.setValue(s.proxyPassword.getValue());
        passwordField.setResponder(val -> s.proxyPassword.setValue(val));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        passwordField.addFormatter((text, cursorPos) -> {
            StringBuilder masked = new StringBuilder();
            for (int i = 0; i < text.length(); i++) masked.append('*');
            return FormattedCharSequence.forward(masked.toString(), Style.EMPTY);
        });

<<<<<<< HEAD
        selectedType = types.indexOf(Proxy.getType());
        if (selectedType < 0) selectedType = 0;
        typeLabel = "Type: " + types.get(selectedType);

        authEnabled = Proxy.hasAuth();
=======
        selectedType = types.indexOf(s.proxyType.getValue());
        if (selectedType < 0) selectedType = 0;
        typeLabel = "Type: " + types.get(selectedType);

        authEnabled = s.proxyAuth.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

        addRenderableWidget(hostField);
        addRenderableWidget(portField);
        addRenderableWidget(usernameField);
        addRenderableWidget(passwordField);

        toggleBtn = Button.builder(
<<<<<<< HEAD
            Component.literal("Proxy: " + (Proxy.isEnabled() ? "§aON" : "§cOFF")),
            btn -> {
                Proxy.setEnabled(!Proxy.isEnabled());
                btn.setMessage(Component.literal("Proxy: " + (Proxy.isEnabled() ? "§aON" : "§cOFF")));
=======
            Component.literal("Proxy: " + (s.proxyEnabled.getValue() ? "§aON" : "§cOFF")),
            btn -> {
                s.proxyEnabled.setValue(!s.proxyEnabled.getValue());
                btn.setMessage(Component.literal("Proxy: " + (s.proxyEnabled.getValue() ? "§aON" : "§cOFF")));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        ).bounds(cx - 50, 50, 100, 20).build();
        addRenderableWidget(toggleBtn);

        addRenderableWidget(Button.builder(
            Component.literal(typeLabel),
            btn -> {
                selectedType = (selectedType + 1) % types.size();
<<<<<<< HEAD
                Proxy.setType(types.get(selectedType));
=======
                s.proxyType.setValue(types.get(selectedType));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                btn.setMessage(Component.literal("Type: " + types.get(selectedType)));
            }
        ).bounds(cx - 50, 145, 100, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Auth: " + (authEnabled ? "ON" : "OFF")),
            btn -> {
                authEnabled = !authEnabled;
<<<<<<< HEAD
                Proxy.setAuth(authEnabled);
=======
                s.proxyAuth.setValue(authEnabled);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                btn.setMessage(Component.literal("Auth: " + (authEnabled ? "ON" : "OFF")));
            }
        ).bounds(cx - 50, 230, 100, 20).build());

        saveBtn = Button.builder(
            Component.literal("§aSave & Close"),
            btn -> {
                saveSettings();
                minecraft.setScreen(parent);
            }
        ).bounds(cx - 50, height - 40, 100, 20).build();
        addRenderableWidget(saveBtn);
    }

    private void saveSettings() {
<<<<<<< HEAD
        Proxy.setHost(hostField.getValue());
=======
        Settings s = Settings.INSTANCE;
        s.proxyHost.setValue(hostField.getValue());
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        try {
            int p = Integer.parseInt(portField.getValue());
            p = Math.max(1, Math.min(65535, p));
            portField.setValue(String.valueOf(p));
<<<<<<< HEAD
            Proxy.setPort(p);
        } catch (NumberFormatException ignored) {
            portField.setValue("1080");
            Proxy.setPort(1080);
        }
        Proxy.setType(types.get(selectedType));
        Proxy.setAuth(authEnabled);
        Proxy.setUsername(usernameField.getValue());
        Proxy.setPassword(passwordField.getValue());
=======
            s.proxyPort.setValue((double) p);
        } catch (NumberFormatException ignored) {
            portField.setValue("1080");
            s.proxyPort.setValue(1080.0);
        }
        s.proxyType.setValue(types.get(selectedType));
        s.proxyAuth.setValue(authEnabled);
        s.proxyUsername.setValue(usernameField.getValue());
        s.proxyPassword.setValue(passwordField.getValue());
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xEA06060E, 0xEA0C0C1A);

        Font font = Minecraft.getInstance().font;
        int cx = width / 2;

        graphics.fill(0, 0, width, 38, 0xCC08081A);
        graphics.fill(0, 37, width, 38, 0xFFAA3355);
        FontRenderUtility.drawString(graphics, "Proxy Configuration", 18, 10, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "Configure your proxy connection", 18, 23, 0xFF7070A0, false);

        graphics.drawString(font, "Host", cx - 90, 68, 0xFF909090, true);
        graphics.drawString(font, "Port", cx - 90, 103, 0xFF909090, true);

        if (authEnabled) {
            graphics.drawString(font, "Username", cx - 90, 153, 0xFF909090, true);
            graphics.drawString(font, "Password", cx - 90, 188, 0xFF909090, true);
        }

        if (statusTimer > 0) {
            graphics.drawString(font, statusMessage, cx - 90, 260, 0xFFAAFFAA, true);
        }

        graphics.drawString(font, "© RaveX Client", 8, height - 12, 0xFF404060, true);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void removed() {
        saveSettings();
        super.removed();
    }

    @Override
    public void tick() {
        if (statusTimer > 0) statusTimer--;
    }
}
