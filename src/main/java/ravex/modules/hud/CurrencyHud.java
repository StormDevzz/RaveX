package ravex.modules.hud;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.render.Hud;
import ravex.parameter.BooleanParameter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
public class CurrencyHud extends Module {
    public static final CurrencyHud INSTANCE = new CurrencyHud();
    public final BooleanParameter btc = new BooleanParameter("BTC/USD", true);
    public final BooleanParameter usd_rub = new BooleanParameter("USD/RUB", true);
    public final BooleanParameter eur_rub = new BooleanParameter("EUR/RUB", true);
    public final BooleanParameter usd_byn = new BooleanParameter("USD/BYN", false);
    public final BooleanParameter usd_kzt = new BooleanParameter("USD/KZT", false);
    public final BooleanParameter usd_uzs = new BooleanParameter("USD/UZS", false);
    public final BooleanParameter usd_amd = new BooleanParameter("USD/AMD", false);
    public final BooleanParameter usd_kgs = new BooleanParameter("USD/KGS", false);
    public final BooleanParameter usd_tjs = new BooleanParameter("USD/TJS", false);
    public final BooleanParameter usd_azn = new BooleanParameter("USD/AZN", false);
    public final BooleanParameter usd_mdl = new BooleanParameter("USD/MDL", false);
    public final BooleanParameter usd_eur = new BooleanParameter("USD/EUR", false);
    public final BooleanParameter usd_gbp = new BooleanParameter("USD/GBP", false);
    public final BooleanParameter usd_cad = new BooleanParameter("USD/CAD", false);
    public final BooleanParameter usd_try = new BooleanParameter("USD/TRY", false);
    public final BooleanParameter usd_pln = new BooleanParameter("USD/PLN", false);
    public final BooleanParameter usd_nok = new BooleanParameter("USD/NOK", false);
    public final BooleanParameter usd_dkk = new BooleanParameter("USD/DKK", false);
    public final BooleanParameter usd_huf = new BooleanParameter("USD/HUF", false);
    public final BooleanParameter usd_czk = new BooleanParameter("USD/CZK", false);
    public final BooleanParameter usd_ron = new BooleanParameter("USD/RON", false);
    private double usdToRub = 89.50;
    private double usdToByn = 3.25;
    private double usdToKzt = 465.20;
    private double usdToUzs = 12620.0;
    private double usdToAmd = 388.0;
    private double usdToKgs = 87.50;
    private double usdToTjs = 10.70;
    private double usdToAzn = 1.70;
    private double usdToMdl = 17.80;
    private double usdToEur = 0.93;
    private double usdToGbp = 0.79;
    private double usdToCad = 1.37;
    private double usdToTry = 32.85;
    private double usdToPln = 4.02;
    private double usdToNok = 10.58;
    private double usdToDkk = 6.95;
    private double usdToHuf = 368.50;
    private double usdToCzk = 23.20;
    private double usdToRon = 4.63;
    private double usdToBtc = 0.0000148; 
    private long lastFetchMs = 0;
    private long lastTickMs = 0;
    private CurrencyHud() {
        super("Currency", 10, 300, 110, 50);
    }
    private static class DisplayPair {
        String label;
        String valStr;
        DisplayPair(String label, String valStr) {
            this.label = label;
            this.valStr = valStr;
        }
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastFetchMs > 600000) {
            lastFetchMs = now;
            fetchRatesAsync();
        }
        if (now - lastTickMs > 2000) {
            lastTickMs = now;
            simulateTicks();
        }
        List<DisplayPair> active = new ArrayList<>();
        if (btc.getValue()) {
            double price = usdToBtc > 0 ? 1.0 / usdToBtc : 67432.0;
            active.add(new DisplayPair("BTC/USD", String.format("$%,.0f", price)));
        }
        if (usd_rub.getValue()) active.add(new DisplayPair("USD/RUB", String.format("%.2f \u20BD", usdToRub)));
        if (eur_rub.getValue()) {
            double eurRub = usdToEur > 0 ? usdToRub / usdToEur : 96.20;
            active.add(new DisplayPair("EUR/RUB", String.format("%.2f \u20BD", eurRub)));
        }
        if (usd_byn.getValue()) active.add(new DisplayPair("USD/BYN", String.format("%.2f Br", usdToByn)));
        if (usd_kzt.getValue()) active.add(new DisplayPair("USD/KZT", String.format("%.1f \u20B8", usdToKzt)));
        if (usd_uzs.getValue()) active.add(new DisplayPair("USD/UZS", String.format("%.0f UZS", usdToUzs)));
        if (usd_amd.getValue()) active.add(new DisplayPair("USD/AMD", String.format("%.1f AMD", usdToAmd)));
        if (usd_kgs.getValue()) active.add(new DisplayPair("USD/KGS", String.format("%.2f KGS", usdToKgs)));
        if (usd_tjs.getValue()) active.add(new DisplayPair("USD/TJS", String.format("%.2f TJS", usdToTjs)));
        if (usd_azn.getValue()) active.add(new DisplayPair("USD/AZN", String.format("%.2f \u20BC", usdToAzn)));
        if (usd_mdl.getValue()) active.add(new DisplayPair("USD/MDL", String.format("%.2f MDL", usdToMdl)));
        if (usd_eur.getValue()) active.add(new DisplayPair("USD/EUR", String.format("%.3f \u20AC", usdToEur)));
        if (usd_gbp.getValue()) active.add(new DisplayPair("USD/GBP", String.format("%.3f \u00A3", usdToGbp)));
        if (usd_cad.getValue()) active.add(new DisplayPair("USD/CAD", String.format("%.3f CA$", usdToCad)));
        if (usd_try.getValue()) active.add(new DisplayPair("USD/TRY", String.format("%.2f \u20BA", usdToTry)));
        if (usd_pln.getValue()) active.add(new DisplayPair("USD/PLN", String.format("%.2f PLN", usdToPln)));
        if (usd_nok.getValue()) active.add(new DisplayPair("USD/NOK", String.format("%.2f NOK", usdToNok)));
        if (usd_dkk.getValue()) active.add(new DisplayPair("USD/DKK", String.format("%.2f DKK", usdToDkk)));
        if (usd_huf.getValue()) active.add(new DisplayPair("USD/HUF", String.format("%.1f HUF", usdToHuf)));
        if (usd_czk.getValue()) active.add(new DisplayPair("USD/CZK", String.format("%.2f CZK", usdToCzk)));
        if (usd_ron.getValue()) active.add(new DisplayPair("USD/RON", String.format("%.2f RON", usdToRon)));
        if (active.isEmpty()) {
            setWidth(80);
            setHeight(14);
            return;
        }
        int pw = 120;
        int rowH = 11;
        int ph = 16 + active.size() * rowH + 4;
        setWidth(pw);
        setHeight(ph);
        int bx = getX(), by = getY();
        ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, ph, ColorUtility.getActiveColor());
        ravex.utility.render.FontRenderUtility.drawString(graphics, "Currency Rates", bx + 8, by + 4, ColorUtility.getActiveColor(), false);
        int cy = by + 16;
        for (DisplayPair dp : active) {
            ravex.utility.render.FontRenderUtility.drawString(graphics, dp.label, bx + 8, cy, 0xFF8080A0, false);
            int valW = ravex.utility.render.FontRenderUtility.getStringWidth(dp.valStr);
            ravex.utility.render.FontRenderUtility.drawString(graphics, dp.valStr, bx + pw - 8 - valW, cy, 0xFFFFFFF0, false);
            cy += rowH;
        }
    }
    private void simulateTicks() {
        double jitter = 1.0 + (Math.random() - 0.5) * 0.0005; 
        usdToRub = Math.max(50.0, Math.min(150.0, usdToRub * jitter));
        usdToByn = Math.max(1.5, Math.min(5.0, usdToByn * jitter));
        usdToKzt = Math.max(300.0, Math.min(600.0, usdToKzt * jitter));
        usdToBtc = Math.max(0.000005, Math.min(0.00005, usdToBtc * (1.0 + (Math.random() - 0.5) * 0.001))); 
    }
    private void fetchRatesAsync() {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.exchangerate-api.com/v4/latest/USD");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        JsonObject obj = JsonParser.parseString(sb.toString()).getAsJsonObject();
                        if (obj.has("rates")) {
                            JsonObject rates = obj.getAsJsonObject("rates");
                            if (rates.has("RUB")) usdToRub = rates.get("RUB").getAsDouble();
                            if (rates.has("BYN")) usdToByn = rates.get("BYN").getAsDouble();
                            if (rates.has("KZT")) usdToKzt = rates.get("KZT").getAsDouble();
                            if (rates.has("UZS")) usdToUzs = rates.get("UZS").getAsDouble();
                            if (rates.has("AMD")) usdToAmd = rates.get("AMD").getAsDouble();
                            if (rates.has("KGS")) usdToKgs = rates.get("KGS").getAsDouble();
                            if (rates.has("TJS")) usdToTjs = rates.get("TJS").getAsDouble();
                            if (rates.has("AZN")) usdToAzn = rates.get("AZN").getAsDouble();
                            if (rates.has("MDL")) usdToMdl = rates.get("MDL").getAsDouble();
                            if (rates.has("EUR")) usdToEur = rates.get("EUR").getAsDouble();
                            if (rates.has("GBP")) usdToGbp = rates.get("GBP").getAsDouble();
                            if (rates.has("CAD")) usdToCad = rates.get("CAD").getAsDouble();
                            if (rates.has("TRY")) usdToTry = rates.get("TRY").getAsDouble();
                            if (rates.has("PLN")) usdToPln = rates.get("PLN").getAsDouble();
                            if (rates.has("NOK")) usdToNok = rates.get("NOK").getAsDouble();
                            if (rates.has("DKK")) usdToDkk = rates.get("DKK").getAsDouble();
                            if (rates.has("HUF")) usdToHuf = rates.get("HUF").getAsDouble();
                            if (rates.has("CZK")) usdToCzk = rates.get("CZK").getAsDouble();
                            if (rates.has("RON")) usdToRon = rates.get("RON").getAsDouble();
                            if (rates.has("BTC")) usdToBtc = rates.get("BTC").getAsDouble();
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }).start();
    }
}
